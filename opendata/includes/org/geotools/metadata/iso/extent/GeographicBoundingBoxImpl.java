/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.metadata.iso.extent;

import static java.lang.Double.doubleToLongBits;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Locale;

import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.util.Utilities;
import org.opengis.metadata.extent.GeographicBoundingBox;


/**
 * Geographic position of the dataset. This is only an approximate so specifying the coordinate
 * reference system is unnecessary. The CRS shall be geographic with Greenwich prime meridian,
 * but the datum doesn't need to be WGS84.
 *
 * @since 2.1
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/metadata/iso/extent/GeographicBoundingBoxImpl.java $
 * @version $Id: GeographicBoundingBoxImpl.java 37298 2011-05-25 05:16:15Z mbedward $
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane
 */
public class GeographicBoundingBoxImpl extends GeographicExtentImpl
        implements GeographicBoundingBox
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3278089380004172514L;

    /**
     * The method for constructing a string representation of this box.
     * Will be obtained only when first needed.
     */
    private static Method toString;

    /**
     * A bounding box ranging from 180°W to 180°E and 90°S to 90°N.
     *
     * @since 2.2
     */
    public static final GeographicBoundingBox WORLD;
    static {
        final GeographicBoundingBoxImpl world = new GeographicBoundingBoxImpl(-180, 180, -90, 90);
        world.freeze();
        WORLD = world;
    }

    /**
     * The western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private double westBoundLongitude;

    /**
     * The eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private double eastBoundLongitude;

    /**
     * The southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private double southBoundLatitude;

    /**
     * The northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private double northBoundLatitude;

    /**
     * Constructs an initially empty geographic bounding box.
     */
    public GeographicBoundingBoxImpl() {
    }

    /**
     * Constructs a geographic bounding box initialized to the same values than the specified one.
     *
     * @param box The existing box to use for initializing this geographic bounding box.
     *
     * @since 2.2
     */
    public GeographicBoundingBoxImpl(final GeographicBoundingBox box) {
        /*
         * We could invokes super(box), but we will perform the assignations explicitly here
         * for performance reason. Warning: it may be a problem if the user creates a subclass
         * and relies on the default MetadataEntity(Object) behavior. Rather than bothering
         * the user with a javadoc warning, I would prefer to find some trick to avoid this
         * issue (todo).
         */
        super();
        setBounds(box);
    }

    /**
     * Creates a geographic bounding box initialized to the specified values.
     * <p>
     * <strong>Caution:</strong> Arguments are expected in the same order than they appear in the
     * ISO 19115 specification. This is different than the order commonly found in Java world,
     * which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).
     *
     * @param westBoundLongitude The minimal <var>x</var> value.
     * @param eastBoundLongitude The maximal <var>x</var> value.
     * @param southBoundLatitude The minimal <var>y</var> value.
     * @param northBoundLatitude The maximal <var>y</var> value.
     */
    public GeographicBoundingBoxImpl(final double westBoundLongitude,
                                     final double eastBoundLongitude,
                                     final double southBoundLatitude,
                                     final double northBoundLatitude)
    {
        super(true);
        setBounds(westBoundLongitude, eastBoundLongitude,
                  southBoundLatitude, northBoundLatitude);
    }

    /**
     * Returns the western-most coordinate of the limit of the
     * dataset extent. The value is expressed in longitude in
     * decimal degrees (positive east).
     *
     * @return The western-most longitude between -180 and +180°.
     */
    public double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * Returns the eastern-most coordinate of the limit of the
     * dataset extent. The value is expressed in longitude in
     * decimal degrees (positive east).
     *
     * @return The eastern-most longitude between -180 and +180°.
     */
    public double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * Returns the southern-most coordinate of the limit of the
     * dataset extent. The value is expressed in latitude in
     * decimal degrees (positive north).
     *
     * @return The southern-most latitude between -90 and +90°.
     */
    public double getSouthBoundLatitude()  {
        return southBoundLatitude;
    }

    /**
     * Returns the northern-most, coordinate of the limit of the
     * dataset extent. The value is expressed in latitude in
     * decimal degrees (positive north).
     *
     * @return The northern-most latitude between -90 and +90°.
     */
    public double getNorthBoundLatitude()   {
        return northBoundLatitude;
    }

    /**
     * Sets the bounding box to the specified values.
     * <p>
     * <strong>Caution:</strong> Arguments are expected in the same order than they appear in the
     * ISO 19115 specification. This is different than the order commonly found in Java world,
     * which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).
     *
     * @param westBoundLongitude The minimal <var>x</var> value.
     * @param eastBoundLongitude The maximal <var>x</var> value.
     * @param southBoundLatitude The minimal <var>y</var> value.
     * @param northBoundLatitude The maximal <var>y</var> value.
     *
     * @since 2.5
     */
    public synchronized void setBounds(final double westBoundLongitude,
                                       final double eastBoundLongitude,
                                       final double southBoundLatitude,
                                       final double northBoundLatitude)
    {
        checkWritePermission();
        this.westBoundLongitude = westBoundLongitude;
        this.eastBoundLongitude = eastBoundLongitude;
        this.southBoundLatitude = southBoundLatitude;
        this.northBoundLatitude = northBoundLatitude;
    }

    /**
     * Sets the bounding box to the same values than the specified box.
     *
     * @param box The geographic bounding box to use for setting the values of this box.
     *
     * @since 2.5
     */
    public void setBounds(final GeographicBoundingBox box) {
        ensureNonNull("box", box);
        setInclusion(box.getInclusion());
        setBounds(box.getWestBoundLongitude(), box.getEastBoundLongitude(),
                  box.getSouthBoundLatitude(), box.getNorthBoundLatitude());
    }

    /**
     * Adds a geographic bounding box to this box. If the {@linkplain #getInclusion inclusion}
     * status is the same for this box and the box to be added, then the resulting bounding box
     * is the union of the two boxes. If the {@linkplain #getInclusion inclusion} status are
     * opposite (<cite>exclusion</cite>), then this method attempt to exclude some area of
     * specified box from this box. The resulting bounding box is smaller if the exclusion can
     * be performed without ambiguity.
     *
     * @param box The geographic bounding box to add to this box.
     *
     * @since 2.2
     */
    public synchronized void add(final GeographicBoundingBox box) {
        checkWritePermission();
        final double xmin = box.getWestBoundLongitude();
        final double xmax = box.getEastBoundLongitude();
        final double ymin = box.getSouthBoundLatitude();
        final double ymax = box.getNorthBoundLatitude();
        /*
         * Reminder: 'inclusion' is a mandatory attribute, so it should never be null for a
         * valid metadata object.  If the metadata object is invalid, it is better to get a
         * an exception than having a code doing silently some inappropriate work.
         */
        final Boolean inc1 =     getInclusion(); ensureNonNull("inclusion", inc1);
        final Boolean inc2 = box.getInclusion(); ensureNonNull("inclusion", inc2);
        if (inc1.booleanValue() == inc2.booleanValue()) {
            if (xmin < westBoundLongitude) westBoundLongitude = xmin;
            if (xmax > eastBoundLongitude) eastBoundLongitude = xmax;
            if (ymin < southBoundLatitude) southBoundLatitude = ymin;
            if (ymax > northBoundLatitude) northBoundLatitude = ymax;
        } else {
            if (ymin <= southBoundLatitude && ymax >= northBoundLatitude) {
                if (xmin > westBoundLongitude) westBoundLongitude = xmin;
                if (xmax < eastBoundLongitude) eastBoundLongitude = xmax;
            }
            if (xmin <= westBoundLongitude && xmax >= eastBoundLongitude) {
                if (ymin > southBoundLatitude) southBoundLatitude = ymin;
                if (ymax < northBoundLatitude) northBoundLatitude = ymax;
            }
        }
    }

    /**
     * Compares this geographic bounding box with the specified object for equality.
     *
     * @param object The object to compare for equality.
     * @return {@code true} if the given object is equals to this box.
     */
    @Override
    public synchronized boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        // Above code really requires GeographicBoundingBoxImpl.class, not getClass().
        if (object!=null && object.getClass().equals(GeographicBoundingBoxImpl.class)) {
            final GeographicBoundingBoxImpl that = (GeographicBoundingBoxImpl) object;
            return Utilities.equals(this.getInclusion(), that.getInclusion()) &&
                   doubleToLongBits(this.southBoundLatitude) ==
                   doubleToLongBits(that.southBoundLatitude) &&
                   doubleToLongBits(this.northBoundLatitude) ==
                   doubleToLongBits(that.northBoundLatitude) &&
                   doubleToLongBits(this.eastBoundLongitude) ==
                   doubleToLongBits(that.eastBoundLongitude) &&
                   doubleToLongBits(this.westBoundLongitude) ==
                   doubleToLongBits(that.westBoundLongitude);
        }
        return super.equals(object);
    }

    /**
     * Returns a hash code value for this extent.
     *
     * @todo Consider relying on the default implementation, since it cache the hash code.
     */
    @Override
    public synchronized int hashCode() {
        if (!getClass().equals(GeographicBoundingBoxImpl.class)) {
            return super.hashCode();
        }
        final Boolean inclusion = getInclusion();
        int code = (inclusion != null) ? inclusion.hashCode() : 0;
        code += hashCode(southBoundLatitude);
        code += hashCode(northBoundLatitude);
        code += hashCode(eastBoundLongitude);
        code += hashCode(westBoundLongitude);
        return code;
    }

    /**
     * Returns a hash code value for the specified {@code double}.
     */
    private static int hashCode(final double value) {
        final long code = doubleToLongBits(value);
        return (int)code ^ (int)(code >>> 32);
    }

    /**
     * Returns a string representation of this extent using a default angle pattern.
     */
    @Override
    public String toString() {
        return toString(this, "DD°MM'SS.s\"", null);
    }

    /**
     * Returns a string representation of the specified extent using the specified angle pattern
     * and locale. See {@link AngleFormat} for a description of angle patterns.
     *
     * @param box     The bounding box to format.
     * @param pattern The angle pattern (e.g. {@code DD°MM'SS.s"}.
     * @param locale  The locale, or {@code null} for the default one.
     * @return A string representation of the given box in the given locale.
     *
     * @since 2.2
     */
    public static String toString(final GeographicBoundingBox box,
                                  final String                pattern,
                                  final Locale                locale)
    {
        if (toString == null) {
            // No need to synchronize.
            toString = getMethod("toString",  new Class[] {
                        GeographicBoundingBox.class, String.class, Locale.class});
        }
        try {
            return String.valueOf(invoke(toString, new Object[] {box, pattern, locale}));
        } catch (InvocationTargetException exception) {
            throw new UndeclaredThrowableException(exception.getTargetException());
        }
    }

    /**
     * Returns a helper method which depends on the referencing module. We use reflection
     * since we can't have a direct dependency to this module.
     */
    private static Method getMethod(final String name, final Class<?>[] arguments) {
        try {
            return Class.forName("org.geotools.resources.BoundingBoxes").getMethod(name, arguments);
        } catch (ClassNotFoundException exception) {
            throw new UnsupportedOperationException(Errors.format(
                    ErrorKeys.MISSING_MODULE_$1, "referencing"), exception);
        } catch (NoSuchMethodException exception) {
            // Should never happen if we didn't broke our BoundingBoxes helper class.
            throw new AssertionError(exception);
        }
    }

    /**
     * Invokes the specified method with the specified arguments.
     */
    private static Object invoke(final Method method, final Object[] arguments)
            throws InvocationTargetException
    {
        try {
            return method.invoke(null, arguments);
        } catch (IllegalAccessException exception) {
            // Should never happen if our BoundingBoxes helper class is not broken.
            throw new AssertionError(exception);
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw exception;
        }
    }
}
