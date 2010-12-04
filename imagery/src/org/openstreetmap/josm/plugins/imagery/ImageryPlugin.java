package org.openstreetmap.josm.plugins.imagery;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.plugins.imagery.wms.Map_Rectifier_WMSmenuAction;
import org.openstreetmap.josm.plugins.imagery.wms.WMSAdapter;
import org.openstreetmap.josm.plugins.imagery.wms.WMSLayer;
import org.openstreetmap.josm.plugins.imagery.wms.io.WMSLayerExporter;
import org.openstreetmap.josm.plugins.imagery.wms.io.WMSLayerImporter;

public class ImageryPlugin extends Plugin {

    JMenu imageryJMenu;

    public static ImageryPlugin instance;
    public static WMSAdapter wmsAdapter = new WMSAdapter();

    public ImageryLayerInfo info = new ImageryLayerInfo();

    // remember state of menu item to restore on changed preferences
    private boolean menuEnabled = false;

    /***************************************************************
     * Remote control initialization:
     * If you need remote control in some other plug-in
     * copy this stuff and the call to initRemoteControl below
     * and replace the RequestHandler subclass in initRemoteControl
     ***************************************************************/

    /** name of remote control plugin */
    private final String REMOTECONTROL_NAME = "remotecontrol";

    /* if necessary change these version numbers to ensure compatibility */

    /** RemoteControlPlugin older than this SVN revision is not compatible */
    final int REMOTECONTROL_MIN_REVISION = 22734;
    /** WMSPlugin needs this specific API major version of RemoteControlPlugin */
    final int REMOTECONTROL_NEED_API_MAJOR = 1;
    /** All API minor versions starting from this should be compatible */
    final int REMOTECONTROL_MIN_API_MINOR = 0;

    /* these fields will contain state and version of remote control plug-in */
    boolean remoteControlAvailable = false;
    boolean remoteControlCompatible = true;
    boolean remoteControlInitialized = false;
    int remoteControlRevision = 0;
    int remoteControlApiMajor = 0;
    int remoteControlApiMinor = 0;
    int remoteControlProtocolMajor = 0;
    int remoteControlProtocolMinor = 0;

