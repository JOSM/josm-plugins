// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre.api;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Interface to Cadastre GeoAPI.
 */
public final class CadastreAPI {

    private static final StringProperty API_ENDPOINT = new StringProperty(
            "cadastrefr.api.endpoint", "https://geo.api.gouv.fr/cadastre");

    private CadastreAPI() {
        // Hide public sontructor for utility classes
    }

    /**
     * Returns the set of cadastral sheet ids for the given bounding box.
     * @param bounds bounding box
     * @return the set of cadastral sheet ids for the given bounding box
     * @throws IOException if any I/O error occurs
     */
    public static Set<String> getSheets(Bounds bounds) throws IOException {
        return getSheets(bounds.getMinLon(), bounds.getMinLat(), bounds.getMaxLon(), bounds.getMaxLat());
    }

    /**
     * Returns the set of cadastral sheet ids for the given bounding box.
     * @param minlon minimum longitude
     * @param minlat minimum latitude
     * @param maxlon maximum longitude
     * @param maxlat maximum latitude
     * @return the set of cadastral sheet ids for the given bounding box
     * @throws IOException if any I/O error occurs
     */
    public static Set<String> getSheets(double minlon, double minlat, double maxlon, double maxlat) throws IOException {
        URL url = URI.create(API_ENDPOINT.get() + "/feuilles?bbox=" + String.join(",",
                Double.toString(minlon), Double.toString(minlat), Double.toString(maxlon), Double.toString(maxlat)))
                .toURL();
        try {
            Response response = HttpClient.create(url).connect();
            if (response.getResponseCode() >= 400) {
                throw new IOException(
                        tr("Cadastre GeoAPI returned HTTP error {0}. This is not a JOSM bug.\nPlease report the issue to {1}, {2} or {3}",
                        response.getResponseCode(), "https://github.com/etalab/geo.data.gouv.fr/issues", "https://twitter.com/geodatagouv",
                        "geo@data.gouv.fr"));
            }
            try (JsonReader reader = Json.createReader(response.getContentReader())) {
                JsonStructure json = reader.read();
                if (json instanceof JsonArray) {
                    return json.asJsonArray().stream().map(x -> x.asJsonObject().getString("id")).collect(Collectors.toSet());
                } else {
                    JsonObject obj = json.asJsonObject();
                    throw new IOException(new OsmApiException(obj.getInt("code"), null, obj.getString("message"), url.toExternalForm()));
                }
            }
        } catch (MalformedURLException e) {
            throw new JosmRuntimeException(e);
        }
    }
}
