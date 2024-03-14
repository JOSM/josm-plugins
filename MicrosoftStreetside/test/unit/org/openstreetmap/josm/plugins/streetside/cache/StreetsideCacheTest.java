// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

@BasicPreferences
class StreetsideCacheTest {

    @Test
    void testCache() {
        StreetsideCache cache = new StreetsideCache("https://ecn.t0.tiles.virtualearth.net/tiles/hs101320223333223201");
        assertNotNull(cache.getUrl());
        assertNotNull(cache.getCacheKey());

        assertFalse(cache.isObjectLoadable());

        cache = new StreetsideCache("https://ecn.t0.tiles.virtualearth.net/tiles/hs101320223333223201");
        assertNotNull(cache.getCacheKey());
        assertNotNull(cache.getUrl());
    }
}
