// License: GPL. Copyright 2007 by Tim Haussmann

/**
 * This class implements the Mercator Projection as it is used by Openstreetmap (and google). 
 * It provides methods to translate coordinates from 'map space' into latitude and longitude (on
 * the WGS84 ellipsoid) and vice versa. Map space is measured in pixels. The origin of the map space
 * is the top left corner. The map space origin (0,0)  has latitude ~85 and longitude -180
 * 
 * @author Tim Haussmann
 *
 */

public class OsmMercator{

	private static int TILE_SIZE = 256;	
	public static final double MAX_LAT = 85.05112877980659;
	public static final double MIN_LAT = -85.05112877980659;
	
	
	public static double radius(int aZoomlevel){
		return (TILE_SIZE * Math.pow(2, aZoomlevel))/ (2 * Math.PI);		
	}
	
	/**
	 * Returns the absolut number of pixels in y or x, defined as:
	 * 2^Zoomlevel * TILE_WIDTH
	 * where TILE_WIDTH is the width of a tile in pixels 
	 * @param aZoomlevel
	 * @return
	 */
	public static int getMaxPixels(int aZoomlevel){
		return (int) (TILE_SIZE * Math.pow(2, aZoomlevel));
	}
		
	public static int falseEasting(int aZoomlevel){
		return (int)getMaxPixels(aZoomlevel)/2;
	}
	public static int falseNorthing(int aZoomlevel){
		return (int) ( -1*getMaxPixels(aZoomlevel)/2);
	}
	
	/**
	 * Transform longitude to pixelspace
	 * @param aLongitude [-180..180]
	 * @return [0..2^Zoomlevel*TILE_SIZE[
	 */
	public static int LonToX(double aLongitude, int aZoomlevel){		
		double longitude = Math.toRadians(aLongitude);
		return (int) ((radius(aZoomlevel) * longitude) + falseEasting(aZoomlevel));
	}
	
	/**
	 * Transforms latitude to pixelspace	
	 * @param aLat [-90...90]
	 * @return [0..2^Zoomlevel*TILE_SIZE[
	 */
	public static int LatToY(double aLat, int aZoomlevel){		
		if(aLat < MIN_LAT)
			aLat = MIN_LAT;
		else if(aLat > MAX_LAT)
			aLat = MAX_LAT;
		double latitude = Math.toRadians(aLat);
		return (int) (-1*(radius(aZoomlevel)/2.0 * Math.log((1.0 + Math.sin(latitude))/ (1.0 - Math.sin(latitude)))) - falseNorthing(aZoomlevel));
	}
	
	/**
	 * Transforms pixel coordinate X to longitude
	 * @param aX [0..2^Zoomlevel*TILE_WIDTH[
	 * @return ]-180..180[
	 */
	public static double XToLon(int aX, int aZoomlevel){
		aX -= falseEasting(aZoomlevel);
		double longRadians = aX/radius(aZoomlevel);
		double longDegrees = Math.toDegrees(longRadians);
		double rotations = Math.floor((longDegrees + 180)/360);
		double longitude = longDegrees - (rotations * 360);
		return longitude;
	}
	
	/**
	 * Transforms pixel coordinate Y to latitude
	 * @param aY [0..2^Zoomlevel*TILE_WIDTH[
	 * @return [MIN_LAT..MAX_LAT] is about [-85..85]
	 */
	public static double YToLat(int aY, int aZoomlevel){
		aY += falseNorthing(aZoomlevel);
		double latitude =  (Math.PI/2) - (2 * Math.atan(Math.exp(-1.0 * aY /radius(aZoomlevel))));		
		return -1*Math.toDegrees(latitude) ;
	}

	
}
