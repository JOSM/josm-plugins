// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.testutils.annotations.LayerManager;

@Disabled
@LayerManager
class SequenceDownloadRunnableTest {

    @Test
    void testRun1() throws IllegalArgumentException {
        testNumberOfDecodedImages(4, new Bounds(7.246497, 16.432955, 7.249027, 16.432976));
    }

    @Test
    void testRun2() throws IllegalArgumentException {
        testNumberOfDecodedImages(0, new Bounds(0, 0, 0, 0));
    }

    @Test
    void testRun3() throws IllegalArgumentException {
        testNumberOfDecodedImages(0, new Bounds(0, 0, 0, 0));
    }

    @Test
    void testRun4() throws IllegalArgumentException {
        StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.put(true);
        testNumberOfDecodedImages(4, new Bounds(7.246497, 16.432955, 7.249027, 16.432976));
    }

    @Test
    void testRun5() throws IllegalArgumentException {
        StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.put(true);
        testNumberOfDecodedImages(0, new Bounds(0, 0, 0, 0));
    }

    private void testNumberOfDecodedImages(int expectedNumImgs, Bounds bounds)
            throws IllegalArgumentException {
        SequenceDownloadRunnable r = new SequenceDownloadRunnable(StreetsideLayer.getInstance().getData(), bounds);
        r.run();
        assertEquals(expectedNumImgs, StreetsideLayer.getInstance().getData().getImages().size());
    }
}
