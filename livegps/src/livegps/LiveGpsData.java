// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Representation of a single LiveGPS data epoch
 * @author cdaller
 */
public class LiveGpsData {
    private boolean fix;
    private LatLon latLon;
    private float course;
    private float speed;
    private float epx, epy;
    private String wayString;
    private Way way;
    private WayPoint wp;

    public LiveGpsData(double latitude, double longitude, float course, float speed) {
        this.fix = true;
        this.latLon = new LatLon(latitude, longitude);
        this.course = course;
        this.speed = speed;
    }

    public LiveGpsData(double latitude, double longitude, float course, float speed, float epx, float epy) {
        this.fix = true;
        this.latLon = new LatLon(latitude, longitude);
        this.course = course;
        this.speed = speed;
        this.epx = epx;
        this.epy = epy;
    }

    /**
     * Return the waypoint associated with this data set (can be {@code null})
     * @return waypoint with additional information or {@code null}
     */
    public WayPoint getWaypoint() {
        return this.wp;
    }

    /**
     * Set the waypoint to transfer additional data form NMEA input
     * @param wp waypoint to set
     */
    public void setWaypoint(WayPoint wp) {
        this.wp = wp;
    }

    /**
     * Return the course value
     * @return course value in degree
     */
    public float getCourse() {
        return this.course;
    }

    /**
     * Set the course value in degree
     * @param course course to set
     */
    public void setCourse(float course) {
        this.course = course;
    }

    /**
     * Return the fix status (whether there is a position solution or not)
     * @return fix status
     */
    public boolean isFix() {
        return this.fix;
    }

    /**
     * Set the fix status (whether there is a position solution or not)
     * @param haveFix the haveFix to set
     */
    public void setFix(boolean haveFix) {
        this.fix = haveFix;
    }

    /**
     * Return the latitude part of the position
     * @return latitude of position
     */
    public double getLatitude() {
        return this.latLon.lat();
    }

    /**
     * Return the longitude part of the position
     * @return longitude of position
     */
    public double getLongitude() {
        return this.latLon.lon();
    }

    /**
     * Return the speed (m/s)
     * @return speed in metres per second!
     */
    public float getSpeed() {
        return this.speed;
    }

    /**
     * Set the speed (m/s)
     * @param speed the speed to set
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Return the position with latitude and logitude combined
     * @return both position components
     */
    public LatLon getLatLon() {
        return this.latLon;
    }

    /**
     * Set the position with latitude and logitude combined
     * @param latLon position to set
     */
    public void setLatLon(LatLon latLon) {
        this.latLon = latLon;
    }

    public void setEpy(float epy) {
        this.epy = epy;
    }

    public void setEpx(float epx) {
        this.epx = epx;
    }

    public float getEpy() {
        return this.epy;
    }

    public float getEpx() {
        return this.epx;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[fix=" + fix + ", lat=" + latLon.lat()
        + ", long=" + latLon.lon() + ", speed=" + speed + ", course=" + course + ']';
    }

    /**
     * Returns the name of the way that is closest to the current coordinates or an
     * empty string if no way is around.
     *
     * @return the name of the way that is closest to the current coordinates.
     */
    public String getWayInfo() {
        if (wayString == null) {
            Way way = getWay();
            if (way != null) {
                StringBuilder builder = new StringBuilder();
                String tmp = way.get("name");
                if (tmp != null) {
                    builder.append(tmp);
                } else {
                    builder.append(tr("no name"));
                }
                tmp = way.get("ref");
                if (tmp != null) {
                    builder.append(" (").append(tmp).append(")");
                }
                tmp = way.get("highway");
                if (tmp != null) {
                    builder.append(" {").append(tmp).append("}");
                }
                String type = "";
                tmp = way.get("tunnel");
                if (tmp != null) {
                    type = type + "T";
                }
                tmp = way.get("bridge");
                if (tmp != null) {
                    type = type + "B";
                }
                if (type.length() > 0) {
                    builder.append(" [").append(type).append("]");
                }
                wayString = builder.toString();
            } else {
                wayString = "";
            }
        }
        return wayString;
    }

    /**
     * Returns the closest way to this position.
     * @return the closest way to this position.
     */
    public Way getWay() {
        MapFrame map = MainApplication.getMap();
        if (way == null && map != null && map.mapView != null) {
            Point xy = map.mapView.getPoint(getLatLon());
            way = map.mapView.getNearestWay(xy, OsmPrimitive::isUsable);
        }
        return way;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(this.course);
        result = prime * result + ((this.latLon == null) ? 0 : this.latLon.hashCode());
        result = prime * result + Float.floatToIntBits(this.speed);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof LiveGpsData))
            return false;
        final LiveGpsData other = (LiveGpsData) obj;
        if (Float.floatToIntBits(this.course) != Float.floatToIntBits(other.course))
            return false;
        if (this.latLon == null) {
            if (other.latLon != null)
                return false;
        } else if (!this.latLon.equals(other.latLon))
            return false;
        if (Float.floatToIntBits(this.speed) != Float.floatToIntBits(other.speed))
            return false;
        return true;
    }
}
