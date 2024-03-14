// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.mode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideMainDialog;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;

/**
 * Handles the input event related with the layer. Mainly clicks.
 *
 * @author nokutu
 */
public class SelectMode extends AbstractMode {
    private boolean nothingHighlighted;
    private boolean imageHighlighted;

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        // click
        StreetsideLayer.getInstance().getData().setSelectedImage(getClosest(e.getPoint()));
    }

    /**
     * Checks if the mouse is over pictures.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (MainApplication.getLayerManager().getActiveLayer() instanceof OsmDataLayer
                && MainApplication.getMap().mapMode != MainApplication.getMap().mapModeSelect) {
            return;
        }
        if (Boolean.FALSE.equals(StreetsideProperties.HOVER_ENABLED.get())) {
            return;
        }

        StreetsideImage closestTemp = getClosest(e.getPoint());

        final OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (editLayer != null) {
            if (closestTemp != null && !imageHighlighted) {
                if (MainApplication.getMap().mapMode != null) {
                    MainApplication.getMap().mapMode.putValue("active", Boolean.FALSE);
                }
                imageHighlighted = true;
            } else if (closestTemp == null && imageHighlighted && nothingHighlighted) {
                if (MainApplication.getMap().mapMode != null) {
                    MainApplication.getMap().mapMode.putValue("active", Boolean.TRUE);
                }
                nothingHighlighted = false;
            } else if (imageHighlighted && !nothingHighlighted && editLayer.data != null) {
                for (OsmPrimitive primivitive : MainApplication.getLayerManager().getEditLayer().data.allPrimitives()) {
                    primivitive.setHighlighted(false);
                }
                imageHighlighted = false;
                nothingHighlighted = true;
            }
        }

        if (StreetsideLayer.getInstance().getData().getHighlightedImage() != closestTemp && closestTemp != null) {
            StreetsideLayer.getInstance().getData().setHighlightedImage(closestTemp);
            StreetsideMainDialog.getInstance().setImage(closestTemp);
            StreetsideMainDialog.getInstance().updateImage(false);

        } else if (StreetsideLayer.getInstance().getData().getHighlightedImage() != closestTemp
                && closestTemp == null) {
            StreetsideLayer.getInstance().getData().setHighlightedImage(null);
            StreetsideMainDialog.getInstance().setImage(StreetsideLayer.getInstance().getData().getSelectedImage());
            StreetsideMainDialog.getInstance().updateImage();
        }

        StreetsideLayer.invalidateInstance();
    }

    @Override
    public String toString() {
        return tr("Select mode");
    }
}
