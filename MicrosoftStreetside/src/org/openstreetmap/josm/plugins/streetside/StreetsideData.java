// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.cache.CacheUtils;
import org.openstreetmap.josm.plugins.streetside.cache.Caches;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideMainDialog;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideViewerDialog;
import org.openstreetmap.josm.plugins.streetside.gui.imageinfo.ImageInfoPanel;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;

/**
 * Database class for all the {@link StreetsideAbstractImage} objects.
 *
 * @author nokutu
 * @author renerr18 (extended for Streetside)
 * @see StreetsideAbstractImage
 */
public class StreetsideData implements Data {
    private final QuadBuckets<StreetsideImage> images = new QuadBuckets<>();
    private final List<StreetsideImage> sortedImages = new ArrayList<>();
    /**
     * Listeners of the class.
     */
    private final List<StreetsideDataListener> listeners = new CopyOnWriteArrayList<>();
    /**
     * The bounds of the areas for which the pictures have been downloaded.
     */
    private final List<Bounds> bounds;
    /**
     * The image currently selected, this is the one being shown.
     */
    private StreetsideImage selectedImage;
    /**
     * The image under the cursor.
     */
    private StreetsideImage highlightedImage;

    /**
     * Creates a new object and adds the initial set of listeners.
     */
    protected StreetsideData() {
        selectedImage = null;
        bounds = new CopyOnWriteArrayList<>();

        // Adds the basic set of listeners.
        Arrays.stream(StreetsidePlugin.getStreetsideDataListeners()).forEach(this::addListener);
        addListener(StreetsideViewerDialog.getInstance().getStreetsideViewerPanel());
        addListener(StreetsideMainDialog.getInstance());
        addListener(ImageInfoPanel.getInstance());
    }

    /**
     * Downloads surrounding images of this mapillary image in background threads
     *
     * @param streetsideImage the image for which the surrounding images should be downloaded
     */
    private void downloadSurroundingImages(StreetsideImage streetsideImage) {
        MainApplication.worker.execute(() -> downloadSurrounding(streetsideImage, CacheUtils::downloadPicture));
    }

    /**
     * Downloads surrounding images of this mapillary image in background threads
     *
     * @param streetsideImage the image for which the surrounding images should be downloaded
     */
    public void downloadSurroundingCubemaps(StreetsideImage streetsideImage) {
        MainApplication.worker.execute(() -> downloadSurrounding(streetsideImage, CacheUtils::downloadCubemap));
    }

    private void downloadSurrounding(StreetsideImage streetsideImage, Consumer<StreetsideImage> imageDownloader) {
        final int prefetchCount = StreetsideProperties.PRE_FETCH_IMAGE_COUNT.get();
        CacheAccess<String, BufferedImageCacheEntry> imageCache = Caches.ImageCache.getInstance().getCache();

        StreetsideImage nextImage = next(streetsideImage);
        StreetsideImage prevImage = previous(streetsideImage);

        for (var i = 0; i < prefetchCount; i++) {
            if (nextImage != null) {
                if (imageCache.get(nextImage.id()) == null) {
                    imageDownloader.accept(nextImage);
                }
                nextImage = next(nextImage);
            }
            if (prevImage != null) {
                if (imageCache.get(prevImage.id()) == null) {
                    imageDownloader.accept(prevImage);
                }
                prevImage = previous(prevImage);
            }
        }
    }

    /**
     * Adds an StreetsideImage to the object, and then repaints mapView.
     *
     * @param image The image to be added.
     */
    public void add(StreetsideImage image) {
        add(image, true);
    }

    /**
     * Adds a StreetsideImage to the object, but doesn't repaint mapView. This is
     * needed for concurrency.
     *
     * @param image  The image to be added.
     * @param update Whether the map must be updated or not
     *         (updates are currently unsupported by Streetside).
     */
    public void add(StreetsideImage image, boolean update) {
        if (this.images.contains(image)) {
            return;
        }
        this.images.add(image);
        if (update) {
            StreetsideLayer.invalidateInstance();
        }
        fireImagesAdded();
    }

    /**
     * Adds a set of StreetsideImages to the object, and then repaints mapView.
     *
     * @param images The set of images to be added.
     */
    public void addAll(Collection<StreetsideImage> images) {
        addAll(images, true);
    }

    /**
     * Adds a set of {link StreetsideAbstractImage} objects to this object.
     *
     * @param newImages The set of images to be added.
     * @param update  Whether the map must be updated or not.
     */
    public void addAll(Collection<StreetsideImage> newImages, boolean update) {
        newImages = new HashSet<>(newImages);
        newImages.removeIf(this.images::contains);
        images.addAll(newImages);
        sortedImages.addAll(newImages);
        sortedImages.sort(Comparator.naturalOrder());
        if (update) {
            StreetsideLayer.invalidateInstance();
        }
        fireImagesAdded();
    }

    /**
     * Adds a new listener.
     *
     * @param lis Listener to be added.
     */
    public final void addListener(final StreetsideDataListener lis) {
        listeners.add(lis);
    }

    public List<Bounds> getBounds() {
        return bounds;
    }

    /**
     * Removes a listener.
     *
     * @param lis Listener to be removed.
     */
    public void removeListener(StreetsideDataListener lis) {
        listeners.remove(lis);
    }

    /**
     * Returns the image under the mouse cursor.
     *
     * @return The image under the mouse cursor.
     */
    public StreetsideImage getHighlightedImage() {
        return highlightedImage;
    }

    /**
     * Highlights the image under the cursor.
     *
     * @param image The image under the cursor.
     */
    public void setHighlightedImage(StreetsideImage image) {
        highlightedImage = image;
    }

    /**
     * Returns a Set containing all images.
     *
     * @return A Set object containing all images.
     */
    public Collection<StreetsideImage> getImages() {
        return images;
    }

