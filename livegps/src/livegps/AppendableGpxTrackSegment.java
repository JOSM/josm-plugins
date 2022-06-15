// License: Public Domain. For details, see LICENSE file.
package livegps;

import java.util.Collection;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.gpx.WithAttributes;
import org.openstreetmap.josm.tools.CopyList;

/**
 * Thread safe implementation of GpxTrackSegement
 *
 */
public class AppendableGpxTrackSegment extends WithAttributes implements IGpxTrackSegment {

    private WayPoint[] wayPoints = new WayPoint[16];
    private int size;
    private Bounds bounds;
    private double length;

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public Collection<WayPoint> getWayPoints() {
        return new CopyList<>(wayPoints, size);
    }

    public void addWaypoint(WayPoint p) {
        if (wayPoints.length == size) {
            WayPoint[] newWaypoints = new WayPoint[wayPoints.length * 2];
            System.arraycopy(wayPoints, 0, newWaypoints, 0, wayPoints.length);
            wayPoints = newWaypoints;
        }

        if (size > 0) {
            double distance = wayPoints[size - 1].greatCircleDistance(p);
            if (!Double.isNaN(distance) && !Double.isInfinite(distance)) {
                length += distance;
            }
        }

        if (bounds == null) {
            bounds = new Bounds(p.getCoor());
        } else {
            bounds.extend(p.getCoor());
        }

        wayPoints[size] = p;
        size++;
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public int getUpdateCount() {
        return size;
    }

}
