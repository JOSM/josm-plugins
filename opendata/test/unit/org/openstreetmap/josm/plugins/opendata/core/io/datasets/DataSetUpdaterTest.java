// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.datasets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.opendata.core.datasets.DataSetUpdater;
import org.openstreetmap.josm.plugins.opendata.core.io.archive.ZipReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link DataSetUpdater} class.
 */
@BasicPreferences
@org.openstreetmap.josm.testutils.annotations.OsmApi(org.openstreetmap.josm.testutils.annotations.OsmApi.APIType.DEV)
@Projection
@Timeout(value = 1, unit = TimeUnit.MINUTES)
class DataSetUpdaterTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/11166">#11166</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket11166() throws Exception {
        File file = new File(TestUtils.getRegressionDataFile(11166, "raba760dissJosm.zip"));
        try (InputStream is = Files.newInputStream(file.toPath())) {
            Predicate<? super Way> p = w -> w.getNodesCount() >= 0.9 * OsmApi.getOsmApi().getCapabilities().getMaxWayNodes();
            DataSet ds = ZipReader.parseDataSet(is, null, null, false);
            assertTrue(ds.getWays().stream().anyMatch(p));
            DataSetUpdater.updateDataSet(ds, null, file);
            assertFalse(ds.getWays().stream().anyMatch(p));
        }
    }
}
