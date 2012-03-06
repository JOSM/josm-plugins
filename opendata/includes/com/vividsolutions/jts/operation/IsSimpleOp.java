

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
package com.vividsolutions.jts.operation;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.vividsolutions.jts.algorithm.BoundaryNodeRule;
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geomgraph.Edge;
import com.vividsolutions.jts.geomgraph.EdgeIntersection;
import com.vividsolutions.jts.geomgraph.GeometryGraph;
import com.vividsolutions.jts.geomgraph.index.SegmentIntersector;

/**
 * Tests whether a <code>Geometry</code> is simple.
 * In general, the SFS specification of simplicity
 * follows the rule:
 * <ul>
 *    <li> A Geometry is simple if and only if the only self-intersections are at
 *    boundary points.
 * </ul>
 * This definition relies on the definition of boundary points.
 * The SFS uses the Mod-2 rule to determine which points are on the boundary of
 * lineal geometries, but this class supports
 * using other {@link BoundaryNodeRule}s as well.
 * <p>
 * Simplicity is defined for each {@link Geometry} subclass as follows:
 * <ul>
 * <li>Valid polygonal geometries are simple by definition, so
 * <code>isSimple</code> trivially returns true.
 * (Hint: in order to check if a polygonal geometry has self-intersections,
 * use {@link Geometry#isValid}).
 * <li>Linear geometries are simple iff they do not self-intersect at points
 * other than boundary points. 
 * (Using the Mod-2 rule, this means that closed linestrings
 * cannot be touched at their endpoints, since these are
 * interior points, not boundary points).
 * <li>Zero-dimensional geometries (points) are simple iff they have no
 * repeated points.
 * <li>Empty <code>Geometry</code>s are always simple
 * </ul>
 *
 * @see BoundaryNodeRule
 *
 * @version 1.7
 */
public class IsSimpleOp
{
  private Geometry geom;
  private boolean isClosedEndpointsInInterior = true;


  /**
   * Creates a simplicity checker using the default SFS Mod-2 Boundary Node Rule
   *
   * @param geom the geometry to test
   */
  public IsSimpleOp(Geometry geom) {
    this.geom = geom;
  }

  /**
   * Tests whether the geometry is simple.
   *
   * @return true if the geometry is simple
   */
  public boolean isSimple()
  {
    if (geom instanceof LineString) return isSimpleLinearGeometry(geom);
    if (geom instanceof MultiLineString) return isSimpleLinearGeometry(geom);
    if (geom instanceof MultiPoint) return isSimpleMultiPoint((MultiPoint) geom);
    // all other geometry types are simple by definition
    return true;
  }

  private boolean isSimpleMultiPoint(MultiPoint mp)
  {
    if (mp.isEmpty()) return true;
    Set points = new TreeSet();
    for (int i = 0; i < mp.getNumGeometries(); i++) {
      Point pt = (Point) mp.getGeometryN(i);
      Coordinate p = pt.getCoordinate();
      if (points.contains(p)) {
        return false;
      }
      points.add(p);
    }
    return true;
  }

  private boolean isSimpleLinearGeometry(Geometry geom)
  {
    if (geom.isEmpty()) return true;
    GeometryGraph graph = new GeometryGraph(0, geom);
    LineIntersector li = new RobustLineIntersector();
    SegmentIntersector si = graph.computeSelfNodes(li, true);
    // if no self-intersection, must be simple
    if (! si.hasIntersection()) return true;
    if (si.hasProperIntersection()) {
      return false;
    }
    if (hasNonEndpointIntersection(graph)) return false;
    if (isClosedEndpointsInInterior) {
      if (hasClosedEndpointIntersection(graph)) return false;
    }
    return true;
  }

  /**
   * For all edges, check if there are any intersections which are NOT at an endpoint.
   * The Geometry is not simple if there are intersections not at endpoints.
   */
  private boolean hasNonEndpointIntersection(GeometryGraph graph)
  {
    for (Iterator i = graph.getEdgeIterator(); i.hasNext(); ) {
      Edge e = (Edge) i.next();
      int maxSegmentIndex = e.getMaximumSegmentIndex();
      for (Iterator eiIt = e.getEdgeIntersectionList().iterator(); eiIt.hasNext(); ) {
        EdgeIntersection ei = (EdgeIntersection) eiIt.next();
        if (! ei.isEndPoint(maxSegmentIndex)) {
          return true;
        }
      }
    }
    return false;
  }

  private static class EndpointInfo {
    boolean isClosed;
    int degree;

    public EndpointInfo()
    {
      isClosed = false;
      degree = 0;
    }

    public void addEndpoint(boolean isClosed)
    {
      degree++;
      this.isClosed |= isClosed;
    }
  }

  /**
   * Tests that no edge intersection is the endpoint of a closed line.
   * This ensures that closed lines are not touched at their endpoint,
   * which is an interior point according to the Mod-2 rule
   * To check this we compute the degree of each endpoint.
   * The degree of endpoints of closed lines
   * must be exactly 2.
   */
  private boolean hasClosedEndpointIntersection(GeometryGraph graph)
  {
    Map endPoints = new TreeMap();
    for (Iterator i = graph.getEdgeIterator(); i.hasNext(); ) {
      Edge e = (Edge) i.next();
      boolean isClosed = e.isClosed();
      Coordinate p0 = e.getCoordinate(0);
      addEndpoint(endPoints, p0, isClosed);
      Coordinate p1 = e.getCoordinate(e.getNumPoints() - 1);
      addEndpoint(endPoints, p1, isClosed);
    }

    for (Iterator i = endPoints.values().iterator(); i.hasNext(); ) {
      EndpointInfo eiInfo = (EndpointInfo) i.next();
      if (eiInfo.isClosed && eiInfo.degree != 2) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add an endpoint to the map, creating an entry for it if none exists
   */
  private void addEndpoint(Map endPoints, Coordinate p, boolean isClosed)
  {
    EndpointInfo eiInfo = (EndpointInfo) endPoints.get(p);
    if (eiInfo == null) {
      eiInfo = new EndpointInfo();
      endPoints.put(p, eiInfo);
    }
    eiInfo.addEndpoint(isClosed);
  }

}
