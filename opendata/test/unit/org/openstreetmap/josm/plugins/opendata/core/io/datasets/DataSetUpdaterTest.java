// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.datasets;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.opendata.core.datasets.DataSetUpdater;
import org.openstreetmap.josm.plugins.opendata.core.io.archive.ZipReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@link DataSetUpdater} class.
 */
@BasicPreferences
class DataSetUpdaterTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules().projection().devAPI().timeout(60000);

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11166">#11166</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket11166() throws Exception {
        File file = new File(TestUtils.getRegressionDataFile(11166, "raba760dissJosm.zip"));
        try (InputStream is = new FileInputStream(file)) {
            Predicate<? super Way> p = w -> w.getNodesCount() >= 0.9 * OsmApi.getOsmApi().getCapabilities().getMaxWayNodes();
            DataSet ds = ZipReader.parseDataSet(is, null, null, false);
            assertTrue(ds.getWays().stream().anyMatch(p));
            DataSetUpdater.updateDataSet(ds, null, file);
            assertFalse(ds.getWays().stream().anyMatch(p));
        }
    }
}
