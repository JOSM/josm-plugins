// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;

import jakarta.annotation.Nonnull;

/**
 * A record for keeping track of what URL image goes with what side
 * @param side The side of the
 * @param x The x coordinate (top-left is 0)
 * @param y The y coordinate (top-left is 0)
 * @see <a href="https://learn.microsoft.com/en-us/bingmaps/articles/getting-streetside-tiles-from-imagery-metadata">
 *     Getting Streetside Tiles from Imagery Metadata
 * </a>
 */
public record CubeMapTileXY(CubemapUtils.CubemapFaces side, int x, int y) {
    /**
     * Get the quadkey given a zoom level
     * @param zoom The zoom
     * @return The quad key for this tile
     */
    @Nonnull
    public String getQuadKey(int zoom) {
        return xyzToQuadKey(x, y, zoom);
    }

    /**
     * Convert an x y z coordinate to a quadkey
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return The quadkey
     */
    @Nonnull
    private static String xyzToQuadKey(int x, int y, int z) {
        final var string = new char[z];
        for (int i = z; i > 0; i--) {
            var digit = '0';
            final int mask = 1 << (i - 1);
            if ((x & mask) != 0) {
                digit++;
            }
            if ((y & mask) != 0) {
                digit += 2;
            }
            string[z - i] = digit;
        }
        return String.valueOf(string);
    }

}
