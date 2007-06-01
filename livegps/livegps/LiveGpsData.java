/**
 * 
 */
package livegps;

import java.awt.Point;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Way;

/**
 * @author cdaller
 *
 */
public class LiveGpsData {
    private LatLon latLon;
    private float course;
    private float speed;
    private boolean fix;
    private String wayString;
    
    /**
     * @param latitude
     * @param longitude
     * @param course
     * @param speed
     * @param haveFix
     */
    public LiveGpsData(double latitude, double longitude, float course, float speed, boolean haveFix) {
        super();
        this.latLon = new LatLon(latitude, longitude);
        this.course = course;
        this.speed = speed;
        this.fix = haveFix;
    }
    /**
     * 
     */
    public LiveGpsData() {
        // TODO Auto-generated constructor stub
    }
    /**
     * @return the course
     */
    public float getCourse() {
        return this.course;
    }
    /**
     * @param course the course to set
     */
    public void setCourse(float course) {
        this.course = course;
    }
    /**
     * @return the haveFix
     */
    public boolean isFix() {
        return this.fix;
    }
    /**
     * @param haveFix the haveFix to set
     */
    public void setFix(boolean haveFix) {
        this.fix = haveFix;
    }
    /**
     * @return the latitude
     */
    public double getLatitude() {
        return this.latLon.lat();
    }
    /**
     * @return the longitude
     */
    public double getLongitude() {
        return this.latLon.lon();
    }
    /**
     * @return the speed in metres per second!
     */
    public float getSpeed() {
        return this.speed;
    }
    /**
     * @param speed the speed to set
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    
    /**
     * @return the latlon
     */
    public LatLon getLatLon() {
        return this.latLon;
    }
    
    /**
     * @param latLon
     */
    public void setLatLon(LatLon latLon) {
        this.latLon = latLon;
    }
    
    public String toString() {
        return getClass().getSimpleName() + "[fix=" + fix + ", lat=" + latLon.lat() 
        + ", long=" + latLon.lon() + ", speed=" + speed + ", course=" + course + "]";
        
    }
    
    /**
     * Returns the name of the way that is closest to the current coordinates or an
     * empty string if no way is around.
     * 
     * @return the name of the way that is closest to the current coordinates.
     */
    public String getWay() {
        if(wayString == null) {
            EastNorth eastnorth = Main.proj.latlon2eastNorth(getLatLon()); 
            Point xy = Main.map.mapView.getPoint(eastnorth); 
            Way way = Main.map.mapView.getNearestWay(xy);
            if(way != null) {
                wayString = way.get("name") + " (" + way.get("highway") + ")";
            } else {
                wayString = "";
            }
        }
        return wayString;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(this.course);
        result = prime * result + ((this.latLon == null) ? 0 : this.latLon.hashCode());
        result = prime * result + Float.floatToIntBits(this.speed);
        return result;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
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
