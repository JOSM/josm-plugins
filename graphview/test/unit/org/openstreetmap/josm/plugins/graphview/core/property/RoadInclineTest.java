// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.graphview.core.property;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;

class RoadInclineTest implements RoadPropertyTest {

    private static void testIncline(Float expectedInclineForward, Float expectedInclineBackward,
            String inclineString) {

        RoadPropertyTest.testEvaluateW(new RoadIncline(),
                expectedInclineForward, expectedInclineBackward,
                new Tag("incline", inclineString));
    }

    @Test
    void testEvaluate() {
        testIncline(5f, -5f, "5 %");
        testIncline(9.5f, -9.5f, "9.5 %");
        testIncline(-2.5f, 2.5f, "-2.5%");
        testIncline(null, null, "steep");
    }

}
