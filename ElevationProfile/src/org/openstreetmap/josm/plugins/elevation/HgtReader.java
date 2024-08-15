// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.IOUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 *  Class HgtReader reads data from SRTM HGT files. Currently this class is restricted to a resolution of 3 arc seconds.
 *
 *  SRTM data files are available at the <a href="http://dds.cr.usgs.gov/srtm/version2_1/SRTM3">NASA SRTM site</a>
 *  @author Oliver Wieland &lt;oliver.wieland@online.de&gt;
 */
public class HgtReader {
    private static final int SRTM_EXTENT = 1; // degree
    private static final List<String> COMPRESSION_EXT = Arrays.asList("xz", "gzip", "zip", "bz", "bz2");

    public static final String HGT_EXT = ".hgt";

    // alter these values for different SRTM resolutions
    public static final int HGT_VOID = Short.MIN_VALUE; // magic number which indicates 'void data' in HGT file

    private static final HashMap<String, short[][]> cache = new HashMap<>();

    public static double getElevationFromHgt(ILatLon coor) {
        try {
            String file = getHgtFileName(coor);
            // given area in cache?
            if (!cache.containsKey(file)) {

                // fill initial cache value. If no file is found, then
                // we use it as a marker to indicate 'file has been searched
                // but is not there'
                cache.put(file, null);
                // Try all resource directories
                for (String location : Preferences.getAllPossiblePreferenceDirs()) {
                    String fullPath = new File(location + File.separator + "elevation", file).getPath();
                    File f = new File(fullPath);
                    if (!f.exists()) {
                        for (String ext : COMPRESSION_EXT) {
                            f = new File(fullPath + "." + ext);
                            if (f.exists()) break;
                        }
                    }
                    if (f.exists()) {
                        read(f);
                        break;
                    }
                }
            }

            // read elevation value
            return readElevation(coor, file);
        } catch (FileNotFoundException e) {
            Logging.error("Get elevation from HGT " + coor + " failed: => " + e.getMessage());
            // no problem... file not there
            return ElevationHelper.NO_ELEVATION;
        } catch (Exception ioe) {
            // oops...
            Logging.error(ioe);
            // fallback
            return ElevationHelper.NO_ELEVATION;
        }
    }

    public static Bounds read(File file) throws IOException {
        String location = file.getName();
        for (String ext : COMPRESSION_EXT) {
            location = location.replaceAll("\\." + ext + "$", "");
        }
        short[][] sb = readHgtFile(file.getPath());
        // Overwrite the cache file (assume that is desired)
        cache.put(location, sb);
        Pattern pattern = Pattern.compile("([NS])(\\d{2})([EW])(\\d{3})");
        Matcher matcher = pattern.matcher(location);
        if (matcher.lookingAt()) {
            int lat = ("S".equals(matcher.group(1)) ? -1 : 1) * Integer.parseInt(matcher.group(2));
            int lon = ("W".equals(matcher.group(3)) ? -1 : 1) * Integer.parseInt(matcher.group(4));
            return new Bounds(lat, lon, lat + 1, lon + 1);
        }
        return null;
    }

    private static short[][] readHgtFile(String file) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(file);

        short[][] data = null;

        try (InputStream fis = Compression.getUncompressedFileInputStream(Paths.get(file))) {
            // choose the right endianness
            ByteBuffer bb = ByteBuffer.wrap(fis.readAllBytes());
            bb.order(ByteOrder.BIG_ENDIAN);
            int size = (int) Math.sqrt(bb.array().length / 2.0);
            data = new short[size][size];
            int x = 0;
            int y = 0;
            while (x < size) {
                while (y < size) {
                    data[x][y] = bb.getShort(2 * (x * size + y));
                    y++;
                }
                x++;
                y = 0;
            }
        }

