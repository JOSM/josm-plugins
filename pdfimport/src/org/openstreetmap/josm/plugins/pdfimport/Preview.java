// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pdfimport;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class Preview {
    public static OsmDataLayer dataLayer;
    public static FilePlacement placement;

    public static synchronized void set(OsmDataLayer dataLayer, FilePlacement placement) {
        clear();
        Preview.dataLayer = dataLayer;
        Preview.placement = placement;
        MainApplication.getLayerManager().addLayer(dataLayer);
    }

    public static void clear() {
        if (Preview.dataLayer != null) {
            MainApplication.getLayerManager().removeLayer(Preview.dataLayer);
            Preview.dataLayer.data.clear(); // saves memory
        }
        Preview.dataLayer = null;
        Preview.placement = null;
    }

    public void save() {
//        TODO: implement
    }
}
