// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.datasets.NationalHandlers;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GmlReader extends GeographicReader {

    public static final String GML_FEATURE_MEMBER = "featureMember";
    public static final String GML_LINE_STRING = "LineString";
    public static final String GML_LINEAR_RING = "LinearRing";
    public static final String GML_POINT = "Point";
    public static final String GML_SURFACE = "Surface";
    public static final String GML_SRS_NAME = "srsName";
    public static final String GML_SRS_DIMENSION = "srsDimension";
    public static final String GML_POS_LIST = "posList";
    public static final String GML_COORDINATES = "coordinates";

    private final GeometryFactory geometryFactory = new GeometryFactory();

    private final GmlHandler gmlHandler;

    private XMLStreamReader parser;

    private int dim;

    private final class CrsData {
        public CoordinateReferenceSystem crs;
        public MathTransform transform;
        public int dim;
        CrsData(CoordinateReferenceSystem crs, MathTransform transform, int dim) {
            this.crs = crs;
            this.transform = transform;
            this.dim = dim;
        }
    }

    private final Map<String, CrsData> crsDataMap = new HashMap<>();

    public GmlReader(XMLStreamReader parser, GmlHandler handler) {
        super(handler, NationalHandlers.DEFAULT_GML_HANDLERS);
        this.parser = parser;
        this.gmlHandler = handler;
    }

    public static DataSet parseDataSet(InputStream in, AbstractDataSetHandler handler, ProgressMonitor instance)
            throws IOException, XMLStreamException {
        InputStreamReader ir = UTFInputStreamReader.create(in, OdConstants.UTF8);
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(ir);
        try {
            return new GmlReader(parser, handler != null ? handler.getGmlHandler() : null).parseDoc(instance);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private boolean isElement(String element) {
        return parser.getLocalName().matches("(gml:)?"+element);
    }

    private DataSet parseDoc(ProgressMonitor instance) throws XMLStreamException, GeoCrsException, FactoryException,
    GeoMathTransformException, MismatchedDimensionException, TransformException {
        Component parent = instance != null ? instance.getWindowParent() : MainApplication.getMainFrame();
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (isElement(GML_FEATURE_MEMBER)) {
                    try {
                        parseFeatureMember(parent);
                    } catch (UserCancelException e) {
                        return ds;
                    }
                }
            }
        }
        return ds;
    }

    private void findCRS(String srs) throws NoSuchAuthorityCodeException, FactoryException {
        Logging.info("Finding CRS for "+srs);
        if (gmlHandler != null) {
            crs = gmlHandler.getCrsFor(srs);
        } else {
            for (GmlHandler h : NationalHandlers.DEFAULT_GML_HANDLERS) {
                if ((crs = h.getCrsFor(srs)) != null) {
                    return;
                }
            }
        }
    }

    private void parseSrs(Component parent) throws GeoCrsException, FactoryException, UserCancelException, GeoMathTransformException {
        String srs = parser.getAttributeValue(null, GML_SRS_NAME);
        String sdim = parser.getAttributeValue(null, GML_SRS_DIMENSION);
        dim = sdim != null ? Integer.parseInt(sdim) : 2;
        CrsData crsData = crsDataMap.get(srs);
        if (crsData == null) {
            try {
                findCRS(srs);
            } catch (NoSuchAuthorityCodeException e) {
                e.printStackTrace();
            } catch (FactoryException e) {
                e.printStackTrace();
            }
            if (crs == null) {
                throw new GeoCrsException("Unable to detect CRS for srs '"+srs+"' !");
            } else {
                findMathTransform(parent, false);
            }
            crsDataMap.put(srs, new CrsData(crs, transform, dim));
        } else {
            crs = crsData.crs;
            transform = crsData.transform;
            dim = crsData.dim;
        }
    }

    private void parseFeatureMember(Component parent) throws XMLStreamException, GeoCrsException, FactoryException,
    UserCancelException, GeoMathTransformException, MismatchedDimensionException, TransformException {
        Way way = null;
        Node node = null;
        Map<String, StringBuilder> tags = new HashMap<>();
        OsmPrimitive prim = null;
        String key = null;
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (isElement(GML_LINE_STRING)) {
                    prim = way = createWay();
                    parseSrs(parent);
                } else if (isElement(GML_LINEAR_RING)) {
                    prim = way = createWay();
                } else if (isElement(GML_POINT)) {
                    parseSrs(parent);
                } else if (isElement(GML_SURFACE)) {
                    parseSrs(parent);
                } else if (isElement(GML_POS_LIST)) {
                    String[] tab = parser.getElementText().split(" ");
                    for (int i = 0; i < tab.length; i += dim) {
                        Point p = geometryFactory.createPoint(new Coordinate(Double.valueOf(tab[i]), Double.valueOf(tab[i+1])));
                        node = createOrGetNode(p, dim > 2 && !tab[i+2].equals("0") ? tab[i+2] : null);
                        if (way != null) {
                            way.addNode(node);
                        } else {
                            prim = node;
                        }
                    }
                } else if (isElement(GML_COORDINATES)) {
                    String[] tab = parser.getElementText().trim().split(",");
                    Point p = geometryFactory.createPoint(new Coordinate(Double.valueOf(tab[0]), Double.valueOf(tab[1])));
                    node = createOrGetNode(p);
                    if (way == null) {
                        prim = node;
                    }
                } else {
                    key = parser.getLocalName();
                    if (key.startsWith("ogr:")) {
                        key = key.substring("ogr:".length());
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (isElement(GML_FEATURE_MEMBER)) {
                    break;
                }
            } else if (event == XMLStreamConstants.CHARACTERS && key != null) {
                StringBuilder sb = tags.get(key);
                if (sb == null) {
                    sb = new StringBuilder();
                    tags.put(key, sb);
                }
                sb.append(parser.getTextCharacters(), parser.getTextStart(), parser.getTextLength());
            }
        }
        if (prim != null) {
            for (String k : tags.keySet()) {
                prim.put(k, tags.get(k).toString());
            }
        }
    }
}
