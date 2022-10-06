// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic.geopackage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.io.AbstractImporter;

public class GeoPackageImporter extends AbstractImporter {
    public static final ExtensionFileFilter GEOPACKAGE_FILE_FILTER = new ExtensionFileFilter(
            OdConstants.GEOPACKAGE_EXT, OdConstants.GEOPACKAGE_EXT, tr("Shapefiles") + " (*."+ OdConstants.GEOPACKAGE_EXT+")");
    public GeoPackageImporter() {
        super(GEOPACKAGE_FILE_FILTER);
    }

    @Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        try {
            return GeoPackageReader.parseDataSet(in, file, handler, progressMonitor);
        } catch (IOException e) {
            throw new IllegalDataException(e);
        }
    }
}
