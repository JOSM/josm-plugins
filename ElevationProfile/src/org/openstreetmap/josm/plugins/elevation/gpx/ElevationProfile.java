// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.gpx;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.plugins.elevation.ElevationHelper;
import org.openstreetmap.josm.plugins.elevation.IElevationProfile;


/**
 * Base class for an elevation profile. An elevation profile is constructed out
 * of a set of way points. The profile computes min/max/average height from the
 * full way point set and then reduces the number of way points to a given
 * amount, if necessary.
 * 
 * The computation is done via implementing {@link IGpxWaypointVisitor},
 * subclasses may override the {@link ElevationProfile#visitWayPoint(WayPoint)}
 * method to compute own values or run specific actions. The computation is
 * triggered by calling {@link ElevationProfile#updateValues()}.
 * 
 * Elevation profiles can break down into further child profiles. This is
 * intended to show different levels of details, if the number of way points
 * exceed the display space (which is usually the case).
 * 
 * {@link IElevationProfile} {@link IGpxWaypointVisitor} {@link GpxIterator}
 * 
 * @author Oliver Wieland <oliver.wieland@online.de>
 * 
 */
public class ElevationProfile implements IElevationProfile,
IGpxWaypointVisitor {
    public static final int WAYPOINT_START = 0;
    public static final int WAYPOINT_END = 1;
    public static final int WAYPOINT_MIN = 2;
    public static final int WAYPOINT_MAX = 3;

    private String name;
    private int minHeight;
    private int maxHeight;
    private int avrgHeight;
    private double dist;
    private Date start = new Date();
    private Date end = new Date();
    private final WayPoint[] importantWayPoints = new WayPoint[4];
    private IElevationProfile parent;
    private int sumEle; // temp var for average height
    private List<WayPoint> wayPoints;
    private int numWayPoints; // cached value
    private int gain;
    private int lastEle;
    private Bounds bounds;

    private static boolean ignoreZeroHeight = true;

    /**
     * Creates a name elevation profile without any way points.
     * 
     * @param name
     */
    public ElevationProfile(String name) {
        this(name, null, null, 0);
    }

    /**
     * Creates a name elevation profile with a given set of way points.
     * 
     * @param name
     *            The name of the profile.
     * @param parent
     *            The (optional) parent profile.
     * @param wayPoints
     *            The list containing the way points of the profile.
     * @param sliceSize
     *            The requested target size of the profile.
     */
    public ElevationProfile(String name, IElevationProfile parent,
            List<WayPoint> wayPoints, int sliceSize) {
        super();
        this.name = name;
        this.parent = parent;

        setWayPoints(wayPoints);
    }

    /**
     * Checks if zero elevation should be ignored or not.
     *
     * @return true, if is ignore zero height
     */
    public static boolean isIgnoreZeroHeight() {
        return ignoreZeroHeight;
    }

    /**
     * Sets the ignore zero height.
     *
     * @param ignoreZeroHeight the new ignore zero height
     */
    public static void setIgnoreZeroHeight(boolean ignoreZeroHeight) {
        ElevationProfile.ignoreZeroHeight = ignoreZeroHeight;
    }

    @Override
    public void updateElevationData() {
        updateValues();
    }

    /**
     * Revisits all way points and recomputes the characteristic values like
     * min/max elevation.
     */
    protected void updateValues() {
        if (wayPoints == null)
            return;

        int n = this.wayPoints.size();
        if (n == 0)
            return;

        start = new Date();
        end = new Date(0L);
        this.minHeight = Integer.MAX_VALUE;
        this.maxHeight = Integer.MIN_VALUE;
        sumEle = 0;
        gain = 0;
        lastEle = 0;

        for (WayPoint wayPoint : this.wayPoints) {
            visitWayPoint(wayPoint);
        }

        if (this.minHeight == Integer.MAX_VALUE && this.maxHeight == Integer.MIN_VALUE) {
            // file does not contain elevation data    at all
            minHeight = 0;
            maxHeight = 0;
            setMinWayPoint(wayPoints.get(0));
            setMaxWayPoint(wayPoints.get(n-1));
        }

        //if (start.after(end) || start.equals(end)) {
        // GPX does not contain time stamps -> use sequential order
        setStart(wayPoints.get(0));
        setEnd(wayPoints.get(n-1));
        //}

        avrgHeight = sumEle / n;
    }

    /**
     * Gets the name of the profile.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the profile.
     * @param name The new name of the profile.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the way point with the lowest elevation.
     * @param wp The way point instance having the lowest elevation.
     */
    protected void setMinWayPoint(WayPoint wp) {
        importantWayPoints[WAYPOINT_MIN] = wp;
        this.minHeight = (int) ElevationHelper.getElevation(wp);
    }

    /**
     * Sets the way point with the highest elevation.
     * @param wp The way point instance having the highest elevation.
     */
    protected void setMaxWayPoint(WayPoint wp) {
        importantWayPoints[WAYPOINT_MAX] = wp;
        this.maxHeight = (int) ElevationHelper.getElevation(wp);
    }

    /**
     * Sets the average height.
     * @param avrgHeight
     */
    protected void setAvrgHeight(int avrgHeight) {
        this.avrgHeight = avrgHeight;
    }

    /**
     * Sets the very first way point.
     * @param wp
     */
    protected void setStart(WayPoint wp) {
        importantWayPoints[WAYPOINT_START] = wp;
        this.start = wp.getTime();
    }

    /**
     * Sets the very last way point.
     * @param wp
     */
    protected void setEnd(WayPoint wp) {
        importantWayPoints[WAYPOINT_END] = wp;
        this.end = wp.getTime();
    }

    public void setParent(IElevationProfile parent) {
        this.parent = parent;
    }

    /**
     * Sets the way points of this profile.
     * 
     * @param wayPoints
     */
    public void setWayPoints(List<WayPoint> wayPoints) {
        if (this.wayPoints != wayPoints) {
            this.wayPoints = new ArrayList<WayPoint>(wayPoints);
            numWayPoints = wayPoints != null ? wayPoints.size() : 0;
            updateValues();

        }
    }

    /**
     * Checks if the given index is valid or not.
     * 
     * @param index
     *            The index to check.
     * @return true, if the given index is valid; otherwise false.
     */
    protected boolean checkIndex(int index) {
        return index >= 0 && index < getNumberOfWayPoints();
    }

    @Override
    public int elevationValueAt(int i) {
        if (checkIndex(i)) {
            return (int) ElevationHelper.getElevation(wayPoints.get(i));
        } else {
            throw new IndexOutOfBoundsException(String.format(
                    "Invalid index: %d, expected 0..%d", i,
                    getNumberOfWayPoints()));
        }
    }

    @Override
    public int getAverageHeight() {
        return avrgHeight;
    }

    @Override
    public List<IElevationProfile> getChildren() {
        return null;
    }

    @Override
    public Date getEnd() {
        return end;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    /**
     * Gets the difference between min and max elevation.
     * 
     * @return
     */
    @Override
    public int getHeightDifference() {
        return maxHeight - minHeight;
    }

    /**
     * Gets the elevation gain.
     * 
     * @return
     */
    @Override
    public int getGain() {
        return gain;
    }

    @Override
    public double getDistance() {
        return dist; // dist is in meters
    }

    /**
     * Sets the distance of the elevation profile.
     * @param dist
     */
    protected void setDistance(double dist) {
        this.dist = dist;
    }

    /**
     * Returns the time between start and end of the track.
     * @return
     */
    @Override
    public long getTimeDifference() {
        WayPoint wp1 = getStartWayPoint();
        WayPoint wp2 = getEndWayPoint();

        if (wp1 != null && wp2 != null) {
            long diff = wp2.getTime().getTime() - wp1.getTime().getTime();
            return diff;
        }

        return 0L;
    }

    @Override
    public IElevationProfile getParent() {
        return parent;
    }

    @Override
    public Date getStart() {
        return start;
    }

    @Override
    public WayPoint getEndWayPoint() {
        return importantWayPoints[WAYPOINT_END];
    }

    @Override
    public WayPoint getMaxWayPoint() {
        return importantWayPoints[WAYPOINT_MAX];
    }

    @Override
    public WayPoint getMinWayPoint() {
        return importantWayPoints[WAYPOINT_MIN];
    }

    @Override
    public WayPoint getStartWayPoint() {
        return importantWayPoints[WAYPOINT_START];
    }

    @Override
    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    @Override
    public int getNumberOfWayPoints() {
        return numWayPoints;// wayPoints != null ? wayPoints.size() : 0;
    }

    /**
     * Gets the coordinate bounds of this profile. See {@link Bounds} for details.
     *
     * @return the bounds of this elevation profile
     */
    @Override
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Gets a flag indicating whether the associated way points contained
     * elevation data or not. This is the case if min and max height or both
     * zero.
     * 
     * @return
     */
    @Override
    public boolean hasElevationData() {
        return minHeight != maxHeight;
    }

    /**
     * Visits a way point in order to update statistical values about the given
     * way point list.
     */
    @Override
    public void visitWayPoint(WayPoint wp) {
        if (wp.getTime().after(end)) {
            setEnd(wp);
        }

        if (wp.getTime().before(start)) {
            setStart(wp);
        }

        // update boundaries
        if (bounds == null) {
            bounds = new Bounds(wp.getCoor());
        } else {
            bounds.extend(wp.getCoor());
        }

        int ele = (int) ElevationHelper.getElevation(wp);

        if (!isIgnoreZeroHeight() || ele > 0) {
            if (ele > maxHeight) {
                setMaxWayPoint(wp);
            }
            if (ele < minHeight) {
                setMinWayPoint(wp);
            }

            if (ele > lastEle) {
                gain += ele - lastEle;
            }

            sumEle += ele;
            lastEle = ele;
        }
    }

    @Override
    public String toString() {
        return name; /*"ElevationProfileBase [start=" + getStart() + ", end=" + getEnd()
                + ", minHeight=" + getMinHeight() + ", maxHeight="
                + getMaxHeight() + "]";*/
    }
}
