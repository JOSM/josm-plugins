// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.surveyor;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests of {@link #SurveyorShowAction}
 */
@BasicPreferences
class SurveyorShowActionTest {
    @Test
    void testCreateComponent() {
        Logging.clearLastErrorAndWarnings();
        SurveyorComponent comp = SurveyorShowAction.createComponent();
        assertNotNull(comp);
        List<String> errors = Logging.getLastErrorAndWarnings();
        assertTrue(errors.isEmpty(), errors.toString());
    }
}
