// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

/**
 * Tests {@link PluginState} class.
 *
 * @author nokutu
 * @see PluginState
 */
class PluginStateTest {

    /**
     * Test the methods related to the download.
     */
    @Test
    void testDownload() {
        assertFalse(PluginState.isDownloading());
        PluginState.startDownload();
        assertTrue(PluginState.isDownloading());
        PluginState.startDownload();
        assertTrue(PluginState.isDownloading());
        PluginState.finishDownload();
        assertTrue(PluginState.isDownloading());
        PluginState.finishDownload();
        assertFalse(PluginState.isDownloading());
    }

    /**
     * Tests the methods related to the upload.
     */
    @Test
    void testUpload() {
        TestUtils.assumeWorkingJMockit();
        JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();
        jopsMocker.getMockResultMap().put("You have successfully uploaded 2 images to Bing.com", JOptionPane.OK_OPTION);
        assertFalse(PluginState.isUploading());
        PluginState.addImagesToUpload(2);
        assertEquals(2, PluginState.getImagesToUpload());
        assertEquals(0, PluginState.getImagesUploaded());
        assertTrue(PluginState.isUploading());
        PluginState.imageUploaded();
        assertEquals(1, PluginState.getImagesUploaded());
        assertTrue(PluginState.isUploading());
        PluginState.imageUploaded();
        assertFalse(PluginState.isUploading());
        assertEquals(2, PluginState.getImagesToUpload());
        assertEquals(2, PluginState.getImagesUploaded());
    }
}
