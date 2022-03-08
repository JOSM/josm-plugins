// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lwjgl;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This exists to load the plugin and nothing else.
 *
 * @author Taylor Smock
**/
public class LWJGLNativesPlugin extends Plugin {
    /**
     * Main constructor.
     *
     * @param info Required information of the plugin. Obtained from the jar file.
     */
    public LWJGLNativesPlugin(PluginInformation info) {
        super(info);
    }
}
