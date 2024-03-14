// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsidePlugin;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that triggers the plugin. If in automatic mode, it will automatically
 * download the images in the areas where there is OSM data.
 *
 * @author nokutu
 *
 */
public class StreetsideDownloadAction extends JosmAction {

    public static final Shortcut SHORTCUT = Shortcut.registerShortcut("Streetside", "Open Streetside layer",
            KeyEvent.VK_COMMA, Shortcut.SHIFT);
    @Serial
    private static final long serialVersionUID = 4426446157849005029L;
    private static final Logger LOGGER = Logger.getLogger(StreetsideDownloadAction.class.getCanonicalName());

    /**
     * Main constructor.
     */
    public StreetsideDownloadAction() {
        super(tr("Streetside"), new ImageProvider(StreetsidePlugin.LOGO).setSize(ImageSizes.DEFAULT),
                tr("Open Streetside layer"), SHORTCUT, false, "streetsideDownload", false);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (!StreetsideLayer.hasInstance()
                || !MainApplication.getLayerManager().containsLayer(StreetsideLayer.getInstance())) {
            MainApplication.getLayerManager().addLayer(StreetsideLayer.getInstance());
            return;
        }

        try {
            // Successive calls to this action toggle the active layer between the OSM data layer and the streetside layer
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            if (MainApplication.getLayerManager().getActiveLayer() != StreetsideLayer.getInstance()) {
                MainApplication.getLayerManager().setActiveLayer(StreetsideLayer.getInstance());
            } else if (editLayer != null) {
                MainApplication.getLayerManager().setActiveLayer(editLayer);
            }
        } catch (IllegalArgumentException e) {
            // If the StreetsideLayer is not managed by LayerManager, but you try to set it as active layer
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
