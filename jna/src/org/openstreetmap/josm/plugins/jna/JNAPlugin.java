// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.jna.mac.JosmMacNativeLogHandler;
import org.openstreetmap.josm.plugins.jna.win.JosmWinNativeLogHandler;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * This plugin provides the Java Native Access (JNA) library for JOSM plugins.
 */
public class JNAPlugin extends Plugin {

    /**
     * Constructs a new {@code JNAPlugin}
     * @param info plugin info
     */
    public JNAPlugin(PluginInformation info) {
        super(info);
        if (PlatformManager.isPlatformOsx()) {
            Logging.getLogger().addHandler(new JosmMacNativeLogHandler());
        } else if (PlatformManager.isPlatformWindows()) {
            Logging.getLogger().addHandler(new JosmWinNativeLogHandler());
        }
    }
}
