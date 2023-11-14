// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.streetside.cache.StreetsideCache.Type;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

@BasicPreferences
class StreetsideCacheTest {

    @Test
    void testCache() {
        StreetsideCache cache = new StreetsideCache("00000", Type.FULL_IMAGE);
        assertNotNull(cache.getUrl());
        assertNotNull(cache.getCacheKey());

        assertFalse(cache.isObjectLoadable());

        cache = new StreetsideCache("00000", Type.THUMBNAIL);
        assertNotNull(cache.getCacheKey());
        assertNotNull(cache.getUrl());

        cache = new StreetsideCache(null, null);
        assertNull(cache.getCacheKey());
        assertNull(cache.getUrl());
    }
}
