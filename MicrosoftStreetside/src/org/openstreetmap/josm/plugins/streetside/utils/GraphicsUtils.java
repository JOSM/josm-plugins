// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.openstreetmap.josm.tools.Logging;

import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class GraphicsUtils {

  private static final Logger LOGGER = Logger.getLogger(GraphicsUtils.class.getCanonicalName());

  private GraphicsUtils() {
    // Private constructor to avoid instantiation
  }

  public static javafx.scene.image.Image convertBufferedImage2JavaFXImage(BufferedImage bf) {
    WritableImage res = null;
    if (bf != null) {
      res = new WritableImage(bf.getWidth(), bf.getHeight());
      PixelWriter pw = res.getPixelWriter();
      for (int x = 0; x < bf.getWidth(); x++) {
        for (int y = 0; y < bf.getHeight(); y++) {
          pw.setArgb(x, y, bf.getRGB(x, y));
        }
      }
    }
    return res;
  }

  public static BufferedImage buildMultiTiledCubemapFaceImage(final BufferedImage[] tiles) {

    long start = System.currentTimeMillis();

    BufferedImage res = null;

    int pixelBuffer = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) ? 2 : 1;

    BufferedImage[] croppedTiles = cropMultiTiledImages(tiles, pixelBuffer);

    // we assume the no. of rows and cols are known and each chunk has equal width and height
    int rows = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) ? 4 : 2;
    int cols = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) ? 4 : 2;

    int chunkWidth;
    int chunkHeight;

    chunkWidth = croppedTiles[0].getWidth();
    chunkHeight = croppedTiles[0].getHeight();

    //Initializing the final image
    BufferedImage img = new BufferedImage(chunkWidth * cols, chunkHeight * rows, BufferedImage.TYPE_INT_ARGB);

    int num = 0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        // TODO: unintended mirror image created with draw call - requires
        // extra reversal step - fix!
        img.createGraphics().drawImage(croppedTiles[num], chunkWidth * j, (chunkHeight * i), null);

        int width = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) ? 1014
            : 510;
        int height = width;

        // BufferedImage for mirror image
        res = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Create mirror image pixel by pixel
        for (int y = 0; y < height; y++) {
          for (int lx = 0, rx = width - 1; lx < width; lx++, rx--) {
            // lx starts from the left side of the image
            // rx starts from the right side of the image
            // lx is used since we are getting pixel from left side
            // rx is used to set from right side
            // get source pixel value
            int p = img.getRGB(lx, y);

            // set mirror image pixel value
            res.setRGB(rx, y, p);
          }
        }
        num++;
      }
    }

    if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
      LOGGER.log(Logging.LEVEL_DEBUG,
          MessageFormat.format("Image concatenated in {0} millisecs.", (System.currentTimeMillis() - start)));
    }
    return res;
  }

  public static BufferedImage rotateImage(BufferedImage bufImg) {
    AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
    tx.translate(-bufImg.getWidth(null), -bufImg.getHeight(null));
    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    bufImg = op.filter(bufImg, null);
    return bufImg;
  }

  private static BufferedImage[] cropMultiTiledImages(BufferedImage[] tiles, int pixelBuffer) {

    long start = System.currentTimeMillis();

    BufferedImage[] res = new BufferedImage[tiles.length];

    for (int i = 0; i < tiles.length; i++) {
      if (Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
        res[i] = tiles[i].getSubimage(pixelBuffer, pixelBuffer, 256 - pixelBuffer, 256 - pixelBuffer);
      } else {
        res[i] = tiles[i].getSubimage(pixelBuffer, pixelBuffer, 256 - pixelBuffer, 256 - pixelBuffer);
      }
    }

    if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
      LOGGER.log(Logging.LEVEL_DEBUG,
          MessageFormat.format("Images cropped in {0} millisecs.", (System.currentTimeMillis() - start)));
    }

    return res;
  }

  public static class PlatformHelper {

    private PlatformHelper() {
      // Private constructor to avoid instantiation
    }

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
