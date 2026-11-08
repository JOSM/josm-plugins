// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.housenumbertool;

import java.io.File;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Simple tool to tag house numbers. Select house and press 'k'. Select your addr-tags and press OK.
 * Press 'shift+k' to switch between incrementing the number and the suffix.
 */
public class HouseNumberTaggingToolPlugin extends Plugin {

    /**
     * constructor
     * @param info plugin info
     */
    public HouseNumberTaggingToolPlugin(PluginInformation info) {
        super(info);
        File pluginDir = getPluginDirs().getUserDataDirectory(false);
        MainMenu.add(MainApplication.getMenu().dataMenu, new LaunchAction(pluginDir), false, 0);
        MainMenu.add(MainApplication.getMenu().dataMenu, new LaunchAction(pluginDir, true), false, 1);
    }
}
