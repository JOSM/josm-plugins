// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

class StreetsideColorSchemeTest {

  @Test
  void testUtilityClass() {
    TestUtil.testUtilityClass(StreetsideColorScheme.class);
  }

  @Test
  void testStyleAsDefaultPanel() {
    assertDoesNotThrow(() -> StreetsideColorScheme.styleAsDefaultPanel());
    assertDoesNotThrow(() -> StreetsideColorScheme.styleAsDefaultPanel((JComponent[]) null));
  }
}
