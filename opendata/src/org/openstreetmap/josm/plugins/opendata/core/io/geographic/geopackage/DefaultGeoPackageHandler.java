// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic.geopackage;

import org.openstreetmap.josm.plugins.opendata.core.io.geographic.DefaultGeographicHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeotoolsHandler;

/**
 * The default handler for GeoPackages
 * @author Taylor Smock
 */
public class DefaultGeoPackageHandler extends DefaultGeographicHandler implements GeoPackageHandler, GeotoolsHandler {
    private boolean preferMultipolygonToSimpleWay;
    private boolean checkNodeProximity;
    private boolean useNodeMap;
    @Override
    public void setPreferMultipolygonToSimpleWay(boolean prefer) {
        this.preferMultipolygonToSimpleWay = prefer;
    }

    @Override
    public boolean preferMultipolygonToSimpleWay() {
        return this.preferMultipolygonToSimpleWay;
    }

    @Override
    public void setCheckNodeProximity(boolean check) {
        this.checkNodeProximity = check;
    }

    @Override
    public boolean checkNodeProximity() {
        return this.checkNodeProximity;
    }

    @Override
    public void setUseNodeMap(boolean use) {
        this.useNodeMap = use;
    }

    @Override
    public boolean useNodeMap() {
        return this.useNodeMap;
    }
}
