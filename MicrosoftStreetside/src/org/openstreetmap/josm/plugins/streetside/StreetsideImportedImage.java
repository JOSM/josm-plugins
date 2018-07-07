// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideUtils;

/**
 * A StreetsideImoprtedImage object represents a picture imported locally.
 *
 * @author nokutu
 *
 */
public class StreetsideImportedImage extends StreetsideAbstractImage {

  /** The picture file. */
  protected File file;

  /**
   * Creates a new StreetsideImportedImage object using as date the current date.
   * Using when the EXIF tags doesn't contain that info.
   *
   * @param id  The Streetside image id
   * @param latLon  The latitude and longitude where the picture was taken.
   * @param ca  Direction of the picture (0 means north).
   * @param file  The file containing the picture.
   */
  public StreetsideImportedImage(final String id, final LatLon latLon, final double ca, final File file) {
    this(id, latLon, ca, file, Calendar.getInstance().getTimeInMillis());
  }

  /**
   * Main constructor of the class.
   *
   * @param id  The Streetside image id
   * @param latLon  Latitude and Longitude where the picture was taken.
   * @param ca  Direction of the picture (0 means north),
   * @param file  The file containing the picture.
   * @param datetimeOriginal  The date the picture was taken.
   */
  public StreetsideImportedImage(final String id, final LatLon latLon, final double ca, final File file, final String datetimeOriginal) {
    this(id, latLon, ca, file, parseTimestampElseCurrentTime(datetimeOriginal));
  }

  /**
   * Constructs a new image from an image entry of a {@link GeoImageLayer}.
   * @param geoImage the {@link ImageEntry}, from which the corresponding fields are taken
   * @return new image
   */
  public static StreetsideImportedImage createInstance(final ImageEntry geoImage) {
    if (geoImage == null) {
      return null;
    }
    if (geoImage.getFile() == null) {
      throw new IllegalArgumentException("Can't create an imported image from an ImageEntry without associated file.");
    }
    final CachedLatLon cachedCoord = geoImage.getPos();
    LatLon coord = cachedCoord == null ? null : cachedCoord.getRoundedToOsmPrecision();
    if (coord == null) {
      final MapView mv = StreetsidePlugin.getMapView();
      coord = mv == null ? new LatLon(0, 0) : mv.getProjection().eastNorth2latlon(mv.getCenter());
    }
    final double ca = geoImage.getExifImgDir() == null ? 0 : geoImage.getExifImgDir();
    final long time = geoImage.hasGpsTime()
      ? geoImage.getGpsTime().getTime()
      : geoImage.hasExifTime() ? geoImage.getExifTime().getTime() : System.currentTimeMillis();
    return new StreetsideImportedImage(CubemapUtils.IMPORTED_ID, coord, ca, geoImage.getFile(), time);
  }

  private static long parseTimestampElseCurrentTime(final String timestamp) {
    try {
      return StreetsideUtils.getEpoch(timestamp, "yyyy:MM:dd HH:mm:ss");
    } catch (ParseException e) {
      try {
        return StreetsideUtils.getEpoch(timestamp, "yyyy/MM/dd HH:mm:ss");
      } catch (ParseException e1) {
        return StreetsideUtils.currentTime();
      }
    }
  }

  public StreetsideImportedImage(final String id, final LatLon latLon, final double he, final File file, final long ca) {
    super(id, latLon, he);
    this.file = file;
    this.cd = ca;
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
    if (file != null)
      return ImageIO.read(file);
    return null;
  }

  /**
   * Returns the {@link File} object where the picture is located.
   *
   * @return The {@link File} object where the picture is located.
   */
  public File getFile() {
    return file;
  }

  @Override
  public int compareTo(StreetsideAbstractImage image) {
    if (image instanceof StreetsideImportedImage)
      return file.compareTo(((StreetsideImportedImage) image).getFile());
    return hashCode() - image.hashCode();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((file == null) ? 0 : file.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof StreetsideImportedImage)) {
      return false;
    }
    StreetsideImportedImage other = (StreetsideImportedImage) obj;
    if (file == null) {
      if (other.file != null) {
        return false;
      }
    } else if (!file.equals(other.file)) {
      return false;
    }
    return true;
  }
}
