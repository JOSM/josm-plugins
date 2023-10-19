// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;

public interface GeographicHandler {

    void setPreferMultipolygonToSimpleWay(boolean prefer);

    boolean preferMultipolygonToSimpleWay();

    void setCheckNodeProximity(boolean check);

    boolean checkNodeProximity();

    void setUseNodeMap(boolean use);

    boolean useNodeMap();

    CoordinateReferenceSystem getCrsFor(String crsName) throws NoSuchAuthorityCodeException, FactoryException;

    MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, boolean lenient)
            throws FactoryException;
}
