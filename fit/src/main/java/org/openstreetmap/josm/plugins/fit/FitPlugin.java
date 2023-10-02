// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.fit.lib.FitReader;
import org.openstreetmap.josm.plugins.fit.lib.FitReaderOptions;
import org.openstreetmap.josm.plugins.fit.lib.global.HeartRateCadenceDistanceSpeed;

/**
 * The POJO entry point for JOSM
 */
public class FitPlugin extends Plugin {
    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public FitPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addImporter(new FitImporter());
    }

    private static final class FitImporter extends FileImporter {

        /**
         * Constructs a new {@code FitImporter} with the given extension file filter.
         */
        public FitImporter() {
            super(new ExtensionFileFilter("fit", "fit", tr("FIT Files (*.fit)")));
        }

        @Override
        public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                final var records = FitReader.read(inputStream, FitReaderOptions.TRY_TO_FINISH);
                final var gpxData = new GpxData(true);
                progressMonitor.beginTask(tr("Processing FIT records"), records.length);
                final var waypoints = new ArrayList<WayPoint>(records.length % 1000);
                for (int i = 0; i < records.length; i++) {
                    var r = records[i];
                    if (i % 1000 == 0) {
                        progressMonitor.worked(1);
                    }
                    if (r instanceof HeartRateCadenceDistanceSpeed(
                            Instant timestamp, double lat, double lon, double ele, short heartRate, short cadence,
                            int distance, int speed, long[][] unknown,
                            org.openstreetmap.josm.plugins.fit.lib.global.FitDevDataRecord devData
                    )) {
                        final var waypoint = new WayPoint(new LatLon(lat, lon));
                        waypoint.setInstant(timestamp);
                        waypoint.attr.putAll(Map.of("ele", ele, "heart_rate", heartRate,
                                "cadence", cadence, "distance", distance, "speed", speed,
                                "unknown", unknown, "devData", devData));
                        waypoints.add(waypoint);
                    }
                }
                waypoints.trimToSize();
                gpxData.addTrack(new GpxTrack(Collections.singleton(waypoints), Collections.emptyMap()));
                gpxData.endUpdate();
                MainApplication.getLayerManager().addLayer(new GpxLayer(gpxData, file.getName(), true));
            }
        }
    }
}
