// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic.geopackage;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeoCrsException;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeoMathTransformException;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeographicReader;
import org.openstreetmap.josm.plugins.opendata.core.io.geographic.GeotoolsConverter;

/**
 * Read a geopackage file
 * @author Taylor Smock
 */
public final class GeoPackageReader extends GeographicReader {
    /**
     * Parse the dataset
     * @param in The {@link InputStream} to read (we actually close it and use the file instead)
     * @param file The originating file
     * @param handler The handler to use, may be {@code null}
     * @param instance The {@link ProgressMonitor} instance to update
     * @return The parsed dataset
     * @throws IOException If something prevented us from parsing the dataset
     */
    public static DataSet parseDataSet(InputStream in, File file,
                                       AbstractDataSetHandler handler, ProgressMonitor instance) throws IOException {
        if (in != null) {
            in.close();
        }
        return new GeoPackageReader(handler != null ? handler.getGeoPackageHandler() : null).parse(file, instance);
    }

    private GeoPackageReader(GeoPackageHandler handler) {
        super(handler, new GeoPackageHandler[0]);
    }

    private DataSet parse(File file, ProgressMonitor instance) throws IOException {
        if (file != null) {
            Map<String, Serializable> params = new HashMap<>();
            params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
            params.put(GeoPkgDataStoreFactory.READ_ONLY.key, true);
            params.put(GeoPkgDataStoreFactory.DBTYPE.key, (String) GeoPkgDataStoreFactory.DBTYPE.sample);
            DataStore dataStore = DataStoreFinder.getDataStore(params);
            try {
                new GeotoolsConverter(this, dataStore).convert(instance);
            } catch (FactoryException | GeoCrsException | GeoMathTransformException | TransformException e) {
                throw new IOException(e);
            } finally {
                dataStore.dispose();
            }
        }
        return ds;
    }
}
