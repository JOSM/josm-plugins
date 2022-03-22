// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.directdownload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

public class DirectDownload extends Plugin {
    /**
     * Will be invoked by JOSM to bootstrap the plugin
     *
     * @param info  information about the plugin and its local installation
     */
    public DirectDownload(PluginInformation info) {
        super(info);

        MainMenu.add(MainApplication.getMenu().gpsMenu, new DownloadAction());
    }

    static class DownloadAction extends JosmAction {
        DownloadAction() {
            super(tr("Download Track ..."), "DownloadAction", tr("Download GPX track from openstreetmap.org"),
                    Shortcut.registerShortcut("directdownload:downloadgpxaction", tr("DirectDownload: Download GPX Track"),
                            KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            DownloadDataGui go = new DownloadDataGui();
            go.setVisible(true);

            List<UserTrack> tracks = go.getSelectedUserTracks();

            if (!((go.getValue() == 1) && (tracks != null))) {
                return;
            }

            for (UserTrack track: tracks) {

                GpxData data = new GpxServerReader().loadGpx(Long.parseLong(track.id));
                if (data == null) {
                    return;
                }
                GpxData dataNew = new GpxData();

                for (IGpxTrack trk : data.getTracks()) {
                    HashMap<String, Object> attrib = new HashMap<>(trk.getAttributes());
                    if (!trk.getAttributes().containsKey(GpxConstants.GPX_NAME)) {
                        attrib.put(GpxConstants.GPX_NAME, track.filename);
                    }
                    if (!trk.getAttributes().containsKey(GpxConstants.GPX_DESC)) {
                        attrib.put(GpxConstants.GPX_DESC, track.description);
                    }
                    // replace the existing trace in the unmodifiable tracks
                    dataNew.addTrack(new GpxTrack(new ArrayList<>(trk.getSegments()), attrib));
                }

                data = dataNew;

                final GpxLayer gpxLayer = new GpxLayer(data, (track.filename + " " + track.description).trim());

                if (data.hasRoutePoints() || data.hasTrackPoints()) {
                    MainApplication.getLayerManager().addLayer(gpxLayer);
                }

                if (Config.getPref().getBoolean("marker.makeautomarkers", true) && !data.waypoints.isEmpty()) {
                    MarkerLayer ml = new MarkerLayer(data, tr("Markers from {0}", track.filename), null, gpxLayer);
                    if (!ml.data.isEmpty()) {
                        MainApplication.getLayerManager().addLayer(ml);
                    }
                }
            }
        }
    }
}
