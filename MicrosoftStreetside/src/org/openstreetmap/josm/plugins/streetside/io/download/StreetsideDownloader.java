// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsidePlugin;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class that concentrates all the ways of downloading of the plugin. All the
 * download petitions will be managed one by one.
 *
 * @author nokutu
 */
public final class StreetsideDownloader {

    private static final Logger LOGGER = Logger.getLogger(StreetsideDownloader.class.getCanonicalName());
    /**
     * Max area to be downloaded
     */
    private static final double MAX_AREA = StreetsideProperties.MAX_DOWNLOAD_AREA.get();
    /**
     * Executor that will run the petitions.
     */
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 100, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardPolicy());
    /**
     * Indicates whether the last download request has been rejected because it requested an area that was too big.
     * Iff true, the last download has been rejected, if false, it was executed.
     */
    private static boolean stoppedDownload;

    private StreetsideDownloader() {
        // Private constructor to avoid instantiation
    }

    /**
     * Downloads all images of the area covered by the OSM data.
     */
    public static void downloadOSMArea() {
        if (MainApplication.getLayerManager().getEditLayer() == null) {
            return;
        }
        if (isAreaTooBig(MainApplication.getLayerManager().getEditLayer().data.getDataSourceBounds().stream()
                .map(Bounds::getArea).reduce(0.0, Double::sum))) {
            return;
        }
        MainApplication.getLayerManager().getEditLayer().data.getDataSourceBounds().stream()
                .filter(bounds -> !StreetsideLayer.getInstance().getData().getBounds().contains(bounds))
                .forEach(bounds -> {
                    StreetsideLayer.getInstance().getData().getBounds().add(bounds);
                    StreetsideDownloader.getImages(bounds.getMin(), bounds.getMax());
                });
    }

    /**
     * Gets all the images in a square. It downloads all the images of all the
     * sequences that pass through the given rectangle.
     *
     * @param minLatLon The minimum latitude and longitude of the rectangle.
     * @param maxLatLon The maximum latitude and longitude of the rectangle
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
     * @param bounds A {@link Bounds} object containing the area to be downloaded.
     */
    public static void getImages(Bounds bounds) {
        run(new StreetsideSquareDownloadRunnable(bounds));
    }

    /**
     * Returns the current download mode.
     *
     * @return the currently enabled {@link DOWNLOAD_MODE}
     */
    public static DOWNLOAD_MODE getMode() {
        return DOWNLOAD_MODE.fromPrefId(StreetsideProperties.DOWNLOAD_MODE.get());
    }

    private static void run(Runnable t) {
        executor.execute(t);
    }

    /**
     * If some part of the current view has not been downloaded, it is downloaded.
     */
    public static void downloadVisibleArea() {
        final var view = MainApplication.getMap().mapView.getRealBounds();
        if (isAreaTooBig(view.getArea())) {
            return;
        }
        if (isViewDownloaded(view)) {
            return;
        }
        StreetsideLayer.getInstance().getData().getBounds().add(view);
        getImages(view);
    }

    private static boolean isViewDownloaded(Bounds view) {
        final var n = 15;
        final var inside = new boolean[n][n];
        for (var i = 0; i < n; i++) {
            for (var j = 0; j < n; j++) {
                if (isInBounds(new LatLon(view.getMinLat() + (view.getMaxLat() - view.getMinLat()) * ((double) i / n),
                        view.getMinLon() + (view.getMaxLon() - view.getMinLon()) * ((double) j / n)))) {
                    inside[i][j] = true;
                }
            }
        }
        for (var i = 0; i < n; i++) {
            for (var j = 0; j < n; j++) {
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
     * @param latlon The coordinates to check.
     * @return true if it lies inside the bounds; false otherwise;
     */
    private static boolean isInBounds(LatLon latlon) {
        return StreetsideLayer.getInstance().getData().getBounds().parallelStream().anyMatch(b -> b.contains(latlon));
    }

    /**
     * Checks if the area for which Streetside images should be downloaded is too big. This means that probably
     * lots of Streetside images are going to be downloaded, slowing down the
     * program too much. A notification is shown when the download has stopped or continued.
     *
     * @param area area to check
     * @return {@code true} if the area is too big
     */
    private static boolean isAreaTooBig(final double area) {
        final boolean tooBig = area > MAX_AREA;
        if (!stoppedDownload && tooBig) {
            new Notification(I18n
                    .tr("The Streetside layer has stopped downloading images, because the requested area is too big!")
                    + getDownloadMessage()).setIcon(StreetsidePlugin.LOGO.get()).setDuration(Notification.TIME_LONG)
                            .show();
        }
        if (stoppedDownload && !tooBig) {
            new Notification("The Streetside layer now continues to download images…")
                    .setIcon(StreetsidePlugin.LOGO.get()).show();
        }
        stoppedDownload = tooBig;
        return tooBig;
    }

    private static String getDownloadMessage() {
        return switch (getMode()) {
        case VISIBLE_AREA -> "\n"
                + I18n.tr("To solve this problem, you could zoom in and load a smaller area of the map.");
        case OSM_AREA -> "\n" + I18n.tr("To solve this problem, you could switch to download mode ''{0}'' and "
                + "load Streetside images for a smaller portion of the map.");
        case MANUAL_ONLY -> "";
        };
    }

    /**
     * Stops all running threads.
     */
    public static void stopAll() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
        }
        executor = new ThreadPoolExecutor(3, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    /**
     * Possible download modes.
     */
    public enum DOWNLOAD_MODE {
        VISIBLE_AREA("visibleArea", I18n.tr("everything in the visible area")),

        OSM_AREA("osmArea", I18n.tr("areas with downloaded OSM-data")),

        MANUAL_ONLY("manualOnly", I18n.tr("only when manually requested"));

        public static final DOWNLOAD_MODE DEFAULT = OSM_AREA;

        private final String prefId;
        private final String label;

        DOWNLOAD_MODE(String prefId, String label) {
            this.prefId = prefId;
            this.label = label;
        }

        /**
         * Convert a preference value to a mode
         * @param prefId The preference value to convert
         * @return The download mode, or {@link #DEFAULT}
         */
        public static DOWNLOAD_MODE fromPrefId(String prefId) {
            for (DOWNLOAD_MODE mode : DOWNLOAD_MODE.values()) {
                if (mode.getPrefId().equals(prefId)) {
                    return mode;
                }
            }
            return DEFAULT;
        }

        /**
         * Convert a label to a mode
         * @param label The label to convert
         * @return The download mode, or {@link #DEFAULT}
         */
        public static DOWNLOAD_MODE fromLabel(String label) {
            for (DOWNLOAD_MODE mode : DOWNLOAD_MODE.values()) {
                if (mode.getLabel().equals(label)) {
                    return mode;
                }
            }
            return DEFAULT;
        }

        /**
         * Get the id for the mode
         * @return the ID that is used to represent this download mode in the JOSM preferences
         */
        public String getPrefId() {
            return prefId;
        }

        /**
         * Get the label for the mode
         * @return the (internationalized) label describing this download mode
         */
        public String getLabel() {
            return label;
        }
    }
}
