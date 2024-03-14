// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.tools.Logging;

/**
 * A class for building the base URLs for Streetside objects
 */
public final class StreetsideURL {

    private static final Logger LOGGER = Logger.getLogger(StreetsideURL.class.getCanonicalName());

    /**
     * Base URL of the Bing Bubble API.
     */
    private static final String STREETSIDE_BASE_URL = "https://dev.virtualearth.net/REST/v1/Imagery/MetaData/Streetside";
    /**
     * Base URL for Streetside privacy concerns.
     */
    private static final String STREETSIDE_PRIVACY_URL = "https://www.bing.com/maps/privacyreport/streetsideprivacyreport?bubbleid=";

    private static final int OSM_BBOX_NORTH = 3;
    private static final int OSM_BBOX_SOUTH = 1;
    private static final int OSM_BBOXEAST = 2;
    private static final int OSM_BBOX_WEST = 0;

    private StreetsideURL() {
        // Private constructor to avoid instantiation
    }

    /**
     * Convert a map of query parameters to a string
     * @param parts The query parameters
     * @return The string to send the server
     */
    static String queryStreetsideBoundsString(Map<String, String> parts) {
        final var ret = new StringBuilder(100);
        if (parts != null) {
            ret.append("?count=500").append("&key=").append(StreetsideProperties.BING_MAPS_KEY.get());
            if (parts.containsKey("bbox")) {
                final String[] bbox = parts.get("bbox").split(",");
                ret.append("&mapArea=").append(bbox[OSM_BBOX_SOUTH]).append(',').append(bbox[OSM_BBOX_WEST]).append(',')
                        .append(bbox[OSM_BBOX_NORTH]).append(',').append(bbox[OSM_BBOXEAST]);
            }
        }

        return ret.toString();
    }

    /**
     * Converts a {@link String} into a {@link URL} without throwing a {@link MalformedURLException}.
     * Instead, such an exception will lead to an {@link Logger}.
     * So you should be very confident that your URL is well-formed when calling this method.
     *
     * @param strings the Strings describing the URL
     * @return the URL that is constructed from the given string
     */
    static URL string2URL(String... strings) {
        final var builder = new StringBuilder();
        for (var i = 0; strings != null && i < strings.length; i++) {
            builder.append(strings[i]);
        }
        try {
            return new URI(builder.toString()).toURL();
        } catch (final IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            LOGGER.log(Logging.LEVEL_ERROR, e,
                    () -> MessageFormat.format("The class ''{0}'' produces malformed URLs like ''{1}''!",
                            StreetsideURL.class.getName(), builder));
            return null;
        }
    }

    /**
     * A class for the current Streetside API
     */
    public static final class APIv3 {

        private APIv3() {
            // Private constructor to avoid instantiation
        }

        /**
         * Search for images inside some bounds
         * @param bounds The bounds to search
         * @return The URL to get
         */
        public static URL searchStreetsideImages(Bounds bounds) {
            return StreetsideURL.string2URL(StreetsideURL.STREETSIDE_BASE_URL, APIv3.queryStreetsideString(bounds));
        }

        /**
         * The APIv3 returns a Link header for each request. It contains a URL for requesting more results.
         * If you supply the value of the Link header, this method returns the next URL,
         * if such a URL is defined in the header.
         *
         * @param value the value of the HTTP-header with key "Link"
         * @return the {@link URL} for the next result page, or <code>null</code> if no such URL could be found
         */
        public static URL parseNextFromLinkHeaderValue(String value) {
            if (value != null) {
                // Iterate over the different entries of the Link header
                for (final String link : value.split(",", Integer.MAX_VALUE)) {
                    var isNext = false;
                    URL url = null;
                    // Iterate over the parts of each entry (typically it's one `rel="‹linkType›"` and one like `<https://URL>`)
                    for (String linkPart : link.split(";", Integer.MAX_VALUE)) {
                        linkPart = linkPart.trim();
                        isNext |= linkPart.matches("rel\\s*=\\s*\"next\"");
                        if (linkPart.length() >= 1 && linkPart.charAt(0) == '<' && linkPart.endsWith(">")) {
                            try {
                                url = new URI(linkPart.substring(1, linkPart.length() - 1)).toURL();
                            } catch (final URISyntaxException | MalformedURLException e) {
                                Logging.log(Logging.LEVEL_WARN,
                                        "Mapillary API v3 returns a malformed URL in the Link header.", e);
                            }
                        }
                    }
                    // If both a URL and the rel=next attribute are present, return the URL. Otherwise null is returned
                    if (url != null && isNext) {
                        return url;
                    }
                }
            }
            return null;
        }

        /**
         * Query for images inside some bounds
         * @param bounds The bounds to query
         * @return The URL to GET
         */
        public static String queryStreetsideString(final Bounds bounds) {
            if (bounds != null) {
                final Map<String, String> parts = new HashMap<>();
                parts.put("bbox", bounds.toBBox().toStringCSV(","));
                return StreetsideURL.queryStreetsideBoundsString(parts);
            }
            return StreetsideURL.queryStreetsideBoundsString(null);
        }

    }

    /**
     * A class for Microsoft Streetside website pages
     */
    public static final class MainWebsite {

        private MainWebsite() {
            // Private constructor to avoid instantiation
        }

        /**
         * Gives you the URL for the online viewer of a specific Streetside image.
         *
         * @param image The image to show
         * @return the URL of the online viewer for the image with the given image key
         * @throws IllegalArgumentException if the image key is <code>null</code>
         */
        public static URL browseImage(ILatLon image) {
            if (image == null || !image.isLatLonKnown()) {
                throw new IllegalArgumentException("The image and image lat/lon may not be null!");
            }
            // The online viewer only takes lat/lon for args
            // lvl == zoom level (needs to be high enough for MS to show streetside imagery)
            // style=x -- needed to show the streetside imagery
            return StreetsideURL.string2URL("https://www.bing.com/maps?lvl=16&style=x&cp=",
                    Double.toString(image.lat()), "~", Double.toString(image.lon()));
        }

        /**
         * Gives you the URL for the blur editor of the image with the given key.
         *
         * @param id the key of the image for which you want to open the blur editor
         * @return the URL of the blur editor
         * @throws IllegalArgumentException if the image key is <code>null</code>
         */
        public static URL streetsidePrivacyLink(final String id) {
            if (id == null) {
                throw new IllegalArgumentException("The image id must not be null!");
            }
            String urlEncodedId;
            urlEncodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
            return StreetsideURL.string2URL(StreetsideURL.STREETSIDE_PRIVACY_URL, urlEncodedId);
        }

    }
}
