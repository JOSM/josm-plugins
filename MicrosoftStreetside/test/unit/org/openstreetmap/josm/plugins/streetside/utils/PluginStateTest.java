// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
