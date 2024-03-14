// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * A task for downloading tiles of an image
 */
public class TileDownloadingTask implements Callable<List<String>> {

    private static final Logger LOGGER = Logger.getLogger(TileDownloadingTask.class.getCanonicalName());
    private static final Semaphore limitConnections = new Semaphore(
            CubemapUtils.NUM_SIDES * Config.getPref().getInt("streetside.download.threads.per.side", 64)); // z3 is 8x8
    /**
     * Listeners of the class.
     */
    private final List<ITileDownloadingTaskListener> listeners = new CopyOnWriteArrayList<>();
    private final StreetsideAbstractImage image;
    private final CubemapUtils.CubemapFaces face;
    private final CubeMapTileXY tileId;
    protected final CubemapBuilder cb;

    /**
     * Download a tile
     * @param image The image that we are downloading the tile for
     * @param face The face for the image (since Streetside provides cubemaps)
     * @param tileId The tile id to download
     */
    public TileDownloadingTask(StreetsideAbstractImage image, CubemapUtils.CubemapFaces face, CubeMapTileXY tileId) {
        this.image = image;
        this.face = face;
        this.tileId = tileId;
        this.cb = CubemapBuilder.getInstance();
        addListener(this.cb);
    }

    /**
     * Adds a new listener.
     *
     * @param lis Listener to be added.
     */
    public final void addListener(final ITileDownloadingTaskListener lis) {
        listeners.add(lis);
    }

    /**
     * Get the id for this task
     * @return the tileId
     */
    public String getId() {
        return tileId.toString();
    }

    @Override
    public List<String> call() {
        try {
            limitConnections.acquire();
            try {
                return download();
            } finally {
                limitConnections.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logging.trace(e);
            return Collections.emptyList();
        }
    }

    private List<String> download() {
        List<String> res = new ArrayList<>();

        final int zoom = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())
                ? this.image.zoomMax()
                : this.image.zoomMin();
        if (Boolean.TRUE.equals(StreetsideProperties.DOWNLOAD_CUBEFACE_TILES_TOGETHER.get())) {
            // download all imagery for each cubeface at once
            res.addAll(this.image.getFaceTiles(this.face, zoom).map(pair -> downloadTile(pair.a, pair.b)).toList());
            // task downloads just one tile
        } else {
            res.add(downloadTile(tileId, this.image.getTile(this.face.getValue(), tileId.getQuadKey(zoom))));
        }
        return res;
    }

    private String downloadTile(CubeMapTileXY tile, String url) {
        BufferedImage img;

        long startTime = System.currentTimeMillis();

        try {
            img = ImageIO.read(URI.create(url).toURL());

            if (img == null) {
                LOGGER.log(Logging.LEVEL_ERROR, "Download of BufferedImage {0} is null!", url);
            }

            fireTileAdded(this.image, tile, img);

            if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
                long endTime = System.currentTimeMillis();
                long runTime = (endTime - startTime) / 1000;
                LOGGER.log(Logging.LEVEL_DEBUG, "Loaded image for {0} in {1} seconds.", new Object[] { url, runTime });
            }
        } catch (IOException e) {
            LOGGER.log(Logging.LEVEL_ERROR, MessageFormat.format("Error downloading image for tileId {0}", url), e);
            return null;
        }
        return url;
    }

    /**
     * Fire a tile add event
     * @param image The Streetside image we are getting tiled images for
     * @param tile The tile in the image
     * @param tileImage The actual tile image
     */
    private void fireTileAdded(StreetsideAbstractImage image, CubeMapTileXY tile, BufferedImage tileImage) {
        listeners.stream().filter(Objects::nonNull).forEach(lis -> lis.tileAdded(image, tile, tileImage));
    }
}
