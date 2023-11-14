// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideCubemap;
import org.openstreetmap.josm.plugins.streetside.StreetsideDataListener;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideViewerDialog;
import org.openstreetmap.josm.plugins.streetside.gui.imageinfo.StreetsideViewerPanel;
import org.openstreetmap.josm.plugins.streetside.gui.imageinfo.ThreeSixtyDegreeViewerPanel;
import org.openstreetmap.josm.plugins.streetside.utils.GraphicsUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.tools.Logging;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// JavaFX access in Java 8
public class CubemapBuilder implements ITileDownloadingTaskListener, StreetsideDataListener {

  private static final Logger LOGGER = Logger.getLogger(CubemapBuilder.class.getCanonicalName());

  private static CubemapBuilder instance;
  protected boolean isBuilding;
  private StreetsideCubemap cubemap;
  private long startTime;

  private Map<String, BufferedImage> tileImages = new ConcurrentHashMap<>();
  private ExecutorService pool;

  private int currentTileCount = 0;

  private CubemapBuilder() {
    // private constructor to avoid instantiation
  }

  public static CubemapBuilder getInstance() {
    if (instance == null) {
      instance = new CubemapBuilder();
    }
    return instance;
  }

  /**
   * @return true, iff the singleton instance is present
   */
  public static boolean hasInstance() {
    return CubemapBuilder.instance != null;
  }

  /**
   * Destroys the unique instance of the class.
   */
  public static synchronized void destroyInstance() {
    CubemapBuilder.instance = null;
  }

  /**
   * @return the tileImages
   */
  public Map<String, BufferedImage> getTileImages() {
    return tileImages;
  }

  /**
   * @param tileImages the tileImages to set
   */
  public void setTileImages(Map<String, BufferedImage> tileImages) {
    this.tileImages = tileImages;
  }

  /**
   * Fired when any image is added to the database.
   */
  @Override
  public void imagesAdded() {
    // Not implemented by the CubemapBuilder
  }

