package net.simon04.comfort0;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@BasicPreferences
class OsmToLevel0LTest {

    @BeforeEach
    public void setUp() {
        Config.getPref().putBoolean("osm-primitives.showcoor", true);
    }

    @Test
    void testNode() {
        final Node node = new Node(1234L, 42);
        node.setCoor(new LatLon(123.45, 67.89));
        node.put("name", "Comfort0");
        final OsmToLevel0L converter = new OsmToLevel0L();
        converter.visit(node);
        assertThat(converter.toString(), is(""
                + "node 1234: 123.45, 67.89 #Comfort0 (123.45, 67.89)\n"
                + "  name = Comfort0\n"));
    }

    @Test
    void testLargerExample() throws Exception {
        // from https://wiki.openstreetmap.org/wiki/OSM_XML#OSM_XML_file_format
        // compare to https://wiki.openstreetmap.org/wiki/Level0L#Examples
        try (InputStream in = getClass().getClassLoader().getResource("example.osm").openStream()) {
            final DataSet dataSet = OsmReader.parseDataSet(in, null);
            final OsmToLevel0L converter = new OsmToLevel0L();
            converter.visit(dataSet.allPrimitives());
            assertThat(converter.toString(), is("" +
                    "way 4579143 #incomplete\n" +
                    "\n" +
                    "way 26659127 #Pastower Straße (3 nodes)\n" +
                    "  highway = unclassified\n" +
                    "  name = Pastower Straße\n" +
                    "  nd 292403538 #incomplete\n" +
                    "  nd 298884289 #incomplete\n" +
                    "  nd 261728686 #261728686 (54.0906309, 12.2441924)\n" +
                    "\n" +
                    "relation 56688 #route (\"Küstenbus Linie 123–124\", 4 members, incomplete)\n" +
                    "  name = Küstenbus Linie 123–124\n" +
                    "  network = VVW\n" +
                    "  operator = Regionalverkehr Küste\n" +
                    "  ref = 123\n" +
                    "  route = bus\n" +
                    "  type = route\n" +
                    "  nd 294942404  #incomplete\n" +
                    "  nd 364933006  #incomplete\n" +
                    "  wy 4579143  #incomplete\n" +
                    "  nd 249673494  #incomplete\n" +
                    "\n" +
                    "node 249673494 #incomplete\n" +
                    "\n" +
                    "node 261728686: 54.0906309, 12.2441924 #261728686 (54.0906309, 12.2441924)\n" +
                    "\n" +
                    "node 292403538 #incomplete\n" +
                    "\n" +
                    "node 294942404 #incomplete\n" +
                    "\n" +
                    "node 298884269: 54.0901746, 12.2482632 #298884269 (54.0901746, 12.2482632)\n" +
                    "\n" +
                    "node 298884272: 54.0901447, 12.2516513 #298884272 (54.0901447, 12.2516513)\n" +
                    "\n" +
                    "node 298884289 #incomplete\n" +
                    "\n" +
                    "node 364933006 #incomplete\n" +
                    "\n" +
                    "node 1831881213: 54.0900666, 12.2539381 #Neu Broderstorf (54.0900666, 12.2539381)\n" +
                    "  name = Neu Broderstorf\n" +
                    "  traffic_sign = city_limit\n"));
        }
    }
}
