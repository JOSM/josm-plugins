/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2000-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.measure;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;


/**
 * A converter from fractional degrees to sexagesimal degrees.
 * Sexagesimal degrees are pseudo-unit in the format
 *
 * <cite>sign - degrees - decimal point - minutes (two digits) - integer seconds (two digits) -
 * fraction of seconds (any precision)</cite>.
 *
 * Unfortunatly, this pseudo-unit is extensively used in the EPSG database.
 *
 * @since 2.1
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/referencing/src/main/java/org/geotools/measure/SexagesimalConverter.java $
 * @version $Id: SexagesimalConverter.java 30776 2008-06-20 17:00:11Z desruisseaux $
 * @author Martin Desruisseaux (IRD)
 */
class SexagesimalConverter extends UnitConverter {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 3873494343412121773L;

    /**
     * Small tolerance factor for rounding errors.
     */
    private static final double EPS = 1E-8;

    /**
     * The value to divide DMS unit by.
     * For "degree minute second" (EPSG code 9107), this is 1.
     * For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    final int divider;

    /**
     * The inverse of this converter.
     */
    private final UnitConverter inverse;

    /**
     * Constructs a converter for sexagesimal units.
     *
     * @param divider The value to divide DMS unit by.
     *        For "degree minute second" (EPSG code 9107), this is 1.
     *        For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    private SexagesimalConverter(final int divider) {
        this.divider = divider;
        this.inverse = new Inverse(this);
    }

    /**
     * Constructs a converter for sexagesimal units.
     * This constructor is for {@link Inverse} usage only.
     */
    private SexagesimalConverter(final int divider, final UnitConverter inverse) {
        this.divider = divider;
        this.inverse = inverse;
    }

    /**
     * Returns the inverse of this converter.
     */
    public final UnitConverter inverse() {
        return inverse;
    }

    /**
     * Performs a conversion from fractional degrees to sexagesimal degrees.
     */
    public double convert(double value) throws ConversionException {
        final int deg,min,sec;  deg = (int) value; // Round toward 0
        value = (value-deg)*60; min = (int) value; // Round toward 0
        value = (value-min)*60; sec = (int) value; // Round toward 0
        value -= sec;          // The remainer (fraction of seconds)
        return (((deg*100 + min)*100 + sec) + value)/divider;
    }

    /**
     * Returns {@code false} since this converter is non-linear.
     */
    public final boolean isLinear() {
        return false;
    }

    /**
     * Compares this converter with the specified object.
     */
    @Override
    public final boolean equals(final Object object) {
        return object != null && object.getClass().equals(getClass()) &&
                ((SexagesimalConverter) object).divider == divider;
    }

    /**
     * Returns a hash value for this converter.
     */
    @Override
    public int hashCode() {
        return (int) serialVersionUID + divider;
    }

    /**
     * The inverse of {@link SexagesimalConverter}.
     */
    private static final class Inverse extends SexagesimalConverter {
        /**
         * Serial number for compatibility with different versions.
         */
        private static final long serialVersionUID = -7171869900634417819L;

        /**
         * Constructs a converter.
         */
        public Inverse(final SexagesimalConverter inverse) {
            super(inverse.divider, inverse);
        }

        /**
         * Performs a conversion from sexagesimal degrees to fractional degrees.
         */
        @Override
        public double convert(double value) throws ConversionException {
            value *= this.divider;
            int deg,min;
            deg = (int) (value/10000); value -= 10000*deg;
            min = (int) (value/  100); value -=   100*min;
            if (min<=-60 || min>=60) {  // Accepts NaN
                if (Math.abs(Math.abs(min) - 100) <= EPS) {
                    if (min >= 0) deg++; else deg--;
                    min = 0;
                } else {
                    throw new ConversionException("Invalid minutes: "+min);
                }
            }
            if (value<=-60 || value>=60) { // Accepts NaN
                if (Math.abs(Math.abs(value) - 100) <= EPS) {
                    if (value >= 0) min++; else min--;
                    value = 0;
                } else {
                    throw new ConversionException("Invalid secondes: "+value);
                }
            }
            value = ((value/60) + min)/60 + deg;
            return value;
        }

        /**
         * Returns a hash value for this converter.
         */
        @Override
        public int hashCode() {
            return (int) serialVersionUID + divider;
        }
    }
}
