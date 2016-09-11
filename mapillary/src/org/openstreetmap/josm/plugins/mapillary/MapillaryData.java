// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.mapillary.cache.CacheUtils;
import org.openstreetmap.josm.plugins.mapillary.gui.MapillaryMainDialog;

/**
 * Database class for all the {@link MapillaryAbstractImage} objects.
 *
 * @author nokutu
 * @see MapillaryAbstractImage
 * @see MapillarySequence
 */
public class MapillaryData {
  private final Set<MapillaryAbstractImage> images;
  /**
   * The image currently selected, this is the one being shown.
   */
  private MapillaryAbstractImage selectedImage;
  /**
   * The image under the cursor.
   */
  private MapillaryAbstractImage highlightedImage;
  /**
   * All the images selected, can be more than one.
   */
  private final Set<MapillaryAbstractImage> multiSelectedImages;
  /**
   * Listeners of the class.
   */
  private final List<MapillaryDataListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The bounds of the areas for which the pictures have been downloaded.
   */
  private final List<Bounds> bounds;

  /**
   * Creates a new object and adds the initial set of listeners.
   */
  protected MapillaryData() {
    this.images = Collections.newSetFromMap(new ConcurrentHashMap<MapillaryAbstractImage, Boolean>());
    this.multiSelectedImages = Collections.newSetFromMap(new ConcurrentHashMap<MapillaryAbstractImage, Boolean>());
    this.selectedImage = null;
    this.bounds = new CopyOnWriteArrayList<>();

    // Adds the basic set of listeners.
    addListener(MapillaryPlugin.getWalkAction());
    addListener(MapillaryPlugin.getZoomAction());
    addListener(MapillaryPlugin.getUploadAction());
    if (Main.main != null)
      addListener(MapillaryMainDialog.getInstance());
  }

  /**
   * Adds an MapillaryImage to the object, and then repaints mapView.
   *
   * @param image The image to be added.
   */
  public synchronized void add(MapillaryAbstractImage image) {
    add(image, true);
  }

  /**
   * Adds a MapillaryImage to the object, but doesn't repaint mapView. This is
   * needed for concurrency.
   *
   * @param image  The image to be added.
   * @param update Whether the map must be updated or not.
   * @throws NullPointerException if parameter <code>image</code> is <code>null</code>
   */
  public synchronized void add(MapillaryAbstractImage image, boolean update) {
    if (!this.images.contains(image)) {
      this.images.add(image);
    }
    if (update) {
      dataUpdated();
    }
    fireImagesAdded();
  }

  /**
   * Adds a set of MapillaryImages to the object, and then repaints mapView.
   *
   * @param images The set of images to be added.
   */
  public synchronized void add(Set<MapillaryAbstractImage> images) {
    add(images, true);
  }

  /**
   * Adds a set of {link MapillaryAbstractImage} objects to this object.
   *
   * @param images The set of images to be added.
   * @param update Whether the map must be updated or not.
   */
  public synchronized void add(Set<MapillaryAbstractImage> images, boolean update) {
    for (MapillaryAbstractImage image : images) {
      add(image, update);
    }
  }

  /**
   * Adds a new listener.
   *
   * @param lis Listener to be added.
   */
  public final void addListener(MapillaryDataListener lis) {
    this.listeners.add(lis);
  }

  /**
   * Adds a {@link MapillaryImage} object to the list of selected images, (when
   * ctrl + click)
   *
   * @param image The {@link MapillaryImage} object to be added.
   */
  public void addMultiSelectedImage(MapillaryAbstractImage image) {
    if (!this.multiSelectedImages.contains(image)) {
      if (this.getSelectedImage() == null) {
        this.setSelectedImage(image);
      } else {
        this.multiSelectedImages.add(image);
      }
    }
    if (Main.main != null)
      Main.map.mapView.repaint();
  }

  /**
   * Adds a set of {@code MapillaryAbstractImage} objects to the list of
   * selected images.
   *
   * @param images A List object containing the set of images to be added.
   */
  public void addMultiSelectedImage(Set<MapillaryAbstractImage> images) {
    images.stream().filter(image -> !this.multiSelectedImages.contains(image)).forEach(image -> {
      if (this.getSelectedImage() == null) {
        this.setSelectedImage(image);
      } else {
        this.multiSelectedImages.add(image);
      }
    });
    Main.map.mapView.repaint();
  }

