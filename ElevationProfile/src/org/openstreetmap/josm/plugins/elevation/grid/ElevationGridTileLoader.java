// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.grid;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Tile loader for the elevation grid display
 * @author Olli
 *
 */
public class ElevationGridTileLoader extends TMSCachedTileLoader {

    static class ElevationGridTileJob extends JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> implements TileJob {

        private final Tile tile;
        private final TileLoaderListener listener;

        protected ElevationGridTileJob(TileLoaderListener listener, Tile tile, ICacheAccess<String, BufferedImageCacheEntry> cache, TileJobOptions options,
                ThreadPoolExecutor downloadJobExecutor) {
            super(cache, options, downloadJobExecutor);
            this.tile = tile;
            this.listener = listener;
        }

        @Override
        public void run() {
            synchronized (tile) {
                if ((tile.isLoaded() && !tile.hasError()) || tile.isLoading())
                    return;
                tile.initLoading();
            }
            try {
                tile.loadImage(null);
                tile.setLoaded(true);
                listener.tileLoadingFinished(tile, true);
            } catch (Exception e) {
                tile.setError(e.getMessage());
                listener.tileLoadingFinished(tile, false);
            } finally {
                tile.finishLoading();
            }
        }

        @Override
        public void submit() {
            run();
        }

        @Override
        public void submit(boolean force) {
            submit();
        }

        @Override
        public String getCacheKey() {
            if (tile != null) {
                TileSource tileSource = tile.getTileSource();
                return Optional.ofNullable(tileSource.getName()).orElse("").replace(':', '_') + ':'
                        + tileSource.getTileId(tile.getZoom(), tile.getXtile(), tile.getYtile());
            }
            return null;
        }

        @Override
        public URL getUrl() throws IOException {
            return new URL(String.format("http://localhost/elevation/%d/%d", tile.getTileXY().getXIndex(), tile.getTileXY().getYIndex()));
        }

        @Override
        protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
            return new BufferedImageCacheEntry(content);
        }
    }

    /**
     * Constructor
     * @param listener          called when tile loading has finished
     * @param cache             of the cache
     * @param options           tile job options
     */
    public ElevationGridTileLoader(TileLoaderListener listener, ICacheAccess<String, BufferedImageCacheEntry> cache,
           TileJobOptions options) {
        super(listener, cache, options);
    }

    @Override
    public TileJob createTileLoaderJob(final Tile tile) {
        CheckParameterUtil.ensureParameterNotNull(tile);

        return new ElevationGridTileJob(listener,
                tile,
                cache,
                options,
                getDownloadExecutor());
    }

    @Override
    public void cancelOutstandingTasks() {
        // intentionally left blank
    }

}
