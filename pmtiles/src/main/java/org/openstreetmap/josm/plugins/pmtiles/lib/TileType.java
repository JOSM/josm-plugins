// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

/**
 * The type of tiles served
 */
public enum TileType {
    /** Unknown tile type */
    UNKNOWN,
    /** Mapbox Vector Tiles */
    MVT,
    /** Portable Network Graphics */
    PNG,
    /** Joint Photographic Experts Group */
    JPEG,
    /** Google's WebP format */
    WEBP,
    /** AV1 Image File Format */
    AVIF
}
