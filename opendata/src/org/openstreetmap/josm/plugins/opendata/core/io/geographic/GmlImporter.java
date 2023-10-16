// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.io.AbstractImporter;

public class GmlImporter extends AbstractImporter {
    
    public static final ExtensionFileFilter GML_FILE_FILTER = new ExtensionFileFilter(
            OdConstants.GML_EXT, OdConstants.GML_EXT, tr("GML files") + " (*."+OdConstants.GML_EXT+")");

    public GmlImporter() {
        super(GML_FILE_FILTER);
    }

    @Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor instance)
            throws IllegalDataException {
        try {
            return GmlReader.parseDataSet(in, handler, instance);
        } catch (IOException | XMLStreamException e) {
            throw new IllegalDataException(e);
        }
    }
}
