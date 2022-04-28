// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

@BasicPreferences
class HgtReaderTest {

    /**
     * Setup test.
     * @throws IOException if SRTM files cannot be installed
     */
    @BeforeEach
    void setUp() throws IOException {
        // Install SRTM files to plugin directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(TestUtils.getTestDataRoot()), "*.hgt")) {
            Path dir = Config.getDirs().getUserDataDirectory(true).toPath().resolve("elevation");

            if (!Files.exists(dir)) {
                Files.createDirectory(dir);
            }
            for (Path src: stream) {
                Path dst = dir.resolve(src.getFileName());
                if (!Files.exists(dst)) {
                    Files.copy(src, dst);
                }
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            throw ex.getCause();
        }
    }
    static Stream<Arguments> testHgtData() {
        return Stream.of(
            // Staufenberg, Hessen
            Arguments.of(50.6607106, 8.7337029, "N50E008.hgt", 199),
            // Ulrichstein, Hessen
            Arguments.of(50.5767627, 9.1938483, "N50E009.hgt", 560),
            // Fujijama
            //testHgtData(35.360555, 138.727777, "N35E138.hgt", 3741),
            // Some random location in the middle of a file
            Arguments.of(50.5, 8.5, "N50E008.hgt", 274),
            Arguments.of(50.0000001, 8.999999, "N50E008.hgt", 132)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testHgtData(final double lat, final double lon,
            final String expTag, final int expHeight) {
        LatLon l = new LatLon(lat, lon);
        String text = HgtReader.getHgtFileName(l);

        assertEquals(expTag, text);

        double d = HgtReader.getElevationFromHgt(l);
        Logging.trace(Double.toString(d));
        assertFalse(Double.isNaN(d), "Data missing or void for coor " + l);

        assertEquals(expHeight, (int) d);
    }
}
