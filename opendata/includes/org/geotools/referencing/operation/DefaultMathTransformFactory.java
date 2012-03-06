/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2001-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.referencing.operation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.measure.converter.ConversionException;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.geotools.factory.FactoryRegistry;
import org.geotools.factory.Hints;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.Parameters;
import org.geotools.referencing.cs.AbstractCS;
import org.geotools.referencing.factory.ReferencingFactory;
import org.geotools.referencing.operation.matrix.MatrixFactory;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.PassThroughTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.resources.CRSUtilities;
import org.geotools.resources.LazySet;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.util.CanonicalSet;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Operation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;


/**
 * Low level factory for creating {@linkplain MathTransform math transforms}.
 * Many high level GIS applications will never need to use this factory directly;
 * they can use a {@linkplain DefaultCoordinateOperationFactory coordinate operation factory}
 * instead. However, the {@code MathTransformFactory} interface can be used directly
 * by applications that wish to transform other types of coordinates (e.g. color coordinates,
 * or image pixel coordinates).
 * <P>
 * A {@linkplain MathTransform math transform} is an object that actually does
 * the work of applying formulae to coordinate values. The math transform does
 * not know or care how the coordinates relate to positions in the real world.
 * This lack of semantics makes implementing {@code MathTransformFactory}
 * significantly easier than it would be otherwise.
 *
 * For example the affine transform applies a matrix to the coordinates
 * without knowing how what it is doing relates to the real world. So if
 * the matrix scales <var>Z</var> values by a factor of 1000, then it could
 * be converting meters into millimeters, or it could be converting kilometers
 * into meters.
 * <P>
 * Because {@linkplain MathTransform math transforms} have low semantic value
 * (but high mathematical value), programmers who do not have much knowledge
 * of how GIS applications use coordinate systems, or how those coordinate
 * systems relate to the real world can implement {@code MathTransformFactory}.
 * The low semantic content of {@linkplain MathTransform math transforms} also
 * means that they will be useful in applications that have nothing to do with
 * GIS coordinates. For example, a math transform could be used to map color
 * coordinates between different color spaces, such as converting (red, green, blue)
 * colors into (hue, light, saturation) colors.
 * <P>
 * Since a {@linkplain MathTransform math transform} does not know what its source
 * and target coordinate systems mean, it is not necessary or desirable for a math
 * transform object to keep information on its source and target coordinate systems.
 *
 * @since 2.1
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/referencing/src/main/java/org/geotools/referencing/operation/DefaultMathTransformFactory.java $
 * @version $Id: DefaultMathTransformFactory.java 37299 2011-05-25 05:21:24Z mbedward $
 * @author Martin Desruisseaux (IRD)
 *
 * @tutorial http://docs.codehaus.org/display/GEOTOOLS/Coordinate+Transformation+Parameters
 */
public class DefaultMathTransformFactory extends ReferencingFactory implements MathTransformFactory {
    /**
     * The hints to provide to math transform providers. Null for now, but may be non-null
     * in some future version.
     */
    private static final Hints HINTS = null;

    /**
     * The last value returned by {@link #getProvider}. Stored as an
     * optimization since the same provider is often asked many times.
     */
    private transient MathTransformProvider lastProvider;

    /**
     * The operation method for the last transform created.
     */
    private static final ThreadLocal<OperationMethod> lastMethod = new ThreadLocal<OperationMethod>();

    /**
     * A pool of math transform. This pool is used in order to
     * returns instance of existing math transforms when possible.
     */
    private final CanonicalSet<MathTransform> pool;

    /**
     * The service registry for finding {@link MathTransformProvider} implementations.
     */
    private final FactoryRegistry registry;

    /**
     * Constructs a default {@link MathTransform math transform} factory.
     */
    public DefaultMathTransformFactory() {
        this(new Class<?>[] {MathTransformProvider.class});
    }

