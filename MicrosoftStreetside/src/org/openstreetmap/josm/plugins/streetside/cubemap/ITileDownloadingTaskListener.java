// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import java.awt.image.BufferedImage;

import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;

/**
* Interface for listeners of the class {@link org.openstreetmap.josm.plugins.streetside.StreetsideData}.
*
* @author renerr18
*
*/
public interface ITileDownloadingTaskListener {

    /**
     * Fired when a cubemap tile image is downloaded by a download worker.
     *
     * @param image The image for which we are downloading tiles
     * @param tile The tile that we downloaded an image for
     * @param tileImage The image for the tile
     */
    void tileAdded(StreetsideAbstractImage image, CubeMapTileXY tile, BufferedImage tileImage);

}
