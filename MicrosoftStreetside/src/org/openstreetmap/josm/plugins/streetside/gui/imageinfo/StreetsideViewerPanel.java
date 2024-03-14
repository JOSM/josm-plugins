// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui.imageinfo;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.io.Serial;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.preferences.AbstractProperty.ValueChangeListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideDataListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.actions.ImageReloadAction;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapBox;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapBuilder;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.gui.boilerplate.StreetsideButton;
import org.openstreetmap.josm.plugins.streetside.utils.GraphicsUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

/**
 * The panel to view 360 images in
 */
public final class StreetsideViewerPanel extends JPanel implements StreetsideDataListener {

    @Serial
    private static final long serialVersionUID = 4141847503072417190L;

    private static final Logger LOGGER = Logger.getLogger(StreetsideViewerPanel.class.getCanonicalName());
    private static ThreeSixtyDegreeViewerPanel threeSixtyDegreeViewerPanel;
    private WebLinkAction imgLinkAction;
    private ValueChangeListener<Boolean> imageLinkChangeListener;

    /**
     * Create a new 360 viewer
     */
    public StreetsideViewerPanel() {
        super(new BorderLayout());

        SwingUtilities.invokeLater(this::initializeAndStartGUI);

        selectedImageChanged(null, null);

        setToolTipText(
                I18n.tr("Select Microsoft Streetside from the Imagery menu, then click on a blue vector bubble.."));
    }

    /**
     * Get the {@link CubemapBox} for showing images
     * @return The box for images
     */
    public static CubemapBox getCubemapBox() {
        return threeSixtyDegreeViewerPanel.getCubemapBox();
    }

    /**
     * Get the current 360 viewer panel
     * @return the threeSixtyDegreeViewerPanel
     */
    public static ThreeSixtyDegreeViewerPanel getThreeSixtyDegreeViewerPanel() {
        return threeSixtyDegreeViewerPanel;
    }

    private synchronized void initializeAndStartGUI() {
        try {
            threeSixtyDegreeViewerPanel = new ThreeSixtyDegreeViewerPanel();
        } catch (NoClassDefFoundError e) {
            Logging.trace(e);
            return;
        }

        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsUtils.PlatformHelper.run(threeSixtyDegreeViewerPanel::initialize);
        }

        add(threeSixtyDegreeViewerPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        final var checkPanel = new JPanel();

        final var imgReloadAction = new ImageReloadAction("Reload");

        final var imgReloadButton = new StreetsideButton(imgReloadAction);

        final var highResImageryCheck = new JCheckBox("High resolution");
        highResImageryCheck.setSelected(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get());
        highResImageryCheck.addActionListener(
                action -> StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.put(highResImageryCheck.isSelected()));
        StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.addListener(valueChange -> highResImageryCheck
                .setSelected(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()));
        checkPanel.add(highResImageryCheck, BorderLayout.WEST);
        checkPanel.add(imgReloadButton, BorderLayout.EAST);

        final var privacyLink = new JPanel();

        imgLinkAction = new WebLinkAction("Report a privacy concern with this image", null);
        privacyLink.add(new StreetsideButton(imgLinkAction, true));
        checkPanel.add(privacyLink, BorderLayout.PAGE_END);

        add(threeSixtyDegreeViewerPanel, BorderLayout.CENTER);

        final var bottomPanel = new JPanel();
        bottomPanel.add(checkPanel, BorderLayout.NORTH);
        bottomPanel.add(privacyLink, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.PAGE_END);
    }

    @Override
    public void imagesAdded() {
        // Method is not needed, but enforcesd by the interface StreetsideDataListener
    }

    @Override
    public synchronized void selectedImageChanged(final StreetsideImage oldImage, final StreetsideImage newImage) {
        // method is invoked with null initially by framework
        if (newImage != null) {
            LOGGER.info(() -> String.format("Selected Streetside image changed from %s to %s.",
                    oldImage != null ? oldImage.id() : "‹none›", newImage.id()));

            final var newImageId = CubemapBuilder.getInstance().getCubemap() != null
                    ? CubemapBuilder.getInstance().getCubemap().id()
                    : newImage.id();
            if (newImageId != null) {
                updateLinksToNewImage(newImageId);
            } else {
                if (imageLinkChangeListener != null) {
                    StreetsideProperties.CUBEMAP_LINK_TO_BLUR_EDITOR.removeListener(imageLinkChangeListener);
                    imageLinkChangeListener = null;
                }
                imgLinkAction.setURL(null);
            }
        }
    }

    private void updateLinksToNewImage(String newImageId) {
        final var matcher = Pattern.compile("/tiles/hs([0-9]*)").matcher(newImageId);
        if (matcher.find()) {
            final var bubbleId = CubemapUtils.convertQuaternary2Decimal(matcher.group(1));
            imageLinkChangeListener = b -> imgLinkAction
                    .setURL(StreetsideURL.MainWebsite.streetsidePrivacyLink(bubbleId));

            if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
                LOGGER.log(Logging.LEVEL_DEBUG, "Privacy link set for Streetside image {0} quadKey {1}",
                        new Object[] {bubbleId, newImageId});
            }

            imageLinkChangeListener.valueChanged(null);
            StreetsideProperties.CUBEMAP_LINK_TO_BLUR_EDITOR.addListener(imageLinkChangeListener);
        }
    }
}
