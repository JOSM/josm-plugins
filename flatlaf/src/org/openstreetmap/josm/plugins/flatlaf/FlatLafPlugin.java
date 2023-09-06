// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.flatlaf;

import javax.swing.UIManager;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Utils;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

/**
 * FlatLaf for JOSM
 */
public class FlatLafPlugin extends Plugin {

    /**
     * Constructs a new {@code FlatLafPlugin}.
     *
     * @param info plugin info
     */
    public FlatLafPlugin(PluginInformation info) {
        super(info);
        UIManager.getDefaults().put("ClassLoader", getClass().getClassLoader());
        // Load the built-in themes
        FlatDarculaLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatLightLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();

        // enable loading of FlatLaf.properties, FlatLightLaf.properties and FlatDarkLaf.properties from package
        FlatLaf.registerCustomDefaultsSource("org.openstreetmap.josm.plugins.flatlaf", getClass().getClassLoader());
        if (PlatformManager.isPlatformOsx() && Utils.getSystemProperty("apple.awt.application.appearance") == null) {
            // See https://www.formdev.com/flatlaf/macos/
            // This makes the title bar match the system settings
            Utils.updateSystemProperty("apple.awt.application.appearance", "system");
        }
    }

}
