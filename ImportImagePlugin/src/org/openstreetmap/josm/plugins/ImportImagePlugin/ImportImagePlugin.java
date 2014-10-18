package org.openstreetmap.josm.plugins.ImportImagePlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javax.swing.JMenu;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Plugin class.
 * Provides basic routines for plugin installation and provides the plugin properties.
 *
 *
 * @author Christoph Beekmans, Fabian Kowitz, Anna Robaszkiewicz, Oliver Kuhn, Martin Ulitzny
 *
 */
public class ImportImagePlugin extends Plugin{

    private static Logger logger;

    JMenu mainmenu = null;
    JosmAction loadFileAction = null;

    // custom Classloader
    static ClassLoader pluginClassLoader;

    // plugin proerties
    static Properties pluginProps;

    // path constants
    static final String PLUGIN_DIR = Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/";
    static final String PLUGINPROPERTIES_PATH = Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/pluginProperties.properties";
    static final String PLUGINLIBRARIES_DIR = Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/lib/";
    static final String PLUGINPROPERTIES_FILENAME = "pluginProperties.properties";
    static final String LOGGING_PROPERTIES_FILEPATH = Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/log4j.properties/";


    public Properties getPluginProps() {
        return pluginProps;
    }


    /**
     * constructor
     *
     * @param info
     */
    public ImportImagePlugin(PluginInformation info){
        super(info);

        // switch to x=lon and y=lat for EPSG:4326 as JOSM does
        // (formally incorrect, but reasonable)
        System.setProperty("org.geotools.referencing.forceXY", "true");

        try {

            // First create custom ClassLoader to load resources from the main JAR
            pluginClassLoader = createPluginClassLoader();

            // Initialize logger
            initializeLogger(pluginClassLoader);

            // Check whether plugin has already been installed. Otherwise install
            checkInstallation();

            // If resources are available load properties from plugin directory
            if(pluginProps == null || pluginProps.isEmpty())
            {
                pluginProps = new Properties();
                pluginProps.load(new File(PLUGINPROPERTIES_PATH).toURI().toURL().openStream());
                logger.debug("Plugin properties loaded");
            }

            // load information about supported reference systems
            PluginOperations.loadCRSData(pluginProps);

            // create new Action for menu entry
            loadFileAction = new LoadImageAction();
            loadFileAction.setEnabled(true);
            MainMenu.add(Main.main.menu.imagerySubMenu, loadFileAction);

            // add menu entries
            //Main.main.menu.fileMenu.insert(loadFileAction, 8);

            //Main.main.menu.fileMenu.insertSeparator(9);

            ExtensionFileFilter.importers.add(new ImportImageFileImporter());

        } catch (Exception e) {
            logger.fatal("Error while loading plugin", e);
            try {
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        logger.info("Plugin successfully loaded.");
    }

    /**
     * Checks whether plugin resources are available.
     * If not, start install procedure.
     *
     * @throws IOException
     */
    private void checkInstallation() throws IOException
    {
        // check plugin resource state
        boolean isInstalled = true;
        if(!new File(PLUGINPROPERTIES_PATH).exists()
                || !new File(PLUGIN_DIR).exists()
                || !new File(PLUGINLIBRARIES_DIR).exists())
            isInstalled = false;


        // if properties file doesn't exist, install plugin
        if (!isInstalled) {

            /*----------- Begin installation ---------------*/

            // check if plugin directory exist
            File pluginDir = new File(PLUGIN_DIR);
            if(!pluginDir.exists()){
                pluginDir.mkdir();
            }

            // check if "lib" directory exist
            File libDir = new File(PLUGINLIBRARIES_DIR);
            if(!libDir.exists()){
                libDir.mkdir();
            }

            // create local properties file
            if (pluginProps == null || pluginProps.isEmpty()) {
                try (FileWriter fw = new FileWriter(new File(PLUGINPROPERTIES_PATH))) {
	                URL propertiesURL = pluginClassLoader.getResource("resources/" + PLUGINPROPERTIES_FILENAME);
	                pluginProps = new Properties();
	                pluginProps.load(propertiesURL.openStream());
	                pluginProps.store(fw, null);
                }
                logger.debug("Plugin properties loaded");
            }

            if (!new File(LOGGING_PROPERTIES_FILEPATH).exists()) {
                try (FileWriter fw = new FileWriter(new File(LOGGING_PROPERTIES_FILEPATH))) {
	                URL propertiesURL = pluginClassLoader.getResource("resources/log4j.properties");
	                Properties loggingProps = new Properties();
	                loggingProps.load(propertiesURL.openStream());
	                loggingProps.store(fw, null);
                }
                logger.debug("Logging properties created");
            }

            logger.debug("Plugin successfully installed");
        }
    }

    /**
     * Initialize logger using plugin classloader.
     *
     * @param cl
     */
    private void initializeLogger(ClassLoader cl) {

        Properties props = new Properties();
        try {
            props.load(new File(LOGGING_PROPERTIES_FILEPATH).toURI().toURL().openStream());

            // Set file for logging here:
            props.setProperty("log4j.appender.MyRoFiAppender.file",
                    (Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/" + "log.log"));

            PropertyConfigurator.configure(props);

            logger = Logger.getLogger(ImportImagePlugin.class);

            logger.info("Logger successfully initialized.");

            return;

        } catch (IOException e) {
            System.out.println("Logging properties file not found. Using standard settings.");
        }

        // if no log4j.properties file can be found, initialize manually:

        props.setProperty("log4j.rootLogger", "INFO, A");
        props.setProperty("log4j.appender.A", "org.apache.log4j.FileAppender");

        props.setProperty("log4j.appender.A.layout",
                "org.apache.log4j.PatternLayout ");
        props.setProperty("log4j.appender.A.layout.ConversionPattern",
                "%d{ISO8601} %-5p [%t] %c: %m%n");

        // Set file for logging here:
        props.setProperty("log4j.appender.A.file",
                (Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin/" + "log.log"));

        PropertyConfigurator.configure(props);
        logger = Logger.getLogger(ImportImagePlugin.class);
        logger.info("Logger successfully initialized with standard settings.");

    }

    /**
     * get a plugin-specific classloader.
     *
     * @return
     * @throws MalformedURLException
     */
    private ClassLoader createPluginClassLoader() throws MalformedURLException
    {
        ClassLoader loader = null;
        loader = URLClassLoader.newInstance(
                new URL[] { new File(Main.pref.getPluginsDirectory().getAbsolutePath() + "/ImportImagePlugin.jar").toURI().toURL()},
                ImportImagePlugin.class.getClassLoader()
                );

        return loader;
    }

}
