// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.AddrInterpolation;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class AddrInterpolationPlugin extends Plugin {

    /**
     * constructor
     */
    public AddrInterpolationPlugin(PluginInformation info) {
        super(info);
        MainMenu.add(MainApplication.getMenu().dataMenu, new AddrInterpolationAction(), false, 0);
    }
}
