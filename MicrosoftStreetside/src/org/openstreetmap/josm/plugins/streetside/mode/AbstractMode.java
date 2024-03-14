// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.mode;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.util.Calendar;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.io.download.StreetsideDownloader;

/**
 * Superclass for all the mode of the {@link StreetsideLayer}.
 *
 * @author nokutu
 * @see StreetsideLayer
 */
public abstract class AbstractMode extends MouseAdapter implements ZoomChangeListener {

    private static final int DOWNLOAD_COOLDOWN = 2000;
    private static SemiautomaticThread semiautomaticThread = new SemiautomaticThread();

    /**
     * Cursor that should become active when this mode is activated.
     */
    public final int cursor = Cursor.DEFAULT_CURSOR;

    /**
     * Resets the semiautomatic mode thread.
     */
    public static void resetThread() {
        semiautomaticThread.interrupt();
        semiautomaticThread = new SemiautomaticThread();
    }

    protected StreetsideImage getClosest(Point clickPoint) {
        double snapDistance = 10;
        double minDistance = Double.MAX_VALUE;
        StreetsideImage closest = null;
        for (StreetsideImage image : StreetsideLayer.getInstance().getData().getImages()) {
            Point imagePoint = MainApplication.getMap().mapView.getPoint(image);
            imagePoint.setLocation(imagePoint.getX(), imagePoint.getY());
            double dist = clickPoint.distanceSq(imagePoint);
            if (minDistance > dist && clickPoint.distance(imagePoint) < snapDistance && image.visible()) {
                minDistance = dist;
                closest = image;
            }
        }
        return closest;
    }

    @Override
    public void zoomChanged() {
        if (StreetsideDownloader.getMode() == StreetsideDownloader.DOWNLOAD_MODE.VISIBLE_AREA) {
            if (!semiautomaticThread.isAlive())
                semiautomaticThread.start();
            semiautomaticThread.moved();
        }
    }

    private static class SemiautomaticThread extends Thread {

        /**
         * If in semiautomatic mode, the last Epoch time when there was a download
         */
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
