// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.api.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.locationtech.jts.geom.Point;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.datasets.NationalHandlers;
import org.openstreetmap.josm.tools.Logging;

/**
 * Reader of SHP (Shapefile) files.
 */
public class ShpReader extends GeographicReader {

    private final ShpHandler handler;
    private final Set<OsmPrimitive> featurePrimitives = new HashSet<>();

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

    public DataSet parse(File file, ProgressMonitor instance) throws IOException {
        crs = null;
        transform = null;
        try {
            if (file != null) {
                Map<String, Serializable> params = new HashMap<>();
                Charset charset = null;
                params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
                if (handler != null && handler.getDbfCharset() != null) {
                    charset = handler.getDbfCharset();
                } else {
                    String path = file.getAbsolutePath();
                    // See https://gis.stackexchange.com/a/3663/17245
                    path = path.substring(0, path.lastIndexOf('.')) + ".cpg";
                    Path cpg = new File(path).toPath();
                    if (Files.exists(cpg)) {
                        try (BufferedReader reader = Files.newBufferedReader(cpg, StandardCharsets.UTF_8)) {
                            String cs = reader.readLine();
                            if (cs.matches("\\d+")) {
                                // We only have the code page number, we need to find the good alias for java.nio API
                                // see https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
                                for (String prefix : Arrays.asList("IBM", "IBM0", "IBM00", "x-IBM", "ISO-", "windows-", "x-windows-")) {
                                    try {
                                        charset = Charset.forName(prefix+cs);
                                        break;
                                    } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
                                        Logging.trace(e);
                                    }
                                }
                            } else {
                                // We may have a charset name, such as "UTF-8"
                                charset = Charset.forName(cs);
                            }
                        } catch (IOException | UnsupportedCharsetException | IllegalCharsetNameException e) {
                            Logging.warn(e);
                        }
                    }
                }
                if (charset != null) {
                    Logging.info("Using charset "+charset);
                    params.put(ShapefileDataStoreFactory.DBFCHARSET.key, charset.name());
                }
                DataStore dataStore = new ShapefileDataStoreFactory().createDataStore(params);
                if (dataStore == null) {
                    throw new IOException(tr("Unable to find a data store for file {0}", file.getName()));
                }
                new GeotoolsConverter(this, dataStore).convert(instance);
            }
        } catch (IOException e) {
            Logging.error(e);
            throw e;
        } catch (FactoryException | GeoMathTransformException | TransformException | GeoCrsException e) {
            Logging.error(e);
            throw new IOException(e);
        }
        return ds;
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
