// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.openstreetmap.josm.plugins.opendata.core.io.geographic.MifDatum.CUSTOM;
import static org.openstreetmap.josm.plugins.opendata.core.io.geographic.MifDatum.GEODETIC_REFERENCE_SYSTEM_1980_GRS_80;
import static org.openstreetmap.josm.plugins.opendata.core.io.geographic.MifProjection.Hotine_Oblique_Mercator;
import static org.openstreetmap.josm.plugins.opendata.core.io.geographic.MifProjection.Longitude_Latitude;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.CustomProjection.Param;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.gui.ChooserLauncher;
import org.openstreetmap.josm.plugins.opendata.core.io.InputStreamReaderUnbuffered;
import org.openstreetmap.josm.plugins.opendata.core.util.OdUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * MapInfo Interchange File (MIF) reader, based on these specifications:<ul>
 * <li><a href="https://github.com/tricycle/electrodrive-market-analysis/blob/master/specifications/Mapinfo_Mif.pdf">Mapinfo_Mif.pdf</a></li>
 * <li><a href="http://resource.mapinfo.com/static/files/document/1074660800077/interchange_file.pdf">interchange_file.pdf</a></li>
 * </ul>
 * These files have been stored in reference directory to avoid future dead links.
 */
public final class MifReader extends AbstractMapInfoReader {

    private enum State {
        UNKNOWN,
        READING_COLUMNS,
        START_POLYGON,
        READING_POINTS,
        END_POLYGON,
        START_POLYLINE_SEGMENT,
        END_POLYLINE
    }

    private final AbstractDataSetHandler handler;

    private File file;
    private InputStream stream;
    private BufferedReader midReader;

    private Character delimiter = '\t';

    private State state = State.UNKNOWN;

    private Projection josmProj;
    private DataSet dataSet;
    private Relation region;
    private Way polygon;
    private Way polyline;

    // CoordSys clause
    private String units;
    private double originLon = Double.NaN;
    private double originLat = Double.NaN;
    private double stdP1 = Double.NaN;
    private double stdP2 = Double.NaN;
    private double scaleFactor = Double.NaN;
    private double falseEasting = Double.NaN;
    private double falseNorthing = Double.NaN;
    private double minx = Double.NaN;
    private double miny = Double.NaN;
    private double maxx = Double.NaN;
    private double maxy = Double.NaN;

    // Region clause
    private int numpolygons = -1;
    private int numpts = -1;

    // PLine clause
    private int numsections = -1;

    private MifReader(AbstractDataSetHandler handler) {
        this.handler = handler;
    }

    public static DataSet parseDataSet(InputStream in, File file,
            AbstractDataSetHandler handler) throws IOException {
        return new MifReader(handler).parse(in, file, Charset.forName(OdConstants.ISO8859_15));
    }

    private void parseDelimiter(String[] words) {
        delimiter = words[1].charAt(1);
    }

    private void parseUnique() {
        // TODO
        Logging.warn("TODO Unique: "+line);
    }

    private void parseIndex() {
        // TODO
        Logging.warn("TODO Index: "+line);
    }

    private static String param(Param p, Object value) {
        return " +"+p.key+"="+value;
    }

