//    JOSM opendata plugin.
//    Copyright (C) 2011-2012 Don-vip
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.AbstractDerivedCRS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.operation.projection.LambertConformal;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransform;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.AbstractProjection;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic.Parameters;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic.Parameters1SP;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic.Parameters2SP;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.tools.Pair;

public class DefaultShpHandler implements ShpHandler, OdConstants {

	private static final List<Pair<org.opengis.referencing.datum.Ellipsoid, Ellipsoid>> 
		ellipsoids = new ArrayList<Pair<org.opengis.referencing.datum.Ellipsoid, Ellipsoid>>();
	static {
		ellipsoids.add(new Pair<org.opengis.referencing.datum.Ellipsoid, Ellipsoid>(DefaultEllipsoid.GRS80, Ellipsoid.GRS80));
		ellipsoids.add(new Pair<org.opengis.referencing.datum.Ellipsoid, Ellipsoid>(DefaultEllipsoid.WGS84, Ellipsoid.WGS84));
	}
	
	private static final Double get(ParameterValueGroup values, ParameterDescriptor desc) {
		return (Double) values.parameter(desc.getName().getCode()).getValue();
	}
	
	private static final boolean equals(Double a, Double b) {
		boolean res = Math.abs(a - b) <= Main.pref.getDouble(PREF_CRS_COMPARISON_TOLERANCE, DEFAULT_CRS_COMPARISON_TOLERANCE);
		if (Main.pref.getBoolean(PREF_CRS_COMPARISON_DEBUG, false)) {
			System.out.println("Comparing "+a+" and "+b+" -> "+res);
		}
		return res; 
	}
	
	private boolean checkNodeProximity = false;
	private boolean preferMultipolygonToSimpleWay = false;

	@Override
	public MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS,
			CoordinateReferenceSystem targetCRS, boolean lenient)
			throws FactoryException {
		if (sourceCRS instanceof GeographicCRS && sourceCRS.getName().getCode().equalsIgnoreCase("GCS_ETRS_1989")) {
			return CRS.findMathTransform(CRS.decode("EPSG:4258"), targetCRS, lenient);
		} else if (sourceCRS instanceof AbstractDerivedCRS && sourceCRS.getName().getCode().equalsIgnoreCase("Lambert_Conformal_Conic")) {
			List<MathTransform> result = new ArrayList<MathTransform>();
			AbstractDerivedCRS crs = (AbstractDerivedCRS) sourceCRS;
			MathTransform transform = crs.getConversionFromBase().getMathTransform();
			if (transform instanceof LambertConformal && crs.getDatum() instanceof GeodeticDatum) {
				LambertConformal lambert = (LambertConformal) transform;
				GeodeticDatum geo = (GeodeticDatum) crs.getDatum();
				for (Projection p : Projections.getProjections()) {
					if (p instanceof AbstractProjection) {
						AbstractProjection ap = (AbstractProjection) p;
						if (ap.getProj() instanceof LambertConformalConic) {
							for (Pair<org.opengis.referencing.datum.Ellipsoid, Ellipsoid> pair : ellipsoids) {
								if (pair.a.equals(geo.getEllipsoid()) && pair.b.equals(ap.getEllipsoid())) {
									boolean ok = true;
									ParameterValueGroup values = lambert.getParameterValues();
									Parameters params = ((LambertConformalConic) ap.getProj()).getParameters();
									
									ok = ok ? equals(get(values, AbstractProvider.LATITUDE_OF_ORIGIN), params.latitudeOrigin) : ok;
									ok = ok ? equals(get(values, AbstractProvider.CENTRAL_MERIDIAN), ap.getCentralMeridian()) : ok;
									ok = ok ? equals(get(values, AbstractProvider.SCALE_FACTOR), ap.getScaleFactor()) : ok;
									ok = ok ? equals(get(values, AbstractProvider.FALSE_EASTING), ap.getFalseEasting()) : ok;
									ok = ok ? equals(get(values, AbstractProvider.FALSE_NORTHING), ap.getFalseNorthing()) : ok;
									
									if (lambert instanceof LambertConformal2SP && params instanceof Parameters2SP) {
										Parameters2SP param = (Parameters2SP) params;
										ok = ok ? equals(Math.min(get(values, AbstractProvider.STANDARD_PARALLEL_1),get(values, AbstractProvider.STANDARD_PARALLEL_2)), 
														 Math.min(param.standardParallel1, param.standardParallel2)) : ok;
										ok = ok ? equals(Math.max(get(values, AbstractProvider.STANDARD_PARALLEL_1), get(values, AbstractProvider.STANDARD_PARALLEL_2)),
												         Math.max(param.standardParallel1, param.standardParallel2)) : ok;
										
									} else if (!(lambert instanceof LambertConformal1SP && params instanceof Parameters1SP)) {
										ok = false;
									}

									if (ok) {
										try {
											result.add(CRS.findMathTransform(CRS.decode(p.toCode()), targetCRS, lenient));
										} catch (FactoryException e) {
											System.err.println(e.getMessage());
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
					System.err.println("Found multiple projections !"); // TODO: something
				}
				return result.get(0);
			}
		}
		return null;
	}

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
}
