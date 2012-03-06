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
package com.vividsolutions.jts.geom.prep;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.noding.FastSegmentSetIntersectionFinder;
import com.vividsolutions.jts.noding.SegmentStringUtil;

/**
 * A prepared version for {@link Lineal} geometries.
 * 
 * @author mbdavis
 *
 */
public class PreparedLineString
  extends BasicPreparedGeometry
{
  private FastSegmentSetIntersectionFinder segIntFinder = null;

  public PreparedLineString(Lineal line) {
    super((Geometry) line);
  }

  public FastSegmentSetIntersectionFinder getIntersectionFinder()
  {
  	/**
  	 * MD - Another option would be to use a simple scan for 
  	 * segment testing for small geometries.  
  	 * However, testing indicates that there is no particular advantage 
  	 * to this approach.
  	 */
  	if (segIntFinder == null)
  		segIntFinder = new FastSegmentSetIntersectionFinder(SegmentStringUtil.extractSegmentStrings(getGeometry()));
    return segIntFinder;
  }
  
  public boolean intersects(Geometry g)
  {
  	if (! envelopesIntersect(g)) return false;
    return PreparedLineStringIntersects.intersects(this, g);
  }
  
  /**
   * There's not much point in trying to optimize contains, since 
   * contains for linear targets requires the entire test geometry 
   * to exactly match the target linework.
   */
}
