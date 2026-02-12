// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;


class TurnRestrictionTypeTest {

    @Test
    void testFromTagValue() {

        TurnRestrictionType type = TurnRestrictionType.fromTagValue("no_left_turn");
        assertEquals(TurnRestrictionType.NO_LEFT_TURN, type);

        type = TurnRestrictionType.fromTagValue("doesnt_exist");
        assertNull(type);

        type = TurnRestrictionType.fromTagValue(null);
        assertNull(type);
    }
}
