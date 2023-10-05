// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;

/**
 * Test class for {@link PMTilesImageLayer}
 */
class PMTilesImageLayerTest implements PMTilesLayerTest {
    @Override
    public PMTilesImageLayer getLayer(PMTilesImageryInfo info) {
        return new PMTilesImageLayer(info);
    }
}
