// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.plugins.opendata.core.io.NonRegFunctionalTests;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link KmlReader} class.
 */
@BasicPreferences
class KmlReaderTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules().projection();

    /**
     * Unit test of {@link KmlReader#COLOR_PATTERN}
     */
    @Test
    void testColorPattern() {
        assertTrue(KmlReader.COLOR_PATTERN.matcher("00112233").matches());
        assertTrue(KmlReader.COLOR_PATTERN.matcher("44556677").matches());
        assertTrue(KmlReader.COLOR_PATTERN.matcher("8899aabb").matches());
        assertTrue(KmlReader.COLOR_PATTERN.matcher("CCDDEEFF").matches());
        assertFalse(KmlReader.COLOR_PATTERN.matcher("0011223").matches());
        assertFalse(KmlReader.COLOR_PATTERN.matcher("001122330").matches());
        assertFalse(KmlReader.COLOR_PATTERN.matcher("gg112233").matches());
        assertFalse(KmlReader.COLOR_PATTERN.matcher("red").matches());
        assertFalse(KmlReader.COLOR_PATTERN.matcher("yellow").matches());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16402">#16402</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket16402() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(16402, "MapsMe-new.kml")) {
            NonRegFunctionalTests.testGeneric("#16402", KmlReader.parseDataSet(is, null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12694">#12694</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket12694() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(12694, "Alvinรณpolis_314946.kml")) {
            NonRegFunctionalTests.testGeneric("#12694", KmlReader.parseDataSet(is, null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10214">#10214</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket10214() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(10214, "utf8_test.kml")) {
            NonRegFunctionalTests.testTicket10214(KmlReader.parseDataSet(is, null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/7714">#7714</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket7714() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(7714, "doc.kml")) {
            NonRegFunctionalTests.testGeneric("#7714", KmlReader.parseDataSet(is, null));
        }
    }
}
