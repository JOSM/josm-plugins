package org.openstreetmap.josm.plugins.elevation;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

/**
 * Allow drag-n-drop for hgt files
 * @author Taylor Smock
 *
 */
public class HgtFileImporter extends FileImporter {

    private static ExtensionFileFilter filter = ExtensionFileFilter.newFilterWithArchiveExtensions("hgt", "hgt", "HGT (SRTM) elevation files", true);

    protected HgtFileImporter() {
        super(filter);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        Bounds bounds = HgtReader.read(file);
        if (bounds != null && MainApplication.getMap() != null && MainApplication.getMap().mapView != null)
            MainApplication.getMap().mapView.zoomTo(bounds);
    }
}
