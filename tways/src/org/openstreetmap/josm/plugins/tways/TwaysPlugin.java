package org.openstreetmap.josm.plugins.tways;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;

/**
 * A plugin to add ways manipulation things
 *
 * @author Thomas.Walraet
 */
public class TwaysPlugin extends Plugin {

    public TwaysPlugin() {
        JMenu waysMenu = new JMenu(tr("Ways"));
        waysMenu.add(new JMenuItem(new CreateLinearWaysAction()));
        Main.main.menu.add(waysMenu, 2);
    }

}
