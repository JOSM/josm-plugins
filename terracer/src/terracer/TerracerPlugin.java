/**
 * Terracer: A JOSM Plugin for terraced houses.
 * 
 * Copyright 2009 CloudMade Ltd.
 * 
 * Released under the GPLv2, see LICENSE file for details.
 */
package terracer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;

/**
 * Plugin interface implementation for Terracer.
 * 
 * @author zere
 */
public class TerracerPlugin extends Plugin {
	public TerracerPlugin() {
		MainMenu.add(Main.main.menu.toolsMenu, new TerracerAction());
		MainMenu.add(Main.main.menu.toolsMenu, new ReverseTerraceAction());
	}
}
