package org.openstreetmap.josm.plugins.buildings_tools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.jar.Attributes;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link BuildingsToolsPlugin}
 */
@BasicPreferences
@Main
@Projection
class BuildingsToolsPluginTest {
    /**
     * This makes certain we don't have an IAE after removing last layer and adding a new one
     * @throws PluginException If something occurs during {@link PluginInformation} construction
     */
    @Test
    void testMapReinitialization() throws PluginException {
        final BuildingsToolsPlugin plugin =
                new BuildingsToolsPlugin(new PluginInformation(new Attributes(), "buildings_tools", "https://example.com"));
        MainApplication.getMainPanel().addMapFrameListener(plugin);
        try {
            final LayerManager layerManager = MainApplication.getLayerManager();
            for (int i = 0; i < 20; i++) {
                final Layer layer = new OsmDataLayer(new DataSet(), "testMapReinitialization", null);
                assertDoesNotThrow(() -> layerManager.addLayer(layer));
                assertDoesNotThrow(() -> layerManager.removeLayer(layer));
            }
        } finally {
            MainApplication.getMainPanel().removeMapFrameListener(plugin);
        }
    }

}