// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import java.io.Serializable;
import java.net.URI;

/**
 * The header for a PMTiles file
 * @param location The location of the PMTiles. Not actually part of the PMTiles header; this is used to avoid passing URIs around.
 * @param rootOffset The offset of the root directory
 * @param rootLength The length of the root directory
 * @param metadataOffset The offset of the metadata directory
 * @param metadataLength The length of the metadata directory
 * @param leafOffset The offset of leaf directories
 * @param leafLength The length of leaf directories
 * @param tileOffset The offset of tile data
 * @param tileLength The length of tile data
 * @param addressedTiles The number of addressed tiles; 0 if unknown
 * @param tileEntries The number of tile entries; 0 if unknown
 * @param tileContents The number of tile contents; 0 if unknown
 * @param clustered {@code true} if the tiles are ordered by a Hilbert `TileId`
 * @param internalCompression The compression type used for internal data
 * @param tileCompression The compression type used for the tiles
 * @param tileType The type of the tiles
 * @param minZoom The minimum zoom level
 * @param maxZoom The maximum zoom level
 * @param minLongitude The minimum longitude
 * @param minLatitude The minimum latitude
 * @param maxLongitude The maximum longitude
 * @param maxLatitude The maximum latitude
 * @param centerZoom The center zoom
 * @param centerLongitude The center longitude
 * @param centerLatitude The center latitude
 */
public record Header(URI location, long rootOffset, long rootLength, long metadataOffset, long metadataLength,
                     long leafOffset, long leafLength, long tileOffset, long tileLength, long addressedTiles,
                     long tileEntries, long tileContents, boolean clustered, InternalCompression internalCompression,
                     InternalCompression tileCompression, TileType tileType, int minZoom, int maxZoom, double minLongitude,
                     double minLatitude, double maxLongitude, double maxLatitude, int centerZoom, double centerLongitude,
                     double centerLatitude) implements Serializable {
}