    /**
     * Constructs a default {@link MathTransform math transform} factory using the
     * specified {@linkplain MathTransformProvider transform providers} categories.
     *
     * @param categories The providers categories, as implementations
     *                   of {@link MathTransformProvider}.
     */
    private DefaultMathTransformFactory(final Class<?>[] categories) {
        registry   = new FactoryRegistry(Arrays.asList(categories));
        pool       = CanonicalSet.newInstance(MathTransform.class);
    }

    /**
     * Returns the vendor responsible for creating this factory implementation. Many implementations
     * may be available for the same factory interface. The default implementation returns
     * {@linkplain Citations#GEOTOOLS Geotools}.
     *
     * @return The vendor for this factory implementation.
     */
    @Override
    public Citation getVendor() {
        return Citations.GEOTOOLS;
    }

    /**
     * Returns a set of available methods for {@linkplain MathTransform math transforms}. For
     * each element in this set, the {@linkplain OperationMethod#getName operation method name}
     * must be known to the {@link #getDefaultParameters} method in this factory.
     * The set of available methods is implementation dependent.
     *
     * @param  type <code>{@linkplain Operation}.class</code> for fetching all operation methods,
     *           or <code>{@linkplain Projection}.class</code> for fetching only map projection
     *           methods.
     * @return All {@linkplain MathTransform math transform} methods available in this factory.
     *
     * @see #getDefaultParameters
     * @see #createParameterizedTransform
     */
    public Set<OperationMethod> getAvailableMethods(final Class<? extends Operation> type) {
        return new LazySet<OperationMethod>(registry.getServiceProviders(MathTransformProvider.class,
                (type!=null) ? new MethodFilter(type) : null, HINTS));
    }

    /**
     * A filter for the set of available operations.
     */
    private static final class MethodFilter implements FactoryRegistry.Filter {
        /**
         * The expected type ({@code Projection.class}) for projections).
         */
        private final Class<? extends Operation> type;

        /**
         * Constructs a filter for the set of math operations methods.
         */
        public MethodFilter(final Class<? extends Operation> type) {
            this.type = type;
        }

