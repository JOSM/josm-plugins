// License: GPL. For details, see LICENSE file.
package reverter;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Test class for {@link ChangesetReverter}
 */
@BasicPreferences
@HTTP
class ChangesetReverterTest {
    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort().extensions(new MultiplePrimitiveTransformer()))
            .build();

    /**
     * Non-regression test for #22520: IllegalStateException: Missing merge target for node
     */
    @Test
    void testTicket22520(WireMockRuntimeInfo wireMockRuntimeInfo) throws ExecutionException, InterruptedException {
        wireMockRuntimeInfo.getWireMock().loadMappingsFrom(TestUtils.getRegressionDataDir(22520));
        wireMockRuntimeInfo.getWireMock().register(WireMock.get(WireMock.urlPathMatching("/0.6/nodes")).willReturn(WireMock.aResponse().withTransformers("MultiplePrimitiveTransformer")));
        wireMockRuntimeInfo.getWireMock().register(WireMock.get(WireMock.urlPathMatching("/0.6/relations")).willReturn(WireMock.aResponse().withTransformers("MultiplePrimitiveTransformer")));
        wireMockRuntimeInfo.getWireMock().register(WireMock.get(WireMock.urlPathMatching("/0.6/ways")).willReturn(WireMock.aResponse().withTransformers("MultiplePrimitiveTransformer")));
        Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl());
        PrimitiveId building = new SimplePrimitiveId(233056719, OsmPrimitiveType.WAY);
        new DownloadOsmTask().loadUrl(new DownloadParams().withLayerName("testTicket22520"), wireMockRuntimeInfo.getHttpBaseUrl() + "/0.6/way/233056719/3", NullProgressMonitor.INSTANCE).get();
        OsmDataLayer layer = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).stream().filter(l -> "testTicket22520".equals(l.getName())).findFirst().orElse(null);
        assertNotNull(layer);
        layer.getDataSet().setSelected(building);
        RevertChangesetTask task = new RevertChangesetTask(NullProgressMonitor.INSTANCE, Collections.singletonList(36536612), ChangesetReverter.RevertType.SELECTION_WITH_UNDELETE, true, false);
        assertDoesNotThrow(task::realRun);
        GuiHelper.runInEDTAndWait(() -> { /* Sync threads */ });
        assertTrue(UndoRedoHandler.getInstance().hasUndoCommands());
    }

    /**
     * A transformer for the /nodes?node, /ways?ways, and /relations?relations endpoints. This is needed since we don't always do the requests in the same order.
     */
    private static class MultiplePrimitiveTransformer extends ResponseTransformer {
        @Override
        public String getName() {
            return "MultiplePrimitiveTransformer";
        }

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            final QueryParameter wayParam = request.queryParameter("ways");
            final QueryParameter nodeParam = request.queryParameter("nodes");
            final QueryParameter relParam = request.queryParameter("relations");
            if (wayParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(OsmPrimitiveType.WAY, wayParam)).build();
            } else if (nodeParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(OsmPrimitiveType.NODE, nodeParam)).build();
            } else if (relParam.isPresent()) {
                return Response.Builder.like(response).but().body(getReturnXml(OsmPrimitiveType.RELATION, relParam)).build();
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

        private static String getReturnXml(OsmPrimitiveType type, QueryParameter parameter) {
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
            final Set<OsmParameterInformation> objectIds = (parameter.isSingleValued()
                    ? Arrays.asList(parameter.values().get(0).split(",", -1))
                    : parameter.values())
                    .stream().map(s -> new OsmParameterInformation(type, s)).collect(Collectors.toSet());
            try (InputStream fis = Files.newInputStream(Paths.get(TestUtils.getRegressionDataDir(22520), file));
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
                        if (info.isPresent() && (matchingElements.get(info.get()) == null || matchingElements.get(info.get()).getInt("version") < i.version())) {
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
                StringBuilder sb = new StringBuilder("<osm version=\"" + version + "\" generator=\"" + generator + "\" copyright=\"" + copyright + "\" attribution=\"" + attribution + "\" license=\"" + license + "\">");
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
                    sb.append("<tag k=\"").append(tag.getKey()).append("\" v=\"").append(((JsonString) tag.getValue()).getString().replace("\"", "&quot;")).append("\"/>");
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
            public OsmParameterInformation(OsmPrimitiveType type, String param) {
                this(type, Long.parseLong(param.split("v")[0]),
                        param.split("v").length == 2 ? Integer.parseInt(param.split("v")[1]) : Integer.MIN_VALUE);
            }

            /**
             * Create a new record
             * @param type The type
             * @param id The primitive id
             * @param version The primitive version. Anything less than or equal to 0 means latest.`
             */
            public OsmParameterInformation(OsmPrimitiveType type, long id, int version) {
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
}