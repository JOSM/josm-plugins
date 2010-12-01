package org.openstreetmap.josm.plugins.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;

public class ImageryLayerInfo {
    ArrayList<ImageryInfo> layers = new ArrayList<ImageryInfo>();
    ArrayList<ImageryInfo> defaultLayers = new ArrayList<ImageryInfo>();
    private final static String[] DEFAULT_LAYER_SITES
        = { "http://svn.openstreetmap.org/applications/editors/josm/plugins/imagery/sources.cfg"};

    public void load() {
        layers.clear();
        Collection<String> defaults = Main.pref.getCollection(
            "imagery.layers.default", Collections.<String>emptySet());
        for(Collection<String> c : Main.pref.getArray("imagery.layers",
        Collections.<Collection<String>>emptySet())) {
            layers.add(new ImageryInfo(c));
        }

        { /* REMOVE following old block in spring 2011 */
            defaults = new LinkedList<String>(defaults);
            Map<String,String> prefs = Main.pref.getAllPrefix("imagery.layers.default.");
            for(String s : prefs.keySet()) {
                Main.pref.put(s, null);
                defaults.add(s.substring(18));
            }
            prefs = Main.pref.getAllPrefix("imagery.layers.url.");
            for(String s : prefs.keySet()) {
                Main.pref.put(s, null);
            }
            TreeSet<String> keys = new TreeSet<String>(prefs.keySet());

            // And then the names+urls of imagery layers
            int prefid = 0;
            String name = null;
            String url = null;
            String cookies = null;
            double pixelPerDegree = 0.0;
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
                    name = url = cookies = null; pixelPerDegree = 0.0; lastid = prefid;
                }
                if (elements[3].equals("name")) {
                    name = prefs.get(key);
                    int codeIndex = name.indexOf("#PPD=");
                    if (codeIndex != -1) {
                        pixelPerDegree = Double.valueOf(name.substring(codeIndex+5));
                        name = name.substring(0, codeIndex);
                    }
                }
                else if (elements[3].equals("url"))
                {
                    url = prefs.get(key);
                }
                else if (elements[3].equals("cookies"))
                    cookies = prefs.get(key);
                if (name != null && url != null)
                    layers.add(new ImageryInfo(name, url, cookies, pixelPerDegree));
            }
        }
        ArrayList<String> defaultsSave = new ArrayList<String>();
        for(String source : Main.pref.getCollection("imagery.layers.sites", Arrays.asList(DEFAULT_LAYER_SITES)))
        {
            try
            {
                MirroredInputStream s = new MirroredInputStream(source, ImageryPlugin.instance.getPluginDir(), -1);
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
                    if(!line.startsWith("#") && (val.length == 3 || val.length == 4)) {
                        boolean force = "true".equals(val[0]);
                        String name = tr(val[1]);
                        String url = val[2];
                        String eulaAcceptanceRequired = null;
                        if (val.length == 4) {
                            // 4th parameter optional for license agreement (EULA)
                            eulaAcceptanceRequired = val[3];
                        }
                        defaultLayers.add(new ImageryInfo(name, url, eulaAcceptanceRequired));

                        if(force) {
                            defaultsSave.add(url);
                            if(!defaults.contains(url)) {
                                for(ImageryInfo i : layers) {
                                    if(url.equals(i.url))
                                        force = false;
                                }
                                if(force)
                                    layers.add(new ImageryInfo(name, url));
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
            }
        }

        Main.pref.putCollection("imagery.layers.default", defaultsSave.size() > 0
            ? defaultsSave : defaults);
        Collections.sort(layers);
        save();
    }

    public void add(ImageryInfo info) {
        layers.add(info);
    }

    public void remove(ImageryInfo info) {
        layers.remove(info);
    }

    public void save() {
        LinkedList<Collection<String>> coll = new LinkedList<Collection<String>>();
        for (ImageryInfo info : layers) {
            coll.add(info.getInfoArray());
        }
        Main.pref.putArray("imagery.layers", coll);
    }
}
