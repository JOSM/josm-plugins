// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.io.Serializable;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.josm.data.IQuadBucketType;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.tools.Pair;

import jakarta.annotation.Nonnull;

/**
 * Abstract superclass for all image objects. At the moment there is one,
 * {@link StreetsideImage}.
 *
 * @author nokutu
 * @author renerr18
 *
 */
public sealed

interface StreetsideAbstractImage extends ILatLon, IQuadBucketType, Comparable<StreetsideAbstractImage>, Serializable
permits StreetsideImage
{

    /**
     * Get the ID for this image
     * @return the id
     */
    String id();

    /**
     * Get the subdomains that can be used for this image
     * @return The valid subdomains
     */
    List<String> imageUrlSubdomains();

    /**
     * Returns the original direction towards the image has been taken.
     *
     * @return The direction of the image (0 means north and goes clockwise).
     */
    double heading();

    /**
     * The maximum zoom for this image
     * @return The max zoom
     */
    int zoomMax();

    /**
     * The minimum zoom for this image
     * @return The min zoom
     */
    int zoomMin();

    /**
     * Get the number of x columns
     * @param zoom The zoom level
     * @return The columns for the x-axis
     */
    default int xCols(int zoom) {
        return yCols(zoom);
    }

    /**
     * Get the number of y columns
     * @param zoom The zoom level
     * @return The columns for the y-axis
     */
    default int yCols(int zoom) {
        return 1 << zoom;
    }

    /**
     * Check if the image is visible
     * @return {@code true} if the image is visible
     */
    default boolean visible() {
        return true;
    }

    @Override
    default BBox getBBox() {
        return new BBox(this);
    }

    /**
     * Get a thumbnail for the image
     * @return The URL for the thumbnail
     */
    default String getThumbnail() {
        // This is the "front" min zoom image
        return getTile(Integer.toString(0), Integer.toString(1));
    }

    /**
     * Get the tiles for a face
     * @param face The id of the face
     * @param zoom The zoom level for the face
     * @return A stream of tile location + URL pairs
     */
    default Stream<Pair<CubeMapTileXY, String>> getFaceTiles(CubemapUtils.CubemapFaces face, int zoom) {
        if (zoom > this.zoomMax() || zoom < this.zoomMin()) {
            throw new IndexOutOfBoundsException(zoom);
        }
        final var faceId = face.faceId();
        final var startingTileId = face.startingTileId();
        // The {tileId} is chainable after the starting tile id
        // ---------------
        // | 0, 0 | 1, 0 |
        // | 0, 1 | 1, 1 |
        // ---------------
        // (0, 0) is 0
        // (1, 0) is 1
        // (0, 1) is 2
        // (1, 1) is 3
        // Zoom starts at 1
        int currentZoom = this.zoomMin();
        Stream<String> level = IntStream.range(0, 4).mapToObj(String::valueOf).map(startingTileId::concat);
        while (currentZoom < zoom) {
            level = level.flatMap(s -> IntStream.range(0, 4).mapToObj(String::valueOf).map(s::concat));
            currentZoom++;
        }
        return level.map(s -> new Pair<>(mapToTiles(face, zoom, s), getTile(faceId, s)));
    }

    /**
     * Convert a tileId to a TileXY coordinate
     * @param face The cubemap face
     * @param zoom the zoom level
     * @param tileId The tile id (quadkey)
     * @return The xy coordinate
     */
    private static CubeMapTileXY mapToTiles(CubemapUtils.CubemapFaces face, int zoom, String tileId) {
        // Given a z level, the quadkey is the last z characters
        final var xy = quadKeyToTile(tileId.subSequence(tileId.length() - zoom, tileId.length()));
        return new CubeMapTileXY(face, xy.getXIndex(), xy.getYIndex());
    }

    /**
     * Convert a quadkey to a tile
     * @param quadkey The quadkey to convert
     * @return The tile for that quadkey
     */
    @Nonnull
    static TileXY quadKeyToTile(@Nonnull CharSequence quadkey) {
        final int z = quadkey.length();
        var x = 0;
        var y = 0;
        for (int i = z; i > 0; i--) {
            final var mask = 1 << (i - 1);
            switch (quadkey.charAt(z - i)) {
            case '0':
                break;
            case '1':
                x |= mask;
                break;
            case '2':
                y |= mask;
                break;
            case '3':
                x |= mask;
                y |= mask;
                break;
            default:
                throw new IllegalArgumentException("Bad quadtile character at " + (i - 1) + " for '" + quadkey + "'");
            }
        }
        return new TileXY(x, y);
    }

    /**
     * Get a tile for an image
     * @param faceId The face id
     * @param tileId The tile id
     * @return The URL for the face and tile
     * @see <a href="https://learn.microsoft.com/en-us/bingmaps/articles/getting-streetside-tiles-from-imagery-metadata">
     *     Getting Streetside Tiles from Imagery Metadata
     * </a>
     */
    default String getTile(String faceId, String tileId) {
        return this.id().replace("{subdomain}", this.imageUrlSubdomains().get(0)).replace("{faceId}", faceId)
                .replace("{tileId}", tileId);
    }
}
