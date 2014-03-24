// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.corrector.UserCancelException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.datasets.NationalHandlers;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShpReader extends GeographicReader {

	private final ShpHandler handler;
	private final Set<OsmPrimitive> featurePrimitives = new HashSet<OsmPrimitive>();
	
	public ShpReader(ShpHandler handler) {
		super(handler, NationalHandlers.DEFAULT_SHP_HANDLERS);
		this.handler = handler;
	}

	public static DataSet parseDataSet(InputStream in, File file,
			AbstractDataSetHandler handler, ProgressMonitor instance) throws IOException {
		if (in != null) {
			in.close();
		}
		try {
			return new ShpReader(handler != null ? handler.getShpHandler() : null).parse(file, instance);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(t);
		}
	}
	
	private void parseFeature(Feature feature, final Component parent) 
			throws UserCancelException, GeoMathTransformException, FactoryException, GeoCrsException, MismatchedDimensionException, TransformException {
		featurePrimitives.clear();
		GeometryAttribute geometry = feature.getDefaultGeometryProperty();
		if (geometry != null) {

			GeometryDescriptor desc = geometry.getDescriptor();
			
			if (crs == null) {
    			if (desc != null && desc.getCoordinateReferenceSystem() != null) {
    				crs = desc.getCoordinateReferenceSystem();
    			} else {
    			    GuiHelper.runInEDTAndWait(new Runnable() {
                        @Override
                        public void run() {
                            if (0 == JOptionPane.showConfirmDialog(
                                    parent,
                                    tr("Unable to detect Coordinate Reference System.\nWould you like to fallback to ESPG:4326 (WGS 84) ?"),
                                    tr("Warning: CRS not found"),
                                    JOptionPane.YES_NO_CANCEL_OPTION
                            )) {
                                crs = wgs84;
                            }
                        }
                    });
    			}
                if (crs != null) {
                    findMathTransform(parent, true);
                } else {
                    throw new GeoCrsException(tr("Unable to detect CRS !"));
                }
			}
			
			OsmPrimitive primitive = null;
			
			if (geometry.getValue() instanceof Point) {
				primitive = createOrGetEmptyNode((Point) geometry.getValue());
				
			} else if (geometry.getValue() instanceof GeometryCollection) { // Deals with both MultiLineString and MultiPolygon
				GeometryCollection mp = (GeometryCollection) geometry.getValue();
				int nGeometries = mp.getNumGeometries(); 
				if (nGeometries < 1) {
					System.err.println("Error: empty geometry collection found");
				} else {
					Relation r = null;
					Way w = null;
					
					for (int i=0; i<nGeometries; i++) {
						Geometry g = mp.getGeometryN(i);
						if (g instanceof Polygon) {
							Polygon p = (Polygon) g;
							// Do not create relation if there's only one polygon without interior ring
							// except if handler prefers it
							if (r == null && (nGeometries > 1 || p.getNumInteriorRing() > 0 || (handler != null && handler.preferMultipolygonToSimpleWay()))) {
								r = createMultipolygon();
							}
							w = createWay(p.getExteriorRing());
							if (r != null) {
								addWayToMp(r, "outer", w);
								for (int j=0; j<p.getNumInteriorRing(); j++) {
									addWayToMp(r, "inner", createWay(p.getInteriorRingN(j)));
								}
							}
						} else if (g instanceof LineString) {
							w = createWay((LineString) g);
						} else if (g instanceof Point) {
							// Some belgian data sets hold points into collections ?!
							readNonGeometricAttributes(feature, createOrGetNode((Point) g));
						} else {
							System.err.println("Error: unsupported geometry : "+g);
						}
					}
					primitive = r != null ? r : w;
				}
			} else {
				// Debug unknown geometry
				System.out.println("\ttype: "+geometry.getType());
				System.out.println("\tbounds: "+geometry.getBounds());
				System.out.println("\tdescriptor: "+desc);
				System.out.println("\tname: "+geometry.getName());
				System.out.println("\tvalue: "+geometry.getValue());
				System.out.println("\tid: "+geometry.getIdentifier());
				System.out.println("-------------------------------------------------------------");
			}
			
			if (primitive != null) {
				// Read primitive non geometric attributes
				readNonGeometricAttributes(feature, primitive);
			}
		}
	}

	public DataSet parse(File file, ProgressMonitor instance) throws IOException {
		crs = null;
		transform = null;
		try {
			if (file != null) { 
		        Map params = new HashMap();
		        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
		        if (handler != null && handler.getDbfCharset() != null) {
		        	params.put(ShapefileDataStoreFactory.DBFCHARSET.key, handler.getDbfCharset());
		        }
				DataStore dataStore = new ShapefileDataStoreFactory().createDataStore(params);//FIXME
				if (dataStore == null) {
					throw new IOException(tr("Unable to find a data store for file {0}", file.getName()));
				}
				
				String[] typeNames = dataStore.getTypeNames();
				String typeName = typeNames[0];
	
				FeatureSource<?,?> featureSource = dataStore.getFeatureSource(typeName);
				FeatureCollection<?,?> collection = featureSource.getFeatures();
				FeatureIterator<?> iterator = collection.features();
				
				if (instance != null) {
					instance.beginTask(tr("Loading shapefile ({0} features)", collection.size()), collection.size());
				}
				
				int n = 0;
				
				Component parent = instance != null ? instance.getWindowParent() : Main.parent;
				
				try {
					while (iterator.hasNext()) {
						n++;
						try {
							Feature feature = iterator.next();
							parseFeature(feature, parent);
							if (handler != null) {
								handler.notifyFeatureParsed(feature, ds, featurePrimitives);
							}
						} catch (UserCancelException e) {
                                                        e.printStackTrace();
							return ds;
						}
						if (instance != null) {
							instance.worked(1);
							instance.setCustomText(n+"/"+collection.size());
						}
					}
				} catch (Throwable e) {
                                        e.printStackTrace();
				} finally {
					iterator.close();
					nodes.clear();
					if (instance != null) {
						instance.setCustomText(null);
					}
				}
			}
		} catch (IOException e) {
                        e.printStackTrace();
			throw e;
		} catch (Throwable t) {
                        t.printStackTrace();
			throw new IOException(t);
		}
		return ds;
	}
	
	private static final void readNonGeometricAttributes(Feature feature, OsmPrimitive primitive) {
            try {
		for (Property prop : feature.getProperties()) {
			if (!(prop instanceof GeometryAttribute)) {
				Name name = prop.getName();
				Object value = prop.getValue();
				if (name != null && value != null) {
					String sName = name.toString();
					String sValue = value.toString();
					if (!sName.isEmpty() && !sValue.isEmpty()) {
                                               primitive.put(sName, sValue);
					}
				}
			}
		}
            } catch (Exception e) {
                e.printStackTrace();
            }
	}

	@Override
	protected Node createOrGetNode(Point p) throws MismatchedDimensionException, TransformException {
		Node n = super.createOrGetNode(p);
		featurePrimitives.add(n);
		return n;
	}
	
	@Override
	protected <T extends OsmPrimitive> T addOsmPrimitive(T p) {
		featurePrimitives.add(p);
		return super.addOsmPrimitive(p);
	}
}