        /**
         * Returns {@code true} if the specified element should be included. If the type is
         * unknown, conservatively returns {@code true}.
         */
        public boolean filter(final Object element) {
            if (element instanceof MathTransformProvider) {
                final Class<? extends Operation> t = ((MathTransformProvider) element).getOperationType();
                if (t!=null && !type.isAssignableFrom(t)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Returns the operation method used for the latest call to
     * {@link #createParameterizedTransform createParameterizedTransform}
     * in the currently running thread. Returns {@code null} if not applicable.
     *
     * @see #createParameterizedTransform
     *
     * @since 2.5
     */
    public OperationMethod getLastMethodUsed() {
        return lastMethod.get();
    }

    /**
     * Returns the math transform provider for the specified operation method.
     * This provider can be used in order to query parameter for a method name
     * (e.g. <code>getProvider("Transverse_Mercator").getParameters()</code>),
     * or any of the alias in a given locale.
     *
     * @param  method The case insensitive
     *         {@linkplain org.opengis.metadata.Identifier#getCode identifier code}
     *         of the operation method to search for (e.g. {@code "Transverse_Mercator"}).
     * @return The math transform provider.
     * @throws NoSuchIdentifierException if there is no provider registered for the specified
     *         method.
     */
    private MathTransformProvider getProvider(final String method) throws NoSuchIdentifierException {
        /*
         * Copies the 'lastProvider' reference in order to avoid synchronization. This is safe
         * because copy of object references are atomic operations.  Note that this is not the
         * deprecated "double check" idiom since we are not creating new objects, but checking
         * for existing ones.
         */
        MathTransformProvider provider = lastProvider;
        if (provider!=null && provider.nameMatches(method)) {
            return provider;
        }
        final Iterator<MathTransformProvider> providers =
                registry.getServiceProviders(MathTransformProvider.class, null, HINTS);
        while (providers.hasNext()) {
            provider = providers.next();
            if (provider.nameMatches(method)) {
                return lastProvider = provider;
            }
        }
        throw new NoSuchIdentifierException(Errors.format(
                  ErrorKeys.NO_TRANSFORM_FOR_CLASSIFICATION_$1, method), method);
    }

    /**
     * Returns the default parameter values for a math transform using the given method.
     * The method argument is the name of any operation method returned by the
     * {@link #getAvailableMethods} method. A typical example is
     * <code>"<A HREF="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">Transverse_Mercator</A>"</code>).
     *
     * <P>This method creates new parameter instances at every call. It is intented to be modified
     * by the user before to be passed to <code>{@linkplain #createParameterizedTransform
     * createParameterizedTransform}(parameters)</code>.</P>
     *
     * @param  method The case insensitive name of the method to search for.
     * @return The default parameter values.
     * @throws NoSuchIdentifierException if there is no transform registered for the specified
     *         method.
     *
     * @see #getAvailableMethods
     * @see #createParameterizedTransform
     * @see org.geotools.referencing.operation.transform.AbstractMathTransform#getParameterValues
     */
    public ParameterValueGroup getDefaultParameters(final String method)
            throws NoSuchIdentifierException
    {
        return getProvider(method).getParameters().createValue();
    }

    /**
     * Creates a {@linkplain #createParameterizedTransform parameterized transform} from a base
     * CRS to a derived CS. If the {@code "semi_major"} and {@code "semi_minor"} parameters are
     * not explicitly specified, they will be inferred from the {@linkplain Ellipsoid ellipsoid}
     * and added to {@code parameters}. In addition, this method performs axis switch as needed.
     * <p>
     * The {@linkplain OperationMethod operation method} used can be obtained by a call to
     * {@link #getLastUsedMethod}.
     *
     * @param  baseCRS The source coordinate reference system.
     * @param  parameters The parameter values for the transform.
     * @param  derivedCS the target coordinate system.
     * @return The parameterized transform.
     * @throws NoSuchIdentifierException if there is no transform registered for the method.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     */
    public MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
                                             final ParameterValueGroup       parameters,
                                             final CoordinateSystem          derivedCS)
            throws NoSuchIdentifierException, FactoryException
    {
        /*
         * If the user's parameter do not contains semi-major and semi-minor axis length, infers
         * them from the ellipsoid. This is a convenience service since the user often omit those
         * parameters (because they duplicate datum information).
         */
        final Ellipsoid ellipsoid = CRSUtilities.getHeadGeoEllipsoid(baseCRS);
        if (ellipsoid != null) {
            final Unit<Length> axisUnit = ellipsoid.getAxisUnit();
            Parameters.ensureSet(parameters, "semi_major", ellipsoid.getSemiMajorAxis(), axisUnit, false);
            Parameters.ensureSet(parameters, "semi_minor", ellipsoid.getSemiMinorAxis(), axisUnit, false);
        }
        MathTransform baseToDerived = createParameterizedTransform(parameters);
        final OperationMethod method = lastMethod.get();
        baseToDerived = createBaseToDerived(baseCRS, baseToDerived, derivedCS);
        lastMethod.set(method);
        return baseToDerived;
    }

    /**
     * Creates a transform from a base CRS to a derived CS. This method expects a "raw"
     * transform without unit conversion or axis switch, typically a map projection
     * working on (<cite>longitude</cite>, <cite>latitude</cite>) axes in degrees and
     * (<cite>x</cite>, <cite>y</cite>) axes in metres. This method inspects the coordinate
     * systems and prepend or append the unit conversions and axis switchs automatically.
     *
     * @param  baseCRS The source coordinate reference system.
     * @param  projection The "raw" <cite>base to derived</cite> transform.
     * @param  derivedCS the target coordinate system.
     * @return The parameterized transform.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @since 2.5
     */
    public MathTransform createBaseToDerived(final CoordinateReferenceSystem baseCRS,
                                             final MathTransform          projection,
                                             final CoordinateSystem        derivedCS)
            throws FactoryException
    {
        /*
         * Computes matrix for swapping axis and performing units conversion.
         * There is one matrix to apply before projection on (longitude,latitude)
         * coordinates, and one matrix to apply after projection on (easting,northing)
         * coordinates.
         */
        final CoordinateSystem sourceCS = baseCRS.getCoordinateSystem();
        final Matrix swap1, swap3;
        try {
            swap1 = AbstractCS.swapAndScaleAxis(sourceCS, AbstractCS.standard(sourceCS));
            swap3 = AbstractCS.swapAndScaleAxis(AbstractCS.standard(derivedCS), derivedCS);
        } catch (IllegalArgumentException cause) {
            // User-specified axis don't match.
            throw new FactoryException(cause);
        } catch (ConversionException cause) {
            // A Unit conversion is non-linear.
            throw new FactoryException(cause);
        }
        /*
         * Prepares the concatenation of the matrix computed above and the projection.
         * Note that at this stage, the dimensions between each step may not be compatible.
         * For example the projection (step2) is usually two-dimensional while the source
         * coordinate system (step1) may be three-dimensional if it has a height.
         */
        MathTransform step1 = createAffineTransform(swap1);
        MathTransform step3 = createAffineTransform(swap3);
        MathTransform step2 = projection;
        /*
         * If the target coordinate system has a height, instructs the projection to pass
         * the height unchanged from the base CRS to the target CRS. After this block, the
         * dimensions of 'step2' and 'step3' should match.
         */
        final int numTrailingOrdinates = step3.getSourceDimensions() - step2.getTargetDimensions();
        if (numTrailingOrdinates > 0) {
            step2 = createPassThroughTransform(0, step2, numTrailingOrdinates);
        }
        /*
         * If the source CS has a height but the target CS doesn't, drops the extra coordinates.
         * After this block, the dimensions of 'step1' and 'step2' should match.
         */
        final int sourceDim = step1.getTargetDimensions();
        final int targetDim = step2.getSourceDimensions();
        if (sourceDim > targetDim) {
            final Matrix drop = MatrixFactory.create(targetDim+1, sourceDim+1);
            drop.setElement(targetDim, sourceDim, 1);
            step1 = createConcatenatedTransform(createAffineTransform(drop), step1);
        }
        return createConcatenatedTransform(createConcatenatedTransform(step1, step2), step3);
    }

    /**
     * Creates a transform from a group of parameters. The method name is inferred from the
     * {@linkplain org.opengis.parameter.ParameterDescriptorGroup#getName parameter group name}.
     * Example:
     *
     * <blockquote><pre>
     * ParameterValueGroup p = factory.getDefaultParameters("Transverse_Mercator");
     * p.parameter("semi_major").setValue(6378137.000);
     * p.parameter("semi_minor").setValue(6356752.314);
     * MathTransform mt = factory.createParameterizedTransform(p);
     * </pre></blockquote>
     *
     * @param  parameters The parameter values.
     * @return The parameterized transform.
     * @throws NoSuchIdentifierException if there is no transform registered for the method.
     * @throws FactoryException if the object creation failed. This exception is thrown
     *         if some required parameter has not been supplied, or has illegal value.
     *
     * @see #getDefaultParameters
     * @see #getAvailableMethods
     * @see #getLastUsedMethod
     */
    public MathTransform createParameterizedTransform(ParameterValueGroup parameters)
            throws NoSuchIdentifierException, FactoryException
    {
        MathTransform transform;
        OperationMethod method = null;
        try {
            final String classification = parameters.getDescriptor().getName().getCode();
            final MathTransformProvider provider = getProvider(classification);
            method = provider;
            try {
                parameters = provider.ensureValidValues(parameters);
                transform  = provider.createMathTransform(parameters);
            } catch (IllegalArgumentException exception) {
                /*
                 * Catch only exceptions which may be the result of improper parameter
                 * usage (e.g. a value out of range). Do not catch exception caused by
                 * programming errors (e.g. null pointer exception).
                 */
                throw new FactoryException(exception);
            }
            if (transform instanceof MathTransformProvider.Delegate) {
                final MathTransformProvider.Delegate delegate = (MathTransformProvider.Delegate) transform;
                method    = delegate.method;
                transform = delegate.transform;
            }
            transform = pool.unique(transform);
        } finally {
            lastMethod.set(method); // May be null in case of failure, which is intented.
        }
        return transform;
    }

    /**
     * Creates an affine transform from a matrix.
     * If the transform's input dimension is {@code M}, and output dimension
     * is {@code N}, then the matrix will have size {@code [N+1][M+1]}.
     * The +1 in the matrix dimensions allows the matrix to do a shift, as well as
     * a rotation. The {@code [M][j]} element of the matrix will be the j'th
     * ordinate of the moved origin. The {@code [i][N]} element of the matrix
     * will be 0 for <var>i</var> less than {@code M}, and 1 for <var>i</var>
     * equals {@code M}.
     *
     * @param matrix The matrix used to define the affine transform.
     * @return The affine transform.
     * @throws FactoryException if the object creation failed.
     */
    public MathTransform createAffineTransform(final Matrix matrix)
            throws FactoryException
    {
        lastMethod.remove(); // To be strict, we should set ProjectiveTransform.Provider
        return pool.unique(ProjectiveTransform.create(matrix));
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two
     * transforms, one after the other.
     *
     * The dimension of the output space of the first transform must match
     * the dimension of the input space in the second transform.
     * If you wish to concatenate more than two transforms, then you can
     * repeatedly use this method.
     *
     * @param  transform1 The first transform to apply to points.
     * @param  transform2 The second transform to apply to points.
     * @return The concatenated transform.
     * @throws FactoryException if the object creation failed.
     */
    public MathTransform createConcatenatedTransform(final MathTransform transform1,
                                                     final MathTransform transform2)
            throws FactoryException
    {
        MathTransform tr;
        try {
            tr = ConcatenatedTransform.create(transform1, transform2);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        tr = pool.unique(tr);
        return tr;
    }

    /**
     * Creates a transform which passes through a subset of ordinates to another transform.
     * This allows transforms to operate on a subset of ordinates. For example, if you have
     * (<var>Lat</var>,<var>Lon</var>,<var>Height</var>) coordinates, then you may wish to
     * convert the height values from meters to feet without affecting the
     * (<var>Lat</var>,<var>Lon</var>) values.
     *
     * @param  firstAffectedOrdinate The lowest index of the affected ordinates.
     * @param  subTransform Transform to use for affected ordinates.
     * @param  numTrailingOrdinates Number of trailing ordinates to pass through.
     *         Affected ordinates will range from {@code firstAffectedOrdinate}
     *         inclusive to {@code dimTarget-numTrailingOrdinates} exclusive.
     * @return A pass through transform with the following dimensions:<br>
     *         <pre>
     * Source: firstAffectedOrdinate + subTransform.getSourceDimensions() + numTrailingOrdinates
     * Target: firstAffectedOrdinate + subTransform.getTargetDimensions() + numTrailingOrdinates</pre>
     * @throws FactoryException if the object creation failed.
     */
    public MathTransform createPassThroughTransform(final int firstAffectedOrdinate,
                                                    final MathTransform subTransform,
                                                    final int numTrailingOrdinates)
            throws FactoryException
    {
        MathTransform tr;
        try {
            tr = PassThroughTransform.create(firstAffectedOrdinate,
                                             subTransform,
                                             numTrailingOrdinates);
        } catch (IllegalArgumentException exception) {
            throw new FactoryException(exception);
        }
        tr = pool.unique(tr);
        return tr;
    }
}
