// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.mapillary.utils.MapillaryUtils;

/**
 * A MapillaryImoprtedImage object represents a picture imported locally.
 *
 * @author nokutu
 *
 */
public class MapillaryImportedImage extends MapillaryAbstractImage {

  /** The picture file. */
  protected File file;
  /** The date when the picture was taken. */

  /**
   * Creates a new MapillaryImportedImage object using as date the current date.
   * Using when the EXIF tags doesn't contain that info.
   *
   * @param latLon  The latitude and longitude where the picture was taken.
   * @param ca  Direction of the picture (0 means north).
   * @param file  The file containing the picture.
   */
  public MapillaryImportedImage(final LatLon latLon, final double ca, final File file) {
    this(latLon, ca, file, MapillaryUtils.currentDate());
  }

  /**
   * Main constructor of the class.
   *
   * @param latLon  Latitude and Longitude where the picture was taken.
   * @param ca  Direction of the picture (0 means north),
   * @param file  The file containing the picture.
   * @param datetimeOriginal  The date the picture was taken.
   */
  public MapillaryImportedImage(final LatLon latLon, final double ca, final File file, final String datetimeOriginal) {
    super(latLon, ca);
    this.file = file;
    try {
      this.capturedAt = MapillaryUtils.getEpoch(datetimeOriginal, "yyyy:MM:dd HH:mm:ss");
    } catch (ParseException e) {
      try {
        this.capturedAt = MapillaryUtils.getEpoch(datetimeOriginal, "yyyy/MM/dd HH:mm:ss");
      } catch (ParseException e1) {
        this.capturedAt = MapillaryUtils.currentTime();
      }
    }
  }

  /**
   * Returns the pictures of the file.
   *
   * @return A {@link BufferedImage} object containing the picture, or null if
   *         the {@link File} given in the constructor was null.
   * @throws IOException
   *           If the file parameter of the object isn't an image.
   */
  public BufferedImage getImage() throws IOException {
    if (this.file != null)
      return ImageIO.read(this.file);
    return null;
  }

  /**
   * Returns the {@link File} object where the picture is located.
   *
   * @return The {@link File} object where the picture is located.
   */
  public File getFile() {
    return this.file;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other.getClass() == this.getClass() && this.file.equals(((MapillaryImportedImage) other).file);
  }

  @Override
  public int compareTo(MapillaryAbstractImage image) {
    if (image instanceof MapillaryImportedImage)
      return this.file.compareTo(((MapillaryImportedImage) image).getFile());
    return hashCode() - image.hashCode();
  }

  @Override
  public int hashCode() {
    return this.file.hashCode();
  }
}
