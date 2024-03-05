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
    /** The executor for fetching the tiles. */
    private static final ThreadPoolExecutor EXECUTOR = TMSCachedTileLoader.getNewThreadPoolExecutor("pmtiles");
    /** The current jobs for the loader */
    private final Collection<PMTileJob> jobs = new HashSet<>();
    /** The cache for downloaded tiles */
    private final ICacheAccess<String, CacheEntry> cache;
    /** The options for the tile loader */
    private final TileJobOptions options;
    /** The PMTiles header */
    private Header header;
    /** The cache of PMTiles directories */
    private DirectoryCache directoryCache;

    /**
     * Create a new tile loader
     * @param ignored The listener to notify
     * @param cache The cache to use
     * @param options The options to use
     */
    public PMTilesLoader(TileLoaderListener ignored, ICacheAccess<String, CacheEntry> cache,
                         TileJobOptions options) {
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

    /**
     * Set the PMTiles info for this loader
     * @param info The info to use
     */
    void setInfo(PMTilesImageryInfo info) {
        this.header = info.header();
        try {
            this.directoryCache = new DirectoryCache(PMTiles.readRootDirectory(this.header));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
