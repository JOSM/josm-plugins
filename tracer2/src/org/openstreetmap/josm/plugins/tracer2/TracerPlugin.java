// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.tracer2;

import java.io.File;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.tracer2.preferences.ServerParamList;
import org.openstreetmap.josm.plugins.tracer2.preferences.ServerParamPreference;

public class TracerPlugin extends Plugin {

    public static TracerPlugin s_oPlugin;

    public final ServerParamList m_oParamList;

    public TracerPlugin(PluginInformation info) {
        super(info);
        MainMenu.add(Main.main.menu.moreToolsMenu, new TracerAction(Main.map));

        s_oPlugin = this;

        File plugindir = new File(this.getPluginDir());
        if (!plugindir.exists()) {
            plugindir.mkdirs();
        }

        m_oParamList = new ServerParamList(new File(plugindir, "serverParam.cfg").getAbsolutePath());
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new ServerParamPreference(this);
    }
}
