// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * Integration tests of {@link ToulouseModule} class.
 */
@BasicPreferences
@Timeout(30)
class ToulouseModuleTestIT {
    @Test
    void testUrlValidity() throws IOException {
        Map<String, Integer> errors = new TreeMap<>();
        for (AbstractDataSetHandler handler : new ToulouseModule(null).getNewlyInstanciatedHandlers()) {
            int code = HttpClient.create(handler.getLocalPortalURL()).connect().getResponseCode();
            if (code >= 400) {
                errors.put(handler.getLocalPortalURL().toExternalForm(), code);
            }
        }
        assertTrue(errors.isEmpty(), errors.toString());
    }
}
