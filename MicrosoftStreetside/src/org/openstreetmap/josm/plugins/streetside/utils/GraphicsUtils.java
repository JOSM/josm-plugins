// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.tools.Logging;

import javafx.application.Platform;

/**
 * Various graphic utilities, mostly for images.
 */
public final class GraphicsUtils {

    private static final Logger LOGGER = Logger.getLogger(GraphicsUtils.class.getCanonicalName());

    private GraphicsUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Build the face given the tiles
     * @param tiles The tile images
     * @param zoom The zoom level
     * @return The rendered image
     */
    public static BufferedImage buildMultiTiledCubemapFaceImage(Map<CubeMapTileXY, BufferedImage> tiles, int zoom) {
        var faceTileImages = new BufferedImage[tiles.size()];
        for (var entry : tiles.entrySet()) {
            final var index = cubeMapTileToIndex(entry.getKey(), zoom);
            if (index >= faceTileImages.length) { // Due to some null tiles during loading... TODO: How is this happening?
                faceTileImages = Arrays.copyOf(faceTileImages, index + 1);
            }
            faceTileImages[index] = entry.getValue();
        }
        return buildMultiTiledCubemapFaceImage(faceTileImages);
    }

    /**
     * Convert a tile to an index for painting
     * @param tile The tile to convert
     * @param zoom The zoom
     * @return The index for the array
     */
    static int cubeMapTileToIndex(CubeMapTileXY tile, int zoom) {
        return (1 << zoom) * tile.y() + tile.x();
    }

    /**
     * Build an image given a list of tiles
     * @param tiles The tiles to use (note: there should be a factor of 4 images, e.g. 1, 4, 16, 64, ...)
     * @return The rendered image
     */
    public static BufferedImage buildMultiTiledCubemapFaceImage(final BufferedImage[] tiles) {

        long start = System.currentTimeMillis();

        final int zoom = Math.toIntExact(Math.round(Math.log(tiles.length) / Math.log(4)));
        int pixelBuffer = zoom >= 1 ? 2 : 1;

        BufferedImage[] croppedTiles = cropMultiTiledImages(tiles, pixelBuffer);

        // we assume the no. of rows and cols are known and each chunk has equal width and height
        final int rows = Math.toIntExact(Math.round(Math.pow(2, zoom)));
        final int cols = rows;

        int chunkWidth;
        int chunkHeight;

        chunkWidth = Arrays.stream(croppedTiles).filter(Objects::nonNull).findFirst().orElseThrow().getWidth();
        chunkHeight = Arrays.stream(croppedTiles).filter(Objects::nonNull).findFirst().orElseThrow().getHeight();

        //Initializing the final image
        final var img = new BufferedImage(chunkWidth * cols, chunkHeight * rows, BufferedImage.TYPE_INT_ARGB);

        int num = 0;
        final var g2d = img.createGraphics();
        for (var i = 0; i < rows; i++) {
            for (var j = 0; j < cols; j++) {
                final var tile = croppedTiles[num++];
                if (tile != null) {
                    // TODO: unintended mirror image created with draw call - requires
                    // extra reversal step - fix!
                    g2d.drawImage(tile, chunkWidth * j, (chunkHeight * i), null);
                }
            }
        }
        g2d.dispose();

        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Image concatenated in {0} millisecs.",
                    (System.currentTimeMillis() - start));
        }
        return img;
    }

    /**
     * Rotate an image by 180 degrees
     * @param bufImg The image to rotate
     * @return The rotated image
     */
    public static BufferedImage rotateImage(BufferedImage bufImg) {
        // FIXME: Does AffineTransform.getRotateInstance(Math.PI) work? (docs indicate that this is optimized)
        final var tx = AffineTransform.getScaleInstance(-1, -1);
        tx.translate(-bufImg.getWidth(null), -bufImg.getHeight(null));
        final var op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bufImg = op.filter(bufImg, null);
        return bufImg;
    }

    private static BufferedImage[] cropMultiTiledImages(BufferedImage[] tiles, int pixelBuffer) {

        final long start = System.currentTimeMillis();

        final var res = new BufferedImage[tiles.length];

        for (var i = 0; i < tiles.length; i++) {
            if (tiles[i] != null) {
                res[i] = tiles[i].getSubimage(pixelBuffer, pixelBuffer, 256 - pixelBuffer, 256 - pixelBuffer);
            }
        }

        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Images cropped in {0} millisecs.", (System.currentTimeMillis() - start));
        }

        return res;
    }

    /**
     * Utilities for running in the JavaFX platform thread
     */
    public static final class PlatformHelper {

        private PlatformHelper() {
            // Private constructor to avoid instantiation
        }

        /**
         * Run a job in the JavaFX UI thread
         * @param treatment The runnable to run
         */
        public static void run(Runnable treatment) {
            if (treatment == null)
                throw new IllegalArgumentException("The treatment to perform can not be null");

            if (Platform.isFxApplicationThread())
                treatment.run();
            else
                Platform.runLater(treatment);
        }
    }
}
