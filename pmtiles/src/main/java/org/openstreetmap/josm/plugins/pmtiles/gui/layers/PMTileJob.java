// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.plugins.pmtiles.lib.DirectoryCache;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;
import org.openstreetmap.josm.tools.Logging;

/**
 * A job for loading a PMTile.
 */
class PMTileJob extends JCSCachedTileLoaderJob<String, CacheEntry> implements TileJob, ICachedLoaderListener {
    /** The tile we are fetching */
    private final Tile tile;
    /** The tiles information */
    private final Header header;
    /** The directory cache */
    private final DirectoryCache directoryCache;

    /**
     * Create a new job
     * @param cache cache instance that we will work on
     * @param options options of the request
     * @param downloadJobExecutor that will be executing the jobs
     * @param header The header for the tiles
     * @param tile The tile to fetch
     * @param directoryCache The cache of directories
     */
    PMTileJob(ICacheAccess<String, CacheEntry> cache,
              TileJobOptions options,
              ThreadPoolExecutor downloadJobExecutor, Header header, Tile tile,
              DirectoryCache directoryCache) {
        super(cache, options, downloadJobExecutor);
        Objects.requireNonNull(directoryCache);
        this.tile = tile;
        this.header = header;
        this.directoryCache = directoryCache;
    }

    @Override
    public void submit() {
        this.submit(false);
    }

    @Override
    public void submit(boolean force) {
        tile.initLoading();
        try {
            super.submit(this, force);
        } catch (IOException | IllegalArgumentException e) {
            // if we fail to submit the job, mark tile as loaded and set error message
            Logging.log(Logging.LEVEL_WARN, e);
            tile.finishLoading();
            tile.setError(e.getMessage());
        }
    }

    @Override
    public String getCacheKey() {
        return this.header.location().toString() + '/' +
                PMTilesTileSource.getTileId(this.header, this.tile.getZoom(), this.tile.getXtile(), this.tile.getYtile());
    }

    @Override
    public URL getUrl() throws IOException {
        return new URL(this.getCacheKey());
    }

    @Override
    public void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result) {
        switch (result) {
            case FAILURE -> this.tile.setError(data == null ?
                    tr("Data could not be read") : new String(data.getContent(), StandardCharsets.UTF_8));
            case CANCELED -> this.tile.setLoaded(false);
            case SUCCESS -> {
                this.tile.finishLoading();
                this.tryLoadData(data);
            }
        }
    }

    @Override
    protected CacheEntry createCacheEntry(byte[] content) {
        return switch (this.header.tileType()) {
            case MVT, UNKNOWN -> new CacheEntry(content);
            case JPEG, PNG, AVIF, WEBP -> new BufferedImageCacheEntry(content);
        };
    }

    /**
     * Try to load the tile image
     * @param data The data to load
     */
    private void tryLoadData(CacheEntry data) {
        try (var is = new ByteArrayInputStream(data.getContent())) {
            this.tile.loadImage(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get the "URL" for copied methods (mostly for logging)
     * @return The "URL"
     */
    private String getUrlNoException() {
        return getCacheKey();
    }

    /**
     * @return true if object was successfully downloaded via http, false, if there was a loading failure
     */
    @Override
    protected boolean loadObject() {
        if (attributes == null) {
            attributes = new CacheEntryAttributes();
        }
        try {
            Logging.debug("JCS - starting HttpClient GET request for URL: {0}", getUrlNoException());
            final var data = PMTiles.readData(this.header,
                    PMTiles.convertToHilbert(this.tile.getZoom(), this.tile.getXtile(), this.tile.getYtile()),
                    this.directoryCache);
            this.cacheData = this.createCacheEntry(data);
            this.cache.put(getCacheKey(), this.cacheData, this.attributes);
            return true;
        } catch (IOException e) {
            this.attributes.setError(e);
            this.attributes.setException(e);
        }
        Logging.warn("JCS - Silent failure during download: {0}", getUrlNoException());
        return false;
    }
}
