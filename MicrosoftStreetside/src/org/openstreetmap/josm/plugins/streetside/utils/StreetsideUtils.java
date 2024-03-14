// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.tools.I18n;

/**
 * Set of utilities.
 *
 * @author nokutu
 */
public final class StreetsideUtils {

    private StreetsideUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Updates the help text at the bottom of the window.
     */
    public static void updateHelpText() {
        if (MainApplication.getMap() == null || MainApplication.getMap().statusLine == null) {
            return;
        }
        final var ret = new StringBuilder();
        if (PluginState.isDownloading()) {
            ret.append(I18n.tr("Downloading Streetside images"));
        } else if (StreetsideLayer.hasInstance() && !StreetsideLayer.getInstance().getData().getImages().isEmpty()) {
            ret.append(I18n.tr("Total Streetside images: {0}", StreetsideLayer.getInstance().getToolTipText()));
        } else {
            ret.append(I18n.tr("No images found"));
        }
        if (StreetsideLayer.hasInstance() && StreetsideLayer.getInstance().mode != null) {
            ret.append(" — ").append(I18n.tr(StreetsideLayer.getInstance().mode.toString()));
        }
        MainApplication.getMap().statusLine.setHelpText(ret.toString());
    }
}
