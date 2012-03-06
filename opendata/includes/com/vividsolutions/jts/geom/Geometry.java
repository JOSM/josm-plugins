/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.vividsolutions.jts.geom;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.util.GeometryCollectionMapper;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.operation.IsSimpleOp;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.overlay.OverlayOp;
import com.vividsolutions.jts.operation.overlay.snap.SnapIfNeededOverlayOp;
import com.vividsolutions.jts.operation.predicate.RectangleContains;
import com.vividsolutions.jts.operation.predicate.RectangleIntersects;
import com.vividsolutions.jts.operation.relate.RelateOp;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.util.Assert;

/**
 * A representation of a planar, linear vector geometry.
 * <P>
 *
 *  <H3>Binary Predicates</H3>
 * Because it is not clear at this time
 * what semantics for spatial
 *  analysis methods involving <code>GeometryCollection</code>s would be useful,
 *  <code>GeometryCollection</code>s are not supported as arguments to binary
 *  predicates (other than <code>convexHull</code>) or the <code>relate</code>
 *  method.
 *
 *  <H3>Overlay Methods</H3>
 *
 *  The overlay methods 
 *  return the most specific class possible to represent the result. If the
 *  result is homogeneous, a <code>Point</code>, <code>LineString</code>, or
 *  <code>Polygon</code> will be returned if the result contains a single
 *  element; otherwise, a <code>MultiPoint</code>, <code>MultiLineString</code>,
 *  or <code>MultiPolygon</code> will be returned. If the result is
 *  heterogeneous a <code>GeometryCollection</code> will be returned. <P>
 *
 *  Because it is not clear at this time what semantics for set-theoretic
 *  methods involving <code>GeometryCollection</code>s would be useful,
 * <code>GeometryCollections</code>
 *  are not supported as arguments to the set-theoretic methods.
 *
 *  <H4>Representation of Computed Geometries </H4>
 *
 *  The SFS states that the result
 *  of a set-theoretic method is the "point-set" result of the usual
 *  set-theoretic definition of the operation (SFS 3.2.21.1). However, there are
 *  sometimes many ways of representing a point set as a <code>Geometry</code>.
 *  <P>
 *
 *  The SFS does not specify an unambiguous representation of a given point set
 *  returned from a spatial analysis method. One goal of JTS is to make this
 *  specification precise and unambiguous. JTS will use a canonical form for
 *  <code>Geometry</code>s returned from spatial analysis methods. The canonical
 *  form is a <code>Geometry</code> which is simple and noded:
 *  <UL>
 *    <LI> Simple means that the Geometry returned will be simple according to
 *    the JTS definition of <code>isSimple</code>.
 *    <LI> Noded applies only to overlays involving <code>LineString</code>s. It
 *    means that all intersection points on <code>LineString</code>s will be
 *    present as endpoints of <code>LineString</code>s in the result.
 *  </UL>
 *  This definition implies that non-simple geometries which are arguments to
 *  spatial analysis methods must be subjected to a line-dissolve process to
 *  ensure that the results are simple.
 *
 *  <H4> Constructed Points And The Precision Model </H4>
 *
 *  The results computed by the set-theoretic methods may
 *  contain constructed points which are not present in the input <code>Geometry</code>
 *  s. These new points arise from intersections between line segments in the
 *  edges of the input <code>Geometry</code>s. In the general case it is not
 *  possible to represent constructed points exactly. This is due to the fact
 *  that the coordinates of an intersection point may contain twice as many bits
 *  of precision as the coordinates of the input line segments. In order to
 *  represent these constructed points explicitly, JTS must truncate them to fit
 *  the <code>PrecisionModel</code>. <P>
 *
 *  Unfortunately, truncating coordinates moves them slightly. Line segments
 *  which would not be coincident in the exact result may become coincident in
 *  the truncated representation. This in turn leads to "topology collapses" --
 *  situations where a computed element has a lower dimension than it would in
 *  the exact result. <P>
 *
 *  When JTS detects topology collapses during the computation of spatial
 *  analysis methods, it will throw an exception. If possible the exception will
 *  report the location of the collapse. <P>
 *
 * <h3>Geometry Equality</h3>
 * 
 * JTS provides two ways of comparing geometries for equality: 
 * <b>structural equality</b> and <b>topological equality</b>.
 * 
 * <h4>Structural Equality</h4>
 *
 * Structural Equality is provided by the 
 * {@link #equalsExact(Geometry)} method.  
 * This implements a comparison based on exact, structural pointwise
 * equality. 
 * The {@link #equals(Object)} is a synonym for this method, 
 * to provide structural equality semantics for
 * use in Java collections.
 * It is important to note that structural pointwise equality
 * is easily affected by things like
 * ring order and component order.  In many situations
 * it will be desirable to normalize geometries before
 * comparing them (using the {@link #norm()} 
 * or {@link #normalize()} methods).
 * {@link #equalsNorm(Geometry)} is provided
 * as a convenience method to compute equality over
 * normalized geometries, but it is expensive to use.
 * Finally, {@link #equalsExact(Geometry, double)}
 * allows using a tolerance value for point comparison.
 * 
 * 
 * <h4>Topological Equality</h4>
 * 
 * Topological Equality 
 * implements the SFS definition of point-set equality
 * defined in terms of the DE-9IM matrix.
 * It is is provided by the 
 * {@link #equalsTopo(Geometry)}
 * method.  
 * To support the SFS naming convention, the method
 *  {@link #equals(Geometry)} is also provided as a synonym.  
 *  However, due to the potential for confusion with {@link #equals(Geometry)}
 *  its use is discouraged.
 * <p>
 *  Since #equals(Object) and #hashCode are overridden, 
 *  Geometries can be used effectively in 
 *  Java collections.
 *
 *@version 1.7
 */