    /**
     * Sets a new {@link Collection} object as the used set of images.
     * Any images that are already present, are removed.
     *
     * @param newImages the new image list (previously set images are completely replaced)
     */
    public void setImages(Collection<StreetsideImage> newImages) {
        synchronized (this) {
            this.images.clear();
            this.sortedImages.clear();
            this.addAll(newImages);
        }
    }

    /**
     * Returns the StreetsideImage object that is currently selected.
     *
     * @return The selected StreetsideImage object.
     */
    public StreetsideImage getSelectedImage() {
        return selectedImage;
    }

    /**
     * Selects a new image.If the user does ctrl + click, this isn't triggered.
     *
     * @param image The StreetsideImage which is going to be selected
     */
    public void setSelectedImage(StreetsideImage image) {
        setSelectedImage(image, false);
    }

    private void fireImagesAdded() {
        listeners.stream().filter(Objects::nonNull).forEach(StreetsideDataListener::imagesAdded);
    }

    /**
     * If the selected StreetsideImage is part of a StreetsideSequence then the
     * following visible StreetsideImage is selected. In case there is none, does
     * nothing.
     *
     * @throws IllegalStateException if the selected image is null or the selected image doesn't
     *                 belong to a sequence.
     */
    public void selectNext() {
        selectNext(StreetsideProperties.MOVE_TO_IMG.get());
    }

    /**
     * If the selected StreetsideImage is part of a StreetsideSequence then the
     * following visible StreetsideImage is selected. In case there is none, does
     * nothing.
     *
     * @param moveToPicture True if the view must me moved to the next picture.
     * @throws IllegalStateException if the selected image is null or the selected image doesn't
     *                 belong to a sequence.
     */
    public void selectNext(boolean moveToPicture) {
        StreetsideImage tempImage = selectedImage;
        if (selectedImage != null) {
            while (next(tempImage) != null) {
                tempImage = next(tempImage);
                if (tempImage != null && tempImage.visible()) {
                    setSelectedImage(tempImage, moveToPicture);
                    break;
                }
            }
        }
    }

    /**
     * If the selected StreetsideImage is part of a StreetsideSequence then the
     * previous visible StreetsideImage is selected. In case there is none, does
     * nothing.
     *
     * @throws IllegalStateException if the selected image is null or the selected image doesn't
     *                 belong to a sequence.
     */
    public void selectPrevious() {
        selectPrevious(StreetsideProperties.MOVE_TO_IMG.get());
    }

    /**
     * If the selected StreetsideImage is part of a StreetsideSequence then the
     * previous visible StreetsideImage is selected. In case there is none, does
     * nothing. * @throws IllegalStateException if the selected image is null or
     * the selected image doesn't belong to a sequence.
     *
     * @param moveToPicture True if the view must me moved to the previous picture.
     * @throws IllegalStateException if the selected image is null or the selected image doesn't
     *                 belong to a sequence.
     */
    public void selectPrevious(boolean moveToPicture) {
        if (selectedImage != null) {
            StreetsideImage tempImage = selectedImage;
            while (previous(tempImage) != null) {
                tempImage = previous(tempImage);
                if (tempImage != null && tempImage.visible()) {
                    setSelectedImage(tempImage, moveToPicture);
                    break;
                }
            }
        }
    }

    /**
     * Selects a new image.If the user does ctrl+click, this isn't triggered. You
     * can choose whether to center the view on the new image or not.
     *
     * @param image The {@link StreetsideImage} which is going to be selected.
     * @param zoom  True if the view must be centered on the image; false otherwise.
     */
    public void setSelectedImage(StreetsideImage image, boolean zoom) {
        StreetsideImage oldImage = selectedImage;
        selectedImage = image;
        final var mv = StreetsidePlugin.getMapView();
        if (image != null && mv != null) {
            // Downloading thumbnails of surrounding pictures.
            downloadSurroundingImages(image);
        }
        if (mv != null && zoom && selectedImage != null) {
            mv.zoomTo(selectedImage);
        }
        fireSelectedImageChanged(oldImage, selectedImage);
        StreetsideLayer.invalidateInstance();
    }

    private void fireSelectedImageChanged(StreetsideImage oldImage, StreetsideImage newImage) {
        listeners.stream().filter(Objects::nonNull).forEach(lis -> lis.selectedImageChanged(oldImage, newImage));
    }

    @Override
    public Collection<DataSource> getDataSources() {
        return Collections.emptyList();
    }

    /**
     * Get the next image
     * @param current The current image
     * @return The next image, if available
     */
    public StreetsideImage next(StreetsideImage current) {
        final int currentIndex = sortedImages.indexOf(current);
        if (currentIndex + 1 >= sortedImages.size()) {
            return null;
        }
        return sortedImages.get(currentIndex + 1);
    }

    /**
     * Get the previous image
     * @param current The current image
     * @return The previous image, if available
     */
    public StreetsideImage previous(StreetsideImage current) {
        final int currentIndex = sortedImages.indexOf(current);
        if (currentIndex - 1 < 0) {
            return null;
        }
        return sortedImages.get(currentIndex - 1);
    }

    /**
     * Search for images
     * @param target The image to look around
     * @param v The {@link LatLon} degrees to search around
     * @return The images found
     */
    public Collection<StreetsideImage> search(StreetsideImage target, double v) {
        final var searchBox = new BBox(target);
        searchBox.addLatLon(new LatLon(target.lat(), target.lon()), v);
        return this.images.search(searchBox);
    }

    /**
     * Search for images
     * @param searchBox The box to search images in
     * @return The images found
     */
    public Collection<StreetsideImage> search(BBox searchBox) {
        return this.images.search(searchBox);
    }
}
