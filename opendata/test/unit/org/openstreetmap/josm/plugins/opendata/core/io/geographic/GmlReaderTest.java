// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link GmlReader} class.
 */
@BasicPreferences
class GmlReaderTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules().projection().timeout(60000);

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11624">#11624</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket11624() throws Exception {
        File file = new File(TestUtils.getRegressionDataFile(11624, "temp3.gml"));
        try (InputStream is = new FileInputStream(file)) {
            for (Node n : GmlReader.parseDataSet(is, null, null).getNodes()) {
                assertNotNull(n.getCoor(), n.toString());
                assertTrue(n.get("KICH_URL").startsWith("http://"), n.get("KICH_URL"));
            }
        }
    }
}
