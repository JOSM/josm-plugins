// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.terracer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link ReverseTerraceAction}
 */
@Main
@Projection
class ReverseTerraceActionTest {
    private static final String ADDR_HOUSENUMBER = "addr:housenumber";
    private static Node[] generateNodes(double... latLons) {
        assertEquals(0, latLons.length % 2);
        final Node[] nodes = new Node[latLons.length / 2];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(new LatLon(latLons[2 * i], latLons[2 * i + 1]));
        }
        return nodes;
    }

    @Test
    void testNonRegression6855() {
        final Way way1 = TestUtils.newWay("building=apartments", generateNodes(49.766198, 4.3270878, 49.7663026, 4.3270911,
                49.7663282, 4.3269524, 49.7662016, 4.3269471));
        final Way way2 = TestUtils.newWay("building=apartments", generateNodes(49.7655382, 4.3269193, 49.7655362, 4.3270665));
        final DataSet dataSet = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(dataSet, "ReverseTerraceActionTest#testNonRegression6855", null));
        dataSet.addPrimitiveRecursive(way1);
        dataSet.addPrimitiveRecursive(way2);
        way2.addNode(0, way1.lastNode());
        way2.addNode(way1.firstNode());
        way1.addNode(way1.firstNode());
        way2.addNode(way2.firstNode());
        dataSet.setSelected(way2);
        new TerracerAction().terraceBuilding(way2, null, null, null, 5,
                "80", "88", 2, Collections.emptyList(), null, false, false, "apartments");
        final List<Way> addressedWays = dataSet.getWays().stream().filter(w -> w != way1)
                .sorted(Comparator.comparingLong(w -> -w.getUniqueId())).collect(Collectors.toList());
        assertAll("Terrace should work as expected", () -> assertEquals(5, addressedWays.size()),
                () -> assertEquals("80", addressedWays.get(0).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("82", addressedWays.get(1).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("84", addressedWays.get(2).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("86", addressedWays.get(3).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("88", addressedWays.get(4).get(ADDR_HOUSENUMBER)));
        dataSet.setSelected(addressedWays);
        new ReverseTerraceAction().actionPerformed(null);
        assertAll("Reverse Terrace should work as expected", () -> assertEquals(5, addressedWays.size()),
                () -> assertEquals("88", addressedWays.get(0).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("86", addressedWays.get(1).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("84", addressedWays.get(2).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("82", addressedWays.get(3).get(ADDR_HOUSENUMBER)),
                () -> assertEquals("80", addressedWays.get(4).get(ADDR_HOUSENUMBER)));
    }
}
