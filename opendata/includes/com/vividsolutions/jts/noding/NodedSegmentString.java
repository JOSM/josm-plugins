
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
package com.vividsolutions.jts.noding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Represents a list of contiguous line segments,
 * and supports noding the segments.
 * The line segments are represented by an array of {@link Coordinate}s.
 * Intended to optimize the noding of contiguous segments by
 * reducing the number of allocated objects.
 * SegmentStrings can carry a context object, which is useful
 * for preserving topological or parentage information.
 * All noded substrings are initialized with the same context object.
 *
 * @version 1.7
 */
public class NodedSegmentString
	implements NodableSegmentString
{
	/**
	 * 
	 * @param segStrings a Collection of NodedSegmentStrings
	 * @return a Collection of NodedSegmentStrings representing the substrings
	 */
  public static List getNodedSubstrings(Collection segStrings)
  {
    List resultEdgelist = new ArrayList();
    getNodedSubstrings(segStrings, resultEdgelist);
    return resultEdgelist;
  }

	/**
	 * 
	 * @param segStrings a Collection of NodedSegmentStrings
	 * @param resultEdgelist a List which will collect the NodedSegmentStrings representing the substrings
	 */
 public static void getNodedSubstrings(Collection segStrings, Collection resultEdgelist)
  {
    for (Iterator i = segStrings.iterator(); i.hasNext(); ) {
      NodedSegmentString ss = (NodedSegmentString) i.next();
      ss.getNodeList().addSplitEdges(resultEdgelist);
    }
  }

  private SegmentNodeList nodeList = new SegmentNodeList(this);
  private Coordinate[] pts;
  private Object data;

  /**
   * Creates a new segment string from a list of vertices.
   *
   * @param pts the vertices of the segment string
   * @param data the user-defined data of this segment string (may be null)
   */
  public NodedSegmentString(Coordinate[] pts, Object data)
  {
    this.pts = pts;
    this.data = data;
  }

  /**
   * Gets the user-defined data for this segment string.
   *
   * @return the user-defined data
   */
  public Object getData() { return data; }


  public SegmentNodeList getNodeList() { return nodeList; }
  public int size() { return pts.length; }
  public Coordinate getCoordinate(int i) { return pts[i]; }
  public Coordinate[] getCoordinates() { return pts; }


  /**
   * Gets the octant of the segment starting at vertex <code>index</code>.
   *
   * @param index the index of the vertex starting the segment.  Must not be
   * the last index in the vertex list
   * @return the octant of the segment at the vertex
   */
  public int getSegmentOctant(int index)
  {
    if (index == pts.length - 1) return -1;
    return safeOctant(getCoordinate(index), getCoordinate(index + 1));
//    return Octant.octant(getCoordinate(index), getCoordinate(index + 1));
  }

  private int safeOctant(Coordinate p0, Coordinate p1)
  {
  	if (p0.equals2D(p1)) return 0;
  	return Octant.octant(p0, p1);
  }
  
  public String toString()
  {
  	return WKTWriter.toLineString(new CoordinateArraySequence(pts));
  }
}