    /**
     * Check if remote control plug-in is available and if its version is
     * high enough and register remote control command for this plug-in.
     */
    private void initRemoteControl() {
        for(PluginProxy pp: PluginHandler.pluginList)
        {
            PluginInformation info = pp.getPluginInformation();
            if(REMOTECONTROL_NAME.equals(info.name))
            {
                remoteControlAvailable = true;
                remoteControlRevision = Integer.parseInt(info.version);
                if(REMOTECONTROL_MIN_REVISION > remoteControlRevision)
                {
                    remoteControlCompatible = false;
                }
            }
        }

        if(remoteControlAvailable && remoteControlCompatible)
        {
            Plugin plugin =
                (Plugin) PluginHandler.getPlugin(REMOTECONTROL_NAME);
            try {
                Method method;
                method = plugin.getClass().getMethod("getVersion");
                Object obj = method.invoke(plugin);
                if((obj != null ) && (obj instanceof int[]))
                {
                    int[] versions = (int[]) obj;
                    if(versions.length >= 4)
                    {
                        remoteControlApiMajor = versions[0];
                        remoteControlApiMinor = versions[1];
                        remoteControlProtocolMajor = versions[2];
                        remoteControlProtocolMinor = versions[3];
                    }
                }

                if((remoteControlApiMajor != REMOTECONTROL_NEED_API_MAJOR) ||
                        (remoteControlApiMinor < REMOTECONTROL_MIN_API_MINOR))
                {
                    remoteControlCompatible = false;
                }
                if(remoteControlCompatible)
                {
                    System.out.println(this.getClass().getSimpleName() + ": initializing remote control");
                    method = plugin.getClass().getMethod("addRequestHandler", String.class, Class.class);
                    // replace command and class when you copy this to some other plug-in
                    // for compatibility with old remotecontrol add leading "/"
                    method.invoke(plugin, "/" + ImageryRemoteHandler.command, ImageryRemoteHandler.class);
                    remoteControlInitialized = true;
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if(remoteControlAvailable)
        {
            String msg = null;

            if(remoteControlCompatible)
            {
                if(!remoteControlInitialized)
                {
                    msg  = tr("Could not initialize remote control.");
                }
            }
            else
            {
                msg  = tr("Remote control plugin is not compatible with {0}.",
                        this.getClass().getSimpleName());
            }

            if(msg != null)
            {
                String additionalMessage = tr("{0} will work but remote control for this plugin is disabled.\n"
                        + "You should update the plugins.",
                        this.getClass().getSimpleName());
                String versionMessage = tr("Current version of \"{1}\": {2}, internal version {3}. "
                        + "Need version {4}, internal version {5}.\n"
                        + "If updating the plugins does not help report a bug for \"{0}\".",
                        this.getClass().getSimpleName(),
                        REMOTECONTROL_NAME,
                        ""+remoteControlRevision,
                        (remoteControlApiMajor != 0) ?
                                ""+remoteControlApiMajor+"."+remoteControlApiMinor :
                                    tr("unknown"),
                                    ""+REMOTECONTROL_MIN_REVISION,
                                    ""+REMOTECONTROL_NEED_API_MAJOR+"."+REMOTECONTROL_MIN_API_MINOR );

                String title = tr("{0}: Problem with remote control",
                        this.getClass().getSimpleName());

                System.out.println(this.getClass().getSimpleName() + ": " +
                        msg + "\n" + versionMessage);

                JOptionPane.showMessageDialog(
                        Main.parent,
                        msg + "\n" + additionalMessage,
                        title,
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }

        if(!remoteControlAvailable) {
            System.out.println(this.getClass().getSimpleName() + ": remote control not available");
        }
    }

    /***************************************
     * end of remote control initialization
     ***************************************/

    protected void initExporterAndImporter() {
        ExtensionFileFilter.exporters.add(new WMSLayerExporter());
        ExtensionFileFilter.importers.add(new WMSLayerImporter());
    }

    public ImageryPlugin(PluginInformation info) {
        super(info);
        instance = this;
        this.info.load();
        refreshMenu();
        initRemoteControl();
    }

    public void addLayer(ImageryInfo info) {
        this.info.add(info);
        this.info.save();
        refreshMenu();
    }

    public void refreshMenu() {
        MainMenu menu = Main.main.menu;

        if (imageryJMenu == null)
            imageryJMenu = menu.addMenu(marktr("Imagery"), KeyEvent.VK_W, menu.defaultMenuPos, ht("/Plugin/Imagery"));
        else
            imageryJMenu.removeAll();

        // for each configured WMSInfo, add a menu entry.
        for (final ImageryInfo u : info.layers) {
            imageryJMenu.add(new JMenuItem(new AddImageryLayerAction(u)));
        }
        imageryJMenu.addSeparator();
        imageryJMenu.add(new JMenuItem(new Map_Rectifier_WMSmenuAction()));

        imageryJMenu.addSeparator();
        imageryJMenu.add(new JMenuItem(new
                JosmAction(tr("Blank Layer"), "blankmenu", tr("Open a blank WMS layer to load data from a file"), null, false) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                Main.main.addLayer(new WMSLayer());
            }
        }));
        setEnabledAll(menuEnabled);
    }

    private void setEnabledAll(boolean isEnabled) {
        for(int i=0; i < imageryJMenu.getItemCount(); i++) {
            JMenuItem item = imageryJMenu.getItem(i);

            if(item != null) item.setEnabled(isEnabled);
        }
        menuEnabled = isEnabled;
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame==null && newFrame!=null) {
            setEnabledAll(true);
            Main.map.addMapMode(new IconToggleButton
                    (new ImageryAdjustAction(Main.map)));
        } else if (oldFrame!=null && newFrame==null ) {
            setEnabledAll(false);
        }
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new ImageryPreferenceEditor();
    }

    @Override
    public String getPluginDir()
    {
        return new File(Main.pref.getPluginsDirectory(), "imagery").getPath();
    }
}
