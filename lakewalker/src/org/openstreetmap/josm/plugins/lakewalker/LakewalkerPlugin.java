package org.openstreetmap.josm.plugins.lakewalker;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;

/**
 * Interface to Darryl Shpak's Lakewalker python module
 * 
 * @author Brent Easton
 */
public class LakewalkerPlugin extends Plugin {
  public LakewalkerPlugin() {
    MainMenu.add(Main.main.menu.toolsMenu, new LakewalkerAction(tr("Lake Walker")));
  }

  public PreferenceSetting getPreferenceSetting() 
  {
    return new LakewalkerPreferences();
  }

}
