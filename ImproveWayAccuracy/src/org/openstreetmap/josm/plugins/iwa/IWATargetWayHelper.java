/*
 * This file is part of ImproveWayAccuracy plugin for JOSM.
 * http://wiki.openstreetmap.org/wiki/JOSM/Plugins/ImproveWayAccuracy
 *
 * Licence: GPL v2 or later
 * Author:  Alexander Kachkaev <alexander@kachkaev.ru>, 2011
 */
package org.openstreetmap.josm.plugins.iwa;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;

/**
 * This static class contains functions used to find target way, node to move or
 * segment to divide.
 */
public class IWATargetWayHelper {

    /**
     * Finds the way to work on. If the mouse is on the node, extracts one of
     * the ways containing it. If the mouse is on the way, simply returns it.
     *
     * @param mv
     * @param p
     * @return Way or null in case there is nothing under the cursor.
     */
    public static Way findWay(MapView mv, Point p) {
        if (mv == null || p == null)
            return null;

        Node node = mv.getNearestNode(p, OsmPrimitive.isSelectablePredicate);
        Way candidate = null;

        if (node != null) {
            final Collection<OsmPrimitive> candidates = node.getReferrers();
            for (OsmPrimitive refferer : candidates) {
                if (refferer instanceof Way) {
                    candidate = (Way) refferer;
                    break;
                }
            }
            if (candidate != null)
                return candidate;
        }

        candidate = Main.map.mapView.getNearestWay(p,
                OsmPrimitive.isSelectablePredicate);

        return candidate;
    }

    /**
     * Returns the nearest node to cursor. All nodes that are “behind” segments
     * are neglected. This is to avoid way self-intersection after moving the
     * candidateNode to a new place.
     *
     * @param mv
     * @param w
     * @param p
     * @return
     */
    public static Node findCandidateNode(MapView mv, Way w, Point p) {
        if (mv == null || w == null || p == null)
            return null;

        EastNorth pEN = mv.getEastNorth(p.x, p.y);

        Double bestDistance = Double.MAX_VALUE;
        Double currentDistance;
        List<Pair<Node, Node>> wpps = w.getNodePairs(false);

        Node result = null;

        mainLoop: for (Node n : w.getNodes()) {
            EastNorth nEN = n.getEastNorth();
            currentDistance = pEN.distance(nEN);

            if (currentDistance < bestDistance) {
                // Making sure this candidate is not behind any segment.
                for (Pair<Node, Node> wpp : wpps) {
                    if (!wpp.a.equals(n)
                            && !wpp.b.equals(n)
                            && Geometry.getSegmentSegmentIntersection(
                                    wpp.a.getEastNorth(), wpp.b.getEastNorth(),
                                    pEN, nEN) != null)
                        continue mainLoop;
                }
                result = n;
                bestDistance = currentDistance;
            }
        }

        return result;
    }

    /**
     * Returns the nearest way segment to cursor. The distance to segment ab is
     * the length of altitude from p to ab (say, c) or the minimum distance from
     * p to a or b if c is out of ab.
     *
     * The priority is given to segments where c is in ab. Otherwise, a segment
     * with the largest angle apb is chosen.
     *
     * @param mv
     * @param w
     * @param p
     * @return
     */
    public static WaySegment findCandidateSegment(MapView mv, Way w, Point p) {
        if (mv == null || w == null || p == null)
            return null;

        EastNorth pEN = mv.getEastNorth(p.x, p.y);

        Double currentDistance;
        Double currentAngle;
        Double bestDistance = Double.MAX_VALUE;
        Double bestAngle = 0.0;

        int candidate = -1;

        List<Pair<Node, Node>> wpps = w.getNodePairs(true);

        int i = -1;
        for (Pair<Node, Node> wpp : wpps) {
            ++i;

            // Finding intersection of the segment with its altitude from p (c)
            EastNorth altitudeIntersection = IWAGeometry
                    .getSegmentAltituteIntersection(wpp.a.getEastNorth(),
                            wpp.b.getEastNorth(), pEN);

            if (altitudeIntersection != null) {
                // If the the segment intersects with the altitude from p
                currentDistance = pEN.distance(altitudeIntersection);

                // Making an angle too big to let this candidate win any others
                // having the same distance.
                currentAngle = Double.MAX_VALUE;

            } else {
                // Otherwise: Distance is equal to the shortest distance from p
                // to a or b
                currentDistance = Math.min(pEN.distance(wpp.a.getEastNorth()),
                        pEN.distance(wpp.b.getEastNorth()));

                // Measuring the angle
                currentAngle = Math.abs(IWAGeometry.getCornerAngle(
                        wpp.a.getEastNorth(), pEN, wpp.b.getEastNorth()));
            }

            if (currentDistance < bestDistance
                    || (currentAngle > bestAngle && currentDistance < bestDistance * 1.0001 /* equality */)) {
                candidate = i;
                bestAngle = currentAngle;
                bestDistance = currentDistance;
            }

        }
        return candidate != -1 ? new WaySegment(w, candidate) : null;
    }
}
