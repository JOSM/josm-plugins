// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.io.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.MapillaryPlugin;

/**
 * Class that concentrates all the ways of downloading of the plugin. All the
 * download petitions will be managed one by one.
 *
 * @author nokutu
 *
 */
public final class MapillaryDownloader {

  /** Possible download modes. */
  public enum MODES {Automatic, Semiautomatic, Manual}

  /** All the Threads that have been run. Used to interrupt them properly. */
  private static List<Thread> threads = new ArrayList<>();

  /** Max area to be downloaded */
  public static final double MAX_AREA = Main.pref.getDouble("mapillary.max-download-area", 0.015);

  /** Executor that will run the petitions. */
  private static ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));

  private MapillaryDownloader() {
    // Private constructor to avoid instantiation
  }

  /**
   * Gets all the images in a square. It downloads all the images of all the
   * sequences that pass through the given rectangle.
   *
   * @param minLatLon
   *          The minimum latitude and longitude of the rectangle.
   * @param maxLatLon
   *          The maximum latitude and longitude of the rectangle
   */
  public static void getImages(LatLon minLatLon, LatLon maxLatLon) {
    if (minLatLon == null || maxLatLon == null) {
      throw new IllegalArgumentException();
    }
    getImages(new Bounds(minLatLon, maxLatLon));
  }

  /**
   * Gets the images within the given bounds.
   *
   * @param bounds
   *          A {@link Bounds} object containing the area to be downloaded.
   */
  public static void getImages(Bounds bounds) {
    run(new MapillarySquareDownloadManagerThread(bounds));
  }

  /**
   * Returns the current download mode.
   *
   * @return the currently enabled of the available {@link MODES}
   */
  public static MapillaryDownloader.MODES getMode() {
    String downloadMode = Main.pref.get("mapillary.download-mode", MODES.Automatic.toString());
    boolean isTempSemiautomatic = MapillaryLayer.hasInstance() && MapillaryLayer.getInstance().tempSemiautomatic;
    if (MODES.Semiautomatic.toString().equals(downloadMode) || isTempSemiautomatic)
      return MODES.Semiautomatic;
    else if (MODES.Manual.toString().equals(downloadMode))
      return MODES.Manual;
    else if (MODES.Automatic.toString().equals(downloadMode))
      return MODES.Automatic;
    else
      throw new IllegalStateException();
  }

  private static void run(Thread t) {
    threads.add(t);
    executor.execute(t);
  }

  /**
   * If some part of the current view has not been downloaded, it is downloaded.
   */
  public static void completeView() {
    if (getMode() != MODES.Semiautomatic && getMode() != MODES.Manual)
      throw new IllegalStateException("Must be in semiautomatic or manual mode");
    Bounds view = Main.map.mapView.getRealBounds();
    if (view.getArea() > MAX_AREA)
      return;
    if (isViewDownloaded(view))
      return;
    MapillaryLayer.getInstance().getData().getBounds().add(view);
    getImages(view);
  }

  private static boolean isViewDownloaded(Bounds view) {
    int n = 15;
    boolean[][] inside = new boolean[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (isInBounds(new LatLon(view.getMinLat()
            + (view.getMaxLat() - view.getMinLat()) * ((double) i / n),
            view.getMinLon() + (view.getMaxLon() - view.getMinLon())
                * ((double) j / n)))) {
          inside[i][j] = true;
        }
      }
    }
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (!inside[i][j])
          return false;
      }
    }
    return true;
  }

  /**
   * Checks if the given {@link LatLon} object lies inside the bounds of the
   * image.
   *
   * @param latlon
   *          The coordinates to check.
   * @return true if it lies inside the bounds; false otherwise;
   */
  private static boolean isInBounds(LatLon latlon) {
    for (Bounds bounds : MapillaryLayer.getInstance().getData().getBounds()) {
      if (bounds.contains(latlon))
        return true;
    }
    return false;
  }

  /**
   * Downloads all images of the area covered by the OSM data. This is only just
   * for automatic download.
   */
  public static void automaticDownload() {
    MapillaryLayer layer = MapillaryLayer.getInstance();
    if (Main.map.mapView.getEditLayer() == null)
      return;
    if (isAreaTooBig()) {
      tooBigErrorDialog();
      return;
    }
    if (getMode() != MODES.Automatic)
      throw new IllegalStateException("Must be in automatic mode.");
    for (Bounds bounds : Main.map.mapView.getEditLayer().data
        .getDataSourceBounds()) {
      if (!layer.getData().getBounds().contains(bounds)) {
        layer.getData().getBounds().add(bounds);
        MapillaryDownloader.getImages(bounds.getMin(), bounds.getMax());
      }
    }
  }

  /**
   * Checks if the area of the OSM data is too big. This means that probably
   * lots of Mapillary images are going to be downloaded, slowing down the
   * program too much. To solve this the automatic is stopped, an alert is shown
   * and you will have to download areas manually.
   */
  private static boolean isAreaTooBig() {
    double area = 0;
    for (Bounds bounds : Main.map.mapView.getEditLayer().data.getDataSourceBounds()) {
      area += bounds.getArea();
    }
    return area > MAX_AREA;
  }

  protected static void tooBigErrorDialog() {
    if (SwingUtilities.isEventDispatchThread()) {
      MapillaryLayer.getInstance().tempSemiautomatic = true;
      MapillaryPlugin.setMenuEnabled(MapillaryPlugin.getDownloadViewMenu(), true);
      JOptionPane
          .showMessageDialog(
              Main.parent,
              tr("The downloaded OSM area is too big. Download mode has been changed to semiautomatic until the layer is restarted."));
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          tooBigErrorDialog();
        }
      });
    }
  }

  /**
   * Stops all running threads.
   */
  public static void stopAll() {
    for (Thread t : threads) {
      if (t.isAlive())
        Main.info(t+" is still alive!");
      t.interrupt();
    }
    threads.clear();
    executor.shutdownNow();
    try {
      executor.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Main.error(e);
    }
    executor = new ThreadPoolExecutor(3, 5, 100, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(100));
  }
}
