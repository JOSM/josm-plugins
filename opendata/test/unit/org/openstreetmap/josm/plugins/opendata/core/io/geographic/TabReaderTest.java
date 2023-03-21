// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.plugins.opendata.core.io.NonRegFunctionalTests;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link TabReader} class.
 */
@BasicPreferences
@Projection
@Timeout(value = 1, unit = TimeUnit.MINUTES)
class TabReaderTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/15159">#15159</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket15159() throws Exception {
        File file = new File(TestUtils.getRegressionDataFile(15159, "Sanisette.tab"));
        try (InputStream is = Files.newInputStream(file.toPath())) {
            NonRegFunctionalTests.testGeneric("#15159", TabReader.parseDataSet(is, file, null, null));
        }
    }
}