public abstract class Geometry
    implements Cloneable, Comparable, Serializable
{
  private static final long serialVersionUID = 8763622679187376702L;
    
  private static Class[] sortedClasses;  
  
  private final static GeometryComponentFilter geometryChangedFilter = new GeometryComponentFilter() {
    public void filter(Geometry geom) {
      geom.geometryChangedAction();
    }
  };

  /**
   *  The bounding box of this <code>Geometry</code>.
   */
  protected Envelope envelope;

  /**
   * The {@link GeometryFactory} used to create this Geometry
   */
  protected final GeometryFactory factory;

  /**
   * An object reference which can be used to carry ancillary data defined
   * by the client.
   */
  private Object userData = null;

  /**
   * Creates a new <tt>Geometry</tt> via the specified GeometryFactory.
   *
   * @param factory
   */
  public Geometry(GeometryFactory factory) {
    this.factory = factory;
  }

  /**
   *  Returns the name of this object's <code>com.vivid.jts.geom</code>
   *  interface.
   *
   *@return    the name of this <code>Geometry</code>s most specific <code>com.vividsolutions.jts.geom</code>
   *      interface
   */
  public abstract String getGeometryType();

  /**
   *  Returns true if the array contains any non-empty <code>Geometry</code>s.
   *
   *@param  geometries  an array of <code>Geometry</code>s; no elements may be
   *      <code>null</code>
   *@return             <code>true</code> if any of the <code>Geometry</code>s
   *      <code>isEmpty</code> methods return <code>false</code>
   */
  protected static boolean hasNonEmptyElements(Geometry[] geometries) {
    for (int i = 0; i < geometries.length; i++) {
      if (!geometries[i].isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   *  Returns true if the array contains any <code>null</code> elements.
   *
   *@param  array  an array to validate
   *@return        <code>true</code> if any of <code>array</code>s elements are
   *      <code>null</code>
   */
  protected static boolean hasNullElements(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the factory which contains the context in which this geometry was created.
   *
   * @return the factory for this geometry
   */
  public GeometryFactory getFactory() {
         return factory;
  }

  /**
   * Gets the user data object for this geometry, if any.
   *
   * @return the user data object, or <code>null</code> if none set
   */
  public Object getUserData() {
        return userData;
  }

  /**
   * Returns the number of {@link Geometry}s in a {@link GeometryCollection}
   * (or 1, if the geometry is not a collection).
   *
   * @return the number of geometries contained in this geometry
   */
  public int getNumGeometries() {
    return 1;
  }

  /**
   * Returns an element {@link Geometry} from a {@link GeometryCollection}
   * (or <code>this</code>, if the geometry is not a collection).
   *
   * @param n the index of the geometry element
   * @return the n'th geometry contained in this geometry
   */
  public Geometry getGeometryN(@SuppressWarnings("unused") int n) {
    return this;
  }


  /**
   * A simple scheme for applications to add their own custom data to a Geometry.
   * An example use might be to add an object representing a Coordinate Reference System.
   * <p>
   * Note that user data objects are not present in geometries created by
   * construction methods.
   *
   * @param userData an object, the semantics for which are defined by the
   * application using this Geometry
   */
  public void setUserData(Object userData) {
        this.userData = userData;
  }


  /**
   *  Returns the <code>PrecisionModel</code> used by the <code>Geometry</code>.
   *
   *@return    the specification of the grid of allowable points, for this
   *      <code>Geometry</code> and all other <code>Geometry</code>s
   */
  public PrecisionModel getPrecisionModel() {
    return factory.getPrecisionModel();
  }

  /**
   *  Returns a vertex of this <code>Geometry</code>
   *  (usually, but not necessarily, the first one).
   *  The returned coordinate should not be assumed
   *  to be an actual Coordinate object used in
   *  the internal representation.
   *
   *@return    a {@link Coordinate} which is a vertex of this <code>Geometry</code>.
   *@return null if this Geometry is empty
   */
  public abstract Coordinate getCoordinate();
  
  /**
   *  Returns an array containing the values of all the vertices for 
   *  this geometry.
   *  If the geometry is a composite, the array will contain all the vertices
   *  for the components, in the order in which the components occur in the geometry.
   *  <p>
   *  In general, the array cannot be assumed to be the actual internal 
   *  storage for the vertices.  Thus modifying the array
   *  may not modify the geometry itself. 
   *  Use the {@link CoordinateSequence#setOrdinate} method
   *  (possibly on the components) to modify the underlying data.
   *  If the coordinates are modified, 
   *  {@link #geometryChanged} must be called afterwards.
   *
   *@return    the vertices of this <code>Geometry</code>
   *@see #geometryChanged
   *@see CoordinateSequence#setOrdinate
   */
  public abstract Coordinate[] getCoordinates();

  /**
   *  Returns the count of this <code>Geometry</code>s vertices. The <code>Geometry</code>
   *  s contained by composite <code>Geometry</code>s must be
   *  Geometry's; that is, they must implement <code>getNumPoints</code>
   *
   *@return    the number of vertices in this <code>Geometry</code>
   */
  public abstract int getNumPoints();

  /**
   * Tests whether this {@link Geometry} is simple.
   * In general, the SFS specification of simplicity
   * follows the rule:
   *  <UL>
   *    <LI> A Geometry is simple iff the only self-intersections are at
   *    boundary points.
   *  </UL>
   * Simplicity is defined for each {@link Geometry} subclass as follows:
   * <ul>
   * <li>Valid polygonal geometries are simple by definition, so
   * <code>isSimple</code> trivially returns true.
   * <li>Linear geometries are simple iff they do not self-intersect at points
   * other than boundary points.
   * <li>Zero-dimensional geometries (points) are simple iff they have no
   * repeated points.
   * <li>Empty <code>Geometry</code>s are always simple
   * <ul>
   *
   * @return    <code>true</code> if this <code>Geometry</code> has any points of
   *      self-tangency, self-intersection or other anomalous points
   * @see #isValid
   */
  public boolean isSimple()
  {
    checkNotGeometryCollection(this);
    IsSimpleOp op = new IsSimpleOp(this);
    return op.isSimple();
  }

  /**
   *  Tests the validity of this <code>Geometry</code>.
   *  Subclasses provide their own definition of "valid".
   *
   *@return    <code>true</code> if this <code>Geometry</code> is valid
   *
   * @see IsValidOp
   */
  public boolean isValid()
  {
  	return IsValidOp.isValid(this);
  }

  /**
   *  Returns whether or not the set of points in this <code>Geometry</code> is
   *  empty.
   *
   *@return    <code>true</code> if this <code>Geometry</code> equals the empty
   *      geometry
   */
  public abstract boolean isEmpty();


  /**
   * Tests whether the distance from this <code>Geometry</code>
   * to another is less than or equal to a specified value.
   *
   * @param geom the Geometry to check the distance to
   * @param distance the distance value to compare
   * @return <code>true</code> if the geometries are less than <code>distance</code> apart.
   */
  public boolean isWithinDistance(Geometry geom, double distance)
  {
    double envDist = getEnvelopeInternal().distance(geom.getEnvelopeInternal());
    if (envDist > distance)
      return false;
    return DistanceOp.isWithinDistance(this, geom, distance);
    /*
    double geomDist = this.distance(geom);
    if (geomDist > distance)
      return false;
    return true;
    */
  }

  public boolean isRectangle()
  {
    // Polygon overrides to check for actual rectangle
    return false;
  }

  /**
   *  Returns the area of this <code>Geometry</code>.
   *  Areal Geometries have a non-zero area.
   *  They override this function to compute the area.
   *  Others return 0.0
   *
   *@return the area of the Geometry
   */
  public double getArea()
  {
    return 0.0;
  }

  /**
   *  Returns the length of this <code>Geometry</code>.
   *  Linear geometries return their length.
   *  Areal geometries return their perimeter.
   *  They override this function to compute the area.
   *  Others return 0.0
   *
   *@return the length of the Geometry
   */
  public double getLength()
  {
    return 0.0;
  }

  /**
   * Returns the dimension of this geometry.
   * The dimension of a geometry is is the topological 
   * dimension of its embedding in the 2-D Euclidean plane.
   * In the JTS spatial model, dimension values are in the set {0,1,2}.
   * <p>
   * Note that this is a different concept to the dimension of 
   * the vertex {@link Coordinate}s.  
   * The geometry dimension can never be greater than the coordinate dimension.
   * For example, a 0-dimensional geometry (e.g. a Point) 
   * may have a coordinate dimension of 3 (X,Y,Z). 
   *
   *@return the topological dimension of this geometry.
   */
  public abstract int getDimension();

  /**
   * Returns the boundary, or an empty geometry of appropriate dimension
   * if this <code>Geometry</code>  is empty.
   * (In the case of zero-dimensional geometries, '
   * an empty GeometryCollection is returned.)
   * For a discussion of this function, see the OpenGIS Simple
   * Features Specification. As stated in SFS Section 2.1.13.1, "the boundary
   * of a Geometry is a set of Geometries of the next lower dimension."
   *
   *@return    the closure of the combinatorial boundary of this <code>Geometry</code>
   */
  public abstract Geometry getBoundary();

  /**
   *  Returns the dimension of this <code>Geometry</code>s inherent boundary.
   *
   *@return    the dimension of the boundary of the class implementing this
   *      interface, whether or not this object is the empty geometry. Returns
   *      <code>Dimension.FALSE</code> if the boundary is the empty geometry.
   */
  public abstract int getBoundaryDimension();


  /**
   *  Returns the minimum and maximum x and y values in this <code>Geometry</code>
   *  , or a null <code>Envelope</code> if this <code>Geometry</code> is empty.
   *
   *@return    this <code>Geometry</code>s bounding box; if the <code>Geometry</code>
   *      is empty, <code>Envelope#isNull</code> will return <code>true</code>
   */
  public Envelope getEnvelopeInternal() {
    if (envelope == null) {
      envelope = computeEnvelopeInternal();
    }
    return envelope;
  }

  /**
   * Notifies this geometry that its coordinates have been changed by an external
   * party (for example, via a {@link CoordinateFilter}). 
   * When this method is called the geometry will flush
   * and/or update any derived information it has cached (such as its {@link Envelope} ).
   */
  public void geometryChanged() {
    apply(geometryChangedFilter);
  }

  /**
   * Notifies this Geometry that its Coordinates have been changed by an external
   * party. When #geometryChanged is called, this method will be called for
   * this Geometry and its component Geometries.
   * @see #apply(GeometryComponentFilter)
   */
  protected void geometryChangedAction() {
    envelope = null;
  }

  /**
   * Tests whether this geometry is disjoint from the argument geometry.
   * <p>
   * The <code>disjoint</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The two geometries have no point in common
   * <li>The DE-9IM Intersection Matrix for the two geometries matches 
   * <code>[FF*FF****]</code>
   * <li><code>! g.intersects(this) = true</code>
   * <br>(<code>disjoint</code> is the inverse of <code>intersects</code>)
   * </ul>
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if the two <code>Geometry</code>s are
   *      disjoint
   *
   * @see Geometry#intersects
   */
  public boolean disjoint(Geometry g) {
    return ! intersects(g);
  }

  /**
   * Tests whether this geometry touches the
   * argument geometry.
   * <p>
   * The <code>touches</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The geometries have at least one point in common, but their interiors do not intersect.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   * at least one of the following patterns
   *  <ul>
   *   <li><code>[FT*******]</code>
   *   <li><code>[F**T*****]</code>
   *   <li><code>[F***T****]</code>
   *  </ul>
   * </ul>
   * If both geometries have dimension 0, this predicate returns <code>false</code>.
   * 
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if the two <code>Geometry</code>s touch;
   *      Returns <code>false</code> if both <code>Geometry</code>s are points
   */
  public boolean touches(Geometry g) {
    // short-circuit test
    if (! getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false;
    return relate(g).isTouches(getDimension(), g.getDimension());
  }

  /**
   * Tests whether this geometry intersects the argument geometry.
   * <p>
   * The <code>intersects</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The two geometries have at least one point in common
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   * at least one of the patterns
   *  <ul>
   *   <li><code>[T********]</code>
   *   <li><code>[*T*******]</code>
   *   <li><code>[***T*****]</code>
   *   <li><code>[****T****]</code>
   *  </ul>
   * <li><code>! g.disjoint(this) = true</code>
   * <br>(<code>intersects</code> is the inverse of <code>disjoint</code>)
   * </ul>
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if the two <code>Geometry</code>s intersect
   *
   * @see Geometry#disjoint
   */
  public boolean intersects(Geometry g) {

    // short-circuit envelope test
    if (! getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false;

    /**
     * TODO: (MD) Add optimizations:
     *
     * - for P-A case:
     * If P is in env(A), test for point-in-poly
     *
     * - for A-A case:
     * If env(A1).overlaps(env(A2))
     * test for overlaps via point-in-poly first (both ways)
     * Possibly optimize selection of point to test by finding point of A1
     * closest to centre of env(A2).
     * (Is there a test where we shouldn't bother - e.g. if env A
     * is much smaller than env B, maybe there's no point in testing
     * pt(B) in env(A)?
     */

    // optimization for rectangle arguments
    if (isRectangle()) {
      return RectangleIntersects.intersects((Polygon) this, g);
    }
    if (g.isRectangle()) {
      return RectangleIntersects.intersects((Polygon) g, this);
    }
    // general case
    return relate(g).isIntersects();
  }

  /**
   * Tests whether this geometry crosses the
   * argument geometry.
   * <p>
   * The <code>crosses</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The geometries have some but not all interior points in common.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   * one of the following patterns:
   *   <ul>
   *    <li><code>[T*T******]</code> (for P/L, P/A, and L/A situations)
   *    <li><code>[T*****T**]</code> (for L/P, A/P, and A/L situations)
   *    <li><code>[0********]</code> (for L/L situations)
   *   </ul>
   * </ul>
   * For any other combination of dimensions this predicate returns <code>false</code>.
   * <p>
   * The SFS defined this predicate only for P/L, P/A, L/L, and L/A situations.
   * In order to make the relation symmetric,
   * JTS extends the definition to apply to L/P, A/P and A/L situations as well.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if the two <code>Geometry</code>s cross.
   */
  public boolean crosses(Geometry g) {
    // short-circuit test
    if (! getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false;
    return relate(g).isCrosses(getDimension(), g.getDimension());
  }

  /**
   * Tests whether this geometry is within the
   * specified geometry.
   * <p>
   * The <code>within</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>Every point of this geometry is a point of the other geometry,
   * and the interiors of the two geometries have at least one point in common.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches 
   * <code>[T*F**F***]</code>
   * <li><code>g.contains(this) = true</code>
   * <br>(<code>within</code> is the converse of {@link #contains})
   * </ul>
   * An implication of the definition is that
   * "The boundary of a Geometry is not within the Geometry".
   * In other words, if a geometry A is a subset of
   * the points in the boundary of a geomtry B, <code>A.within(B) = false</code>
   * (As a concrete example, take A to be a LineString which lies in the boundary of a Polygon B.)
   * For a predicate with similar behaviour but avoiding 
   * this subtle limitation, see {@link #coveredBy}.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if this <code>Geometry</code> is within
   *      <code>g</code>
   *
   * @see Geometry#contains
   * @see Geometry#coveredBy
   */
  public boolean within(Geometry g) {
    return g.contains(this);
  }

  /**
   * Tests whether this geometry contains the
   * argument geometry.
   * <p>
   * The <code>contains</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>Every point of the other geometry is a point of this geometry,
   * and the interiors of the two geometries have at least one point in common.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches 
   * the pattern
   * <code>[T*****FF*]</code>
   * <li><code>g.within(this) = true</code>
   * <br>(<code>contains</code> is the converse of {@link #within} )
   * </ul>
   * An implication of the definition is that "Geometries do not
   * contain their boundary".  In other words, if a geometry A is a subset of
   * the points in the boundary of a geometry B, <code>B.contains(A) = false</code>.
   * (As a concrete example, take A to be a LineString which lies in the boundary of a Polygon B.)
   * For a predicate with similar behaviour but avoiding 
   * this subtle limitation, see {@link #covers}.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if this <code>Geometry</code> contains <code>g</code>
   *
   * @see Geometry#within
   * @see Geometry#covers
   */
  public boolean contains(Geometry g) {
    // short-circuit test
    if (! getEnvelopeInternal().contains(g.getEnvelopeInternal()))
      return false;
    // optimization for rectangle arguments
    if (isRectangle()) {
      return RectangleContains.contains((Polygon) this, g);
    }
    // general case
    return relate(g).isContains();
  }

  /**
   * Tests whether this geometry overlaps the
   * specified geometry.
   * <p>
   * The <code>overlaps</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The geometries have at least one point each not shared by the other
   * (or equivalently neither covers the other),
   * they have the same dimension,
   * and the intersection of the interiors of the two geometries has
   * the same dimension as the geometries themselves.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   *   <code>[T*T***T**]</code> (for two points or two surfaces)
   *   or <code>[1*T***T**]</code> (for two curves)
   * </ul>
   * If the geometries are of different dimension this predicate returns <code>false</code>.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if the two <code>Geometry</code>s overlap.
   */
  public boolean overlaps(Geometry g) {
    // short-circuit test
    if (! getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false;
    return relate(g).isOverlaps(getDimension(), g.getDimension());
  }

  /**
   * Tests whether this geometry covers the
   * argument geometry.
   * <p>
   * The <code>covers</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>Every point of the other geometry is a point of this geometry.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   * at least one of the following patterns:
   *  <ul> 
   *   <li><code>[T*****FF*]</code>
   *   <li><code>[*T****FF*]</code>
   *   <li><code>[***T**FF*]</code>
   *   <li><code>[****T*FF*]</code>
   *  </ul>
   * <li><code>g.coveredBy(this) = true</code>
   * <br>(<code>covers</code> is the converse of {@link #coveredBy})
   * </ul>
   * If either geometry is empty, the value of this predicate is <tt>false</tt>.
   * <p>
   * This predicate is similar to {@link #contains},
   * but is more inclusive (i.e. returns <tt>true</tt> for more cases).
   * In particular, unlike <code>contains</code> it does not distinguish between
   * points in the boundary and in the interior of geometries.
   * For most situations, <code>covers</code> should be used in preference to <code>contains</code>.
   * As an added benefit, <code>covers</code> is more amenable to optimization,
   * and hence should be more performant.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        <code>true</code> if this <code>Geometry</code> covers <code>g</code>
   *
   * @see Geometry#contains
   * @see Geometry#coveredBy
   */
  public boolean covers(Geometry g) {
    // short-circuit test
    if (! getEnvelopeInternal().covers(g.getEnvelopeInternal()))
      return false;
    // optimization for rectangle arguments
    if (isRectangle()) {
    	// since we have already tested that the test envelope is covered
      return true;
    }
    return relate(g).isCovers();
  }


  /**
   * Tests whether the elements in the DE-9IM
   * {@link IntersectionMatrix} for the two <code>Geometry</code>s match the elements in <code>intersectionPattern</code>.
   * The pattern is a 9-character string, with symbols drawn from the following set:
   *  <UL>
   *    <LI> 0 (dimension 0)
   *    <LI> 1 (dimension 1)
   *    <LI> 2 (dimension 2)
   *    <LI> T ( matches 0, 1 or 2)
   *    <LI> F ( matches FALSE)
   *    <LI> * ( matches any value)
   *  </UL>
   *  For more information on the DE-9IM, see the <i>OpenGIS Simple Features
   *  Specification</i>.
   *
   *@param  g                the <code>Geometry</code> with which to compare
   *      this <code>Geometry</code>
   *@param  intersectionPattern  the pattern against which to check the
   *      intersection matrix for the two <code>Geometry</code>s
   *@return                      <code>true</code> if the DE-9IM intersection
   *      matrix for the two <code>Geometry</code>s match <code>intersectionPattern</code>
   * @see IntersectionMatrix
   */
  public boolean relate(Geometry g, String intersectionPattern) {
    return relate(g).matches(intersectionPattern);
  }

  /**
   *  Returns the DE-9IM {@link IntersectionMatrix} for the two <code>Geometry</code>s.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return        an {@link IntersectionMatrix} describing the intersections of the interiors,
   *      boundaries and exteriors of the two <code>Geometry</code>s
   */
  public IntersectionMatrix relate(Geometry g) {
    checkNotGeometryCollection(this);
    checkNotGeometryCollection(g);
    return RelateOp.relate(this, g);
  }

  /**
  * Tests whether this geometry is 
  * topologically equal to the argument geometry.
   * <p>
   * This method is included for backward compatibility reasons.
   * It has been superseded by the {@link #equalsTopo(Geometry)} method,
   * which has been named to clearly denote its functionality.
   * <p>
   * This method should NOT be confused with the method 
   * {@link #equals(Object)}, which implements 
   * an exact equality comparison.
   *
   *@param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return true if the two <code>Geometry</code>s are topologically equal
   *
   *@see #equalsTopo(Geometry)
   */
  public boolean equals(Geometry g) {
    return equalsTopo(g);
  }

  /**
   * Tests whether this geometry is topologically equal to the argument geometry
   * as defined by the SFS <tt>equals</tt> predicate.
   * <p>
   * The SFS <code>equals</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The two geometries have at least one point in common,
   * and no point of either geometry lies in the exterior of the other geometry.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   * the pattern <tt>T*F**FFF*</tt> 
   * <pre>
   * T*F
   * **F
   * FF*
   * </pre>
   * </ul>
   * <b>Note</b> that this method computes <b>topologically equality</b>. 
   * For structural equality, see {@link #equalsExact(Geometry)}.
   *
   *@param g the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return <code>true</code> if the two <code>Geometry</code>s are topologically equal
   *
   *@see #equalsExact(Geometry) 
   */
  public boolean equalsTopo(Geometry g)
  {
    // short-circuit test
    if (! getEnvelopeInternal().equals(g.getEnvelopeInternal()))
      return false;
    return relate(g).isEquals(getDimension(), g.getDimension());
  }
  
  /**
   * Tests whether this geometry is structurally and numerically equal
   * to a given <tt>Object</tt>.
   * If the argument <tt>Object</tt> is not a <tt>Geometry</tt>, 
   * the result is <tt>false</tt>.
   * Otherwise, the result is computed using
   * {@link #equalsExact(Geometry)}.
   * <p>
   * This method is provided to fulfill the Java contract
   * for value-based object equality. 
   * In conjunction with {@link #hashCode()} 
   * it provides semantics which are most useful 
   * for using
   * <tt>Geometry</tt>s as keys and values in Java collections.
   * <p>
   * Note that to produce the expected result the input geometries
   * should be in normal form.  It is the caller's 
   * responsibility to perform this where required
   * (using {@link Geometry#norm()
   * or {@link #normalize()} as appropriate).
   * 
   * @param o the Object to compare
   * @return true if this geometry is exactly equal to the argument 
   * 
   * @see #equalsExact(Geometry)
   * @see #hashCode()
   * @see #norm()
   * @see #normalize()
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof Geometry)) return false;
    Geometry g = (Geometry) o;
    return equalsExact(g);
  }
  
  public int hashCode()
  {
    return getEnvelopeInternal().hashCode();
  }
  
  public String toString() {
    return toText();
  }

  /**
   *  Returns the Well-known Text representation of this <code>Geometry</code>.
   *  For a definition of the Well-known Text format, see the OpenGIS Simple
   *  Features Specification.
   *
   *@return    the Well-known Text representation of this <code>Geometry</code>
   */
  public String toText() {
    WKTWriter writer = new WKTWriter();
    return writer.write(this);
  }



  /**
   *  Computes the smallest convex <code>Polygon</code> that contains all the
   *  points in the <code>Geometry</code>. This obviously applies only to <code>Geometry</code>
   *  s which contain 3 or more points; the results for degenerate cases are
   *  specified as follows:
   *  <TABLE>
   *    <TR>
   *      <TH>    Number of <code>Point</code>s in argument <code>Geometry</code>   </TH>
   *      <TH>    <code>Geometry</code> class of result     </TH>
   *    </TR>
   *    <TR>
   *      <TD>        0      </TD>
   *      <TD>        empty <code>GeometryCollection</code>      </TD>
   *    </TR>
   *    <TR>  <TD>      1     </TD>
   *      <TD>     <code>Point</code>     </TD>
   *    </TR>
   *    <TR>
   *      <TD>      2     </TD>
   *      <TD>     <code>LineString</code>     </TD>
   *    </TR>
   *    <TR>
   *      <TD>       3 or more     </TD>
   *      <TD>      <code>Polygon</code>     </TD>
   *    </TR>
   *  </TABLE>
   *
   *@return    the minimum-area convex polygon containing this <code>Geometry</code>'
   *      s points
   */
  public Geometry convexHull() {
    return (new ConvexHull(this)).getConvexHull();
  }

  /**
   * Computes a new geometry which has all component coordinate sequences
   * in reverse order (opposite orientation) to this one.
   * 
   * @return a reversed geometry
   */
  public abstract Geometry reverse();
  
  /**
   * Computes a <code>Geometry</code> representing the points shared by this
   * <code>Geometry</code> and <code>other</code>.
   * <p>
   * Intersection of {@link GeometryCollection}s is supported
   * only for homogeneous collection types.
   * The result is a {@link GeometryCollection} of the
   * intersection of each element of the target with the argument. 
   * <p>
   * The intersection of two geometries of different dimension produces a result
   * geometry of dimension equal to the minimum dimension of the input
   * geometries. The result geometry is always a homogeneous
   * {@link GeometryCollection}.
   *
   * @param  other the <code>Geometry</code> with which to compute the intersection
   * @return the point-set common to the two <code>Geometry</code>s
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if the argument is a non-empty heterogeneous GeometryCollection
   */
  public Geometry intersection(Geometry other)
  {
  	/**
  	 * TODO: MD - add optimization for P-A case using Point-In-Polygon
  	 */
    // special case: if one input is empty ==> empty
    if (this.isEmpty()) return this.getFactory().createGeometryCollection(null);
    if (other.isEmpty()) return this.getFactory().createGeometryCollection(null);

    // compute for GCs
    if (isGeometryCollection(this)) {
      final Geometry g2 = other;
      return GeometryCollectionMapper.map(
          (GeometryCollection) this,
          new GeometryCollectionMapper.MapOp() {
        public Geometry map(Geometry g) {
          return g.intersection(g2);
        }
      });
    }
//    if (isGeometryCollection(other))
//      return other.intersection(this);
    
    checkNotGeometryCollection(this);
    checkNotGeometryCollection(other);
    return SnapIfNeededOverlayOp.overlayOp(this, other, OverlayOp.INTERSECTION);
  }

  
  /**
   * Returns true if the two <code>Geometry</code>s are exactly equal,
   * up to a specified distance tolerance.
   * Two Geometries are exactly equal within a distance tolerance
   * if and only if:
   * <ul>
   * <li>they have the same structure
   * <li>they have the same values for their vertices,
   * within the given tolerance distance, in exactly the same order.
   * </ul>
   * This method does <i>not</i>
   * test the values of the <tt>GeometryFactory</tt>, the <tt>SRID</tt>, 
   * or the <tt>userData</tt> fields.
   * <p>
   * To properly test equality between different geometries,
   * it is usually necessary to {@link #normalize()} them first.
   *
   * @param other the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param tolerance distance at or below which two <code>Coordinate</code>s
   *   are considered equal
   * @return <code>true</code> if this and the other <code>Geometry</code>
   *   have identical structure and point values, up to the distance tolerance.
   *   
   * @see #equalsExact(Geometry)
   * @see #normalize()
   * @see #norm()
   */
  public abstract boolean equalsExact(Geometry other, double tolerance);

  /**
   * Returns true if the two <code>Geometry</code>s are exactly equal.
   * Two Geometries are exactly equal iff:
   * <ul>
   * <li>they have the same structure
   * <li>they have the same values for their vertices,
   * in exactly the same order.
   * </ul>
   * This provides a stricter test of equality than
   * {@link #equalsTopo(Geometry)}, which is more useful
   * in certain situations
   * (such as using geometries as keys in collections).
   * <p>
   * This method does <i>not</i>
   * test the values of the <tt>GeometryFactory</tt>, the <tt>SRID</tt>, 
   * or the <tt>userData</tt> fields.
   * <p>
   * To properly test equality between different geometries,
   * it is usually necessary to {@link #normalize()} them first.
   *
   *@param  other  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return <code>true</code> if this and the other <code>Geometry</code>
   *      have identical structure and point values.
   *      
   * @see #equalsExact(Geometry, double)
   * @see #normalize()
   * @see #norm()
   */
  public boolean equalsExact(Geometry other) { return equalsExact(other, 0); }


  /**
   *  Performs an operation with or on this <code>Geometry</code>'s
   *  coordinates. 
   *  If this method modifies any coordinate values,
   *  {@link #geometryChanged} must be called to update the geometry state. 
   *  Note that you cannot use this method to
   *  modify this Geometry if its underlying CoordinateSequence's #get method
   *  returns a copy of the Coordinate, rather than the actual Coordinate stored
   *  (if it even stores Coordinate objects at all).
   *
   *@param  filter  the filter to apply to this <code>Geometry</code>'s
   *      coordinates
   */
  public abstract void apply(CoordinateFilter filter);

  /**
   *  Performs an operation on the coordinates in this <code>Geometry</code>'s
   *  {@link CoordinateSequence}s. 
   *  If the filter reports that a coordinate value has been changed, 
   *  {@link #geometryChanged} will be called automatically.
   *
   *@param  filter  the filter to apply
   */
  public abstract void apply(CoordinateSequenceFilter filter);

  /**
   *  Performs an operation with or on this <code>Geometry</code> and its
   *  subelement <code>Geometry</code>s (if any).
   *  Only GeometryCollections and subclasses
   *  have subelement Geometry's.
   *
   *@param  filter  the filter to apply to this <code>Geometry</code> (and
   *      its children, if it is a <code>GeometryCollection</code>).
   */
  public abstract void apply(GeometryFilter filter);

  /**
   *  Performs an operation with or on this Geometry and its
   *  component Geometry's.  Only GeometryCollections and
   *  Polygons have component Geometry's; for Polygons they are the LinearRings
   *  of the shell and holes.
   *
   *@param  filter  the filter to apply to this <code>Geometry</code>.
   */
  public abstract void apply(GeometryComponentFilter filter);

  /**
   * Creates and returns a full copy of this {@link Geometry} object
   * (including all coordinates contained by it).
   * Subclasses are responsible for overriding this method and copying
   * their internal data.  Overrides should call this method first.
   *
   * @return a clone of this instance
   */
  public Object clone() {
    try {
      Geometry clone = (Geometry) super.clone();
      if (clone.envelope != null) { clone.envelope = new Envelope(clone.envelope); }
      return clone;
    }
    catch (CloneNotSupportedException e) {
      Assert.shouldNeverReachHere();
      return null;
    }
  }

  /**
   *  Converts this <code>Geometry</code> to <b>normal form</b> (or <b>
   *  canonical form</b> ). Normal form is a unique representation for <code>Geometry</code>
   *  s. It can be used to test whether two <code>Geometry</code>s are equal
   *  in a way that is independent of the ordering of the coordinates within
   *  them. Normal form equality is a stronger condition than topological
   *  equality, but weaker than pointwise equality. The definitions for normal
   *  form use the standard lexicographical ordering for coordinates. "Sorted in
   *  order of coordinates" means the obvious extension of this ordering to
   *  sequences of coordinates.
   *  <p>
   *  NOTE that this method mutates the value of this geometry in-place.
   *  If this is not safe and/or wanted, the geometry should be
   *  cloned prior to normalization.
   */
  public abstract void normalize();

  
  /**
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code>. <P>
   *
   *  If their classes are different, they are compared using the following
   *  ordering:
   *  <UL>
   *    <LI> Point (lowest)
   *    <LI> MultiPoint
   *    <LI> LineString
   *    <LI> LinearRing
   *    <LI> MultiLineString
   *    <LI> Polygon
   *    <LI> MultiPolygon
   *    <LI> GeometryCollection (highest)
   *  </UL>
   *  If the two <code>Geometry</code>s have the same class, their first
   *  elements are compared. If those are the same, the second elements are
   *  compared, etc.
   *
   *@param  o  a <code>Geometry</code> with which to compare this <code>Geometry</code>
   *@return    a positive number, 0, or a negative number, depending on whether
   *      this object is greater than, equal to, or less than <code>o</code>, as
   *      defined in "Normal Form For Geometry" in the JTS Technical
   *      Specifications
   */
  public int compareTo(Object o) {
    Geometry other = (Geometry) o;
    if (getClassSortIndex() != other.getClassSortIndex()) {
      return getClassSortIndex() - other.getClassSortIndex();
    }
    if (isEmpty() && other.isEmpty()) {
      return 0;
    }
    if (isEmpty()) {
      return -1;
    }
    if (other.isEmpty()) {
      return 1;
    }
    return compareToSameClass(o);
  }


  /**
   *  Returns whether the two <code>Geometry</code>s are equal, from the point
   *  of view of the <code>equalsExact</code> method. Called by <code>equalsExact</code>
   *  . In general, two <code>Geometry</code> classes are considered to be
   *  "equivalent" only if they are the same class. An exception is <code>LineString</code>
   *  , which is considered to be equivalent to its subclasses.
   *
   *@param  other  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *      for equality
   *@return        <code>true</code> if the classes of the two <code>Geometry</code>
   *      s are considered to be equal by the <code>equalsExact</code> method.
   */
  protected boolean isEquivalentClass(Geometry other) {
    return this.getClass().getName().equals(other.getClass().getName());
  }

  /**
   *  Throws an exception if <code>g</code>'s class is <code>GeometryCollection</code>
   *  . (Its subclasses do not trigger an exception).
   *
   *@param  g                          the <code>Geometry</code> to check
   *@throws  IllegalArgumentException  if <code>g</code> is a <code>GeometryCollection</code>
   *      but not one of its subclasses
   */
  protected void checkNotGeometryCollection(Geometry g) {
    //Don't use instanceof because we want to allow subclasses
    if (g.getClass().getName().equals("com.vividsolutions.jts.geom.GeometryCollection")) {
      throw new IllegalArgumentException("This method does not support GeometryCollection arguments");
    }
  }

  protected boolean isGeometryCollection(Geometry g)
  {
    return g.getClass().equals(com.vividsolutions.jts.geom.GeometryCollection.class);
  }

  /**
   *  Returns the minimum and maximum x and y values in this <code>Geometry</code>
   *  , or a null <code>Envelope</code> if this <code>Geometry</code> is empty.
   *  Unlike <code>getEnvelopeInternal</code>, this method calculates the <code>Envelope</code>
   *  each time it is called; <code>getEnvelopeInternal</code> caches the result
   *  of this method.
   *
   *@return    this <code>Geometry</code>s bounding box; if the <code>Geometry</code>
   *      is empty, <code>Envelope#isNull</code> will return <code>true</code>
   */
  protected abstract Envelope computeEnvelopeInternal();

  /**
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code> having the same class.
   *
   *@param  o  a <code>Geometry</code> having the same class as this <code>Geometry</code>
   *@return    a positive number, 0, or a negative number, depending on whether
   *      this object is greater than, equal to, or less than <code>o</code>, as
   *      defined in "Normal Form For Geometry" in the JTS Technical
   *      Specifications
   */
  protected abstract int compareToSameClass(Object o);

  /**
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code> of the same class.
   * using the given {@link CoordinateSequenceComparator}.
   *
   *@param  o  a <code>Geometry</code> having the same class as this <code>Geometry</code>
   *@param comp a <code>CoordinateSequenceComparator</code>
   *@return    a positive number, 0, or a negative number, depending on whether
   *      this object is greater than, equal to, or less than <code>o</code>, as
   *      defined in "Normal Form For Geometry" in the JTS Technical
   *      Specifications
   */
  protected abstract int compareToSameClass(Object o, CoordinateSequenceComparator comp);

  /**
   *  Returns the first non-zero result of <code>compareTo</code> encountered as
   *  the two <code>Collection</code>s are iterated over. If, by the time one of
   *  the iterations is complete, no non-zero result has been encountered,
   *  returns 0 if the other iteration is also complete. If <code>b</code>
   *  completes before <code>a</code>, a positive number is returned; if a
   *  before b, a negative number.
   *
   *@param  a  a <code>Collection</code> of <code>Comparable</code>s
   *@param  b  a <code>Collection</code> of <code>Comparable</code>s
   *@return    the first non-zero <code>compareTo</code> result, if any;
   *      otherwise, zero
   */
  protected int compare(Collection a, Collection b) {
    Iterator i = a.iterator();
    Iterator j = b.iterator();
    while (i.hasNext() && j.hasNext()) {
      Comparable aElement = (Comparable) i.next();
      Comparable bElement = (Comparable) j.next();
      int comparison = aElement.compareTo(bElement);
      if (comparison != 0) {
        return comparison;
      }
    }
    if (i.hasNext()) {
      return 1;
    }
    if (j.hasNext()) {
      return -1;
    }
    return 0;
  }

  protected boolean equal(Coordinate a, Coordinate b, double tolerance) {
    if (tolerance == 0) { return a.equals(b); }
    return a.distance(b) <= tolerance;
  }

  private int getClassSortIndex() {
		if (sortedClasses == null)
			initSortedClasses();

		for (int i = 0; i < sortedClasses.length; i++) {
			if (sortedClasses[i].isInstance(this))
				return i;
		}
		Assert.shouldNeverReachHere("Class not supported: " + this.getClass());
		return -1;
	}

  private static void initSortedClasses()
  {
		sortedClasses = new Class[] { 
					Point.class, 
					MultiPoint.class,
					LineString.class, 
					LinearRing.class, 
					MultiLineString.class,
					Polygon.class, 
					MultiPolygon.class, 
					GeometryCollection.class };
  }
  
  private Point createPointFromInternalCoord(Coordinate coord, Geometry exemplar)
  {
    exemplar.getPrecisionModel().makePrecise(coord);
    return exemplar.getFactory().createPoint(coord);
  }


}

