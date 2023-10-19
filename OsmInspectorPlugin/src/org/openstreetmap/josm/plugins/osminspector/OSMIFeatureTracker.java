package org.openstreetmap.josm.plugins.osminspector;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.openstreetmap.josm.tools.Logging;

public class OSMIFeatureTracker {

    private final Map<Long, SimpleFeature> hashFeatures;
    private final MemoryFeatureCollection features;

    public OSMIFeatureTracker(
            FeatureCollection<SimpleFeatureType, SimpleFeature> featuresIn) {
        hashFeatures = new HashMap<>();
        features = new MemoryFeatureCollection(featuresIn.getSchema());

        for (SimpleFeature element : features) {
            try {
                Long id = (Long.parseLong((String) element
                        .getAttribute("problem_id")));

                if (!hashFeatures.containsKey(id)) {
                    hashFeatures.put(id, element);
                    features.add(element);
                }
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }

        try (FeatureIterator<SimpleFeature> it = featuresIn.features()) {
            while (it.hasNext()) {
                features.add(it.next());
            }
        }
    }

    public boolean mergeFeatures(
            FeatureCollection<SimpleFeatureType, SimpleFeature> newFeatures) {
        try (FeatureIterator<SimpleFeature> it = newFeatures.features()) {
            while (it.hasNext()) {
                SimpleFeature element = it.next();
                try {
                    Long id = (Long.parseLong((String) element
                            .getAttribute("problem_id")));
    
                    if (!hashFeatures.containsKey(id)) {
                        hashFeatures.put(id, element);
                        features.add(element);
                    }
                } catch (NumberFormatException e) {
                    Logging.trace(e);
                }
            }
        }

        return true;
    }

    public Map<Long, SimpleFeature> getFeatureHash() {
        return hashFeatures;
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatures() {
        return features;
    }
}
