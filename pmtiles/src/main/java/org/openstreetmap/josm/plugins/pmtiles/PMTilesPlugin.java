// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.session.SessionReader;
import org.openstreetmap.josm.io.session.SessionWriter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.pmtiles.actions.downloadtasks.DownloadPMTilesTask;
import org.openstreetmap.josm.plugins.pmtiles.gui.io.importexport.PMTilesFileImporter;
import org.openstreetmap.josm.plugins.pmtiles.gui.io.session.PMTilesImageSessionExporter;
import org.openstreetmap.josm.plugins.pmtiles.gui.io.session.PMTilesImageSessionImporter;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesImageLayer;

/**
 * This is the plugin entrypoint
 */
public final class PMTilesPlugin extends Plugin {
    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public PMTilesPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addImporter(new PMTilesFileImporter());
        ExtensionFileFilter.updateAllFormatsImporter();
        MainApplication.getMenu().openLocation.addDownloadTaskClass(DownloadPMTilesTask.class);

        // Register session exporter and importer for PMTiles image layers
        SessionWriter.registerSessionLayerExporter(PMTilesImageLayer.class, PMTilesImageSessionExporter.class);
        SessionReader.registerSessionLayerImporter("pmtiles-image", PMTilesImageSessionImporter.class);
    }
}
