// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lwjgl;

import org.lwjgl.system.Configuration;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;

/**
 * This exists to load the plugin and nothing else.
 *
 * @author Taylor Smock
**/
public class LWJGLPlugin extends Plugin {
    /**
     * Main constructor.
     *
     * @param info Required information of the plugin. Obtained from the jar file.
     */
    public LWJGLPlugin(PluginInformation info) {
        super(info);
        Configuration.DEBUG.set(Logging.isDebugEnabled());
        Configuration.DEBUG_STACK.set(Logging.isDebugEnabled());
        Configuration.DEBUG_LOADER.set(Logging.isDebugEnabled());
        Configuration.DEBUG_MEMORY_ALLOCATOR.set(Logging.isDebugEnabled());
        // TODO FIXME use trace instead of debug for this one
        Configuration.DEBUG_FUNCTIONS.set(Logging.isDebugEnabled());
    }
}