    private void parseCoordSysSyntax1(String[] words) {
        MifProjection proj = MifProjection.forCode(Integer.parseInt(words[3]));
        MifDatum datum = MifDatum.forCode(Integer.parseInt(words[4]));

        // Custom datum: TODO: use custom decalage values
        int offset = datum == CUSTOM ? 4 : 0;

        if (proj == Longitude_Latitude) {
            josmProj = Projections.getProjectionByCode("EPSG:4326"); // WGS 84
            return;
        }

        // Initialize proj4-like parameters
        String params = param(Param.proj, proj.getProj4Id());

        // Units
        units = words[5+offset];
        params += param(Param.units, units);

        // Origin, longitude
        originLon = Double.parseDouble(words[6+offset]);
        params += param(Param.lon_0, originLon);

        // Origin, latitude
        switch(proj) {
        case Albers_Equal_Area_Conic:
        case Azimuthal_Equidistant_polar_aspect_only:
        case Equidistant_Conic_also_known_as_Simple_Conic:
        case Hotine_Oblique_Mercator:
        case Lambert_Azimuthal_Equal_Area_polar_aspect_only:
        case Lambert_Conformal_Conic:
        case Lambert_Conformal_Conic_modified_for_Belgium_1972:
        case New_Zealand_Map_Grid:
        case Stereographic:
        case Swiss_Oblique_Mercator:
        case Transverse_Mercator_also_known_as_Gauss_Kruger:
        case Transverse_Mercator_modified_for_Danish_System_34_Jylland_Fyn:
        case Transverse_Mercator_modified_for_Danish_System_45_Bornholm:
        case Transverse_Mercator_modified_for_Finnish_KKJ:
        case Transverse_Mercator_modified_for_Sjaelland:
        case Polyconic:
            originLat = Double.parseDouble(words[7+offset]);
            params += param(Param.lat_0, originLat);
            break;
        default:
            Logging.trace("originLat not set for " + proj);
        }

        // Standard Parallel 1
        switch (proj) {
        case Cylindrical_Equal_Area:
        case Regional_Mercator:
            stdP1 = Double.parseDouble(words[7+offset]);
            params += param(Param.lat_1, stdP1);
            break;
        case Albers_Equal_Area_Conic:
        case Equidistant_Conic_also_known_as_Simple_Conic:
        case Lambert_Conformal_Conic:
        case Lambert_Conformal_Conic_modified_for_Belgium_1972:
            stdP1 = Double.parseDouble(words[8+offset]);
            params += param(Param.lat_1, stdP1);
            break;
        default:
            Logging.trace("stdP1 not set for " + proj);
        }

        // Standard Parallel 2
        switch (proj) {
        case Albers_Equal_Area_Conic:
        case Equidistant_Conic_also_known_as_Simple_Conic:
        case Lambert_Conformal_Conic:
        case Lambert_Conformal_Conic_modified_for_Belgium_1972:
            stdP2 = Double.parseDouble(words[9+offset]);
            params += param(Param.lat_2, stdP2);
            break;
        default:
            Logging.trace("stdP2 not set for " + proj);
        }

        // Azimuth
        if (proj == Hotine_Oblique_Mercator) {
            Double.parseDouble(words[8+offset]);
            // TODO: what's proj4 parameter ?
        }

        // Scale Factor
        switch (proj) {
        case Hotine_Oblique_Mercator:
            scaleFactor = Double.parseDouble(words[9+offset]);
            params += param(Param.k_0, scaleFactor);
            break;
        case Stereographic:
        case Transverse_Mercator_also_known_as_Gauss_Kruger:
        case Transverse_Mercator_modified_for_Danish_System_34_Jylland_Fyn:
        case Transverse_Mercator_modified_for_Danish_System_45_Bornholm:
        case Transverse_Mercator_modified_for_Finnish_KKJ:
        case Transverse_Mercator_modified_for_Sjaelland:
            scaleFactor = Double.parseDouble(words[8+offset]);
            params += param(Param.k_0, scaleFactor);
            break;
        default:
            Logging.trace("scaleFactor not set for " + proj);
        }

        // False Easting/Northing
        switch (proj) {
        case Albers_Equal_Area_Conic:
        case Equidistant_Conic_also_known_as_Simple_Conic:
        case Hotine_Oblique_Mercator:
        case Lambert_Conformal_Conic:
        case Lambert_Conformal_Conic_modified_for_Belgium_1972:
            falseEasting = Double.parseDouble(words[10+offset]);
            falseNorthing = Double.parseDouble(words[11+offset]);
            params += param(Param.x_0, falseEasting);
            params += param(Param.y_0, falseNorthing);
            break;
        case Stereographic:
        case Transverse_Mercator_also_known_as_Gauss_Kruger:
        case Transverse_Mercator_modified_for_Danish_System_34_Jylland_Fyn:
        case Transverse_Mercator_modified_for_Danish_System_45_Bornholm:
        case Transverse_Mercator_modified_for_Finnish_KKJ:
        case Transverse_Mercator_modified_for_Sjaelland:
            falseEasting = Double.parseDouble(words[9+offset]);
            falseNorthing = Double.parseDouble(words[10+offset]);
            params += param(Param.x_0, falseEasting);
            params += param(Param.y_0, falseNorthing);
            break;
        case New_Zealand_Map_Grid:
        case Swiss_Oblique_Mercator:
        case Polyconic:
            falseEasting = Double.parseDouble(words[8+offset]);
            falseNorthing = Double.parseDouble(words[9+offset]);
            params += param(Param.x_0, falseEasting);
            params += param(Param.y_0, falseNorthing);
            break;
        default:
            Logging.trace("falseEasting/falseNorthing not set for " + proj);
        }

        // Range
        switch (proj) {
        case Azimuthal_Equidistant_polar_aspect_only:
        case Lambert_Azimuthal_Equal_Area_polar_aspect_only:
            Double.parseDouble(words[8+offset]);
            // TODO: what's proj4 parameter ?
        default:
            Logging.trace("range not set for " + proj);
        }

        switch (proj) {
        case Lambert_Conformal_Conic:
            if ((datum == GEODETIC_REFERENCE_SYSTEM_1980_GRS_80 || datum == CUSTOM) && equals(originLon, 3.0)) {
                // This sounds good for Lambert 93 or Lambert CC 9
                if (equals(originLat, 46.5) && equals(stdP1, 44.0) && equals(stdP2, 49.0)
                        && equals(falseEasting, 700000.0) && equals(falseNorthing, 6600000.0)) {
                    josmProj = Projections.getProjectionByCode("EPSG:2154"); // Lambert 93
                } else if (equals(falseEasting, 1700000.0)) {
                    for (int i = 0; josmProj == null && i < 9; i++) {
                        if (equals(originLat, 42.0+i) && equals(stdP1, 41.25+i) && equals(stdP2, 42.75+i)
                                && equals(falseNorthing, (i+1)*1000000.0 + 200000.0)) {
                            josmProj = Projections.getProjectionByCode("EPSG:"+ (3942 + i)); // LambertCC9Zones
                        }
                    }
                }
            }
            break;
        default:
            Logging.trace("josmProj not set for " + proj);
        }

        // TODO: handle cases with Affine declaration
        int index = parseAffineUnits();

        // handle cases with Bounds declaration
        parseBounds(words, index);

        if (josmProj == null) {
            Logging.info(line);
            Logging.info(params);
            josmProj = new CustomProjection(params);
        }
    }