  public List<Bounds> getBounds() {
    return bounds;
  }

  /**
   * Removes an image from the database. From the {@link List} in this object
   * and from its {@link MapillarySequence}.
   *
   * @param image The {@link MapillaryAbstractImage} that is going to be deleted.
   */
  public synchronized void remove(MapillaryAbstractImage image) {
    if (Main.main != null
            && MapillaryMainDialog.getInstance().getImage() != null) {
      MapillaryMainDialog.getInstance().setImage(null);
      MapillaryMainDialog.getInstance().updateImage();
    }
    setSelectedImage(null);
    this.images.remove(image);
    if (image.getSequence() != null)
      image.getSequence().remove(image);
    if (Main.main != null)
      dataUpdated();
  }

  /**
   * Removes a set of images from the database.
   *
   * @param images A {@link List} of {@link MapillaryAbstractImage} objects that are
   *               going to be removed.
   */
  public synchronized void remove(Set<MapillaryAbstractImage> images) {
    images.forEach(this::remove);
  }

  /**
   * Removes a listener.
   *
   * @param lis Listener to be removed.
   */
  public void removeListener(MapillaryDataListener lis) {
    this.listeners.remove(lis);
  }

  /**
   * Highlights the image under the cursor.
   *
   * @param image The image under the cursor.
   */
  public void setHighlightedImage(MapillaryAbstractImage image) {
    this.highlightedImage = image;
  }

  /**
   * Returns the image under the mouse cursor.
   *
   * @return The image under the mouse cursor.
   */
  public MapillaryAbstractImage getHighlightedImage() {
    return this.highlightedImage;
  }

  /**
   * Repaints mapView object.
   */
  public static synchronized void dataUpdated() {
    if (Main.main != null)
      Main.map.mapView.repaint();
  }

  /**
   * Returns a Set containing all images.
   *
   * @return A Set object containing all images.
   */
  public synchronized Set<MapillaryAbstractImage> getImages() {
    return this.images;
  }

  /**
   * Returns a Set of all sequences, that the images are part of.
   * @return
   */
  public synchronized Set<MapillarySequence> getSequences() {
    Set<MapillarySequence> result = getImages().stream().map(MapillaryAbstractImage::getSequence).collect(Collectors.toSet());
    return result;
  }

  /**
   * Returns the MapillaryImage object that is currently selected.
   *
   * @return The selected MapillaryImage object.
   */
  public MapillaryAbstractImage getSelectedImage() {
    return this.selectedImage;
  }

  private void fireImagesAdded() {
    if (this.listeners.isEmpty())
      return;
    this.listeners.stream().filter(lis -> lis != null).forEach(MapillaryDataListener::imagesAdded);
  }

  /**
   * If the selected MapillaryImage is part of a MapillarySequence then the
   * following visible MapillaryImage is selected. In case there is none, does
   * nothing.
   *
   * @throws IllegalStateException if the selected image is null or the selected image doesn't
   *                               belong to a sequence.
   */
  public void selectNext() {
    selectNext(Main.pref.getBoolean("mapillary.move-to-picture", true));
  }

  /**
   * If the selected MapillaryImage is part of a MapillarySequence then the
   * following visible MapillaryImage is selected. In case there is none, does
   * nothing.
   *
   * @param moveToPicture True if the view must me moved to the next picture.
   * @throws IllegalStateException if the selected image is null or the selected image doesn't
   *                               belong to a sequence.
   */
  public void selectNext(boolean moveToPicture) {
    if (getSelectedImage() == null)
      throw new IllegalStateException();
    if (getSelectedImage().getSequence() == null)
      throw new IllegalStateException();
    MapillaryAbstractImage tempImage = this.selectedImage;
    while (tempImage.next() != null) {
      tempImage = tempImage.next();
      if (tempImage.isVisible()) {
        setSelectedImage(tempImage, moveToPicture);
        break;
      }
    }
  }

