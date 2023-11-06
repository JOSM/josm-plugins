// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

@BasicPreferences
@Main
@Projection
class StreetsideLayerTest {
  private static Layer getDummyLayer() {
    return ImageryLayer.create(new ImageryInfo("dummy", "https://example.org"));
  }

  @Test
  void testGetIcon() {
    assertNotNull(StreetsideLayer.getInstance().getIcon());
  }

  @Test
  void testIsMergable() {
    assertFalse(StreetsideLayer.getInstance().isMergable(getDummyLayer()));
  }

  @Test
  void testMergeFrom() {
    StreetsideLayer layer = StreetsideLayer.getInstance();
    Layer dummyLayer = getDummyLayer();
    assertThrows(UnsupportedOperationException.class, () -> layer.mergeFrom(dummyLayer));
  }

  @Test
  void testSetVisible() {
    StreetsideLayer.getInstance().getData().add(new StreetsideImage(CubemapUtils.TEST_IMAGE_ID, new LatLon(0.0, 0.0), 0.0));
    StreetsideLayer.getInstance().getData().add(new StreetsideImage(CubemapUtils.TEST_IMAGE_ID, new LatLon(0.0, 0.0), 0.0));
    StreetsideImage invisibleImage = new StreetsideImage(CubemapUtils.TEST_IMAGE_ID, new LatLon(0.0, 0.0), 0.0);
    invisibleImage.setVisible(false);
    StreetsideLayer.getInstance().getData().add(invisibleImage);

    StreetsideLayer.getInstance().setVisible(false);
    for (StreetsideAbstractImage img : StreetsideLayer.getInstance().getData().getImages()) {
      assertFalse(img.isVisible());
    }


    StreetsideLayer.getInstance().setVisible(true);
    for (StreetsideAbstractImage img : StreetsideLayer.getInstance().getData().getImages()) {
      assertTrue(img.isVisible());
    }
  }

  @Test
  void testGetInfoComponent() {
    Object comp = StreetsideLayer.getInstance().getInfoComponent();
    assertInstanceOf(String.class, comp);
    assertTrue(((String) comp).length() >= 9);
  }

  @DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "Listener for destruction is only registered in non-headless environments")
  @Test
  void testClearInstance() {
    StreetsideLayer.getInstance();
    assertTrue(StreetsideLayer.hasInstance());
    JOSMTestRules.cleanLayerEnvironment();
    assertFalse(StreetsideLayer.hasInstance());
    StreetsideLayer.getInstance();
    assertTrue(StreetsideLayer.hasInstance());
  }
}