  /**
   * Fired when the selected image is changed by something different from
   * manually clicking on the icon.
   *
   * @param oldImage Old selected {@link StreetsideAbstractImage}
   * @param newImage New selected {@link StreetsideAbstractImage}
   * @see StreetsideDataListener
   */
  @Override
  public void selectedImageChanged(StreetsideAbstractImage oldImage, StreetsideAbstractImage newImage) {

    startTime = System.currentTimeMillis();

    if (newImage != null) {

      cubemap = null;
      cubemap = new StreetsideCubemap(newImage.getId(), newImage.getLatLon(), newImage.getHe());
      currentTileCount = 0;
      resetTileImages();

      // download cubemap images in different threads and then subsequently
      // set the cubeface images in JavaFX
      downloadCubemapImages(cubemap.getId());

      long runTime = (System.currentTimeMillis() - startTime) / 1000;
      if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
        LOGGER.log(Logging.LEVEL_DEBUG, MessageFormat
            .format("Completed downloading tiles for {0} in {1} seconds.", newImage.getId(), runTime));
      }
    }
  }

  public void reload(String imageId) {
    if (cubemap != null && imageId.equals(cubemap.getId())) {
      tileImages = new HashMap<>();
      downloadCubemapImages(imageId);
    }
  }

  public void downloadCubemapImages(String imageId) {
    ThreeSixtyDegreeViewerPanel panel360 = StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel();
    if (panel360 != null && panel360.getScene() != panel360.getLoadingScene()) {
      panel360.setScene(panel360.getLoadingScene());
    }

    final int maxThreadCount = Boolean.TRUE.equals(StreetsideProperties.DOWNLOAD_CUBEFACE_TILES_TOGETHER.get()) ? 6
        : 6 * CubemapUtils.getMaxCols() * CubemapUtils.getMaxRows();

    int fails = 0;

    // TODO: message for progress bar
    String[] message = new String[2];
    message[0] = MessageFormat.format("Downloading Streetside imagery for {0}", imageId);
    message[1] = "Wait for completion…….";

    long startTime = System.currentTimeMillis();

    if (!CubemapBuilder.getInstance().getTileImages().keySet().isEmpty()) {
      pool.shutdownNow();
      CubemapBuilder.getInstance().resetTileImages();
    }

    try {

      pool = Executors.newFixedThreadPool(maxThreadCount);
      List<Callable<List<String>>> tasks = new ArrayList<>(maxThreadCount);

      if (Boolean.TRUE.equals(StreetsideProperties.DOWNLOAD_CUBEFACE_TILES_TOGETHER.get())) {
        EnumSet.allOf(CubemapUtils.CubemapFaces.class).forEach(face -> {
          String tileId = imageId + face.getValue();
          tasks.add(new TileDownloadingTask(tileId));
        });
      } else {

        // launch 4-tiled (low-res) downloading tasks . . .
        if (Boolean.FALSE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
          // download all imagery for each cubeface at once

          for (int i = 0; i < CubemapUtils.NUM_SIDES; i++) {
            int tileNr = 0;
            for (int j = 0; j < CubemapUtils.getMaxCols(); j++) {
              for (int k = 0; k < CubemapUtils.getMaxRows(); k++) {

                String tileId = imageId + CubemapUtils.getFaceNumberForCount(i) + tileNr++;
                tasks.add(new TileDownloadingTask(tileId));
              }
            }
          }
          // launch 16-tiled (high-res) downloading tasks
        } else if (Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {

          for (int i = 0; i < CubemapUtils.NUM_SIDES; i++) {
            for (int j = 0; j < CubemapUtils.getMaxCols(); j++) {
              for (int k = 0; k < CubemapUtils.getMaxRows(); k++) {

                String tileId = imageId + CubemapUtils.getFaceNumberForCount(i) + j + k;
                tasks.add(new TileDownloadingTask(tileId));
              }
            }
          }
        }
      } // finish preparing tasks for invocation

      // execute tasks
      MainApplication.worker.submit(() -> {
        try {
          List<Future<List<String>>> results = pool.invokeAll(tasks);

          if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get()) && results != null) {
            for (Future<List<String>> ff : results) {
              try {
                LOGGER.log(Logging.LEVEL_DEBUG,
                    MessageFormat.format("Completed tile downloading task {0} in {1} seconds.",
                        ff.get().toString(), (System.currentTimeMillis() - startTime) / 1000));
              } catch (ExecutionException e) {
                LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
              }
            }
          }
        } catch (InterruptedException e) {
          LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
        }
      });
    } catch (Exception ee) {
      fails++;
      LOGGER.log(Logging.LEVEL_ERROR, ee, () -> "Error loading tile for image " + imageId);
    }

    long stopTime = System.currentTimeMillis();
    long runTime = stopTime - startTime;

    if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
      LOGGER.log(Logging.LEVEL_DEBUG,
          MessageFormat.format("Tile imagery downloading tasks completed in {0} seconds.", runTime / 1000));
    }

    if (fails > 0) {
      LOGGER.log(Logging.LEVEL_ERROR, fails + " downloading tasks failed!");
    }
  }

  /**
   * Fired when a TileDownloadingTask has completed downloading an image tile. When all of the tiles for the Cubemap
   * have been downloaded, the CubemapBuilder assembles the cubemap.
   *
   * @param tileId
   *      the complete quadKey of the imagery tile, including cubeface and row/column in quaternary.
   * @see TileDownloadingTask
   */
  @Override
  public void tileAdded(String tileId) {
    // determine whether four tiles have been set for each of the
    // six cubemap faces. If so, build the images for the faces
    // and set the views in the cubemap box.

    if (!tileId.startsWith(cubemap.getId())) {
      return;
    }

    currentTileCount++;

    if (currentTileCount == (CubemapUtils.NUM_SIDES * CubemapUtils.getMaxCols() * CubemapUtils.getMaxRows())) {
      if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
        long endTime = System.currentTimeMillis();
        long runTime = (endTime - startTime) / 1000;
        LOGGER.log(Logging.LEVEL_DEBUG, MessageFormat.format(
            "{0} tile images ready for building cumbemap faces for cubemap {1} in {2} seconds.",
            currentTileCount, CubemapBuilder.getInstance().getCubemap().getId(), Long.toString(runTime)));
      }

      buildCubemapFaces();
    }
  }

  /**
   * Assembles the cubemap once all of the tiles have been downloaded.
   * <p>
   * The tiles for each cubemap face are cropped and stitched together
   * then the ImageViews of the cubemap are set with the new imagery.
   *
   * @see     StreetsideCubemap
   */
  private void buildCubemapFaces() {
    StreetsideViewerDialog.getInstance();
    CubemapBox cmb = StreetsideViewerPanel.getCubemapBox();
    ImageView[] views = cmb.getViews();

    Image[] finalImages = new Image[CubemapUtils.NUM_SIDES];

    // build 4-tiled cubemap faces and crop buffers
    if (Boolean.FALSE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
      for (int i = 0; i < CubemapUtils.NUM_SIDES; i++) {

        BufferedImage[] faceTileImages = new BufferedImage[CubemapUtils.getMaxCols()
            * CubemapUtils.getMaxRows()];

        for (int j = 0; j < (CubemapUtils.getMaxCols() * CubemapUtils.getMaxRows()); j++) {
          String tileId = getCubemap().getId() + CubemapUtils.getFaceNumberForCount(i) + j;
          BufferedImage currentTile = tileImages.get(tileId);

          faceTileImages[j] = currentTile;
        }

        BufferedImage finalImg = GraphicsUtils.buildMultiTiledCubemapFaceImage(faceTileImages);

        // rotate top cubeface 180 degrees - misalignment workaround
        if (i == 4) {
          finalImg = GraphicsUtils.rotateImage(finalImg);
        }
        finalImages[i] = GraphicsUtils.convertBufferedImage2JavaFXImage(finalImg);
      }
      // build 16-tiled cubemap faces and crop buffers
    } else if (Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
      for (int i = 0; i < CubemapUtils.NUM_SIDES; i++) {

        int tileCount = 0;

        BufferedImage[] faceTileImages = new BufferedImage[Boolean.TRUE
            .equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) ? 16 : 4];

        for (int j = 0; j < CubemapUtils.getMaxCols(); j++) {
          for (int k = 0; k < CubemapUtils.getMaxRows(); k++) {
            String tileId = getCubemap().getId() + CubemapUtils.getFaceNumberForCount(i)
                + CubemapUtils.convertDoubleCountNrto16TileNr(j + Integer.toString(k));
            BufferedImage currentTile = tileImages.get(tileId);
            faceTileImages[tileCount++] = currentTile;
          }
        }
        BufferedImage finalImg = GraphicsUtils.buildMultiTiledCubemapFaceImage(faceTileImages);
        // rotate top cubeface 180 degrees - misalignment workaround
        if (i == 4) {
          finalImg = GraphicsUtils.rotateImage(finalImg);
        }
        finalImages[i] = GraphicsUtils.convertBufferedImage2JavaFXImage(finalImg);
      }
    }

    for (int i = 0; i < CubemapUtils.NUM_SIDES; i++) {
      views[i].setImage(finalImages[i]);
    }

    StreetsideViewerDialog.getInstance().getStreetsideViewerPanel().revalidate();
    StreetsideViewerDialog.getInstance().getStreetsideViewerPanel().repaint();

    StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel()
        .setScene(StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().getCubemapScene());

    StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().revalidate();
    StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().repaint();

    long endTime = System.currentTimeMillis();
    long runTime = (endTime - startTime) / 1000;

    String message = MessageFormat.format(
        "Completed downloading, assembling and setting cubemap imagery for cubemap {0} in  {1} seconds.",
        cubemap.getId(), runTime);

    if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
      LOGGER.log(Logging.LEVEL_DEBUG, message);
    }

    // reset count and image map after assembly
    resetTileImages();
    currentTileCount = 0;
    isBuilding = false;
  }

  private void resetTileImages() {
    tileImages = new HashMap<>();
  }

  /**
   * @return the cubemap
   */
  public synchronized StreetsideCubemap getCubemap() {
    return cubemap;
  }

  /**
   * @param cubemap
   *      the cubemap to set
   */
  public static void setCubemap(StreetsideCubemap cubemap) {
    CubemapBuilder.getInstance().cubemap = cubemap;
  }

  /**
   * @return the isBuilding
   */
  public boolean isBuilding() {
    return isBuilding;
  }
}
