// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.streetside.utils.TestUtil;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Tests for {@link StreetsideData} class.
 *
 * @author nokutu
 * @see StreetsideData
 */
@Main
class StreetsideDataTest {

    private StreetsideData data;
    private StreetsideImage img1;
    private StreetsideImage img2;
    private StreetsideImage img3;
    private StreetsideImage img4;

    /**
     * Creates a sample {@link StreetsideData} object and 4 {@link StreetsideImage}
     * objects.
     */
    @BeforeEach
    public void setUp() {
        img1 = TestUtil.generateImage("1", 0.1, 0.1);
        img2 = TestUtil.generateImage("2", 0.2, 0.2);
        img3 = TestUtil.generateImage("3", 0.3, 0.3);
        img4 = TestUtil.generateImage("4", 0.4, 0.4);

        data = new StreetsideData();
        data.addAll(Arrays.asList(img1, img2, img3, img4));
    }

    /**
     * Tests the addition of new images. If a second image with the same key as
     * another one in the database, the one that is being added should be ignored.
     */
    @Test
    void testAdd() {
        data = new StreetsideData();
        assertEquals(0, data.getImages().size());
        data.add(img1);
        assertEquals(1, data.getImages().size());
        data.add(img1);
        assertEquals(1, data.getImages().size());
        data.addAll(new ConcurrentSkipListSet<>(Arrays.asList(img2, img3)));
        assertEquals(3, data.getImages().size());
        data.addAll(new ConcurrentSkipListSet<>(Arrays.asList(img3, img4)));
        assertEquals(4, data.getImages().size());
    }

    /**
     * Test that the size is properly calculated.
     */
    @Test
    void testSize() {
        assertEquals(4, data.getImages().size());
        data.add(TestUtil.generateImage("5", 0.1, 0.1));
        assertEquals(5, data.getImages().size());
    }

    /**
     * Test the {@link StreetsideData#setHighlightedImage(StreetsideImage)}
     * and {@link StreetsideData#getHighlightedImage()} methods.
     */
    @Test
    void testHighlight() {
        data.setHighlightedImage(img1);
        assertEquals(img1, data.getHighlightedImage());

        data.setHighlightedImage(null);
        assertNull(data.getHighlightedImage());
    }

    /**
     * Tests the selection of images.
     */
    @Disabled("The imgs have non-int identifiers while the code expects the identifiers to be int in string form")
    @Test
    void testSelect() {
        data.setSelectedImage(img1);
        assertEquals(img1, data.getSelectedImage());

        data.setSelectedImage(img4);
        assertEquals(img4, data.getSelectedImage());

        data.setSelectedImage(null);
        assertNull(data.getSelectedImage());
    }

    /**
     * Tests the {@link StreetsideData#selectNext()} and
     * {@link StreetsideData#selectPrevious()} methods.
     */
    @Test
    @Disabled("The imgs have non-int identifiers while the code expects the identifiers to be int in string form")
    void testNextAndPrevious() {
        data.setSelectedImage(img1);

        data.selectNext();
        assertEquals(img2, data.getSelectedImage());
        data.selectNext();
        assertEquals(img3, data.getSelectedImage());
        data.selectPrevious();
        assertEquals(img2, data.getSelectedImage());

        data.setSelectedImage(null);
    }

    @Disabled("Someone decided to not throw an IllegalStateException. No clue why.")
    @Test
    void testNextOfNullImg() {
        data.setSelectedImage(null);
        assertThrows(IllegalStateException.class, data::selectNext);
    }

    @Disabled("Someone decided to not throw an IllegalStateException. No clue why.")
    @Test
    void testPreviousOfNullImg() {
        data.setSelectedImage(null);
        assertThrows(IllegalStateException.class, data::selectPrevious);
    }
}
