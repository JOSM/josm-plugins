// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui.imageinfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Collection;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.preferences.AbstractProperty.ValueChangeListener;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.streetside.StreetsideDataListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.gui.boilerplate.StreetsideButton;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

/**
 * A panel for showing image information
 */
public final class ImageInfoPanel extends ToggleDialog implements StreetsideDataListener, DataSelectionListener {
    @Serial
    private static final long serialVersionUID = 1898445061036887054L;

    private static final Logger LOGGER = Logger.getLogger(ImageInfoPanel.class.getCanonicalName());

    private static ImageInfoPanel instance;

    private final WebLinkAction imgLinkAction;

    private ValueChangeListener<Boolean> imageLinkChangeListener;

    private ImageInfoPanel() {
        super(I18n.tr("Streetside 360° image info"), "streetside-info",
                I18n.tr("Displays detail information on the currently selected Streetside image"), null, 150);
        SelectionEventManager.getInstance().addSelectionListener(this);

        imgLinkAction = new WebLinkAction(I18n.tr("View in browser"), null);

        final var root = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        root.add(new JLabel(I18n.tr("Image actions")), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        root.add(new StreetsideButton(imgLinkAction, true), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.gridx = 2;
        gbc.gridy = 1;

        createLayout(root, true, null);
        selectedImageChanged(null, null);
    }

    public static ImageInfoPanel getInstance() {
        synchronized (ImageInfoPanel.class) {
            if (instance == null) {
                instance = new ImageInfoPanel();
            }
            return instance;
        }
    }

    /**
     * Destroys the unique instance of the class.
     */
    public static synchronized void destroyInstance() {
        instance = null;
    }

    @Override
    protected void stateChanged() {
        super.stateChanged();
        if (isDialogShowing()) { // If the user opens the dialog once, no longer show the help message
            StreetsideProperties.IMAGEINFO_HELP_COUNTDOWN.put(0);
        }
    }

    @Override
    public void imagesAdded() {
        // Method is not needed, but enforced by the interface StreetsideDataListener
    }

    @Override
    public synchronized void selectedImageChanged(final StreetsideImage oldImage, final StreetsideImage newImage) {
        LOGGER.info(() -> String.format("Selected Streetside image changed from %s to %s.",
                oldImage != null ? oldImage.id() : "‹none›", newImage != null ? newImage.id() : "‹none›"));

        final String newImageKey = newImage != null ? newImage.id() : null;
        if (newImageKey != null) {
            imageLinkChangeListener = b -> imgLinkAction.setURL(StreetsideURL.MainWebsite.browseImage(newImage));
            imageLinkChangeListener.valueChanged(null);
            StreetsideProperties.IMAGE_LINK_TO_BLUR_EDITOR.addListener(imageLinkChangeListener);

        } else {
            if (imageLinkChangeListener != null) {
                StreetsideProperties.IMAGE_LINK_TO_BLUR_EDITOR.removeListener(imageLinkChangeListener);
                imageLinkChangeListener = null;
            }
            imgLinkAction.setURL(null);
        }
    }

    @Override
    public synchronized void selectionChanged(final SelectionChangeEvent event) {
        final Collection<? extends OsmPrimitive> sel = event.getSelection();
        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Selection changed. {0} primitives are selected.",
                    sel == null ? 0 : sel.size());
        }
    }
}
