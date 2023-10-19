// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.AbstractDerivedCRS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.operation.projection.LambertConformal;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.api.parameter.ParameterDescriptor;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.GeodeticDatum;
import org.geotools.api.referencing.operation.MathTransform;
import org.openstreetmap.josm.data.projection.AbstractProjection;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic.Parameters1SP;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic.Parameters2SP;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * A common handler for geotools
 */
public interface GeotoolsHandler extends GeographicHandler {
    /**
     * A mapping of GeoTools ellipsoid to JOSM ellipsoids. Don't use outside the {@link GeotoolsHandler} class.
     */
    List<Pair<org.geotools.api.referencing.datum.Ellipsoid, Ellipsoid>>
            ellipsoids = Collections.unmodifiableList(Arrays.asList(
            new Pair<>(DefaultEllipsoid.GRS80, Ellipsoid.GRS80),
            new Pair<>(DefaultEllipsoid.WGS84, Ellipsoid.WGS84)
    ));

    @Override
    default MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, boolean lenient)
            throws FactoryException {
        if (getCrsFor(sourceCRS.getName().getCode()) != null) {
            return CRS.findMathTransform(getCrsFor(sourceCRS.getName().getCode()), targetCRS, lenient);
        } else if (sourceCRS instanceof AbstractDerivedCRS && "Lambert_Conformal_Conic".equalsIgnoreCase(sourceCRS.getName().getCode())) {
            List<MathTransform> result = new ArrayList<>();
            AbstractDerivedCRS crs = (AbstractDerivedCRS) sourceCRS;
            MathTransform transform = crs.getConversionFromBase().getMathTransform();
            if (transform instanceof LambertConformal && crs.getDatum() instanceof GeodeticDatum) {
                LambertConformal lambert = (LambertConformal) transform;
                GeodeticDatum geo = (GeodeticDatum) crs.getDatum();
                for (ProjectionChoice choice : ProjectionPreference.getProjectionChoices()) {
                    Projection p = choice.getProjection();
                    if (p instanceof AbstractProjection) {
                        AbstractProjection ap = (AbstractProjection) p;
                        if (ap.getProj() instanceof LambertConformalConic) {
                            for (Pair<org.geotools.api.referencing.datum.Ellipsoid, Ellipsoid> pair : ellipsoids) {
                                if (pair.a.equals(geo.getEllipsoid()) && pair.b.equals(ap.getEllipsoid())) {
                                    boolean ok = true;
                                    ParameterValueGroup values = lambert.getParameterValues();
                                    LambertConformalConic.Parameters params = ((LambertConformalConic) ap.getProj()).getParameters();

                                    ok = ok ? equals(get(values, AbstractProvider.LATITUDE_OF_ORIGIN), params.latitudeOrigin) : ok;
                                    ok = ok ? equals(get(values, AbstractProvider.CENTRAL_MERIDIAN), ap.getCentralMeridian()) : ok;
                                    ok = ok ? equals(get(values, AbstractProvider.SCALE_FACTOR), ap.getScaleFactor()) : ok;
                                    ok = ok ? equals(get(values, AbstractProvider.FALSE_EASTING), ap.getFalseEasting()) : ok;
                                    ok = ok ? equals(get(values, AbstractProvider.FALSE_NORTHING), ap.getFalseNorthing()) : ok;

                                    if (lambert instanceof LambertConformal2SP && params instanceof Parameters2SP) {
                                        Parameters2SP param = (Parameters2SP) params;
                                        ok = ok ? equals(Math.min(get(values, AbstractProvider.STANDARD_PARALLEL_1),
                                                        get(values, AbstractProvider.STANDARD_PARALLEL_2)),
                                                Math.min(param.standardParallel1, param.standardParallel2)) : ok;
                                        ok = ok ? equals(Math.max(get(values, AbstractProvider.STANDARD_PARALLEL_1),
                                                        get(values, AbstractProvider.STANDARD_PARALLEL_2)),
                                                Math.max(param.standardParallel1, param.standardParallel2)) : ok;

                                    } else if (!(lambert instanceof LambertConformal1SP && params instanceof Parameters1SP)) {
                                        ok = false;
                                    }

                                    if (ok) {
                                        try {
                                            result.add(CRS.findMathTransform(CRS.decode(p.toCode()), targetCRS, lenient));
                                        } catch (FactoryException e) {
                                            Logging.error(e.getMessage());
                                            Logging.debug(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!result.isEmpty()) {
                if (result.size() > 1) {
                    Logging.warn("Found multiple projections !"); // TODO: something
                }
                return result.get(0);
            }
        }
        return null;
    }

    /**
     * Get a value from a group of values
     * @param values The values to look at
     * @param desc The description of the value to get
     * @return The value
     */
    static Double get(ParameterValueGroup values, ParameterDescriptor<?> desc) {
        return (Double) values.parameter(desc.getName().getCode()).getValue();
    }

    /**
     * Check if two doubles are equal to within the {@link OdConstants#PREF_CRS_COMPARISON_TOLERANCE}
     * @param a The first double
     * @param b The second double
     * @return {@code true} if the difference is less than the tolerance
     */
    static boolean equals(Double a, Double b) {
        boolean res = Math.abs(a - b) <= Config.getPref().getDouble(
                OdConstants.PREF_CRS_COMPARISON_TOLERANCE, OdConstants.DEFAULT_CRS_COMPARISON_TOLERANCE);
        if (Config.getPref().getBoolean(OdConstants.PREF_CRS_COMPARISON_DEBUG, false)) {
            Logging.debug("Comparing "+a+" and "+b+" -> "+res);
        }
        return res;
    }
}
