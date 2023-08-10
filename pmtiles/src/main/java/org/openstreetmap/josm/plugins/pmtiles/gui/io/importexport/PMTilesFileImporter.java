// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesImageLayer;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesMVTLayer;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;
import org.openstreetmap.josm.plugins.pmtiles.lib.TileType;

/**
 * Read PMTiles
 */
public class PMTilesFileImporter extends FileImporter {
    /**
     * Constructs a new {@link PMTilesFileImporter}
     */
    public PMTilesFileImporter() {
        super(new ExtensionFileFilter("pmtiles", "pmtiles", tr("PMTiles tilesets ({0})", ".pmtiles")));
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        final var header = PMTiles.readHeader(file.toURI());
        final var info = new PMTilesImageryInfo(header);
        if (header.tileType() == TileType.MVT) {
            MainApplication.getLayerManager().addLayer(new PMTilesMVTLayer(info));
        } else {
            MainApplication.getLayerManager().addLayer(new PMTilesImageLayer(info));
        }
    }
}