        return data;
    }

    /**
     * Reads the elevation value for the given coordinate.
     *
     * See also <a href="https://gis.stackexchange.com/questions/43743/how-to-extract-elevation-from-hgt-file">stackexchange.com</a>
     * @param coor the coordinate to get the elevation data for
     * @return the elevation value or <code>Double.NaN</code>, if no value is present
     */
    public static double readElevation(ILatLon coor) {
        String tag = getHgtFileName(coor);
        return readElevation(coor, tag);
    }

    /**
     * Reads the elevation value for the given coordinate.
     *
     * See also <a href="https://gis.stackexchange.com/questions/43743/how-to-extract-elevation-from-hgt-file">stackexchange.com</a>
     * @param coor the coordinate to get the elevation data for
     * @param fileName The expected filename
     * @return the elevation value or <code>Double.NaN</code>, if no value is present
     */
    public static double readElevation(ILatLon coor, String fileName) {

        short[][] sb = cache.get(fileName);

        if (sb == null) {
            return ElevationHelper.NO_ELEVATION;
        }

        int[] index = getIndex(coor, sb.length);
        short ele = sb[index[0]][index[1]];

        if (ele == HGT_VOID) {
            return ElevationHelper.NO_ELEVATION;
        }
        return ele;
    }

    public static Optional<Bounds> getBounds(ILatLon location) {
        final String fileName = getHgtFileName(location);
        final short[][] sb = cache.get(fileName);

        if (sb == null) {
            return Optional.empty();
        }

        final double latDegrees = location.lat();
        final double lonDegrees = location.lon();

        final float fraction = ((float) SRTM_EXTENT) / sb.length;
        final int latitude = (int) Math.floor(latDegrees) + (latDegrees < 0 ? 1 : 0);
        final int longitude = (int) Math.floor(lonDegrees) + (lonDegrees < 0 ? 1 : 0);

        final int[] index = getIndex(location, sb.length);
        final int latSign = latitude > 0 ? 1 : -1;
        final int lonSign = longitude > 0 ? 1 : -1;
        final double minLat = latitude + latSign * fraction * index[0];
        final double maxLat = latitude + latSign * fraction * (index[0] + 1);
        final double minLon = longitude + lonSign * fraction * index[1];
        final double maxLon = longitude + lonSign * fraction * (index[1] + 1);
        return Optional.of(new Bounds(Math.min(minLat, maxLat), Math.min(minLon, maxLon),
                Math.max(minLat, maxLat), Math.max(minLon, maxLon)));
    }

    /**
     * Get the index to use for a short[latitude][longitude] = height in meters array
     *
     * @param latLon
     *            The location to get the index for
     * @param mapSize
     *            The size of the map
     * @return A [latitude, longitude] = int (index) array.
     */
    private static int[] getIndex(ILatLon latLon, int mapSize)
    {
        double latDegrees = latLon.lat();
        double lonDegrees = latLon.lon();

        float fraction = ((float) SRTM_EXTENT) / (mapSize - 1);
        int latitude = (int) Math.round(frac(Math.abs(latDegrees)) / fraction);
        int longitude = (int) Math.round(frac(Math.abs(lonDegrees)) / fraction);
        if (latDegrees >= 0)
        {
            latitude = mapSize - latitude - 1;
        }
        if (lonDegrees < 0)
        {
            longitude = mapSize - longitude - 1;
        }
        return new int[] { latitude, longitude };
    }

    /**
     * Gets the associated HGT file name for the given way point. Usually the
     * format is <tt>[N|S]nn[W|E]mmm.hgt</tt> where <i>nn</i> is the integral latitude
     * without decimals and <i>mmm</i> is the longitude.
     *
     * @param latLon the coordinate to get the filename for
     * @return the file name of the HGT file
     */
    public static String getHgtFileName(ILatLon latLon) {
        int lat = (int) Math.floor(latLon.lat());
        int lon = (int) Math.floor(latLon.lon());

        String latPref = "N";
        if (lat < 0) {
            latPref = "S";
            lat = Math.abs(lat);
        }

        String lonPref = "E";
        if (lon < 0) {
            lonPref = "W";
            lon = Math.abs(lon);
        }

        return String.format("%s%2d%s%03d" + HGT_EXT, latPref, lat, lonPref, lon);
    }

    public static double frac(double d) {
        long iPart;
        double fPart;

        // Get user input
        iPart = (long) d;
        fPart = d - iPart;
        return fPart;
    }

    public static void clearCache() {
        cache.clear();
    }
}
