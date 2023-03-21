// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ToulouseModule} class.
 */
@BasicPreferences
class ToulouseModuleTest {
    @Test
    void testHandlersConstruction() {
        ToulouseModule module = new ToulouseModule(null);
        assertFalse(module.getHandlers().isEmpty());
        assertEquals(module.getHandlers().size(), module.getNewlyInstanciatedHandlers().size());
    }
}
