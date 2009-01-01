/**
 * Copyright by Christof Dallermassl
 * This program is free software and licensed under GPL.
 */
package at.dallermassl.josm.plugin.surveyor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

import livegps.LiveGpsPlugin;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.PluginProxy;

/**
 * Plugin that uses live gps data and a button panel to add nodes/waypoints etc at the current
 * position.
 *
 * TODO: auto save marker layer and data layer?
 * TODO: in action retrieve buttontype state to set on/off values
 * @author cdaller
 *
 */
public class SurveyorPlugin {

    private static JFrame surveyorFrame;
    public static final String PREF_KEY_STREET_NAME_FONT_SIZE = "surveyor.way.fontsize";

    /**
     *
     */
    public SurveyorPlugin() {
        super();

        // try to determine if the livegps plugin was already loaded:
//        PluginInformation pluginInfo = PluginInformation.getLoaded("livegps");
//        if (pluginInfo == null) {
//          JOptionPane.showMessageDialog(null, "Please install wmsplugin");
//          return;
//        }
//        if (!pluginInfo.version.equals("2")) {
//          JOptionPane.showMessageDialog(null, "livegps Version 2 required.");
//          return;
//        }
        try {
            Class.forName("livegps.LiveGpsPlugin");
        } catch(ClassNotFoundException cnfe) {
            String message =
                tr("SurveyorPlugin depends on LiveGpsPlugin!") + "\n" +
                tr("LiveGpsPlugin not found, please install and activate.") + "\n" +
                tr("SurveyorPlugin is disabled for the moment");
            JOptionPane.showMessageDialog(Main.parent, message, tr("SurveyorPlugin"), JOptionPane.ERROR_MESSAGE);
            return;
        }


        LiveGpsPlugin gpsPlugin = null;
        Iterator<PluginProxy> proxyIter = Main.plugins.iterator();
        while(proxyIter.hasNext()) {
            Object plugin = proxyIter.next().plugin;
            if(plugin instanceof LiveGpsPlugin) {
                gpsPlugin = (LiveGpsPlugin) plugin;
                break;
            }
        }
        if(gpsPlugin == null)
            throw new IllegalStateException(tr("SurveyorPlugin needs LiveGpsPlugin, but could not find it!"));

        JMenu m = gpsPlugin.getLgpsMenu();
        m.addSeparator();
        MainMenu.add(m, new SurveyorShowAction(gpsPlugin));

        AutoSaveAction autoSaveAction = new AutoSaveAction();
        JCheckBoxMenuItem autoSaveMenu = new JCheckBoxMenuItem(autoSaveAction);
        m.add(autoSaveMenu);
        autoSaveMenu.setAccelerator(autoSaveAction.getShortcut().getKeyStroke());
    }

    /**
     * @return the surveyorFrame
     */
    public static JFrame getSurveyorFrame() {
        return surveyorFrame;
    }

    /**
     * @param surveyorFrame the surveyorFrame to set
     */
    public static void setSurveyorFrame(JFrame surveyorFrame) {
        SurveyorPlugin.surveyorFrame = surveyorFrame;
    }

}
