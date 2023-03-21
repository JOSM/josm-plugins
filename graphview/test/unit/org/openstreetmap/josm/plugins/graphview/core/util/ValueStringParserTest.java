// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.graphview.core.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseMeasure;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseSpeed;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseWeight;

import org.junit.jupiter.api.Test;


class ValueStringParserTest {

    /* speed */

    @Test
    void testParseSpeedDefault() {
        assertClose(50, parseSpeed("50"));
    }

    @Test
    void testParseSpeedKmh() {
        assertClose(30, parseSpeed("30 km/h"));
        assertClose(100, parseSpeed("100km/h"));
    }

    @Test
    void testParseSpeedMph() {
        assertClose(40.234f, parseSpeed("25mph"));
        assertClose(40.234f, parseSpeed("25 mph"));
    }

    @Test
    void testParseSpeedInvalid() {
        assertNull(parseSpeed("lightspeed"));
    }

    /* measure */

    @Test
    void testParseMeasureDefault() {
        assertClose(3.5f, parseMeasure("3.5"));
    }

    @Test
    void testParseMeasureM() {
        assertClose(2, parseMeasure("2m"));
        assertClose(5.5f, parseMeasure("5.5 m"));
    }

    @Test
    void testParseMeasureKm() {
        assertClose(1000, parseMeasure("1 km"));
        assertClose(7200, parseMeasure("7.2km"));
    }

    @Test
    void testParseMeasureMi() {
        assertClose(1609.344f, parseMeasure("1 mi"));
    }

    @Test
    void testParseMeasureFeetInches() {
        assertClose(3.6576f, parseMeasure("12'0\""));
        assertClose(1.9812f, parseMeasure("6' 6\""));
    }

    @Test
    void testParseMeasureInvalid() {
        assertNull(parseMeasure("very long"));
        assertNull(parseMeasure("6' 16\""));
    }

    /* weight */

    @Test
    void testParseWeightDefault() {
        assertClose(3.6f, parseWeight("3.6"));
    }

    @Test
    void testParseWeightT() {
        assertClose(30, parseWeight("30t"));
        assertClose(3.5f, parseWeight("3.5 t"));
    }

    @Test
    void testParseWeightInvalid() {
        assertNull(parseWeight("heavy"));
    }

    private static void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError("expected " + expected + ", was " + actual);
        }
    }

}
