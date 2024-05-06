// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.streetside.StreetsideData;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideMainDialog;
import org.openstreetmap.josm.plugins.streetside.utils.PluginState;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideUtils;

/**
 * Download Streetside images withing a specified bounds
 */
public class StreetsideSquareDownloadRunnable implements Runnable {

    private final Bounds bounds;

    /**
     * Main constructor.
     *
     * @param bounds the bounds of the area that should be downloaded
     */
    public StreetsideSquareDownloadRunnable(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public void run() {
        PluginState.startDownload();
        StreetsideUtils.updateHelpText();

        // Download basic sequence data synchronously
        // Note that Microsoft limits the downloaded data to 500 images. So we need to split.
        // Start with z16 tiles
        final var zoom = 16;
        final var cancelled = new AtomicBoolean(false);
        final var counter = new AtomicInteger();
        final var data = StreetsideLayer.getInstance().getData();
        DownloadRunnable.download(zoom, bounds, cancelled, counter, data);

        while (counter.get() > 0) {
            try {
                synchronized (counter) {
                    counter.wait(100);
                }
            } catch (InterruptedException e) {
                cancelled.set(true);
                Thread.currentThread().interrupt();
            }
        }
        // if (Boolean.TRUE.equals(StreetsideProperties.PREDOWNLOAD_CUBEMAPS.get())) {
        //     this.data.downloadSurroundingCubemaps(image);
        // }

        if (Thread.interrupted()) {
            return;
        }

        // Image detections are not currently supported for Streetside (Mapillary code removed)

        PluginState.finishDownload();

        StreetsideUtils.updateHelpText();
        StreetsideLayer.invalidateInstance();
        StreetsideMainDialog.getInstance().updateImage();
    }

    private record DownloadRunnable(int zoom, int x, int y, AtomicBoolean cancelled, AtomicInteger counter,
                                    StreetsideData data) implements Runnable {

    @Override
    public void run() {
        try {
            if (cancelled.get()) {
                return;
            }
            final var newData = new SequenceDownloadRunnable(getBounds(zoom, x, y)).call();
            // Microsoft limits API responses to 500 at this time. Split up the bounds.
            // Rather unfortunately, there are no hints in the response for this. So we have to use
            // size checking.
            if (newData.size() >= StreetsideURL.MAX_RETURN) {
                download(zoom + 1, getBounds(zoom, x, y), cancelled, counter, data);
                return;
            }
            if (cancelled.get()) {
                return;
            }
            synchronized (data) {
                data.addAll(newData);
            }
        } finally {
            counter.decrementAndGet();
            synchronized (counter) {
                counter.notifyAll();
            }
        }
    }

    static void download(int zoom, Bounds bounds, AtomicBoolean cancelled, AtomicInteger counter, StreetsideData data) {
        // Yes, we want max lat since tiles start at upper-left.
        final var min = getTile(zoom, bounds.getMaxLat(), bounds.getMinLon());
        final var max = getTile(zoom, bounds.getMinLat(), bounds.getMaxLon());
        for (int x = min.getXIndex(); x <= max.getXIndex(); x++) {
            for (int y = min.getYIndex(); y <= max.getYIndex(); y++) {
                counter.incrementAndGet();
                Thread.ofVirtual().name("streetside-" + zoom + "/" + x + "/" + y)
                        .start(new DownloadRunnable(zoom, x, y, cancelled, counter, data));
            }
        }
    }

    }

    private static TileXY getTile(int zoom, double lat, double lon) {
        final var x = (lon + 180) / 360 * Math.pow(2, zoom);
        final var y = (1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI)
                * Math.pow(2, zoom - 1);
        return new TileXY(x, y);
    }

    private static Bounds getBounds(int zoom, int x, int y) {
        final var minLon = getLon(zoom, x);
        final var maxLon = getLon(zoom, x + 1);
        final var maxLat = getLat(zoom, y);
        final var minLat = getLat(zoom, y + 1);
        return new Bounds(minLat, minLon, maxLat, maxLon);
    }

    private static double getLon(int zoom, int x) {
        return 360 * x / Math.pow(2, zoom) - 180;
    }

    private static double getLat(int zoom, int y) {
        return Math.atan(Math.sinh(Math.PI - 2 * Math.PI * y / Math.pow(2, zoom))) * 180 / Math.PI;
    }
}
