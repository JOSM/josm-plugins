// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import org.geotools.referencing.CRS;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public abstract class DefaultGeographicHandler implements GeographicHandler {

    private boolean useNodeMap = true;
    private boolean checkNodeProximity;
    private boolean preferMultipolygonToSimpleWay;

    @Override
    public boolean preferMultipolygonToSimpleWay() {
        return preferMultipolygonToSimpleWay;
    }

    @Override
    public void setPreferMultipolygonToSimpleWay(boolean prefer) {
        preferMultipolygonToSimpleWay = prefer;
    }

    @Override
    public boolean checkNodeProximity() {
        return checkNodeProximity;
    }

    @Override
    public void setCheckNodeProximity(boolean check) {
        checkNodeProximity = check;
    }

    @Override
    public void setUseNodeMap(boolean use) {
        useNodeMap = use;
    }

    @Override
    public boolean useNodeMap() {
        return useNodeMap;
    }

    @Override
    public CoordinateReferenceSystem getCrsFor(String crsName) throws NoSuchAuthorityCodeException, FactoryException {
        if (crsName.equalsIgnoreCase("GCS_ETRS_1989")) {
            return CRS.decode("EPSG:4258");
        } else if (crsName.startsWith("EPSG:")) {
            return CRS.decode(crsName);
        }
        return null;
    }
}
