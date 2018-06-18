// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.model.UserProfile;

/**
 * A StreetsideImage object represents each of the images stored in Streetside.
 *
 * @author nokutu
 * @author renerr18
 *
 * @see StreetsideSequence
 * @see StreetsideData
 */
public class StreetsideImage extends StreetsideAbstractImage {
  /**
   * Rn is a Bing Streetside image attribute - currently not
   * used, mapped or supported in the Streetside plugin -
   * left out initially because it's an unrequired complex object.
   */
  public static class Rn {
	  // placeholder for nexted Rn attribute
  }

  // latitude of the Streetside image
  private double la;

  //longitude of the Streetside image
  private double lo;

  // The bubble altitude, in meters above the WGS84 ellipsoid
  private double al;

  // Roll
  private double ro;

  // Pitch
  private double pi;

  // Heading (equivalent to Mapillary cd attribute - not currently supported.
  private double he;

  // Blurring instructions - not currently used by the plugin
  private String bl;

  // Undocumented Attributes
  private int ml;
  private long ne;
  private long pr;
  private List<String> nbn;
  private List<String> pbn;
  private int ad;
  private Rn rn;

  /**
   * Set of traffic signs in the image.
   *//*
  private final List<ImageDetection> detections = Collections.synchronizedList(new ArrayList<>());
*/
  /**
   * Main constructor of the class StreetsideImage
   *
   * @param key    The unique identifier of the image.
   * @param latLon The latitude and longitude where it is positioned.
   * @param cd     The direction of the images in degrees, meaning 0 north.
   */

  public StreetsideImage(String id, LatLon latLon, double he) {
    super(id, latLon, he);
  }

  public StreetsideImage(String id, LatLon latLon) {
    super(id, latLon, 0.0);
  }

  public StreetsideImage(String id, double la, double lo) {
    super(id, new LatLon(la,lo), 0.0);
  }

  public StreetsideImage(String id) {
	    super(id);
  }

  // Default constructor for Jackson/JSON Deserializattion
  public StreetsideImage() {
    super(CubemapUtils.IMPORTED_ID, null, 0.0);
  }

  /**
   * Returns the unique identifier of the object.
   *
   * @return A {@code String} containing the unique identifier of the object.
   */
  @Override
public String getId() {
    return String.valueOf(id);
  }

  /*public List<ImageDetection> getDetections() {
    return detections;
  }*/

  /*public void setAllDetections(Collection<ImageDetection> newDetections) {
    Logging.debug("Add {0} detections to image {1}", newDetections.size(), getId());
    synchronized (detections) {
      detections.clear();
      detections.addAll(newDetections);
    }
  }*/

  public UserProfile getUser() {
	    return getSequence().getUser();
  }

  @Override
  public String toString() {
    return String.format(
      // TODO: format date cd (Gradle build error command line)
      "Image[id=%s,lat=%f,lon=%f,he=%f,user=%s]",
      id, latLon.lat(), latLon.lon(), he, "null"//, cd
    );
  }

  // TODO: implement equals @rrh
  @Override
  public boolean equals(Object object) {
    return object instanceof StreetsideImage && id.equals(((StreetsideImage) object).getId());
  }

  // TODO: implement compareTo @rrh
  @Override
  public int compareTo(StreetsideAbstractImage image) {
    if (image instanceof StreetsideImage) {
      return id.compareTo(((StreetsideImage) image).getId());
    }
    return hashCode() - image.hashCode();
  }

  // TODO: implement hashcode @rrh
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public void stopMoving() {
    super.stopMoving();
    checkModified();
  }

  private void checkModified() {
    if (StreetsideLayer.hasInstance()) {
      if (isModified()) {
        StreetsideLayer.getInstance().getLocationChangeset().add(this);
      } else {
        StreetsideLayer.getInstance().getLocationChangeset().remove(this);
      }
    }
  }

  @Override
  public void turn(double ca) {
    super.turn(ca);
    checkModified();
  }

  /**
   * @return the altitude
   */
  public double getAl() {
    return al;
  }

  /**
   * @param altitude the altitude to set
   */
  public void setAl(double altitude) {
    al = altitude;
  }

  /**
   * @return the roll
   */
  public double getRo() {
    return ro;
  }

  /**
   * @param roll the roll to set
   */
  public void setRo(double roll) {
    ro = roll;
  }

  /**
   * @return the pi
   */
  public double getPi() {
    return pi;
  }

  /**
   * @param pi the pi to set
   */
  public void setPi(double pitch) {
    pi = pitch;
  }

  /**
   * @return the burringl
   */
  public String getBl() {
    return bl;
  }

  /**
   * @param burringl the burringl to set
   */
  public void setBl(String blurring) {
    bl = blurring;
  }

  /**
   * @return the ml
   */
  public int getMl() {
    return ml;
  }

  /**
   * @param ml the ml to set
   */
  public void setMl(int ml) {
    this.ml = ml;
  }

  /**
   * @return the ne
   */
  public long getNe() {
    return ne;
  }

  /**
   * @param ne the ne to set
   */
  public void setNe(long ne) {
    this.ne = ne;
  }

  /**
   * @return the pr
   */
  public long getPr() {
    return pr;
  }

  /**
   * @param pr the pr to set
   */
  public void setPr(long pr) {
    this.pr = pr;
  }

  /**
   * @return the nbn
   */
  public List<String> getNbn() {
    return nbn;
  }

  /**
   * @param nbn the nbn to set
   */
  public void setNbn(List<String> nbn) {
    this.nbn = nbn;
  }

  /**
   * @return the pbn
   */
  public List<String> getPbn() {
    return pbn;
  }

  /**
   * @param pbn the pbn to set
   */
  public void setPbn(List<String> pbn) {
    this.pbn = pbn;
  }

  /**
   * @return the ad
   */
  public int getAd() {
    return ad;
  }

  /**
   * @param ad the ad to set
   */
  public void setAd(int ad) {
    this.ad = ad;
  }

  /**
   * @return the la
   */
  public double getLa() {
    return la;
  }

  /**
   * @param la the la to set
   */
  public void setLa(double la) {
    this.la = la;
  }

  /**
   * @return the lo
   */
  public double getLo() {
    return lo;
  }

  /**
   * @param lo the lo to set
   */
  public void setLo(double lo) {
    this.lo = lo;
  }

  /**
   * @param id the id to set
   */
  @Override
public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the rn
   */
  public Rn getRn() {
    return rn;
  }

  /**
   * @param rn the rn to set
   */
  public void setRn(Rn rn) {
    this.rn = rn;
  }
}