// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CubemapUtilsTest {

    @Test
    void testConvertDecimal2Quaternary() {
        final long decimal0 = 680730040L;
        final long decimal1 = 680931568L;
        String res = CubemapUtils.convertDecimal2Quaternary(decimal0);
        assertEquals("220210301312320", res);
        res = CubemapUtils.convertDecimal2Quaternary(decimal1);
        assertEquals("220211203003300", res);
    }

    @Test
    void testConvertQuaternary2Decimal() {
        final String quadKey0 = "220210301312320";
        final String quadKey1 = "220211203003300";
        String res = CubemapUtils.convertQuaternary2Decimal(quadKey0);
        assertEquals("680730040", res);
        res = CubemapUtils.convertQuaternary2Decimal(quadKey1);
        assertEquals("680931568", res);
    }
}
