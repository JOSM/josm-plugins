// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pointinfo;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This is the main class for the PointInfo plugin.
 * @author Marián Kyral
 */
public class PointInfoPlugin extends Plugin {

    /**
     * Constructs a new {@code PointInfoPlugin}.
     * @param info plugin information
     */
    public PointInfoPlugin(PluginInformation info) {
        super(info);
        MainMenu.add(MainApplication.getMenu().moreToolsMenu, new PointInfoAction());
    }
}
