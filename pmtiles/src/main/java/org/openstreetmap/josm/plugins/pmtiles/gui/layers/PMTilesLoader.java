// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.DirectoryCache;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;

/**
 * The loader class for PMTiles
 */
public class PMTilesLoader implements TileLoader {
    private static final ThreadPoolExecutor EXECUTOR = TMSCachedTileLoader.getNewThreadPoolExecutor("pmtiles");
    private final Collection<PMTileJob> jobs = new HashSet<>();
    private final ICacheAccess<String, CacheEntry> cache;
    private final TileJobOptions options;
    private final TileLoaderListener listener;
    private Header header;
    private DirectoryCache directoryCache;

    /**
     * Create a new tile loader
     * @param listener The listener to notify
     * @param cache The cache to use
     * @param options The options to use
     */
    public PMTilesLoader(TileLoaderListener listener, ICacheAccess<String, CacheEntry> cache,
                         TileJobOptions options) {
        this.listener = listener;
        this.cache = cache;
        this.options = options;
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        final var job = new PMTileJob(cache, options, EXECUTOR, header, tile, directoryCache);
        this.jobs.add(job);
        return job;
    }

    @Override
    public void cancelOutstandingTasks() {
        this.jobs.forEach(PMTileJob::handleJobCancellation);
        this.jobs.clear();
    }

    @Override
    public boolean hasOutstandingTasks() {
        return this.jobs.isEmpty();
    }

    void setInfo(PMTilesImageryInfo info) {
        this.header = info.header();
        try {
            this.directoryCache = new DirectoryCache(PMTiles.readRootDirectory(this.header));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
