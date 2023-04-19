// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.nio.charset.Charset;
import java.util.Set;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * The default shapefile handler
 */
public class DefaultShpHandler extends DefaultGeographicHandler implements ShpHandler, GeotoolsHandler {

    private Charset dbfCharset;

    @Override
    public void notifyFeatureParsed(Object feature, DataSet result, Set<OsmPrimitive> featurePrimitives) {
        // To be overridden by modules handlers
    }

    @Override
    public void setDbfCharset(Charset charset) {
        dbfCharset = charset;
    }

    @Override
    public Charset getDbfCharset() {
        return dbfCharset;
    }
}
