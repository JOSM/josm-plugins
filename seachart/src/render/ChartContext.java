// License: GPL. For details, see LICENSE file.
package render;

import java.awt.Color;
import java.awt.geom.Point2D;

import s57.S57map;
import s57.S57map.Feature;
import s57.S57map.Snode;

/**
 * @author Malcolm Herring
 */
public interface ChartContext {
    enum RuleSet { ALL, BASE, SEAMARK }
    class Chart {
    	public int zoom;
    	public double scale;
    	public int grid;
    	public boolean rose;
    	public double roseLat;
    	public double roseLon;
    	public Chart() {
    		zoom = 15;
    		scale = 1.0;
    		grid = 0;
    		rose = false;
    		roseLat = roseLon = 0;
    	}
    }

    Point2D getPoint(Snode coord);

    double mile(Feature feature);

    boolean clip();
    
    int grid();
    
    Chart chart();

    Color background(S57map map);

    RuleSet ruleset();
}
