// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.tabular;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.NonRegFunctionalTests;

/**
 * Unit tests of {@link CsvReader} class.
 */
public class CsvReaderTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }
    
    private static AbstractDataSetHandler newHandler(final String epsgCode) {
        AbstractDataSetHandler handler = new AbstractDataSetHandler() {
            @Override
            public boolean acceptsFilename(String filename) {
                return true;
            }
            @Override
            public void updateDataSet(DataSet ds) {
            }
        };
        handler.setSpreadSheetHandler(new DefaultCsvHandler() {
            @Override
            public boolean handlesProjection() {
                return true;
            }
            @Override
            public LatLon getCoor(EastNorth en, String[] fields) {
                return Projections.getProjectionByCode(epsgCode).eastNorth2latlon(en);
            }
        });
        return handler;
    }
    
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10214">#10214</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket10214() throws IOException, XMLStreamException, FactoryConfigurationError {
        File file = new File(TestUtils.getRegressionDataFile(10214, "utf8_test.csv"));
        try (InputStream is = new FileInputStream(file)) {
            NonRegFunctionalTests.testTicket10214(CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }
    
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/8805">#8805</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket8805() throws IOException, XMLStreamException, FactoryConfigurationError {
        File file = new File(TestUtils.getRegressionDataFile(8805, "XXX.csv"));
        try (InputStream is = new FileInputStream(file)) {
            NonRegFunctionalTests.testGeneric("#8805", CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }
}
