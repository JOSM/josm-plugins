// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.testutils.annotations.LayerManager;

@Disabled
@LayerManager
class SequenceDownloadRunnableTest {

  private static final Function<Bounds, URL> SEARCH_SEQUENCES_URL_GEN = b -> {
    return SequenceDownloadRunnableTest.class.getResource("/api/v3/responses/searchSequences.json");
  };
  private Field urlGenField;


  @Test
  void testRun1() throws IllegalArgumentException, IllegalAccessException {
    testNumberOfDecodedImages(4, SEARCH_SEQUENCES_URL_GEN, new Bounds(7.246497, 16.432955, 7.249027, 16.432976));
  }

  @Test
  void testRun2() throws IllegalArgumentException, IllegalAccessException {
    testNumberOfDecodedImages(0, SEARCH_SEQUENCES_URL_GEN, new Bounds(0, 0, 0, 0));
  }

  @Test
  void testRun3() throws IllegalArgumentException, IllegalAccessException {
    testNumberOfDecodedImages(0, b -> {
      try { return new URL("https://streetside/nonexistentURL"); } catch (MalformedURLException e) { return null; }
    }, new Bounds(0, 0, 0, 0));
  }

  @Test
  void testRun4() throws IllegalArgumentException, IllegalAccessException {
    StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.put(true);
    testNumberOfDecodedImages(4, SEARCH_SEQUENCES_URL_GEN, new Bounds(7.246497, 16.432955, 7.249027, 16.432976));
  }

  @Test
  void testRun5() throws IllegalArgumentException, IllegalAccessException {
    StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.put(true);
    testNumberOfDecodedImages(0, SEARCH_SEQUENCES_URL_GEN, new Bounds(0, 0, 0, 0));
  }

  private void testNumberOfDecodedImages(int expectedNumImgs, Function<Bounds, URL> urlGen, Bounds bounds)
      throws IllegalArgumentException, IllegalAccessException {
    SequenceDownloadRunnable r = new SequenceDownloadRunnable(StreetsideLayer.getInstance().getData(), bounds);
    urlGenField.set(null, urlGen);
    r.run();
    assertEquals(expectedNumImgs, StreetsideLayer.getInstance().getData().getImages().size());
  }
}
