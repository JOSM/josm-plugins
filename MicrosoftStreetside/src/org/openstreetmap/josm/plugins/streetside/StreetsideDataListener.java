// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

/**
 * Interface for listeners of the class {@link StreetsideData}.
 *
 * @author nokutu
 *
 */
public interface StreetsideDataListener {

    /**
     * Fired when any image is added to the database.
     */
    void imagesAdded();

    /**
     * Fired when the selected image is changed by something different from
     * manually clicking on the icon.
     *
     * @param oldImage Old selected {@link StreetsideImage}
     * @param newImage New selected {@link StreetsideImage}
     */
    void selectedImageChanged(StreetsideImage oldImage, StreetsideImage newImage);
}
