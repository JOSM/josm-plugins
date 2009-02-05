package cadastre_fr;

import java.io.Serializable;

import org.openstreetmap.josm.data.coor.EastNorth;

public class EastNorthBound implements Serializable {

    private static final long serialVersionUID = 8451650309216472069L;
    
    public EastNorth min, max;
    public EastNorthBound(EastNorth min, EastNorth max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(EastNorth eastNorth) {
        if (eastNorth.east() < min.east() || eastNorth.north() < min.north())
            return false;
        if (eastNorth.east() > max.east() || eastNorth.north() > max.north())
            return false;
        return true;
    }

    @Override public String toString() {
        return "EastNorthBound[" + min.east() + "," + min.north() + "," + max.east() + "," + max.north() + "]";
    }
}

