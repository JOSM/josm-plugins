// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeographicReader.wgs84;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Convert a {@link DataStore} to a {@link DataSet}
 */
public class GeotoolsConverter {
    private final DataStore dataStore;
    private final GeographicReader reader;
    private final Set<OsmPrimitive> featurePrimitives = new HashSet<>();

    /**
     * Create a new converter
     * @param reader The reader which should have a dataset
     * @param original The original data store
     */
    public GeotoolsConverter(GeographicReader reader, DataStore original) {
        this.dataStore = original;
        this.reader = reader;
    }

    /**
     * Run the actual conversion process
     * @param progressMonitor The monitor to show progress on
     * @throws IOException If something could not be read
     * @throws FactoryException See {@link GeographicReader#findMathTransform(Component, boolean)}
     * @throws GeoMathTransformException See {@link GeographicReader#findMathTransform(Component, boolean)}
     * @throws TransformException See {@link GeographicReader#createOrGetNode(Point)}
     * @throws GeoCrsException If the CRS cannot be detected
     */
    public void convert(ProgressMonitor progressMonitor)
            throws IOException, FactoryException, GeoMathTransformException, TransformException, GeoCrsException {
        String[] typeNames = dataStore.getTypeNames();
        if (progressMonitor != null) {
            progressMonitor.beginTask(tr("Loading shapefile ({0} layers)", typeNames.length), typeNames.length);
        }
        try {
            for (String typeName : typeNames) {
                FeatureSource<?, ?> featureSource = dataStore.getFeatureSource(typeName);
                FeatureCollection<?, ?> collection = featureSource.getFeatures();
                try {
                    parseFeatures(progressMonitor != null ? progressMonitor.createSubTaskMonitor(1, false) : null, collection);
                    // Geotools wraps an IOException in a RuntimeException. We want to keep parsing layers, even if we could not understand
                    // a previous layer.
                } catch (RuntimeException runtimeException) {
                    if (runtimeException.getCause() instanceof IOException && runtimeException.getCause().getCause() instanceof ParseException) {
                        Logging.error(runtimeException);
                    } else {
                        throw runtimeException;
                    }
                }
            }
        } finally {
            if (progressMonitor != null) {
                progressMonitor.finishTask();
            }
        }
    }

    /**
     * Run the actual conversion process for a collection of features
     * @param progressMonitor The monitor to show progress on
     * @param collection The collection to parse
     * @throws FactoryException See {@link GeographicReader#findMathTransform(Component, boolean)}
     * @throws GeoMathTransformException See {@link GeographicReader#findMathTransform(Component, boolean)}
     * @throws TransformException See {@link GeographicReader#createOrGetNode(Point)}
     * @throws GeoCrsException If the CRS cannot be detected
     */
    private void parseFeatures(ProgressMonitor progressMonitor, FeatureCollection<?, ?> collection)
            throws FactoryException, GeoMathTransformException, TransformException, GeoCrsException {
        if (progressMonitor != null) {
            progressMonitor.beginTask(tr("Loading shapefile ({0} features)", collection.size()), collection.size());
        }

        int n = 0;

        Component parent = progressMonitor != null ? progressMonitor.getWindowParent() : MainApplication.getMainFrame();

        int size = collection.size();
        this.reader.getDataSet().beginUpdate();
        try (FeatureIterator<?> iterator = collection.features()) {
            while (iterator.hasNext()) {
                n++;
                try {
                    Feature feature = iterator.next();
                    parseFeature(feature, parent);
                    if (reader.getHandler() instanceof ShpHandler) {
                        ((ShpHandler) reader.getHandler()).notifyFeatureParsed(feature, reader.getDataSet(), featurePrimitives);
                    }
                } catch (UserCancelException e) {
                    Logging.error(e);
                    return;
                }
                if (progressMonitor != null) {
                    progressMonitor.worked(1);
                    progressMonitor.setCustomText(n+"/"+size);
                    if (progressMonitor.isCanceled()) {
                        return;
                    }
                }
            }
        } finally {
            reader.nodes.clear();
            this.reader.getDataSet().endUpdate();
            if (progressMonitor != null) {
                progressMonitor.setCustomText(null);
            }
        }
    }

