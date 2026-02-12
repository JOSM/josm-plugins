// License: GPL. For details, see LICENSE file.
package poly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for {@link PolyImporter}.
 * @author Gerd Petermann
 */
@BasicPreferences
class PolyImporterTest {
    /**
     * @throws Exception if an error occurs
     */
    @Test
    void testSimple() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/simple.poly");
        assertNotNull(ds);
        assertEquals(4, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());
        assertEquals(0, ds.getRelations().size());
    }

    /**
     * File with human friendly coordinate format
     * @throws Exception if an error occurs
     */
    @Test
    void testSimple2() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/splitter.poly");
        assertNotNull(ds);
        assertEquals(14, ds.getNodes().size());
        assertEquals(1, ds.getWays().size());
        assertEquals(0, ds.getRelations().size());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    void testHoles() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/holes.poly");
        assertNotNull(ds);
        assertEquals(76, ds.getNodes().size());
        assertEquals(4, ds.getWays().size());
        assertEquals(1, ds.getRelations().size());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    void testTwoOuter() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/australia_v.poly");
        assertNotNull(ds);
        assertEquals(18, ds.getNodes().size());
        assertEquals(2, ds.getWays().size());
        assertEquals(1, ds.getRelations().size());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    void testDoubleEnd() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/bremen-double-end.poly");
        assertNotNull(ds);
        assertEquals(337, ds.getNodes().size());
        assertEquals(2, ds.getWays().size());
        assertEquals(1, ds.getRelations().size());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    void testMultipleFile() throws Exception {
        DataSet ds = new PolyImporter().parseDataSet(TestUtils.getTestDataRoot() + "/multi-concat.poly");
        assertNotNull(ds);
        assertEquals(40, ds.getNodes().size());
        assertEquals(4, ds.getWays().size());
        assertEquals(1, ds.getRelations().size());
    }

    /**
     * Should throw an IllegalDataException
     */
    @Test
    void testNameMissing() {
        final PolyImporter importer = new PolyImporter();
        assertThrows(IllegalDataException.class, () -> importer.parseDataSet(TestUtils.getTestDataRoot() + "/name-missing.poly"));
    }

}
