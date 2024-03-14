// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cache;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.tools.Logging;

public final class Caches {

    private static final Logger LOGGER = Logger.getLogger(Caches.class.getCanonicalName());

    private Caches() {
        // Private constructor to avoid instantiation
    }

    public static File getCacheDirectory() {
        final var file = new File(Preferences.main().getPluginsDirectory().getPath() + "/MicrosoftStreetside/cache");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Logging.error("Failed to make directory: {0}", file);
            }
        }
        return file;
    }

    public abstract static class CacheProxy<K, V extends Serializable> {
        private final CacheAccess<K, V> cache;

        protected CacheProxy() {
            CacheAccess<K, V> c;
            try {
                c = createNewCache();
            } catch (IOException e) {
                LOGGER.log(Logging.LEVEL_WARN, e, () -> "Could not initialize cache for " + getClass().getName());
                c = null;
            }
            cache = c;
        }

        protected abstract CacheAccess<K, V> createNewCache() throws IOException;

        public V get(final K key) {
            return cache == null ? null : cache.get(key);
        }

        public void put(final K key, final V value) {
            if (cache != null) {
                cache.put(key, value);
            }
        }
    }

    public static class ImageCache {
        private static ImageCache instance;
        private final CacheAccess<String, BufferedImageCacheEntry> cache;

        public ImageCache() {
            CacheAccess<String, BufferedImageCacheEntry> c;
            try {
                c = JCSCacheManager.getCache("streetside", 10, 10000, Caches.getCacheDirectory().getPath());
            } catch (Exception e) {
                Logging.log(Logging.LEVEL_WARN, "Could not initialize the Streetside image cache.", e);
                c = null;
            }
            cache = c;
        }

        public static ImageCache getInstance() {
            synchronized (ImageCache.class) {
                if (ImageCache.instance == null) {
                    ImageCache.instance = new ImageCache();
                }
                return ImageCache.instance;
            }
        }

        public CacheAccess<String, BufferedImageCacheEntry> getCache() {
            return cache;
        }
    }

    public static class CubemapCache {
        private static CubemapCache instance;
        private final CacheAccess<String, BufferedImageCacheEntry> cache;

        public CubemapCache() {
            CacheAccess<String, BufferedImageCacheEntry> c;
            try {
                c = JCSCacheManager.getCache("streetside", 10, 10000, Caches.getCacheDirectory().getPath());
            } catch (Exception e) {
                LOGGER.log(Logging.LEVEL_WARN, "Could not initialize the Streetside cubemap cache.", e);
                c = null;
            }
            cache = c;
        }

        public static CubemapCache getInstance() {
            synchronized (CubemapCache.class) {
                if (CubemapCache.instance == null) {
                    CubemapCache.instance = new CubemapCache();
                }
                return CubemapCache.instance;
            }
        }

        public CacheAccess<String, BufferedImageCacheEntry> getCache() {
            return cache;
        }
    }

    public static class MapObjectIconCache extends CacheProxy<String, ImageIcon> {
        private static CacheProxy<String, ImageIcon> instance;

        public static CacheProxy<String, ImageIcon> getInstance() {
            synchronized (MapObjectIconCache.class) {
                if (MapObjectIconCache.instance == null) {
                    MapObjectIconCache.instance = new MapObjectIconCache();
                }
                return MapObjectIconCache.instance;
            }
        }

        @Override
        protected CacheAccess<String, ImageIcon> createNewCache() throws IOException {
            return JCSCacheManager.getCache("streetsideObjectIcons", 100, 1000, Caches.getCacheDirectory().getPath());
        }
    }
}
