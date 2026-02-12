// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

@BasicPreferences
class TurnRestrictionTypeRendererTest {
    @Test
    void testConstructor() {
        TurnRestrictionTypeRenderer renderer = new TurnRestrictionTypeRenderer();

        assertNotNull(renderer.icons);
        assertNotNull(renderer.icons.get(TurnRestrictionType.NO_LEFT_TURN));
    }

    @Test
    void testGetListCellRendererComponent1() {
        TurnRestrictionTypeRenderer renderer = new TurnRestrictionTypeRenderer();

        JLabel c = (JLabel) renderer.getListCellRendererComponent(null, null, 0, false, false);
        assertNull(c.getIcon());
        assertNotNull(c.getText());

        c = (JLabel) renderer.getListCellRendererComponent(null, "non-standard-value", 0, false, false);
        assertNull(c.getIcon());
        assertEquals("non-standard-value", c.getText());

        c = (JLabel) renderer.getListCellRendererComponent(null, TurnRestrictionType.NO_LEFT_TURN, 0, false, false);
        assertEquals(renderer.icons.get(TurnRestrictionType.NO_LEFT_TURN), c.getIcon());
        assertEquals(TurnRestrictionType.NO_LEFT_TURN.getDisplayName(), c.getText());
    }
}
