// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cache;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TileJobOptions;

/**
 * Stores the downloaded pictures locally.
 *
 * @author nokutu
 *
 */
public class StreetsideCache extends JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> {

    private final String url;

    /**
     * Main constructor.
     *
     * @param url The image URL
     */
    public StreetsideCache(final String url) {
        super(Caches.ImageCache.getInstance().getCache(), new TileJobOptions(50000, 50000, new HashMap<>(), 50000L));
        this.url = url;
    }

    @Override
    public String getCacheKey() {
        return this.url;
    }

    @Override
    public URL getUrl() {
        try {
            return URI.create(this.url).toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
        return new BufferedImageCacheEntry(content);
    }

    @Override
    protected boolean isObjectLoadable() {
        if (cacheData == null) {
            return false;
        }
        final byte[] content = cacheData.getContent();
        return content != null && content.length > 0;
    }
}