    private void parseFeature(Feature feature, final Component parent) throws UserCancelException, GeoMathTransformException,
            FactoryException, GeoCrsException, MismatchedDimensionException, TransformException {
        featurePrimitives.clear();
        GeometryAttribute geometry = feature.getDefaultGeometryProperty();
        if (geometry != null) {

            GeometryDescriptor desc = geometry.getDescriptor();
            if (reader.crs == null) {
                if (desc != null && desc.getCoordinateReferenceSystem() != null) {
                    reader.crs = desc.getCoordinateReferenceSystem();
                } else if (!GraphicsEnvironment.isHeadless()) {
                    GuiHelper.runInEDTAndWait(() -> {
                        if (0 == JOptionPane.showConfirmDialog(
                                parent,
                                tr("Unable to detect Coordinate Reference System.\nWould you like to fallback to ESPG:4326 (WGS 84) ?"),
                                tr("Warning: CRS not found"),
                                JOptionPane.YES_NO_CANCEL_OPTION
                        )) {
                            reader.crs = wgs84;
                        }
                    });
                } else {
                    // Always use WGS84 in headless mode (used for unit tests only)
                    reader.crs = wgs84;
                }
                if (reader.crs != null) {
                    reader.findMathTransform(parent, true);
                } else {
                    throw new GeoCrsException(tr("Unable to detect CRS !"));
                }
            }

            Object geomObject = geometry.getValue();
            if (geomObject instanceof Point) {
                // Sure you could have a Set of 1 object and join these 2 branches of
                // code, but I feel there would be a performance hit.
                OsmPrimitive primitive = reader.createOrGetEmptyNode((Point) geomObject);
                readNonGeometricAttributes(feature, primitive);
            } else if (geomObject instanceof LineString) {
                OsmPrimitive primitive = reader.createOrGetWay((LineString) geomObject);
                readNonGeometricAttributes(feature, primitive);
            } else if (geomObject instanceof Polygon) {
                Polygon polygon = (Polygon) geomObject;
                Way outer = reader.createOrGetWay(polygon.getExteriorRing());
                Way[] inner = new Way[polygon.getNumInteriorRing()];
                for (int i = 0; i < inner.length; i++) {
                    inner[i] = reader.createOrGetWay(polygon.getInteriorRingN(i));
                }
                final OsmPrimitive primitive;
                if (inner.length == 0) {
                    primitive = outer;
                } else {
                    Relation relation = reader.createMultipolygon();
                    GeographicReader.addWayToMp(relation, "outer", outer);
                    for (Way iWay : inner) {
                        GeographicReader.addWayToMp(relation, "inner", iWay);
                    }
                    primitive = relation;
                }
                readNonGeometricAttributes(feature, primitive);
            } else if (geomObject instanceof GeometryCollection) { // Deals with both MultiLineString and MultiPolygon
                Set<OsmPrimitive> primitives = processGeometryCollection((GeometryCollection) geomObject);
                for (OsmPrimitive prim : primitives) {
                    readNonGeometricAttributes(feature, prim);
                }
            } else {
                // Debug unknown geometry
                Logging.debug("\ttype: "+geometry.getType());
                Logging.debug("\tbounds: "+geometry.getBounds());
                Logging.debug("\tdescriptor: "+desc);
                Logging.debug("\tname: "+geometry.getName());
                Logging.debug("\tvalue: "+geomObject);
                Logging.debug("\tid: "+geometry.getIdentifier());
                Logging.debug("-------------------------------------------------------------");
            }
        }
    }

    protected Set<OsmPrimitive> processGeometryCollection(GeometryCollection gc) throws TransformException {
        // A feture may be a collection.  This set holds the items of the collection.
        Set<OsmPrimitive> primitives = new HashSet<>();
        int nGeometries = gc.getNumGeometries();
        if (nGeometries < 1) {
            Logging.error("empty geometry collection found");
        } else {
            // Create the primitive "op" and add it to the set of primitives.
            for (int i = 0; i < nGeometries; i++) {
                OsmPrimitive op = null;
                Geometry g = gc.getGeometryN(i);
                if (g instanceof Polygon) {
                    // TODO: Split this section between Polygon and MultiPolygon.
                    Relation r = (Relation) op;
                    Polygon p = (Polygon) g;
                    // Do not create relation if there's only one polygon without interior ring
                    // except if handler prefers it
                    if (r == null && (nGeometries > 1 || p.getNumInteriorRing() > 0 ||
                            (reader.getHandler() != null && reader.getHandler().preferMultipolygonToSimpleWay()))) {
                        r = reader.createMultipolygon();
                    }
                    Way w = reader.createOrGetWay(p.getExteriorRing());
                    if (r != null) {
                        GeographicReader.addWayToMp(r, "outer", w);
                        for (int j = 0; j < p.getNumInteriorRing(); j++) {
                            GeographicReader.addWayToMp(r, "inner", reader.createOrGetWay(p.getInteriorRingN(j)));
                        }
                    }
                    op = r != null ? r : w;
                } else if (g instanceof LineString) {
                    op = reader.createOrGetWay((LineString) g);
                } else if (g instanceof Point) {
                    op = reader.createOrGetNode((Point) g);
                } else {
                    Logging.error("unsupported geometry : "+g);
                }
                if (op != null) {
                    primitives.add(op);
                }
            }
        }
        return primitives;
    }

    private static void readNonGeometricAttributes(Feature feature, OsmPrimitive primitive) {
        Collection<Property> properties = feature.getProperties();
        Map<String, String> tagMap = new LinkedHashMap<>(properties.size());
        for (Property prop : properties) {
            if (!(prop instanceof GeometryAttribute)) {
                Name name = prop.getName();
                Object value = prop.getValue();
                if (name != null && value != null) {
                    String sName = name.toString();
                    String sValue = value.toString();
                    if (value instanceof Date) {
                        sValue = new SimpleDateFormat("yyyy-MM-dd").format(value);
                    }
                    if (!sName.isEmpty() && !sValue.isEmpty()) {
                        tagMap.put(sName, sValue);
                        //primitive.put(sName, sValue);
                    }
                }
            }
        }
        primitive.putAll(tagMap);
    }
}
