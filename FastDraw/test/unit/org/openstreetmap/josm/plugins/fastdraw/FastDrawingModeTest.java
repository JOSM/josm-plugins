// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fastdraw;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link FastDrawingMode}
 */
@Main
@Projection
class FastDrawingModeTest {
    /**
     * Non-regression test for #21659: IAE: Listener {@link FastDrawingMode} was not registered before or already removed
     */
    @Test
    void testNonRegression21659() {
        final OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "testNonRegression21659", null);
        final FastDrawingMode mode = new FastDrawingMode();
        assertFalse(mode.isEnabled());
        MainApplication.getLayerManager().addLayer(osmDataLayer);
        mode.updateEnabledState();
        assertTrue(mode.isEnabled());
        osmDataLayer.lock();
        mode.updateEnabledState();
        assertFalse(mode.isEnabled());
        osmDataLayer.unlock();
        mode.enterMode();
        assertDoesNotThrow(mode::exitMode);
    }
}
