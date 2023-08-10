// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;

/**
 * Test class for {@link PMTilesMVTLayer}
 */
class PMTilesMVTLayerTest implements PMTilesLayerTest {
    @Override
    public PMTilesMVTLayer getLayer(PMTilesImageryInfo info) {
        return new PMTilesMVTLayer(info);
    }
}
