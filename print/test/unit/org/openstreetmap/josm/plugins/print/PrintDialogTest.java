// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.print;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import javax.print.attribute.standard.OrientationRequested;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit test of {@link PrintDialog} class.
 */
@BasicPreferences
class PrintDialogTest {
    /**
     * Unit test of {@link PrintDialog#unmarshallPrintSetting}
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUnmarshallPrintSetting() throws ReflectiveOperationException {
        assertEquals(OrientationRequested.PORTRAIT, PrintDialog.unmarshallPrintSetting(Arrays.asList(
                "javax.print.attribute.standard.OrientationRequested",
                "javax.print.attribute.standard.OrientationRequested",
                "javax.print.attribute.EnumSyntax",
                "3")));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/13302">#13302</a>
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    @Disabled("not fixed yet")
    void testTicket13302() throws ReflectiveOperationException {
        assertEquals(OrientationRequested.PORTRAIT, PrintDialog.unmarshallPrintSetting(Arrays.asList(
                "javax.print.attribute.standard.MediaSizeName",
                "sun.print.CustomMediaSizeName",
                "javax.print.attribute.EnumSyntax",
                "82")));
    }
}
