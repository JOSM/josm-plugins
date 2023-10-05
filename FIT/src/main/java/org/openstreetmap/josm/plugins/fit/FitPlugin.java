// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
import org.openstreetmap.josm.plugins.fit.lib.global.FitEvent;
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
            try (var inputStream = Files.newInputStream(file.toPath())) {
                final var records = FitReader.read(inputStream, FitReaderOptions.TRY_TO_FINISH);
                final var gpxData = new GpxData(true);
                progressMonitor.beginTask(tr("Processing FIT records"), records.length);
                final var waypoints = new ArrayList<WayPoint>(records.length % 1000);
                for (var i = 0; i < records.length; i++) {
                    var r = records[i];
                    if (i % 1000 == 0) {
                        progressMonitor.worked(1);
                    }
                    if (r instanceof HeartRateCadenceDistanceSpeed heartRateCadenceDistanceSpeed) {
                        final var lat = heartRateCadenceDistanceSpeed.lat();
                        final var lon = heartRateCadenceDistanceSpeed.lon();
                        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                            final var waypoint = new WayPoint(new LatLon(lat, lon));
                            waypoint.setInstant(heartRateCadenceDistanceSpeed.timestamp());
                            // Use a sorted map for consistency
                            final var map = new TreeMap<String, Object>();
                            for (RecordComponent component : HeartRateCadenceDistanceSpeed.class
                                    .getRecordComponents()) {
                                if (Arrays.asList("lat", "lon", "timestamp", "unknown").contains(component.getName())) {
                                    continue; // skip information that has specific fields
                                }
                                try {
                                    map.put(component.getName(),
                                            component.getAccessor().invoke(heartRateCadenceDistanceSpeed));
                                } catch (ReflectiveOperationException e) {
                                    // This should never happen; the component accessors should _always_ be public.
                                    throw new IOException(e);
                                }
                            }
                            map.put("unknown", Arrays.deepToString(heartRateCadenceDistanceSpeed.unknown()));
                            waypoint.attr.putAll(map);
                            if (!waypoints.isEmpty() && Math.abs(waypoints.getLast().getInstant().getEpochSecond()
                                    - waypoint.getInstant().getEpochSecond()) > TimeUnit.DAYS.toDays(365)) {
                                createTrack(gpxData, new ArrayList<>(waypoints));
                                waypoints.clear();
                            } else {
                                waypoints.add(waypoint);
                            }
                        }
                    } else if (r instanceof FitEvent) {
                        // break up the events. It would be better to only do this on lap events.
                        gpxData.addTrack(new GpxTrack(Collections.singleton(new ArrayList<>(waypoints)),
                                Collections.emptyMap()));
                        waypoints.clear();
                    }
                }
                waypoints.trimToSize();
                createTrack(gpxData, waypoints);
                gpxData.endUpdate();
                MainApplication.getLayerManager().addLayer(new GpxLayer(gpxData, file.getName(), true));
            }
        }

        private static void createTrack(GpxData gpxData, ArrayList<WayPoint> waypoints) {
            if (waypoints.size() > 1) {
                gpxData.addTrack(new GpxTrack(Collections.singleton(waypoints), Collections.emptyMap()));
            } else if (!waypoints.isEmpty()) {
                gpxData.addWaypoint(waypoints.get(0));
            }
        }
    }
}
