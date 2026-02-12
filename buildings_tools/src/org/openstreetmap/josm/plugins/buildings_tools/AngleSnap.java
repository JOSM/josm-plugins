// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.plugins.buildings_tools.BuildingsToolsPlugin.latlon2eastNorth;

import java.util.TreeSet;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Pair;

/**
 * A class for getting angles for snapping
 */
public class AngleSnap {
    private static final double PI_2 = Math.PI / 2;
    final TreeSet<Double> snapSet = new TreeSet<>();

    /**
     * Clear the snap directions
     */
    public final void clear() {
        snapSet.clear();
    }

    /**
     * Add a snap direction
     * @param snap The direction in radians
     */
    public final void addSnap(double snap) {
        snapSet.add(snap % PI_2);
    }

    /**
     * Add node pairs to the possible snap directions (the heading and 45 degrees from heading)
     * @param nodes The nodes to iterate through
     * @return The heading between the nodes in radians
     */
    public final Double addSnap(Node[] nodes) {
        if (nodes.length == 2) {
            EastNorth p1 = latlon2eastNorth(nodes[0]);
            EastNorth p2 = latlon2eastNorth(nodes[1]);
            double heading = p1.heading(p2);
            addSnap(heading);
            addSnap(heading + Math.PI / 4);
            return heading;
        } else {
            return null;
        }
    }

    /**
     * Add way segments to the possible snap directions
     * @param way The way to add node pairs from to the snap directions
     */
    public final void addSnap(Way way) {
        for (Pair<Node, Node> pair : way.getNodePairs(false)) {
            EastNorth a = latlon2eastNorth(pair.a);
            EastNorth b = latlon2eastNorth(pair.b);
            double heading = a.heading(b);
            addSnap(heading);
        }
    }

    /**
     * Get the angle between the first and last snaps added
     * @return The angle between the first and last snaps added
     */
    public final Double getAngle() {
        if (snapSet.isEmpty()) {
            return null;
        }
        double first = snapSet.first();
        double last = snapSet.last();
        if (first < Math.PI / 4 && last > Math.PI / 4) {
            last -= PI_2;
        }
        if (Math.abs(first - last) < 0.001) {
            double center = (first + last) / 2;
            if (center < 0)
                center += PI_2;
            return center;
        } else {
            return null;
        }
    }

    /**
     * Get the snap angle given a starting angle
     * @param angle The angle to get the snap for
     * @return The best angle for snapping, or the originating angle
     */
    public final double snapAngle(double angle) {
        if (snapSet.isEmpty()) {
            return angle;
        }
        int quadrant = (int) Math.floor(angle / PI_2);
        double ang = angle % PI_2;
        Double prev = snapSet.floor(ang);
        if (prev == null)
            prev = snapSet.last() - PI_2;
        Double next = snapSet.ceiling(ang);
        if (next == null)
            next = snapSet.first() + PI_2;

        if (Math.abs(ang - next) > Math.abs(ang - prev)) {
            return prevSnapAngle(quadrant, prev, ang, angle);
        } else {
            return nextSnapAngle(quadrant, next, ang, angle);
        }
    }

    private static double nextSnapAngle(double quadrant, double next, double ang, double angle) {
        if (Math.abs(ang - next) > Math.PI / 8) {
            return angle;
        } else {
            double ret = next + PI_2 * quadrant;
            if (ret > 2 * Math.PI)
                ret -= 2 * Math.PI;
            return ret;
        }
    }

    private static double prevSnapAngle(double quadrant, double prev, double ang, double angle) {
        if (Math.abs(ang - prev) > Math.PI / 8) {
            return angle;
        } else {
            double ret = prev + PI_2 * quadrant;
            if (ret < 0)
                ret += 2 * Math.PI;
            return ret;
        }
    }
}