    private void parseCoordSysSyntax2(String[] words) {
        // handle cases with Affine declaration
        int index = parseAffineUnits();

        units = words[index+1];

        parseBounds(words, index+2);
    }

    private int parseAffineUnits() {
        // TODO: handle affine units
        return 2;
    }

    private void parseBounds(String[] words, int index) {
        if (index < words.length && "Bounds".equals(words[index])) {
            // Useless parenthesis... "(minx, miny) (maxx, maxy)"
            minx = Double.parseDouble(words[index+1].substring(1));
            miny = Double.parseDouble(words[index+2].substring(0, words[index+2].length()-1));
            maxx = Double.parseDouble(words[index+3].substring(1));
            maxy = Double.parseDouble(words[index+4].substring(0, words[index+4].length()-1));
            if (Logging.isTraceEnabled()) {
                Logging.trace(Arrays.toString(words) + " -> "+minx+","+miny+","+maxx+","+maxy);
            }
        }
    }

    private void parseCoordSys(String[] words) {
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replace(",", "");
        }
        switch (words[1].toLowerCase(Locale.ROOT)) {
        case "earth":
            parseCoordSysSyntax1(words);
            break;
        case "nonearth":
            parseCoordSysSyntax2(words);

            // CHECKSTYLE.OFF: LineLength
            // Syntax2 is not meant to be used for maps, and still... # 9592 happened
            // From MapInfo documentation:
            // http://testdrive.mapinfo.com/TDC/mxtreme4java.nsf/22fbc128f401ad818525666a00646bda/50100fdbe3e0a85085256a770053be1a/$FILE/coordsys.txt
            // Use syntax 1 (above) to explicitly define a coordinate system for an Earth map (a map having coordinates which are specified with respect to a
            // location on the surface of the Earth). The optional Projection parameters dictate what map projection, if any, should be used in conjunction with
            // the coordinate system. If the Projection clause is omitted, MapBasic uses a longitude, latitude coordinate system using the North American Datum of 1927 (NAD-27).
            // Use syntax 2 to explicitly define a non-Earth coordinate system, such as the coordinate system used in a floor plan or other CAD drawing.
            // CHECKSTYLE.ON: LineLength

            if (handler != null && handler.getMifHandler() != null && handler.getMifHandler().getCoordSysNonEarthProjection() != null) {
                josmProj = handler.getMifHandler().getCoordSysNonEarthProjection();
            } else {
                josmProj = ChooserLauncher.askForProjection(NullProgressMonitor.INSTANCE);
            }
            break;
        case "layout":
        case "table":
        case "window":
            Logging.error("Unsupported CoordSys clause: "+line);
            break;
        default:
            Logging.error("Line "+lineNum+". Invalid CoordSys clause: "+line);
        }
    }

    private void parseTransform() {
        // TODO
        Logging.warn("TODO Transform: "+line);
    }

    @Override
    protected void parseColumns(String[] words) {
        super.parseColumns(words);
        state = State.READING_COLUMNS;
    }

    private void parseData() {
        if (dataSet == null) {
            dataSet = new DataSet();
        }
    }

    private void parsePoint(String[] words) throws IOException {
        readAttributes(createNode(words[1], words[2]));
    }

    private void parseLine(String[] words) throws IOException {
        Way line = new Way();
        dataSet.addPrimitive(line);
        readAttributes(line);
        line.addNode(createNode(words[1], words[2]));
        line.addNode(createNode(words[3], words[4]));
    }

    private void startPolyLineSegment(boolean initial) throws IOException {
        Way previousPolyline = polyline;
        polyline = new Way();
        dataSet.addPrimitive(polyline);
        if (initial) {
            readAttributes(polyline);
        } else if (previousPolyline != null) {
            // Not sure about how to handle multiple segments. In doubt we create a new way with the same tags
            polyline.setKeys(previousPolyline.getKeys());
        }
        state = State.READING_POINTS;
    }

    private void parsePLine(String[] words) throws IOException {
        numsections = 1;
        if (words.length <= 1 || "MULTIPLE".equalsIgnoreCase(words[1])) {
            numpts = -1;
            state = State.START_POLYLINE_SEGMENT;
            if (words.length >= 3) {
                // pline with multiple sections
                numsections = Integer.parseInt(words[2]);
            }
        } else {
            numpts = Integer.parseInt(words[1]); // Not described in PDF but found in real files: PLINE XX, with XX = numpoints
            startPolyLineSegment(true);
        }
    }

    private void parseRegion(String[] words) throws IOException {
        numpolygons = Integer.parseInt(words[1]);
        if (numpolygons > 1) {
            region = new Relation();
            region.put("type", "multipolygon");
            dataSet.addPrimitive(region);
            readAttributes(region);
        } else {
            region = null;
        }
        state = State.START_POLYGON;
    }

    private void parseArc() {
        // TODO
        Logging.warn("TODO Arc: "+line);
    }

    private void parseText() {
        // TODO
        Logging.warn("TODO Text: "+line);
    }

    private void parseRect() {
        // TODO
        Logging.warn("TODO Rect: "+line);
    }

    private void parseRoundRect() {
        // TODO
        Logging.warn("TODO RoundRect: "+line);
    }

    private void parseEllipse() {
        // TODO
        Logging.warn("TODO Ellipse: "+line);
    }

    private void initializeReaders(InputStream in, File f, Charset cs, int bufSize) throws IOException {
        stream = in;
        file = f;
        Reader isr;
        // Did you know ? new InputStreamReader(in, charset) has a non-configurable buffer of 8kb :(
        if (bufSize < 8192) {
            isr = new InputStreamReaderUnbuffered(in, cs);
        } else {
            isr = new InputStreamReader(in, cs);
        }
        headerReader = new BufferedReader(isr, bufSize);
        if (midReader != null) {
            midReader.close();
        }
        midReader = getDataReader(file, ".mid", cs);
    }

    private DataSet parse(InputStream in, File file, Charset cs) throws IOException {
        try {
            try {
                // Read header byte per byte until we determine correct charset
                initializeReaders(in, file, cs, 1);
                parseHeader();
                return dataSet;
            } finally {
                if (midReader != null) {
                    midReader.close();
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void parseHeaderLine(String[] words) throws IOException {
        if ("Version".equalsIgnoreCase(words[0])) {
            parseVersion(words);
        } else if ("Charset".equalsIgnoreCase(words[0])) {
            // Reinitialize readers with an efficient buffer value now we know for sure the good charset
            initializeReaders(stream, file, parseCharset(words), 8192);
        } else if ("Delimiter".equalsIgnoreCase(words[0])) {
            parseDelimiter(words);
        } else if ("Unique".equalsIgnoreCase(words[0])) {
            parseUnique();
        } else if ("Index".equalsIgnoreCase(words[0])) {
            parseIndex();
        } else if ("CoordSys".equalsIgnoreCase(words[0])) {
            parseCoordSys(words);
        } else if ("Transform".equalsIgnoreCase(words[0])) {
            parseTransform();
        } else if ("Columns".equalsIgnoreCase(words[0])) {
            parseColumns(words);
        } else if ("Data".equalsIgnoreCase(words[0])) {
            parseData();
        } else if (dataSet != null) {
            if (state == State.START_POLYGON) {
                numpts = Integer.parseInt(words[0]);
                polygon = new Way();
                dataSet.addPrimitive(polygon);
                if (region != null) {
                    region.addMember(new RelationMember("outer", polygon));
                } else {
                    readAttributes(polygon);
                }
                state = State.READING_POINTS;

            } else if (state == State.START_POLYLINE_SEGMENT) {
                numpts = Integer.parseInt(words[0]);
                startPolyLineSegment(polyline != null);

            } else if (state == State.READING_POINTS && numpts > 0) {
                if (josmProj != null) {
                    Node node = createNode(words[0], words[1]);
                    if (polygon != null) {
                        polygon.addNode(node);
                    } else if (polyline != null) {
                        polyline.addNode(node);
                    }
                }
                if (--numpts == 0) {
                    if (numpolygons > -1) {
                        if (polygon != null && !polygon.isClosed()) {
                            polygon.addNode(polygon.firstNode());
                        }
                        if (--numpolygons > 0) {
                            state = State.START_POLYGON;
                        } else {
                            state = State.END_POLYGON;
                            polygon = null;
                        }
                    } else if (polyline != null) {
                        if (--numsections > 0) {
                            state = State.START_POLYLINE_SEGMENT;
                        } else {
                            state = State.UNKNOWN;
                            polyline = null;
                        }
                    }
                }
            } else if ("Point".equalsIgnoreCase(words[0])) {
                parsePoint(words);
            } else if ("Line".equalsIgnoreCase(words[0])) {
                parseLine(words);
            } else if ("PLine".equalsIgnoreCase(words[0])) {
                parsePLine(words);
            } else if ("Region".equalsIgnoreCase(words[0])) {
                parseRegion(words);
            } else if ("Arc".equalsIgnoreCase(words[0])) {
                parseArc();
            } else if ("Text".equalsIgnoreCase(words[0])) {
                parseText();
            } else if ("Rect".equalsIgnoreCase(words[0])) {
                parseRect();
            } else if ("RoundRect".equalsIgnoreCase(words[0])) {
                parseRoundRect();
            } else if ("Ellipse".equalsIgnoreCase(words[0])) {
                parseEllipse();
            } else if (!"Pen".equalsIgnoreCase(words[0])
                    && !"Brush".equalsIgnoreCase(words[0])
                    && !"Center".equalsIgnoreCase(words[0])
                    && !"Symbol".equalsIgnoreCase(words[0])
                    && !"Font".equalsIgnoreCase(words[0])
                    && !words[0].isEmpty()) {
                // Pen, Brush, Center, Symbol, and Font we currently ignore
                Logging.warn("Line "+lineNum+". Unknown clause in data section: "+line);
            }
        } else if (state == State.READING_COLUMNS && numcolumns > 0) {
            columns.add(words[0]);
            if (--numcolumns == 0) {
                state = State.UNKNOWN;
            }
        } else if (!line.isEmpty()) {
            Logging.warn("Line "+lineNum+". Unknown clause in header: "+line);
        }
    }

    private void readAttributes(OsmPrimitive p) throws IOException {
        if (midReader != null) {
            String midLine = midReader.readLine();
            if (midLine != null) {
                String[] fields = OdUtils.stripQuotesAndExtraChars(midLine.split(delimiter.toString()), delimiter.toString());
                if (columns.size() != fields.length) {
                    Logging.error("Incoherence between MID and MIF files ("+columns.size()+" columns vs "+fields.length+" fields)");
                }
                for (int i = 0; i < Math.min(columns.size(), fields.length); i++) {
                    String field = fields[i].trim();
                    /*if (field.startsWith("\"") && field.endsWith("\"")) {
                        field = fields[i].substring(fields[i].indexOf('"')+1, fields[i].lastIndexOf('"'));
                    }*/
                    if (!field.isEmpty()) {
                        p.put(columns.get(i), field);
                    }
                }
            }
        }
    }

    private Node createNode(String x, String y) {
        Node newNode = new Node(josmProj.eastNorth2latlon(new EastNorth(Double.parseDouble(x), Double.parseDouble(y))));
        dataSet.addPrimitive(newNode);
        return newNode;
    }

    /**
     * Compare two doubles within a default epsilon
     * @param a first double
     * @param b second double
     * @return {@code true} if {@code a} and {@code b} are equals
     */
    public static boolean equals(double a, double b) {
        if (a == b) return true;
        // If the difference is less than epsilon, treat as equal.
        return Math.abs(a - b) < 0.0000001;
    }
}
