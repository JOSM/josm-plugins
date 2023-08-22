// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;

/**
* A set of utilities related to OAuth.
*
* @author nokutu
*
*/
public final class OAuthUtils {

private OAuthUtils() {
 // Private constructor to avoid instantiation
}

/**
* Returns a JsonObject containing the result of making a GET request with the
* authorization header.
*
* @param url
*          The {@link URL} where the request must be made.
* @return A JsonObject containing the result of the GET request.
* @throws IOException
*           Errors relating to the connection.
*/
public static JsonObject getWithHeader(URL url) throws IOException {
 HttpURLConnection con = (HttpURLConnection) url.openConnection();
 con.setRequestMethod("GET");
 con.setRequestProperty("Authorization", "Bearer " + StreetsideProperties.ACCESS_TOKEN.get());

 try (
   JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8")))
 ) {
   return reader.readObject();
 } catch (JsonException e) {
   throw new IOException(e);
 }
}
}
