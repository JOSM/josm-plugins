// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pbf.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for {@link PbfExporter}.
 */
@BasicPreferences
@Timeout(20)
class PbfExporterTest {
    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/11169">Ticket #11169</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket11169() throws Exception {
        try (InputStream is = Compression.ZIP.getUncompressedInputStream(
                Files.newInputStream(Paths.get(TestUtils.getRegressionDataFile(11169, "Portsmouth_Area.osm.zip"))))) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Path out = Files.createTempFile("pbf-bug-11169", "pbf");
            PbfExporter exporter = new PbfExporter();
            assertDoesNotThrow(() -> exporter.doSave(out.toFile(), new OsmDataLayer(ds, null, null)));
            Files.delete(out);
        }
    }
}
