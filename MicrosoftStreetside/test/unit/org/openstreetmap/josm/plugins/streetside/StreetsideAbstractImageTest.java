// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

class StreetsideAbstractImageTest {
  @Test
  void testIsModified() {
    StreetsideImage img = new StreetsideImage("key___________________", new LatLon(0, 0), 0);
    assertFalse(img.isModified());
    img.turn(1e-4);
    img.stopMoving();
    assertTrue(img.isModified());
    img.turn(-1e-4);
    img.stopMoving();
    assertFalse(img.isModified());
    img.move(1e-4, 1e-4);
    img.stopMoving();
    assertTrue(img.isModified());
    img.move(-1e-4, -1e-4);
    img.stopMoving();
    assertFalse(img.isModified());
  }
}
