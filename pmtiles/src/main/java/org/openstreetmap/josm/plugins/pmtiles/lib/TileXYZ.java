// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

/**
 * A tile coordinate for web mercator
 * @param x The x coordinate
 * @param y The y coordinate
 * @param z The z coordinate (zoom)
 */
public record TileXYZ(int z, int x, int y) {
}
