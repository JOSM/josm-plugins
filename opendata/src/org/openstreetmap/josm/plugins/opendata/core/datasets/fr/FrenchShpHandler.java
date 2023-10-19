// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.datasets.fr;

import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.datum.GeodeticDatum;
import org.geotools.api.referencing.operation.MathTransform;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.DefaultShpHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeotoolsHandler;

/**
 * A handler for french-specific shapefiles
 */
public class FrenchShpHandler extends DefaultShpHandler {

    @Override
    public CoordinateReferenceSystem getCrsFor(String crsName) throws FactoryException {
        if ("RGM04".equalsIgnoreCase(crsName)) {
            return CRS.decode("EPSG:4471");
        } else if ("RGFG95_UTM_Zone_22N".equalsIgnoreCase(crsName)) {
            return CRS.decode("EPSG:2972");
        } else {
            return super.getCrsFor(crsName);
        }
    }

    @Override
    public MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, boolean lenient)
            throws FactoryException {
        if ("Lambert I Nord".equalsIgnoreCase(sourceCRS.getName().getCode()) && sourceCRS instanceof ProjectedCRS) {
            GeodeticDatum datum = ((ProjectedCRS) sourceCRS).getDatum();
            if (datum.getPrimeMeridian().getGreenwichLongitude() > 0.0
                    && ((ProjectedCRS) sourceCRS).getConversionFromBase().getMathTransform() instanceof LambertConformal2SP) {
                LambertConformal2SP lambert = (LambertConformal2SP) ((ProjectedCRS) sourceCRS).getConversionFromBase().getMathTransform();
                Double falseNorthing = GeotoolsHandler.get(lambert.getParameterValues(), AbstractProvider.FALSE_NORTHING);
                Double centralmeridian = GeotoolsHandler.get(lambert.getParameterValues(), AbstractProvider.CENTRAL_MERIDIAN);
                if (centralmeridian.equals(0.0)) {
                    if (falseNorthing.equals(200000.0)) {
                        return CRS.findMathTransform(CRS.decode("EPSG:27561"), targetCRS, lenient);
                    } else if (falseNorthing.equals(1200000.0)) {
                        return CRS.findMathTransform(CRS.decode("EPSG:27571"), targetCRS, lenient);
                    }
                }
            }
        }
        return super.findMathTransform(sourceCRS, targetCRS, lenient);
    }
}
