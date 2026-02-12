// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExceptValueModelTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "hgv", "hgv;psv", "non_standard"})
    void testStringConstructors(String value) {
        assertDoesNotThrow(() -> new ExceptValueModel(value));
    }

    @Test
    void testAdditionalConstructors() {
        assertAll(() -> assertDoesNotThrow(() -> new ExceptValueModel()),
                () -> assertDoesNotThrow(() -> new ExceptValueModel(null)));
    }

    @Test
    void testSetValue() {
        ExceptValueModel evm;

        // null value allowed - means no vehicle exceptions
        evm = new ExceptValueModel();
        evm.setValue(null);
        assertEquals("", evm.getValue());
        assertTrue(evm.isStandard());

        // empty string allowed - means no vehicle expections
        evm = new ExceptValueModel();
        evm.setValue("");
        assertEquals("", evm.getValue());
        assertTrue(evm.isStandard());

        // a single standard vehicle exeption
        evm = new ExceptValueModel();
        evm.setValue("hgv");
        assertEquals("hgv", evm.getValue());
        assertTrue(evm.isVehicleException("hgv"));
        assertFalse(evm.isVehicleException("psv"));
        assertTrue(evm.isStandard());

        // two standard vehicle exceptions
        evm = new ExceptValueModel();
        evm.setValue("hgv;psv");
        assertEquals("hgv;psv", evm.getValue());
        assertTrue(evm.isVehicleException("hgv"));
        assertTrue(evm.isVehicleException("psv"));
        assertTrue(evm.isStandard());

        // white space and lowercase/uppercase mix allowed. Should be normalized
        // by the except value model
        evm = new ExceptValueModel();
        evm.setValue(" hGv ; PsV  ");
        assertEquals("hgv;psv", evm.getValue());
        assertTrue(evm.isVehicleException("hgv"));
        assertTrue(evm.isVehicleException("psv"));
        assertTrue(evm.isStandard());

        // non standard value allowed
        evm = new ExceptValueModel();
        evm.setValue("Non Standard");
        assertEquals("Non Standard", evm.getValue());
        assertFalse(evm.isVehicleException("hgv"));
        assertFalse(evm.isVehicleException("psv"));
        assertFalse(evm.isStandard());
    }
}
