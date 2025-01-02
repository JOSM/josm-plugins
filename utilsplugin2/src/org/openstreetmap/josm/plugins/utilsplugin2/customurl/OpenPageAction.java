// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.customurl;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * Open custom URL
 *
 * @author Alexei Kasatkin
 */
public final class OpenPageAction extends JosmAction {

    public OpenPageAction() {
        super(tr("Open custom URL"), "openurl",
                tr("Opens specified URL browser"),
                Shortcut.registerShortcut("tools:openurl", tr("Data: {0}", tr("Open custom URL")),
                        KeyEvent.VK_H, Shortcut.SHIFT), true);
        putValue("help", ht("/Action/OpenPage"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = getLayerManager().getActiveDataSet().getSelected();
        OsmPrimitive p = null;
        if (!sel.isEmpty()) {
            p = sel.iterator().next();
        }

        if (Config.getPref().getBoolean("utilsplugin2.askurl", false) && 1 != ChooseURLAction.showConfigDialog(true)) {
            return;
        }

        MapView mv = MainApplication.getMap().mapView;
        LatLon center = mv.getLatLon(mv.getWidth()/2, mv.getHeight()/2);

        String addr = URLList.getSelectedURL();
        Pattern pat = Pattern.compile("\\{([^\\}]*)\\}");
        Matcher m = pat.matcher(addr);
        String val, key;
        String[] keys = new String[100], vals = new String[100];
        int i = 0;
        try {
            while (m.find()) {
                key = m.group(1); val = null;
                if ("#id".equals(key)) {
                    if (p != null) {
                        val = Long.toString(p.getId());
                    } else {
                        // id without anything selected does not make any sense, do nothing
                        return;
                    }
                } else if ("#type".equals(key)) {
                    if (p != null) val = OsmPrimitiveType.from(p).getAPIName();
                } else if ("#lat".equals(key)) {
                    val = Double.toString(center.lat());
                } else if ("#lon".equals(key)) {
                    val = Double.toString(center.lon());
                } else if ("#zoom".equals(key)) {
                    val = Integer.toString(OsmUrlToBounds.getZoom(MainApplication.getMap().mapView.getRealBounds()));
                } else {
                    if (p != null) {
                        val = p.get(key);
                        if (val != null) val = URLEncoder.encode(p.get(key), "UTF-8"); else return;
                    }
                }
                keys[i] = m.group();
                if (val != null) vals[i] = val;
                else vals[i] = "";
                i++;
            }
        } catch (UnsupportedEncodingException ex) {
            Logging.log(Logging.LEVEL_ERROR, "Encoding error", ex);
            return;
        }
        for (int j = 0; j < i; j++) {
            addr = addr.replace(keys[j], vals[j]);
        }

        // Opened on the local system instead of the browser: local:http://127.0.0.1:<port>/script
        Pattern pat_direct = Pattern.compile("local:(http.*)$");
        Matcher m_direct = pat_direct.matcher(addr);

        try {
            if (m_direct.find()) {
                addr = m_direct.group(1);   // replace addr so possible error uses it
                Logging.info("Opening on the local system: " + addr);
                URL url = new URL(addr);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setRequestMethod("GET");
                if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException(". GET response:" + urlConn.getResponseCode());
                }

            // See #12836 - do not load invalid history
            } else if (!addr.endsWith("/0/history")) {
                OpenBrowser.displayUrl(addr);
            }
        } catch (Exception ex) {
            Logging.log(Logging.LEVEL_ERROR, "Can not open URL " + addr, ex);
        }
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveDataSet() != null);
    }
}
