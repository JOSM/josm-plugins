// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Logging;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;

public final class JsonStreetsideDecoder {

  private JsonStreetsideDecoder() {
    // Private constructor to avoid instantiation
  }

  /**
   * Parses a given {@link JsonObject} as a GeoJSON FeatureCollection into a {@link Collection}
   * of the desired Java objects. The method, which converts the GeoJSON features into Java objects
   * is given as a parameter to this method.
   *
   * @param <T>      feature type
   * @param json       the {@link JsonObject} to be parsed
   * @param featureDecoder feature decoder which transforms JSON objects to Java objects
   * @return a {@link Collection} which is parsed from the given {@link JsonObject}, which contains GeoJSON.
   * Currently a {@link HashSet} is used, but please don't rely on it, this could change at any time without
   * prior notice. The return value will not be <code>null</code>.
   */
  public static <T> Collection<T> decodeFeatureCollection(final JsonObject json,
      Function<JsonObject, T> featureDecoder) {
    return JsonDecoder.decodeFeatureCollection(json, featureDecoder);
  }

  /**
   * Decodes a {@link JsonArray} of exactly size 2 to a {@link LatLon} instance.
   * The first value in the {@link JsonArray} is treated as longitude, the second one as latitude.
   *
   * @param json the {@link JsonArray} containing the two numbers
   * @return the decoded {@link LatLon} instance, or <code>null</code> if the parameter is
   * not a {@link JsonArray} of exactly size 2 containing two {@link JsonNumber}s.
   */
  static LatLon decodeLatLon(final JsonArray json) {
    return JsonDecoder.decodeLatLon(json);
  }

  /**
   * Decodes a timestamp formatted as a {@link String} to the equivalent UNIX epoch timestamp
   * (number of milliseconds since 1970-01-01T00:00:00.000+0000).
   *
   * @param timestamp the timestamp formatted according to the format <code>yyyy-MM-dd'T'HH:mm:ss.SSSX</code>
   * @return the point in time as a {@link Long} value representing the UNIX epoch time, or <code>null</code> if the
   * parameter does not match the required format (this also triggers a warning via
   * {@link Logging}, or the parameter is <code>null</code>).
   */
  static Long decodeTimestamp(final String timestamp) {
    return JsonDecoder.decodeTimestamp(timestamp);
  }
}
