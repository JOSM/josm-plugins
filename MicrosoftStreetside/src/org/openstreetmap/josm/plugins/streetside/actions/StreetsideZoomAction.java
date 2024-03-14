// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.Serial;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.StreetsideDataListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsidePlugin;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * Zooms to the currently selected image.
 *
 * @author nokutu
 *
 */
public class StreetsideZoomAction extends JosmAction implements StreetsideDataListener {

    @Serial
    private static final long serialVersionUID = -5885977359895624233L;

    /**
     * Main constructor.
     */
    public StreetsideZoomAction() {
        super(tr("Zoom to selected image"), new ImageProvider(StreetsidePlugin.LOGO).setSize(ImageSizes.DEFAULT).setOptional(true),
                tr("Zoom to the currently selected Streetside image"), null, false, "mapillaryZoom", true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (StreetsideLayer.getInstance().getData().getSelectedImage() == null) {
            throw new IllegalStateException();
        }
        MainApplication.getMap().mapView.zoomTo(StreetsideLayer.getInstance().getData().getSelectedImage());
    }

    @Override
    public void imagesAdded() {
        // Nothing
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    public void selectedImageChanged(StreetsideImage oldImage, StreetsideImage newImage) {
        if (oldImage == null && newImage != null) {
            setEnabled(true);
        } else if (oldImage != null && newImage == null) {
            setEnabled(false);
        }
    }

    @Override
    protected void updateEnabledState() {
        super.updateEnabledState();
        setEnabled(StreetsideLayer.hasInstance() && StreetsideLayer.getInstance().getData().getSelectedImage() != null);
    }
}
