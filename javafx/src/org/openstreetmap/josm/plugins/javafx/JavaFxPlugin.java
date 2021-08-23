// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.javafx;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.javafx.io.audio.JavaFxMediaPlayer;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * JavaFX plugin brings OpenJFX (JavaFX) additional features.
 */
public class JavaFxPlugin extends Plugin {

    /**
     * Constructs a new {@code JavaFxPlugin}.
     * @param info plugin info
     */
    public JavaFxPlugin(PluginInformation info) {
        super(info);
        if (!isJavaFx()) {
            Logging.error("JavaFX is not available");
            StringBuilder message = new StringBuilder(tr("JavaFX is not available."));
            PlatformManager.getPlatform();
            if (PlatformManager.isPlatformUnixoid()) {
                message.append(tr(" Please install OpenJFX through your package manager."));
            } else {
                message.append(tr(" Please update to Java 11+."));
            }
            if (PluginHandler.confirmDisablePlugin(MainApplication.getMainFrame(), message.toString(), info.getName())) {
                PluginHandler.removePlugins(Collections.singletonList(info));
            }
            return;
        }
        AudioPlayer.setSoundPlayerClass(JavaFxMediaPlayer.class);
    }

    private boolean isJavaFx() {
        try {
            return Class.forName("javafx.scene.Node") != null && Class.forName("javafx.scene.media.Media") != null;
        } catch (ClassNotFoundException e) {
            Logging.trace(e);
            return false;
        }
    }
}
