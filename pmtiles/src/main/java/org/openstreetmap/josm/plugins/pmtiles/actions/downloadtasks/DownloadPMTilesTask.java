// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesImageLayer;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesMVTLayer;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;
import org.openstreetmap.josm.plugins.pmtiles.lib.TileType;

/**
 * "Download" a PMTiles file. Really, this just adds a PMTiles layer.
 */
public class DownloadPMTilesTask implements DownloadTask {
    /** Zoom to the PMTiles bounds after download */
    private boolean zoomAfterDownload;
    /** Cancel adding the layer if this is true */
    private boolean cancel;
    /** The URL for the tiles */
    private String url;
    /** Any recoverable errors from reading the PMTiles */
    private final List<Object> errorObjects = new ArrayList<>();
    /** The bounds for the layer */
    private Bounds bounds;

    /**
     * Add the appropriate layer to JOSM
     */
    private void addLayer() {
        if (this.cancel) {
            return;
        }
        try {
            final var header = PMTiles.readHeader(URI.create(this.url));
            final var info = new PMTilesImageryInfo(header);
            if (header.tileType() == TileType.MVT) {
                MainApplication.getLayerManager().addLayer(new PMTilesMVTLayer(info));
            } else {
                MainApplication.getLayerManager().addLayer(new PMTilesImageLayer(info));
            }
        } catch (IOException e) {
            this.errorObjects.add(e);
        }

        if (this.zoomAfterDownload && this.bounds != null && MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
            MainApplication.getMap().mapView.zoomTo(this.bounds);
        }
    }

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        this.bounds = downloadArea;
        return MainApplication.worker.submit(this::addLayer);
    }

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        this.url = Objects.requireNonNull(url);
        return download(settings, null, progressMonitor);
    }

    @Override
    public boolean acceptsUrl(String url, boolean isRemotecontrol) {
        return url.endsWith(".pmtiles");
    }

    @Override
    public String getTitle() {
        return tr("Add PMTiles layer");
    }

    @Override
    public String[] getPatterns() {
        return new String[]{".*.pmtiles"};
    }

    @Override
    public List<Object> getErrorObjects() {
        return Collections.unmodifiableList(this.errorObjects);
    }

    @Override
    public void cancel() {
        this.cancel = true;
    }

    @Override
    public String getConfirmationMessage(URL url) {
        return tr("Do you want to add a layer based off of {0}?", url.toExternalForm());
    }

    @Override
    public void setZoomAfterDownload(boolean zoomAfterDownload) {
        this.zoomAfterDownload = zoomAfterDownload;
    }
}