  /**
   * If the selected MapillaryImage is part of a MapillarySequence then the
   * previous visible MapillaryImage is selected. In case there is none, does
   * nothing.
   *
   * @throws IllegalStateException if the selected image is null or the selected image doesn't
   *                               belong to a sequence.
   */
  public void selectPrevious() {
    selectPrevious(Main.pref.getBoolean("mapillary.move-to-picture", true));
  }

  /**
   * If the selected MapillaryImage is part of a MapillarySequence then the
   * previous visible MapillaryImage is selected. In case there is none, does
   * nothing. * @throws IllegalStateException if the selected image is null or
   * the selected image doesn't belong to a sequence.
   *
   * @param moveToPicture True if the view must me moved to the previous picture.
   * @throws IllegalStateException if the selected image is null or the selected image doesn't
   *                               belong to a sequence.
   */
  public void selectPrevious(boolean moveToPicture) {
    if (getSelectedImage() == null)
      throw new IllegalStateException();
    if (getSelectedImage().getSequence() == null)
      throw new IllegalStateException();
    MapillaryAbstractImage tempImage = this.selectedImage;
    while (tempImage.previous() != null) {
      tempImage = tempImage.previous();
      if (tempImage.isVisible()) {
        setSelectedImage(tempImage, moveToPicture);
        break;
      }
    }
  }

  /**
   * Selects a new image.If the user does ctrl + click, this isn't triggered.
   *
   * @param image The MapillaryImage which is going to be selected
   */
  public void setSelectedImage(MapillaryAbstractImage image) {
    setSelectedImage(image, false);
  }

  /**
   * Selects a new image.If the user does ctrl+click, this isn't triggered. You
   * can choose whether to center the view on the new image or not.
   *
   * @param image The {@link MapillaryImage} which is going to be selected.
   * @param zoom  True if the view must be centered on the image; false otherwise.
   */
  public void setSelectedImage(MapillaryAbstractImage image, boolean zoom) {
    MapillaryAbstractImage oldImage = this.selectedImage;
    this.selectedImage = image;
    this.multiSelectedImages.clear();
    if (image != null) {
      this.multiSelectedImages.add(image);
    }
    if (image != null && Main.main != null && image instanceof MapillaryImage) {
      MapillaryImage mapillaryImage = (MapillaryImage) image;
      // Downloading thumbnails of surrounding pictures.
      if (mapillaryImage.next() != null) {
        CacheUtils.downloadPicture((MapillaryImage) mapillaryImage.next());
        if (mapillaryImage.next().next() != null)
          CacheUtils.downloadPicture((MapillaryImage) mapillaryImage.next().next());
      }
      if (mapillaryImage.previous() != null) {
        CacheUtils.downloadPicture((MapillaryImage) mapillaryImage.previous());
        if (mapillaryImage.previous().previous() != null)
          CacheUtils.downloadPicture((MapillaryImage) mapillaryImage.previous().previous());
      }
    }
    if (zoom && Main.main != null)
      Main.map.mapView.zoomTo(getSelectedImage().getMovingLatLon());
    if (Main.main != null)
      Main.map.mapView.repaint();
    fireSelectedImageChanged(oldImage, this.selectedImage);
  }

  private void fireSelectedImageChanged(MapillaryAbstractImage oldImage, MapillaryAbstractImage newImage) {
    if (this.listeners.isEmpty())
      return;
    this.listeners.stream().filter(lis -> lis != null).forEach(lis -> lis.selectedImageChanged(oldImage, newImage));
  }

  /**
   * Returns a List containing all {@code MapillaryAbstractImage} objects
   * selected with ctrl + click.
   *
   * @return A List object containing all the images selected.
   */
  public Set<MapillaryAbstractImage> getMultiSelectedImages() {
    return this.multiSelectedImages;
  }

  /**
   * Sets a new ArrayList object as the used set of images.
   *
   * @param images the new image list (previously set images are completely replaced)
   */
  public synchronized void setImages(Set<MapillaryAbstractImage> images) {
    this.images.clear();
    this.images.addAll(images);
  }

  /**
   * Returns the amount of images contained by this object.
   *
   * @return The amount of images in stored.
   */
  public int size() {
    return this.images.size();
  }
}
