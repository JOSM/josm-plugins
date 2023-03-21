// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.graphview.core.visualisation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.graphview.core.property.RoadMaxweight;

class FloatPropertyColorSchemeTest {

    private FloatPropertyColorScheme subject;

    @BeforeEach
    public void setUp() {

        Map<Float, Color> colorMap = new HashMap<>();
        colorMap.put( 5f, new Color( 42,  42,  42));
        colorMap.put(10f, new Color(100, 100, 100));
        colorMap.put(20f, new Color(200, 200, 200));

        subject = new FloatPropertyColorScheme(RoadMaxweight.class, colorMap, Color.RED);
    }

    @Test
    void testGetColorForValueBelow() {
        assertEquals(new Color(42, 42, 42), subject.getColorForValue(1f));
        assertEquals(new Color(42, 42, 42), subject.getColorForValue(5f));
    }

    @Test
    void testGetColorForValueAbove() {
        assertEquals(new Color(200, 200, 200), subject.getColorForValue(25f));
    }

    @Test
    void testGetColorForValueValue() {
        assertEquals(new Color(100, 100, 100), subject.getColorForValue(10f));
    }

    @Test
    void testGetColorForValueInterpolate() {
        assertEquals(new Color(150, 150, 150), subject.getColorForValue(15f));
    }

}
