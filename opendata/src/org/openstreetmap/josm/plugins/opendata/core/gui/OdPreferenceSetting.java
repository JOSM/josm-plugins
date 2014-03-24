// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.modules.ModuleDownloadTask;
import org.openstreetmap.josm.plugins.opendata.core.modules.ModuleInformation;
import org.openstreetmap.josm.tools.GBC;

public class OdPreferenceSetting extends DefaultTabPreferenceSetting implements OdConstants {

    //private final JRadioButton rbCC43 = new JRadioButton(tr("CC43"));
    //private final JRadioButton rbWGS84 = new JRadioButton(tr("WGS84"));
    
    private final JTextField oapi = new JTextField();
    private final JTextField xapi = new JTextField();
    
    private final JCheckBox rawData = new JCheckBox(tr("Raw data"));
    
    public final JTabbedPane tabPane = new JTabbedPane();
    public JPanel masterPanel;
    
    private final ModulePreference modulePref = new ModulePreference();

    public OdPreferenceSetting() {
    	super(ICON_CORE_48, tr("OpenData Preferences"),
                tr("A special handler for various Open Data portals<br/><br/>"+
                        "Please read the Terms and Conditions of Use of each portal<br/>"+
                        "before any upload of data loaded by this plugin."));
    }
    
    /**
     * Replies the collection of module site URLs from where module lists can be downloaded
     *
     * @return
     */
    public static final Collection<String> getModuleSites() {
        return Main.pref.getCollection(PREF_MODULES_SITES, Arrays.asList(DEFAULT_MODULE_SITES));
    }

    /**
     * Sets the collection of module site URLs.
     *
     * @param sites the site URLs
     */
	public static void setModuleSites(List<String> sites) {
		Main.pref.putCollection(PREF_MODULES_SITES, sites);
    }
    
    @Override
    public void addGui(PreferenceTabbedPane gui) {
        masterPanel = gui.createPreferenceTab(this);
        modulePref.addGui(gui);
        tabPane.add(createGeneralSettings());
        
        JScrollPane scrollpane = new JScrollPane(tabPane);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        masterPanel.add(scrollpane, GBC.eol().fill(GBC.BOTH));
    }

	protected JPanel createGeneralSettings() {
        JPanel general = new JPanel(new GridBagLayout());
        general.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        general.setName(tr("General settings"));

        // option to enable raw data
        rawData.setSelected(Main.pref.getBoolean(PREF_RAWDATA, DEFAULT_RAWDATA));
        rawData.setToolTipText(tr("Import only raw data (i.e. do not add/delete tags or replace them by standard OSM tags)"));
        general.add(rawData, GBC.eop().insets(0, 0, 0, 0));
        
        // separator
        general.add(new JSeparator(SwingConstants.HORIZONTAL), GBC.eol().fill(GBC.HORIZONTAL));
        
        // option to select the coordinates to use
/*        JLabel jLabelRes = new JLabel(tr("Coordinates system to read in CSV files:"));
        p.add(jLabelRes, GBC.std().insets(0, 5, 10, 0));
        ButtonGroup bgCoordinates = new ButtonGroup();
        rbCC43.setToolTipText(tr("CC43"));
        rbWGS84.setToolTipText(tr("WGS84"));
        bgCoordinates.add(rbCC43);
        bgCoordinates.add(rbWGS84);
        String currentCoordinates = Main.pref.get(PREF_COORDINATES, VALUE_CC9ZONES);
        if (currentCoordinates.equals(VALUE_WGS84))
        	rbWGS84.setSelected(true);
        else
        	rbCC43.setSelected(true);
        p.add(rbCC43, GBC.std().insets(5, 0, 5, 0));
        p.add(rbWGS84, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 0, 5));*/

        // option to set the Overpass API server
        JLabel jLabelOapi = new JLabel(tr("Overpass API server:"));
        oapi.setText(Main.pref.get(PREF_OAPI, DEFAULT_OAPI));
        oapi.setToolTipText(tr("Overpass API server used to download OSM data"));
        general.add(jLabelOapi, GBC.std().insets(0, 5, 10, 0));
        general.add(oapi, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 200, 5));
        
        // option to set the XAPI server
        JLabel jLabelXapi = new JLabel(tr("XAPI server:"));
        xapi.setText(Main.pref.get(PREF_XAPI, DEFAULT_XAPI));
        xapi.setToolTipText(tr("XAPI server used to download OSM data when Overpass API is not available"));
        general.add(jLabelXapi, GBC.std().insets(0, 5, 10, 0));
        general.add(xapi, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 200, 5));
        
        // end of dialog, scroll bar
        general.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
        
        return general;
    }
    
    @Override
    public boolean ok() {
    	boolean result = modulePref.ok();
   		//Main.pref.put(PREF_COORDINATES, rbWGS84.isSelected() ? VALUE_WGS84 : VALUE_CC9ZONES);
   		Main.pref.put(PREF_OAPI, oapi.getText());
   		Main.pref.put(PREF_XAPI, xapi.getText());
   		Main.pref.put(PREF_RAWDATA, rawData.isSelected());
        
        // create a task for downloading modules if the user has activated, yet not downloaded,
        // new modules
        //
        final List<ModuleInformation> toDownload = modulePref.getModulesScheduledForUpdateOrDownload();
        final ModuleDownloadTask task;
        if (toDownload != null && ! toDownload.isEmpty()) {
            task = new ModuleDownloadTask(masterPanel, toDownload, tr("Download modules"));
        } else {
        	task = null;
        }
        
        // this is the task which will run *after* the modules are downloaded
        //
        final Runnable continuation = new Runnable() {
            public void run() {
                boolean requiresRestart = false;
                if (task != null && !task.isCanceled()) {
                    if (!task.getDownloadedModules().isEmpty()) {
                        requiresRestart = true;
                    }
                }

                // build the messages. We only display one message, including the status
                // information from the module download task and - if necessary - a hint
                // to restart JOSM
                //
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                if (task != null && !task.isCanceled()) {
                    sb.append(ModulePreference.buildDownloadSummary(task));
                }
                if (requiresRestart) {
                    sb.append(tr("You have to restart JOSM for some settings to take effect."));
                }
                sb.append("</html>");

                // display the message, if necessary
                //
                if ((task != null && !task.isCanceled()) || requiresRestart) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            sb.toString(),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                            );
                }
                Main.parent.repaint();
            }
        };

        if (task != null) {
            // if we have to launch a module download task we do it asynchronously, followed
            // by the remaining "save preferences" activites run on the Swing EDT.
            //
            Main.worker.submit(task);
            Main.worker.submit(
                    new Runnable() {
                        public void run() {
                            SwingUtilities.invokeLater(continuation);
                        }
                    }
                    );
        } else {
            // no need for asynchronous activities. Simply run the remaining "save preference"
            // activities on this thread (we are already on the Swing EDT
            //
            continuation.run();
        }

        return task == null ? result : false;
    }
}
