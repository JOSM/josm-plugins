

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


/**
 *  Interface for classes which provide operations that
 *  can be applied to the coordinates in a {@link CoordinateSequence}. 
 *  A CoordinateSequence filter can either record information about each coordinate or
 *  change the coordinate in some way. CoordinateSequence filters can be
 *  used to implement such things as coordinate transformations, centroid and
 *  envelope computation, and many other functions.
 *  For maximum efficiency, the execution of filters can be short-circuited.
 *  {@link Geometry} classes support the concept of applying a
 *  <code>CoordinateSequenceFilter</code> to each 
 *  {@link CoordinateSequence}s they contain. 
 *  <p>
 *  <code>CoordinateSequenceFilter</code> is
 *  an example of the Gang-of-Four Visitor pattern. 
 *
 *@see Geometry#apply(CoordinateSequenceFilter)
 *@author Martin Davis
 *@version 1.7
 */
public interface CoordinateSequenceFilter 
{
  /**
   * Performs an operation on a coordinate in a {@link CoordinateSequence}.
   *
   *@param seq  the <code>CoordinateSequence</code> to which the filter is applied
   *@param i the index of the coordinate to apply the filter to
   */
  void filter(CoordinateSequence seq, int i);
  
  /**
   * Reports whether the application of this filter can be terminated.
   * Once this method returns <tt>false</tt>, it should 
   * continue to return <tt>false</tt> on every subsequent call.
   * 
   * @return true if the application of this filter can be terminated.
   */
  boolean isDone();
  
  /**
   * Reports whether the execution of this filter
   * has modified the coordinates of the geometry.
   * If so, {@link Geometry#geometryChanged} will be executed
   * after this filter has finished being executed.
   * <p>
   * Most filters can simply return a constant value reflecting
   * whether they are able to change the coordinates.
   * 
   * @return true if this filter has changed the coordinates of the geometry
   */
  boolean isGeometryChanged();
}

