// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fastdraw;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link DrawnPolyLine}
 */
@BasicPreferences
@Main
@Projection
class DrawnPolyLineTest {
    private DrawnPolyLine drawnPolyLine;
    private MapView mv;

    @BeforeEach
    void setup() {
        this.drawnPolyLine = new DrawnPolyLine();
        // We need to add a new layer to get the MapView set up
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "DrawnPolyLineTest", null));
        this.mv = MainApplication.getMap().mapView;
        this.drawnPolyLine.setMv(this.mv);
    }

    @Test
    void testDeleteNode() {
        for (int i = 0; i < 10; i++) {
            this.drawnPolyLine.addLast(new LatLon(i, i));
        }
        assertEquals(10, this.drawnPolyLine.getPoints().size());
        this.drawnPolyLine.deleteNode(0);
        assertEquals(9, this.drawnPolyLine.getPoints().size());
        assertEquals(1, this.drawnPolyLine.getPoints().get(0).lat(), 1e-9);
        assertEquals(1, this.drawnPolyLine.getPoints().get(0).lon(), 1e-9);
    }

    @Test
    void testDeleteSegment() {
        for (int i = 0; i < 10; i++) {
            this.drawnPolyLine.addLast(new LatLon(i, i));
        }
        this.drawnPolyLine.tryToDeleteSegment(mv.getPoint(new LatLon(1, 1)));
        assertEquals(1, this.drawnPolyLine.getPoints().size());
        assertEquals(0, this.drawnPolyLine.getPoints().get(0).lat(), 1e-9);
        assertEquals(0, this.drawnPolyLine.getPoints().get(0).lon(), 1e-9);
        this.drawnPolyLine.deleteNode(0);
        assertTrue(this.drawnPolyLine.getPoints().isEmpty());
    }

    /**
     * Non-regression test for #22317: IOOBE in {@link DrawnPolyLine#getLastPoint()}
     */
    @Test
    void testNonRegression22317() {
        for (int i = 0; i < 10; i++) {
            this.drawnPolyLine.addLast(new LatLon(i, i));
        }
        this.drawnPolyLine.tryToDeleteSegment(mv.getPoint(new LatLon(1, 1)));
        this.drawnPolyLine.deleteNode(0);
        assertDoesNotThrow(() -> this.drawnPolyLine.getLastPoint());
    }

    /**
     * Non-regression test for #22946: NullPointerException in {@link DrawnPolyLine#autoSimplify(double, double, int, double)}
     * The root issue is probably from some kind of race condition.
     */
    @Test
    void testNonRegression22946() {
        this.drawnPolyLine.addLast(new LatLon(0.00001, 0.00001));
        this.drawnPolyLine.addLast(new LatLon(0.00002, 0.00002));
        this.drawnPolyLine.addLast(new LatLon(0.00002, 0.00003));
        this.drawnPolyLine.clear();
        // The maxPKM == -.1 to force the bad code path. As noted, it is probably a race condition issue.
        assertDoesNotThrow(() -> this.drawnPolyLine.autoSimplify(5, 1.1, 10, -.1));
    }
}
