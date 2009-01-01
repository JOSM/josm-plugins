/**
 * Copyright by Christof Dallermassl
 * This program is free software and licensed under GPL.
 */
package at.dallermassl.josm.plugin.colorscheme;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;

/**
 * ColorScheme Plugin for JOSM.
 * @author cdaller
 *
 */
public class ColorSchemePlugin extends Plugin {

    /**
     * Default Constructor
     */
    public ColorSchemePlugin() {

    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new ColorSchemePreference();
    }



}
