// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openstreetmap.josm.plugins.pmtiles.PMTestUtils.ODBL_RASTER_STAMEN;
import static org.openstreetmap.josm.plugins.pmtiles.PMTestUtils.ODBL_VECTOR_FIRENZE;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;
import org.openstreetmap.josm.tools.TextUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test class for {@link PMTilesLayer}
 */
interface PMTilesLayerTest {
    PMTilesLayer getLayer(PMTilesImageryInfo info);

    @Test
    default void testSource() throws IOException {
        final var layerRaster = getLayer(new PMTilesImageryInfo(PMTiles.readHeader(ODBL_RASTER_STAMEN)));
        final var layerVector = getLayer(new PMTilesImageryInfo(PMTiles.readHeader(ODBL_VECTOR_FIRENZE)));
        if (Utils.isLocalUrl(ODBL_RASTER_STAMEN.toString())) {
            assertEquals(ODBL_RASTER_STAMEN.toString().replace(System.getProperty("user.name"), "<user.name>"),
                    layerRaster.getChangesetSourceTag());
        } else {
            assertEquals(TextUtils.stripUrl(ODBL_RASTER_STAMEN.toString()), layerRaster.getChangesetSourceTag());
        }
        assertEquals("Protomaps © OpenStreetMap - protomaps 2023-01-18T07:49:39Z", layerVector.getChangesetSourceTag());
    }

}
