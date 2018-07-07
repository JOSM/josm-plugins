// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.mode;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.util.Calendar;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.io.download.StreetsideDownloader;

/**
 * Superclass for all the mode of the {@link StreetsideLayer}.
 *
 * @author nokutu
 * @see StreetsideLayer
 */
public abstract class AbstractMode extends MouseAdapter implements
  ZoomChangeListener {

  private static final int DOWNLOAD_COOLDOWN = 2000;
  private static SemiautomaticThread semiautomaticThread = new SemiautomaticThread();

  /**
   * Cursor that should become active when this mode is activated.
   */
  public int cursor = Cursor.DEFAULT_CURSOR;

  protected StreetsideAbstractImage getClosest(Point clickPoint) {
    double snapDistance = 10;
    double minDistance = Double.MAX_VALUE;
    StreetsideAbstractImage closest = null;
    for (StreetsideAbstractImage image : StreetsideLayer.getInstance().getData().getImages()) {
      Point imagePoint = MainApplication.getMap().mapView.getPoint(image.getMovingLatLon());
      imagePoint.setLocation(imagePoint.getX(), imagePoint.getY());
      double dist = clickPoint.distanceSq(imagePoint);
      if (minDistance > dist && clickPoint.distance(imagePoint) < snapDistance
        && image.isVisible()) {
        minDistance = dist;
        closest = image;
      }
    }
    return closest;
  }

  /**
   * Paint the dataset using the engine set.
   *
   * @param g {@link Graphics2D} used for painting
   * @param mv The object that can translate GeoPoints to screen coordinates.
   * @param box Area where painting is going to be performed
   */
  public abstract void paint(Graphics2D g, MapView mv, Bounds box);

  @Override
  public void zoomChanged() {
    if (StreetsideDownloader.getMode() == StreetsideDownloader.DOWNLOAD_MODE.VISIBLE_AREA) {
      if (!semiautomaticThread.isAlive())
        semiautomaticThread.start();
      semiautomaticThread.moved();
    }
  }

  /**
   * Resets the semiautomatic mode thread.
   */
  public static void resetThread() {
    semiautomaticThread.interrupt();
    semiautomaticThread = new SemiautomaticThread();
  }

  private static class SemiautomaticThread extends Thread {

    /** If in semiautomatic mode, the last Epoch time when there was a download */
    private long lastDownload;

    private boolean moved;

    @Override
    public void run() {
      while (true) {
        if (this.moved && Calendar.getInstance().getTimeInMillis() - this.lastDownload >= DOWNLOAD_COOLDOWN) {
          this.lastDownload = Calendar.getInstance().getTimeInMillis();
          StreetsideDownloader.downloadVisibleArea();
          this.moved = false;
          StreetsideLayer.invalidateInstance();
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          return;
        }
      }
    }

    public void moved() {
      this.moved = true;
    }
  }
}
