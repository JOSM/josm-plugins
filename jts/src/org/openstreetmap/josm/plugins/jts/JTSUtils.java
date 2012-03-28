package org.openstreetmap.josm.plugins.jts;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.util.List;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Methods to convert JOSM geometry to JTS geometry
 */
public final class JTSUtils {

    public final OsmPrecisionModel osmPrecisionModel = new OsmPrecisionModel();
    public final GeometryFactory osmGeometryFactory = new GeometryFactory(getPrecisionModel());

    public OsmPrecisionModel getPrecisionModel() {
        return osmPrecisionModel;
    }

    public GeometryFactory getGeometryFactory() {
        return osmGeometryFactory;
    }

    /**
     * Simple subclass to match precision with the OSM data model (7 decimal
     * places)
     */
    public class OsmPrecisionModel extends com.vividsolutions.jts.geom.PrecisionModel {

        public OsmPrecisionModel() {
            super(10000000);
        }
    }

    public Coordinate convertNodeToCoordinate(Node node) {
        LatLon ll = node.getCoor();
        return new Coordinate(ll.lon(), ll.lat());
    }

    public CoordinateSequence convertNodesToCoordinateSequence(List<Node> nodes) {
        Coordinate coords[] = new Coordinate[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            coords[i] = convertNodeToCoordinate(nodes.get(i));
        }
        return new CoordinateArraySequence(coords);
    }

    public Point convertNode(Node node) {
        Coordinate coords[] = {convertNodeToCoordinate(node)};
        return new com.vividsolutions.jts.geom.Point(new CoordinateArraySequence(coords), getGeometryFactory());
    }

    public Geometry convertWay(Way way) {
        CoordinateSequence coordSeq = convertNodesToCoordinateSequence(way.getNodes());

        // TODO: need to check tags to determine whether area or not
        if (way.isClosed()) {
            LinearRing ring = new LinearRing(coordSeq, getGeometryFactory());
            return new Polygon(ring, null, getGeometryFactory());
        } else {
            return new LineString(coordSeq, getGeometryFactory());
        }
    }

    public Geometry convert(OsmPrimitive prim) {
        if (prim instanceof Node) {
            return convertNode((Node) prim);
        } else if (prim instanceof Way) {
            return convertWay((Way) prim);
        } else if (prim instanceof Relation) {
            throw new UnsupportedOperationException("Relations not supported yet.");
        } else {
            throw new UnsupportedOperationException("Unknown primitive.");
        }
    }
}
