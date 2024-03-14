// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import java.awt.Image;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.Logging;

/**
 * Various utility methods for images
 */
public final class ImageUtil {

    private static final Logger LOGGER = Logger.getLogger(ImageUtil.class.getCanonicalName());

    private ImageUtil() {
        // Private constructor to avoid instantiation
    }

    /**
     * Scales an {@link ImageIcon} to the desired size
     *
     * @param icon the icon, which should be resized
     * @param size the desired length of the longest edge of the icon
     * @return the resized {@link ImageIcon}. It is the same object that you put in,
     * only the contained {@link Image} is exchanged.
     */
    public static ImageIcon scaleImageIcon(final ImageIcon icon, int size) {
        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Scale icon {0} → {1}", new Object[] { icon.getIconWidth(), size });
        }
        return new ImageIcon(
                icon.getImage()
                        .getScaledInstance(
                                icon.getIconWidth() >= icon.getIconHeight() ? size
                                        : Math.max(1,
                                                Math.round(icon.getIconWidth() / (float) icon.getIconHeight() * size)),
                                icon.getIconHeight() >= icon.getIconWidth() ? size
                                        : Math.max(1,
                                                Math.round(icon.getIconHeight() / (float) icon.getIconWidth() * size)),
                                Image.SCALE_SMOOTH));
    }
}
