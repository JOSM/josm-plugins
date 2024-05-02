// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.streetside.StreetsideData;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL.APIv3;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.stream.JsonParser;

/**
 * Download an area
 */
public final class SequenceDownloadRunnable extends BoundsDownloadRunnable {
    private static final Logger LOG = Logger.getLogger(BoundsDownloadRunnable.class.getCanonicalName());
    private static final Function<Bounds, URL> URL_GEN = APIv3::searchStreetsideImages;
    private final StreetsideData data;
    private String logo;
    private String copyright;

    /**
     * Create a new downloader
     * @param data The data to add to
     * @param bounds The bounds to download
     */
    public SequenceDownloadRunnable(final StreetsideData data, final Bounds bounds) {
        super(bounds);
        this.data = data;
    }

    @Override
    public void run(final URLConnection con) throws IOException {
        if (Thread.interrupted()) {
            return;
        }

        final long startTime = System.currentTimeMillis();

        // Structure is
        // { "authenticationResultCode": "foo", "brandLogoUri": "bar", "copyright": "text", "resource
        try (JsonParser parser = Json.createParser(con.getInputStream())) {
            if (!parser.hasNext() || parser.next() != JsonParser.Event.START_OBJECT) {
                throw new IllegalStateException("Expected an object");
            }

            parseJson(parser);
            final long endTime = System.currentTimeMillis();
            LOG.log(Level.INFO, "Successfully loaded {0} Microsoft Streetside images in {1} seconds.",
                    new Object[] {this.data.getImages().size(), (endTime - startTime) / 1000});
        } catch (DateTimeParseException dateTimeParseException) {
            // Added to debug #23658 -- a valid date string caused an exception
            BugReport.intercept(dateTimeParseException).put("url", con.getURL()).warn();
        }
    }

    void parseJson(JsonParser parser) {
        while (parser.hasNext()) {
            if (Objects.requireNonNull(parser.next()) == JsonParser.Event.KEY_NAME) {
                switch (parser.getString()) {
                case "errorDetails" -> parseErrorDetails(parser);
                case "resourceSets" -> parseResourceSets(parser);
                case "brandLogoUri" -> parseBrandLogoUri(parser);
                case "copyright" -> parseCopyright(parser);
                default -> { /* Do nothing for now */ }
                }
            }
        }
    }

    private static void parseErrorDetails(JsonParser parser) {
        if (parser.next() == JsonParser.Event.START_ARRAY) {
            final var errors = new StringBuilder();
            while (parser.next() != JsonParser.Event.END_ARRAY) {
                if (parser.currentEvent() == JsonParser.Event.VALUE_STRING) {
                    errors.append(parser.getString()).append('\n');
                }
            }
            throw new JosmRuntimeException(errors.toString());
        }
    }

    private void parseResourceSets(JsonParser parser) {
        if (parser.next() == JsonParser.Event.START_ARRAY) {
            while (parser.hasNext() && parser.next() == JsonParser.Event.START_OBJECT) {
                while (parser.hasNext() && parser.currentEvent() != JsonParser.Event.END_OBJECT) {
                    if (parser.next() == JsonParser.Event.KEY_NAME
                            && "resources".equals(parser.getString())) {
                        parser.next();
                        List<StreetsideImage> bubbleImages = new ArrayList<>();
                        parseResource(parser, bubbleImages);
                        this.data.addAll(bubbleImages, true);
                    }
                }
            }
        }
    }

    private void parseBrandLogoUri(JsonParser parser) {
        if (parser.next() == JsonParser.Event.VALUE_STRING) {
            this.logo = parser.getString();
        }
    }

    private void parseCopyright(JsonParser parser) {
        if (parser.next() == JsonParser.Event.VALUE_STRING) {
            this.copyright = parser.getString();
        }
    }

    private void parseResource(JsonParser parser, List<StreetsideImage> bubbleImages) {
        while (parser.hasNext() && parser.next() == JsonParser.Event.START_OBJECT) {
            // read everything from this START_OBJECT to the matching END_OBJECT
            // and return it as a tree model ObjectNode
            JsonObject node = parser.getObject();
            // Discard the first sequence ('enabled') - it does not contain bubble data
            if (node.get("imageUrl") != null && node.get("lat") != null && node.get("lon") != null) {
                final var id = node.getString("imageUrl");
                final var lat = node.getJsonNumber("lat").doubleValue();
                final var lon = node.getJsonNumber("lon").doubleValue();
                final var heading = node.getJsonNumber("he").doubleValue();
                final var pitch = node.containsKey("pi") ? node.getJsonNumber("pi").doubleValue() : Double.NaN;
                final var roll = node.containsKey("ro") ? node.getJsonNumber("ro").doubleValue() : Double.NaN;
                final var vintageStart = LocalDate
                        .parse(node.getString("vintageStart").replace("GMT", "UTC"),
                                DateTimeFormatter.ofPattern("dd LLL yyyy zzz"))
                        .atStartOfDay().toInstant(ZoneOffset.UTC);
                final var vintageEnd = LocalDate
                        .parse(node.getString("vintageStart").replace("GMT", "UTC"),
                                DateTimeFormatter.ofPattern("dd LLL yyyy zzz"))
                        .atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
                final List<String> imageUrlSubdomains = node.getJsonArray("imageUrlSubdomains")
                        .getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
                final var zoomMax = node.getInt("zoomMax");
                final var zoomMin = node.getInt("zoomMin");
                final var imageHeight = node.getInt("imageHeight");
                final var imageWidth = node.getInt("imageWidth");
                final var image = new StreetsideImage(id, lat, lon, heading, pitch, roll, vintageStart,
                        vintageEnd, this.logo, this.copyright, zoomMin, zoomMax, imageHeight, imageWidth,
                        imageUrlSubdomains);
                bubbleImages.add(image);
                LOG.info(() -> "Added image with id <" + image.id() + ">");
                if (Boolean.TRUE.equals(StreetsideProperties.PREDOWNLOAD_CUBEMAPS.get())) {
                    this.data.downloadSurroundingCubemaps(image);
                }
            } else {
                LOG.info(() -> MessageFormat.format("Unparsable JSON node object: {0}", node));
            }
        }
    }

    @Override
    protected Function<Bounds, URL> getUrlGenerator() {
        return URL_GEN;
    }
}
