// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapBuilder;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;

/**
* Utility methods for working with cache.
*
* @author nokutu
*
*/
public final class CacheUtils {

    private static final IgnoreDownload ignoreDownload = new IgnoreDownload();

    private CacheUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Downloads the thumbnail and the full resolution picture of the given
     * image. Does nothing if it is already in cache.
     *
     * @param img The image whose picture is going to be downloaded.
     * @return A map of tiles to cached images (The {@code null} key is the thumbnail, if one was requested)
     */
    public static Map<CubeMapTileXY, StreetsideCache> downloadPicture(StreetsideImage img) {
        return downloadPicture(img, PICTURE.BOTH);
    }

    /**
     * Downloads the thumbnail and the full resolution picture of the given
     * image. Does nothing if it is already in cache.
     *
     * @param cm The image whose picture is going to be downloaded.
     * @return A map of tiles to cached images (The {@code null} key is the thumbnail, if one was requested)
     */
    public static Map<CubeMapTileXY, StreetsideCache> downloadCubemap(StreetsideImage cm) {
        return downloadPicture(cm, PICTURE.CUBEMAP);
    }

    /**
     * Downloads the picture of the given image. Does nothing when it is already
     * in cache.
     *
     * @param img The image to be downloaded.
     * @param pic The picture type to be downloaded (full quality, thumbnail or
     *      both.)
     * @return A map of tiles to cached images (The {@code null} key is the thumbnail, if one was requested)
     */
    public static Map<CubeMapTileXY, StreetsideCache> downloadPicture(StreetsideImage img, PICTURE pic) {
        return downloadPicture(img, pic, ignoreDownload);
    }

    /**
     * Downloads the picture of the given image. Does nothing when it is already
     * in cache.
     *
     * @param img The image to be downloaded.
     * @param pic The picture type to be downloaded (full quality, thumbnail or
     *      both.)
     * @param lis  The listener that is going to receive the picture.
     * @return A map of tiles to cached images (The {@code null} key is the thumbnail, if one was requested)
     */
    public static Map<CubeMapTileXY, StreetsideCache> downloadPicture(StreetsideImage img, PICTURE pic,
            ICachedLoaderListener lis) {
        if (img.id() == null) {
            return Collections.emptyMap();
        }
        return switch (pic) {
        case BOTH -> {
            final Map<CubeMapTileXY, StreetsideCache> jobs = new HashMap<>(
                    1 + (int) Math.round(Math.pow(4, img.zoomMax())));
            jobs.putAll(downloadPicture(img, PICTURE.THUMBNAIL, lis));
            jobs.putAll(downloadPicture(img, PICTURE.FULL_IMAGE, lis));
            yield Collections.unmodifiableMap(jobs);
        }
        case FULL_IMAGE -> img.getFaceTiles(CubemapUtils.CubemapFaces.FRONT, img.zoomMax())
                .collect(Collectors.toMap(p -> p.a, p -> submit(p.b, lis)));
        case CUBEMAP -> CubemapBuilder.getInstance().downloadCubemapImages(img);
        case THUMBNAIL -> Collections.singletonMap(null, submit(img.getThumbnail(), lis));
        };
    }

    /**
     * Requests the picture with the given key and quality and uses the given
     * listener.
     *
     * @param key  The key of the picture to be requested.
     * @param lis  The listener that is going to receive the picture.
     * @return The cache job
     */
    public static StreetsideCache submit(String key, ICachedLoaderListener lis) {
        try {
            final var cache = new StreetsideCache(key);
            cache.submit(lis, false);
            return cache;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Picture quality
     */
    public enum PICTURE {
        /**
         * Thumbnail quality picture (320 p)
         */
        THUMBNAIL,
        /**
         * Full quality picture (2048 p)
         */
        FULL_IMAGE,
        /**
         * Both of them
         */
        BOTH,
        /**
         * Streetside cubemap
         */
        CUBEMAP
    }

    private static class IgnoreDownload implements ICachedLoaderListener {

        @Override
        public void loadingFinished(CacheEntry arg0, CacheEntryAttributes arg1, LoadResult arg2) {
            // Ignore download
        }
    }
}
