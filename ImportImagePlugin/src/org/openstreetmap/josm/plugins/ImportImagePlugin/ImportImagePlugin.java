// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ImportImagePlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Plugin class.
 * Provides basic routines for plugin installation and provides the plugin properties.
 *
 * @author Christoph Beekmans, Fabian Kowitz, Anna Robaszkiewicz, Oliver Kuhn, Martin Ulitzny
 *
 */
public class ImportImagePlugin extends Plugin {

    JosmAction loadFileAction;

    // plugin properties
    static Properties pluginProps;

    // path constants
    static final String PLUGIN_DIR = Preferences.main().getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/";
    static final String PLUGINPROPERTIES_FILENAME = "pluginProperties.properties";
    static final String PLUGINPROPERTIES_PATH = PLUGIN_DIR + PLUGINPROPERTIES_FILENAME;
    static final String PLUGINLIBRARIES_DIR = PLUGIN_DIR + "lib/";

    /**
     * Returns Import image plugin properties.
     * @return Import image plugin properties
     */
    public Properties getPluginProps() {
        return pluginProps;
    }

    /**
     * constructor
     * @param info plugin information
     *
     * @throws IOException if any I/O error occurs
     */
    public ImportImagePlugin(PluginInformation info) throws IOException {
        super(info);

        try {
            // Check whether plugin has already been installed. Otherwise install
            checkInstallation();

            // If resources are available load properties from plugin directory
            loadPluginProps();

            // load information about supported reference systems
            PluginOperations.loadCRSData(pluginProps);

            // create new Action for menu entry
            loadFileAction = new LoadImageAction();
            loadFileAction.setEnabled(true);
            MainMenu.add(MainApplication.getMenu().imagerySubMenu, loadFileAction);

            ExtensionFileFilter.addImporter(new ImportImageFileImporter());

        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static void loadPluginProps() throws IOException {
        if (pluginProps == null || pluginProps.isEmpty()) {
            pluginProps = new Properties();
            try (InputStream stream = Files.newInputStream(Paths.get(PLUGINPROPERTIES_PATH))) {
                pluginProps.load(stream);
            }
            Logging.debug("ImportImagePlugin: Plugin properties loaded");
        }
    }

    /**
     * Checks whether plugin resources are available.
     * If not, start install procedure.
     */
    private static void checkInstallation() throws IOException {
        // check plugin resource state
        boolean isInstalled = new File(PLUGINPROPERTIES_PATH).exists()
                && new File(PLUGIN_DIR).exists()
                && new File(PLUGINLIBRARIES_DIR).exists();


        // if properties file doesn't exist, install plugin
        if (!isInstalled) {

            /*----------- Begin installation ---------------*/

            // check if plugin directory exist
            File pluginDir = new File(PLUGIN_DIR);
            if (!pluginDir.exists()) {
                Utils.mkDirs(pluginDir);
            }

            // check if "lib" directory exist
            File libDir = new File(PLUGINLIBRARIES_DIR);
            if (!libDir.exists()) {
                Utils.mkDirs(libDir);
            }

            // create local properties file
            if (pluginProps == null || pluginProps.isEmpty()) {
                try (BufferedWriter fw = Files.newBufferedWriter(Paths.get(PLUGINPROPERTIES_PATH))) {
                    URL propertiesURL = ImportImagePlugin.class.getResource("resources/" + PLUGINPROPERTIES_FILENAME);
                    if (propertiesURL != null) {
                        pluginProps = new Properties();
                        try (InputStream stream = propertiesURL.openStream()) {
                            pluginProps.load(stream);
                        }
                        pluginProps.store(fw, null);
                    }

                }
                Logging.debug("ImportImagePlugin: Plugin properties loaded");
            }

            Logging.debug("ImportImagePlugin: Plugin successfully installed");
        }
    }
}
