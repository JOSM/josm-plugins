// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.util.logging.Logger;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

/**
 * The current state of the plugin (used for uploads and downloads)
 * @author nokutu
 */
public final class PluginState {

    private static final Logger LOGGER = Logger.getLogger(PluginState.class.getCanonicalName());

    private static int runningDownloads;

    private PluginState() {
        // Empty constructor to avoid instantiation
    }

    /**
     * Called when a download is started.
     */
    public static void startDownload() {
        runningDownloads++;
    }

    /**
     * Called when a download is finished.
     */
    public static void finishDownload() {
        if (runningDownloads == 0) {
            LOGGER.log(Logging.LEVEL_WARN, () -> I18n.tr("The amount of running downloads is equal to 0"));
            return;
        }
        runningDownloads--;
    }

    /**
     * Checks if there is any running download.
     *
     * @return true if the plugin is downloading; false otherwise.
     */
    public static boolean isDownloading() {
        return runningDownloads > 0;
    }
}
