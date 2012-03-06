
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
package com.vividsolutions.jts.geom.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * Extracts all the 1-dimensional ({@link LineString}) components from a {@link Geometry}.
 *
 * @version 1.7
 */
public class LinearComponentExtracter
  implements GeometryComponentFilter
{
  /**
   * Extracts the linear components from a single geometry.
   * If more than one geometry is to be processed, it is more
   * efficient to create a single {@link LinearComponentExtracter} instance
   * and pass it to multiple geometries.
   *
   * @param geom the geometry from which to extract linear components
   * @return the list of linear components
   */
  public static List getLines(Geometry geom)
  {
    return getLines(geom, false);
  }

  /**
   * Extracts the linear components from a single geometry.
   * If more than one geometry is to be processed, it is more
   * efficient to create a single {@link LinearComponentExtracter} instance
   * and pass it to multiple geometries.
   *
   * @param geom the geometry from which to extract linear components
   * @param forceToLineString true if LinearRings should be converted to LineStrings
   * @return the list of linear components
   */
  public static List getLines(Geometry geom, boolean forceToLineString)
  {
    List lines = new ArrayList();
    geom.apply(new LinearComponentExtracter(lines, forceToLineString));
    return lines;
  }

  private Collection lines;
  private boolean isForcedToLineString = false;

  /**
   * Constructs a LineExtracterFilter with a list in which to store LineStrings found.
   */
  public LinearComponentExtracter(Collection lines, boolean isForcedToLineString)
  {
    this.lines = lines;
    this.isForcedToLineString = isForcedToLineString;
  }

  public void filter(Geometry geom)
  {
  	if (isForcedToLineString && geom instanceof LinearRing) {
  		LineString line = geom.getFactory().createLineString( ((LinearRing) geom).getCoordinateSequence());
  		lines.add(line);
  		return;
  	}
  	// if not being forced, and this is a linear component
  	if (geom instanceof LineString) 
  		lines.add(geom);
  	
  	// else this is not a linear component, so skip it
  }

}
