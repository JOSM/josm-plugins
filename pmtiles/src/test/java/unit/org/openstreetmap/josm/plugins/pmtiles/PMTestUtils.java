// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles;

import java.io.File;
import java.net.URI;

/**
 * Utils for testing
 */
public final class PMTestUtils {
    private PMTestUtils() { /* Hide constructor */ }
    /** A sample vector tileset */

    public static final URI ODBL_VECTOR_FIRENZE = new File("protomaps(vector)ODbL_firenze.pmtiles").exists() ?
            new File("protomaps(vector)ODbL_firenze.pmtiles").toURI() :
            URI.create("https://github.com/protomaps/PMTiles/raw/main/spec/v3/protomaps(vector)ODbL_firenze.pmtiles");

    /** A sample raster tileset */
    public static final URI ODBL_RASTER_STAMEN = new File("stamen_toner(raster)CC-BY%2BODbL_z3.pmtiles").exists() ?
            new File("stamen_toner(raster)CC-BY%2BODbL_z3.pmtiles").toURI() :
            URI.create("https://github.com/protomaps/PMTiles/raw/main/spec/v3/stamen_toner(raster)CC-BY%2BODbL_z3.pmtiles");

}
