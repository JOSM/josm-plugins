// License: GPL. For details, see LICENSE file.
package reverter;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Test class for {@link ChangesetReverter}
 */
@BasicPreferences
@HTTP
@Main
@Projection
class ChangesetReverterTest {
    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance().options(
            wireMockConfig().dynamicPort().dynamicHttpsPort()
                    .extensions(new MultiplePrimitiveTransformer(), new PrimitiveTransformer()))
            .build();

    /**
     * Non-regression test for #22520: IllegalStateException: Missing merge target for node
     */
    @Test
    void testTicket22520(WireMockRuntimeInfo wireMockRuntimeInfo) throws ExecutionException, InterruptedException {
        wireMockRuntimeInfo.getWireMock().loadMappingsFrom(TestUtils.getRegressionDataDir(22520));
        MultiplePrimitiveTransformer.register(wireMockRuntimeInfo.getWireMock(), 22520);
        Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl());
        PrimitiveId building = new SimplePrimitiveId(233056719, OsmPrimitiveType.WAY);
        new DownloadOsmTask().loadUrl(new DownloadParams().withLayerName("testTicket22520"),
                wireMockRuntimeInfo.getHttpBaseUrl() + "/0.6/way/233056719/3", NullProgressMonitor.INSTANCE).get();
        OsmDataLayer layer = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).stream()
                .filter(l -> "testTicket22520".equals(l.getName())).findFirst().orElse(null);
        assertNotNull(layer);
        layer.getDataSet().setSelected(building);
        RevertChangesetTask task = new RevertChangesetTask(NullProgressMonitor.INSTANCE, Collections.singletonList(36536612),
                ChangesetReverter.RevertType.SELECTION_WITH_UNDELETE, true, false);
        assertDoesNotThrow(task::realRun);
        GuiHelper.runInEDTAndWait(() -> { /* Sync threads */ });
        assertTrue(UndoRedoHandler.getInstance().hasUndoCommands());
    }

    /**
     * Non-regression test for #23582: Nodes that were not modified by a changeset should not be reverted.
     * Note: This might not be the intended behavior, but it was the behavior prior to r36230.
     */
    @Test
    void testTicket23582(WireMockRuntimeInfo wireMockRuntimeInfo) {
        wireMockRuntimeInfo.getWireMock().loadMappingsFrom(TestUtils.getRegressionDataDir(23582));
        MultiplePrimitiveTransformer.register(wireMockRuntimeInfo.getWireMock(), 23582);
        Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl());
        final RevertChangesetTask task = new RevertChangesetTask(149181932, ChangesetReverter.RevertType.FULL, true, true);
        task.run();
        GuiHelper.runInEDTAndWait(() -> { /* Sync UI thread (some actions are taken on this thread which modify primitives) */ });
        final DataSet reverted = MainApplication.getLayerManager().getEditDataSet();
        assertEquals(1, reverted.allModifiedPrimitives().size());
        final Way oldWay = assertInstanceOf(Way.class, reverted.allModifiedPrimitives().iterator().next());
        assertEquals(8, oldWay.getNodesCount());
    }

    @Test
    void testTicket23584(WireMockRuntimeInfo wireMockRuntimeInfo) {
        new JOptionPaneSimpleMocker(Collections.singletonMap("Conflicts detected", JOptionPane.OK_OPTION));
        wireMockRuntimeInfo.getWireMock().loadMappingsFrom(TestUtils.getRegressionDataDir(23584));
        MultiplePrimitiveTransformer.register(wireMockRuntimeInfo.getWireMock(), 23584);
        PrimitiveTransformer.register(wireMockRuntimeInfo.getWireMock(), 23584);
        Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl());
        final RevertChangesetTask task = new RevertChangesetTask(NullProgressMonitor.INSTANCE, Collections.singleton(97259223),
                ChangesetReverter.RevertType.FULL, true, true);
        assertDoesNotThrow(task::realRun);
        GuiHelper.runInEDTAndWait(() -> { /* Sync UI thread (some actions are taken on this thread which modify primitives) */ });
        final DataSet reverted = MainApplication.getLayerManager().getEditDataSet();
        final Collection<OsmPrimitive> modified = reverted.allModifiedPrimitives();
        assertAll(() -> assertEquals(58, modified.size()),
                () -> assertEquals(18, modified.stream().filter(Relation.class::isInstance).count()),
                () -> assertEquals(24, modified.stream().filter(Way.class::isInstance).count()),
                () -> assertEquals(16, modified.stream().filter(Node.class::isInstance).count()));
    }

    /**
     * A transformer for `/node/:id/:version`, `way/:id/:version`, and `relation/:id/:version` that will use
     * the same data as {@link MultiplePrimitiveTransformer}.
     */
    private static class PrimitiveTransformer extends ResponseTransformer {
        /**
         * Register the URLs for this transformer
         * @param wireMock The wiremock object to register stubs for
         * @param ticket The ticket to get data from
         */
        static void register(WireMock wireMock, int ticket) {
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/node/\\d+/?\\d*")).willReturn(WireMock.aResponse()
                    .withTransformer("PrimitiveTransformer", "ticket", ticket)));
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/way/\\d+/?\\d*")).willReturn(WireMock.aResponse()
                    .withTransformer("PrimitiveTransformer", "ticket", ticket)));
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/relation/\\d+/?\\d*")).willReturn(WireMock.aResponse()
                    .withTransformer("PrimitiveTransformer", "ticket", ticket)));
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public String getName() {
            return "PrimitiveTransformer";
        }

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            final int ticket = parameters.getInt("ticket");
            final String[] parts = request.getUrl().substring(1).split("/", -1);
            final int version = Integer.parseInt(parts[3]);
            final long id = Long.parseLong(parts[2]);
            final OsmParameterInformation info;
            switch (parts[1]) {
                case "node":
                    info = new OsmParameterInformation(OsmPrimitiveType.NODE, id, version);
                    break;
                case "way":
                    info = new OsmParameterInformation(OsmPrimitiveType.WAY, id, version);
                    break;
                case "relation":
                    info = new OsmParameterInformation(OsmPrimitiveType.RELATION, id, version);
                    break;
                default:
                    IllegalArgumentException e = new IllegalArgumentException("No OSM primitive path present");
                    Logging.error(e); // Log first since wiremock is really bad about propagating and logging exceptions.
                    throw e;
            }
            final String body = MultiplePrimitiveTransformer.getReturnXml(ticket, info.type, Collections.singleton(info));
            return Response.Builder.like(response).but().body(body).build();
        }
    }

    /**
     * A transformer for the /nodes?node, /ways?ways, and /relations?relations endpoints.
     * This is needed since we don't always do the requests in the same order.
     */
    private static class MultiplePrimitiveTransformer extends ResponseTransformer {

        /**
         * Register the URLs for this transformer
         * @param wireMock The wiremock object to register stubs for
         * @param ticket The ticket to get data from
         */
        static void register(WireMock wireMock, int ticket) {
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/nodes")).willReturn(WireMock.aResponse()
                    .withTransformer("MultiplePrimitiveTransformer", "ticket", ticket)));
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/relations")).willReturn(WireMock.aResponse()
                    .withTransformer("MultiplePrimitiveTransformer", "ticket", ticket)));
            wireMock.register(WireMock.get(WireMock.urlPathMatching("/0.6/ways")).willReturn(WireMock.aResponse()
                    .withTransformer("MultiplePrimitiveTransformer", "ticket", ticket)));
        }

        @Override
        public String getName() {
            return "MultiplePrimitiveTransformer";
        }

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            final int ticket = parameters.getInt("ticket");
            final QueryParameter wayParam = request.queryParameter("ways");
            final QueryParameter nodeParam = request.queryParameter("nodes");
            final QueryParameter relParam = request.queryParameter("relations");
            if (wayParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(ticket, OsmPrimitiveType.WAY, wayParam)).build();
            } else if (nodeParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(ticket, OsmPrimitiveType.NODE, nodeParam)).build();
            } else if (relParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(ticket, OsmPrimitiveType.RELATION, relParam)).build();
            } else {
                IllegalArgumentException e = new IllegalArgumentException("No query parameter present");
                Logging.error(e); // Log first since wiremock is really bad about propagating and logging exceptions.
                throw e;
            }
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        private static String getReturnXml(int ticket, OsmPrimitiveType type, QueryParameter parameter) {
            final Set<OsmParameterInformation> objectIds = (parameter.isSingleValued()
                    ? Arrays.asList(parameter.values().get(0).split(",", -1))
                    : parameter.values())
                    .stream().map(s -> new OsmParameterInformation(type, s)).collect(Collectors.toSet());
            return getReturnXml(ticket, type, objectIds);
        }

        private static String getReturnXml(int ticket, OsmPrimitiveType type, Collection<OsmParameterInformation> objectIds) {
            final String file;
            switch (type) {
                case NODE:
                    file = "nodes.json";
                    break;
                case WAY:
                    file = "ways.json";
                    break;
                case RELATION:
                    file = "relations.json";
                    break;
                default:
                    throw new IllegalArgumentException("No file for type " + type);
            }
            return getObjectInformation(ticket, file, objectIds);
        }

        private static String getObjectInformation(int ticket, String file, Collection<OsmParameterInformation> objectIds) {
            try (InputStream fis = Files.newInputStream(Paths.get(TestUtils.getRegressionDataDir(ticket), file));
                 JsonReader reader = Json.createReader(fis)) {
                String version = "0.6";
                String generator = "MultiplePrimitiveTransformer";
                String copyright = "OpenStreetMap";
                String attribution = "OpenStreetMap";
                String license = "odbl";
                JsonArray readerElements = reader.readObject().getJsonArray("elements");
                Map<OsmParameterInformation, JsonObject> matchingElements = new HashMap<>();
                for (JsonObject element : readerElements.getValuesAs(JsonObject.class)) {
                    OsmParameterInformation i = getId(element);
                    if (objectIds.contains(i)) {
                        matchingElements.put(i, element);
                    } else {
                        Optional<OsmParameterInformation> info = objectIds.stream()
                                .filter(o -> !o.isVersioned() && o.type() == i.type() && o.id() == i.id()).findFirst();
                        if (info.isPresent() && (matchingElements.get(info.get()) == null
                                || matchingElements.get(info.get()).getInt("version") < i.version())) {
                            Optional<OsmParameterInformation> previous = matchingElements.keySet().stream()
                                    .filter(o -> o.type() == i.type() && o.id() == i.id()).findFirst();
                            if (previous.isPresent() && previous.get().version() < i.version()) {
                                matchingElements.remove(previous.get());
                                matchingElements.put(i, element);
                            } else if (!previous.isPresent()) {
                                matchingElements.put(i, element);
                            }
                        }
                    }
                }
                StringBuilder sb = new StringBuilder("<osm version=\"").append(version).append("\" generator=\"")
                        .append(generator).append("\" copyright=\"").append(copyright).append("\" attribution=\"")
                        .append(attribution).append("\" license=\"").append(license).append("\">");
                for (JsonObject element : matchingElements.values()) {
                    sb.append(elementToString(element));
                }
                sb.append("</osm>");
                return sb.toString();
            } catch (IOException e) {
                Logging.error(e);
                throw new JosmRuntimeException(e);
            }
        }

        private static String elementToString(JsonObject object) {
            long id = object.getJsonNumber("id").longValue();
            int version = object.getInt("version");
            int changeset = object.getInt("changeset");
            String timestamp = object.getString("timestamp");
            String user = object.getString("user");
            long uid = object.getJsonNumber("uid").longValue();
            boolean visible = object.getBoolean("visible", true);
            String type = object.getString("type");
            StringBuilder sb = new StringBuilder("<").append(type).append(" id=\"").append(id).append("\" visible=\"")
                    .append(visible).append("\" version=\"").append(version).append("\" changeset=\"").append(changeset)
                    .append("\" timestamp=\"").append(timestamp).append("\" user=\"").append(user).append("\" uid=\"")
                    .append(uid);
            if (visible && "node".equals(type)) {
                sb.append("\" lat=\"").append(object.getJsonNumber("lat").doubleValue()).append("\" lon=\"")
                        .append(object.getJsonNumber("lon").doubleValue());
            }
            sb.append("\">");
            if (visible && "way".equals(type)) {
                for (JsonNumber nodeId : object.getJsonArray("nodes").getValuesAs(JsonNumber.class)) {
                    sb.append("<nd ref=\"").append(nodeId.longValue()).append("\"/>");
                }
            } else if (visible && "relation".equals(type)) {
                for (JsonObject member : object.getJsonArray("members").asJsonArray().getValuesAs(JsonObject.class)) {
                    sb.append("<member type=\"").append(member.getString("type")).append("\" ref=\"")
                            .append(member.getJsonNumber("ref").longValue()).append("\" role=\"")
                            .append(member.getString("role")).append("\"/>");
                }
            }
            if (object.containsKey("tags")) {
                for (Map.Entry<String, JsonValue> tag : object.getJsonObject("tags").entrySet()) {
                    sb.append("<tag k=\"").append(tag.getKey()).append("\" v=\"")
                            .append(((JsonString) tag.getValue()).getString().replace("\"", "&quot;"))
                            .append("\"/>");
                }
            }
            sb.append("</").append(type).append(">");
            return sb.toString();
        }

        private static OsmParameterInformation getId(JsonObject object) {
            final long id = object.getJsonNumber("id").longValue();
            final int version = object.getInt("version");
            switch(object.getString("type")) {
                case "node":
                    return new OsmParameterInformation(OsmPrimitiveType.NODE, id, version);
                case "way":
                    return new OsmParameterInformation(OsmPrimitiveType.WAY, id, version);
                case "relation":
                    return new OsmParameterInformation(OsmPrimitiveType.RELATION, id, version);
                default:
                    throw new IllegalArgumentException("Unknown type: " + object.getString("type"));
            }
        }
    }

    /**
     * A record class for parameter information.
     */
    private static final class OsmParameterInformation {
        private final OsmPrimitiveType type;
        private final long id;
        private final int version;

        /**
         * Create a new record
         * @param type The type
         * @param param The URl parameter bit (can be something like {@code "123v1"})
         */
        OsmParameterInformation(OsmPrimitiveType type, String param) {
            this(type, Long.parseLong(param.split("v")[0]),
                    param.split("v").length == 2 ? Integer.parseInt(param.split("v")[1]) : Integer.MIN_VALUE);
        }

        /**
         * Create a new record
         * @param type The type
         * @param id The primitive id
         * @param version The primitive version. Anything less than or equal to 0 means latest.`
         */
        OsmParameterInformation(OsmPrimitiveType type, long id, int version) {
            this.type = type;
            this.id = id;
            this.version = Math.max(version, 0);
        }

        public OsmPrimitiveType type() {
            return this.type;
        }

        public long id() {
            return this.id;
        }

        public int version() {
            return this.version;
        }

        /**
         * Check if this is versioned
         * @return {@code true} if we are looking for a specific version instead of the latest.
         */
        public boolean isVersioned() {
            return this.version > 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.id, this.version);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof OsmParameterInformation) {
                OsmParameterInformation other = (OsmParameterInformation) obj;
                return this.type == other.type &&
                        this.id == other.id &&
                        this.version == other.version;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[type=\"" + this.type + "\" id=\"" + this.id + "\" version=\"" + this.version +"\"]";
        }
    }
}
