package org.geotools.data.simple;

import java.io.IOException;

import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * 
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/api/src/main/java/org/geotools/data/simple/SimpleFeatureStore.java $
 */
public interface SimpleFeatureStore extends FeatureStore<SimpleFeatureType,SimpleFeature>, SimpleFeatureSource {
    
    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException;

    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException;
    
    public SimpleFeatureCollection getFeatures() throws IOException;

    public SimpleFeatureCollection getFeatures(Filter filter)
            throws IOException;

    public SimpleFeatureCollection getFeatures(Query query)
            throws IOException;
}
