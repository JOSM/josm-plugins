// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.housenumbertool;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link TagDialog}.
 */
class HouseNumberHelperTest {

    /**
     * Unit test of {@link HouseNumberHelper#incrementHouseNumber}
     */
    @Test
    void testIncrementHouseNumber() {
        assertEquals("2", HouseNumberHelper.incrementHouseNumber("1", 1));
        assertEquals("12", HouseNumberHelper.incrementHouseNumber("10", 2));
        assertEquals("2A", HouseNumberHelper.incrementHouseNumber("1A", 1));
        assertEquals("E2", HouseNumberHelper.incrementHouseNumber("E1", 1));
        //assertEquals("۲", HouseNumberHelper.incrementHouseNumber("۱", 1)); // FIXME: how to increment persian numbers ?
        assertEquals("2", HouseNumberHelper.incrementHouseNumber("۱", 1));
        assertNull(HouseNumberHelper.incrementHouseNumber(null, 1));
        assertEquals("2/1", HouseNumberHelper.incrementHouseNumber("1/1", 1));
    }

    /**
     * Unit test of {@link HouseNumberHelper#incrementHouseNumberSuffix}
     */
    @Test
    void testIncrementHouseNumberSuffix() {
        assertEquals("1a", HouseNumberHelper.incrementHouseNumberSuffix("1"));
        assertEquals("1b", HouseNumberHelper.incrementHouseNumberSuffix("1a"));
        assertEquals("1B", HouseNumberHelper.incrementHouseNumberSuffix("1A"));
        assertEquals("1-b", HouseNumberHelper.incrementHouseNumberSuffix("1-a"));
        assertEquals("1/2", HouseNumberHelper.incrementHouseNumberSuffix("1/1"));
        assertEquals("E1b", HouseNumberHelper.incrementHouseNumberSuffix("E1a"));
        assertNull(HouseNumberHelper.incrementHouseNumberSuffix(null));
    }

    /**
     * Unit test of {@link HouseNumberHelper#hasSuffix} and {@link HouseNumberHelper#removeSuffix}
     */
    @Test
    void testRemoveSuffix() {
        assertFalse(HouseNumberHelper.hasSuffix("1"));
        assertFalse(HouseNumberHelper.hasSuffix("E1"));
        assertTrue(HouseNumberHelper.hasSuffix("1a"));
        assertTrue(HouseNumberHelper.hasSuffix("1/1"));
        assertFalse(HouseNumberHelper.hasSuffix(null));
        assertEquals("1", HouseNumberHelper.removeSuffix("1a"));
        assertEquals("1", HouseNumberHelper.removeSuffix("1-1"));
        assertEquals("E1", HouseNumberHelper.removeSuffix("E1a"));
    }
}
