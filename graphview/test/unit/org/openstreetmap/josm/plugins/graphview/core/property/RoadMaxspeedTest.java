// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.graphview.core.property;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;

class RoadMaxspeedTest implements RoadPropertyTest {

    private static void testMaxspeed(float expectedMaxspeed, String maxspeedString) {
        RoadPropertyTest.testEvaluateBoth(new RoadMaxspeed(), expectedMaxspeed, new Tag("maxspeed", maxspeedString));
    }

    @Test
    void testEvaluateNumeric() {
        testMaxspeed(30, "30");
        testMaxspeed(48.3f, "48.3");
    }

    @Test
    void testEvaluateKmh() {
        testMaxspeed(50, "50 km/h");
        testMaxspeed(120, "120km/h");
        testMaxspeed(30, "30    km/h");
    }

    @Test
    void testEvaluateMph() {
        testMaxspeed(72.42048f, "45 mph");
        testMaxspeed(64.373764f, "40mph");
        testMaxspeed(24.14016f, "15 mph");
    }
}
