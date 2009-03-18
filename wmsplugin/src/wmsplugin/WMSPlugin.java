package wmsplugin;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;
import java.util.TreeMap;
import java.io.*;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;

public class WMSPlugin extends Plugin {

    WMSLayer wmsLayer;
    static JMenu wmsJMenu;

    static ArrayList<WMSInfo> wmsList = new ArrayList<WMSInfo>();
    static TreeMap<String,String> wmsListDefault = new TreeMap<String,String>();

    // remember state of menu item to restore on changed preferences
    static private boolean menuEnabled = false;

    public WMSPlugin() {
        try
        {
            copy("/resources/ymap.html", "ymap.html");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        refreshMenu();
    }

    // this parses the preferences settings. preferences for the wms plugin have to
    // look like this:
    // wmsplugin.1.name=Landsat
    // wmsplugin.1.url=http://and.so.on/

    public void copy(String from, String to) throws FileNotFoundException, IOException
    {
        File pluginDir = new File(getPrefsPath());
        if (!pluginDir.exists())
            pluginDir.mkdirs();
        FileOutputStream out = new FileOutputStream(getPrefsPath() + to);
        InputStream in = WMSPlugin.class.getResourceAsStream(from);
        byte[] buffer = new byte[8192];
        for(int len = in.read(buffer); len > 0; len = in.read(buffer))
            out.write(buffer, 0, len);
        in.close();
        out.close();
    }


    public static void refreshMenu() {
        wmsList.clear();
        Map<String,String> prefs = Main.pref.getAllPrefix("wmsplugin.url.");

        TreeSet<String> keys = new TreeSet<String>(prefs.keySet());
        int prefid = 0;
        String name = null;
        String url = null;
        int lastid = -1;
        for (String key : keys) {
            String[] elements = key.split("\\.");
            if (elements.length != 4) continue;
            try {
                prefid = Integer.parseInt(elements[2]);
            } catch(NumberFormatException e) {
                continue;
            }
            if (prefid != lastid) {
                name = url = null; lastid = prefid;
            }
            if (elements[3].equals("name"))
                name = prefs.get(key);
            else if (elements[3].equals("url"))
                url = prefs.get(key);
            if (name != null && url != null)
                wmsList.add(new WMSInfo(name, url, prefid));
        }
        String source = "http://svn.openstreetmap.org/applications/editors/josm/plugins/wmsplugin/sources.cfg";
        try
        {
            MirroredInputStream s = new MirroredInputStream(source,
            Main.pref.getPreferencesDir() + "plugins/wmsplugin/", -1);
            InputStreamReader r;
            try
            {
                r = new InputStreamReader(s, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                r = new InputStreamReader(s);
            }
            BufferedReader reader = new BufferedReader(r);
            String line;
            while((line = reader.readLine()) != null)
            {
                String val[] = line.split(";");
                if(!line.startsWith("#") && val.length == 3)
                    setDefault("true".equals(val[0]), tr(val[1]), val[2]);
            }
        }
        catch (IOException e)
        {
        }

        Collections.sort(wmsList);
        MainMenu menu = Main.main.menu;

        if (wmsJMenu == null)
            wmsJMenu = menu.addMenu(marktr("WMS"), KeyEvent.VK_W, menu.defaultMenuPos);
        else
            wmsJMenu.removeAll();

        // for each configured WMSInfo, add a menu entry.
        for (final WMSInfo u : wmsList) {
            wmsJMenu.add(new JMenuItem(new WMSDownloadAction(u)));
        }
        wmsJMenu.addSeparator();
        wmsJMenu.add(new JMenuItem(new Map_Rectifier_WMSmenuAction()));

        wmsJMenu.addSeparator();
        wmsJMenu.add(new JMenuItem(new
                JosmAction(tr("Blank Layer"), "blankmenu", tr("Open a blank WMS layer to load data from a file"), null, false) {
            public void actionPerformed(ActionEvent ev) {
                Main.main.addLayer(new WMSLayer());
            }
        }));
        wmsJMenu.addSeparator();
        wmsJMenu.add(new JMenuItem(new Help_WMSmenuAction()));
        setEnabledAll(menuEnabled);
    }

    /* add a default entry in case the URL does not yet exist */
    private static void setDefault(Boolean force, String name, String url)
    {
        String testurl = url.replaceAll("=", "_");
        wmsListDefault.put(name, url);

        if(force && !Main.pref.getBoolean("wmsplugin.default."+testurl))
        {
            Main.pref.put("wmsplugin.default."+testurl, true);
            int id = -1;
            for(WMSInfo i : wmsList)
            {
                if(url.equals(i.url))
                    return;
                if(i.prefid > id)
                    id = i.prefid;
            }
            WMSInfo newinfo = new WMSInfo(name, url, id+1);
            newinfo.save();
            wmsList.add(newinfo);
        }
    }

    public static Grabber getGrabber(String _baseURL, Bounds _b, Projection _proj,
                     double _pixelPerDegree, GeorefImage _image, MapView _mv, WMSLayer _layer){
        if(_baseURL.startsWith("yahoo://"))
            return new YAHOOGrabber(_baseURL, _b, _proj, _pixelPerDegree, _image, _mv, _layer);
        else
            return new WMSGrabber(_baseURL, _b, _proj, _pixelPerDegree, _image, _mv, _layer);
        // OSBGrabber should be rewrite for thread support first
        //if (wmsurl.matches("(?i).*layers=npeoocmap.*") || wmsurl.matches("(?i).*layers=npe.*") ){
        //  return new OSGBGrabber(_b, _proj, _pixelPerDegree,  _images, _mv, _layer);
        //} else {
        //  return new WMSGrabber(_b, _proj, _pixelPerDegree,  _images, _mv, _layer);
        //}
    }

    private static void setEnabledAll(boolean isEnabled) {
        for(int i=0; i < wmsJMenu.getItemCount(); i++) {
            JMenuItem item = wmsJMenu.getItem(i);

            if(item != null) item.setEnabled(isEnabled);
        }
               menuEnabled = isEnabled;
    }

    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame==null && newFrame!=null) {
            setEnabledAll(true);
            Main.map.addMapMode(new IconToggleButton
                        (new WMSAdjustAction(Main.map)));
        } else if (oldFrame!=null && newFrame==null ) {
            setEnabledAll(false);
        }
    }

    public PreferenceSetting getPreferenceSetting() {
        return new WMSPreferenceEditor();
    }

    static public String getPrefsPath()
    {
        return Main.pref.getPluginsDirFile().getPath() + "/wmsplugin/";
    }
}
