// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;

/**
 * Abstract superclass for all image objects. At the moment there are 3,
 * {@link StreetsideImportedImage}, {@link StreetsideImage}, & {@link StreetsideCubemap}.
 *
 * @author nokutu
 * @author renerr18
 *
 */
public abstract class StreetsideAbstractImage implements Comparable<StreetsideAbstractImage> {
	/**
	 * If two values for field cd differ by less than EPSILON both values are
	 * considered equal.
	 */
	private static final float EPSILON = 1e-5f;

	protected String id;

	private long ne;
  private long pr;


	/** The time the image was captured, in Epoch format. */
	protected long cd;
	/** Sequence of pictures containing this object. */
	private StreetsideSequence sequence;

	/** Position of the picture. */
	protected LatLon latLon;
	/** Direction of the picture. */
	protected double he;
	/** Temporal position of the picture until it is uploaded. */
	private LatLon tempLatLon;
	/**
	 * When the object is being dragged in the map, the temporal position is stored
	 * here.
	 */
	private LatLon movingLatLon;
	/** Temporal direction of the picture until it is uploaded */
	private double tempHe;
	/**
	 * When the object direction is being moved in the map, the temporal direction
	 * is stored here
	 */
	protected double movingHe;
	/** Whether the image must be drown in the map or not */
	private boolean visible;

	/**
	 * Creates a new object in the given position and with the given direction.
	 * {@link LatLon}
	 *
	 * @param id - the Streetside image id
	 *
	 * @param latLon
	 *            The latitude and longitude of the image.
	 * @param he
	 *            The direction of the picture (0 means north im Mapillary
	 *            camera direction is not yet supported in the Streetside plugin).
	 */
	protected StreetsideAbstractImage(final String id, final LatLon latLon, final double he) {
		this.id = id;
		this.latLon = latLon;
		tempLatLon = this.latLon;
		movingLatLon = this.latLon;
		this.he = he;
		tempHe = he;
		movingHe = he;
		visible = true;
	}

	/**
	 * Creates a new object with the given id.
	 *
	 * @param id - the image id (All images require ids in Streetside)
	 */
	protected StreetsideAbstractImage(final String id) {
		this.id = id;

		visible = true;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the original direction towards the image has been taken.
	 *
	 * @return The direction of the image (0 means north and goes clockwise).
	 */
	public double getHe() {
		return he;
	}

	/**
	 * Returns the Epoch time when the image was captured.
	 *
	 * @return The long containing the Epoch time when the image was captured.
	 */
	public long getCd() {
		return cd;
	}

	/**
	 * Returns the date the picture was taken in DMY format.
	 *
	 * @return A String object containing the date when the picture was taken.
	 */
	public String getDate() {
		final StringBuilder format = new StringBuilder(26);
		format.append("m/d/YYYY");
		if (StreetsideProperties.DISPLAY_HOUR.get()) {
			if (StreetsideProperties.TIME_FORMAT_24.get()) {
				format.append(" - HH:mm:ss");
			} else {
				format.append(" - h:mm:ss a");
			}
		}
		return getDate(format.toString());
	}

	/**
	 * Returns the date the picture was taken in the given format.
	 *
	 * @param format
	 *            Format of the date. See {@link SimpleDateFormat}.
	 * @return A String containing the date the picture was taken using the given
	 *         format.
	 * @throws NullPointerException
	 *             if parameter format is <code>null</code>
	 */
	public String getDate(String format) {
		final Date date = new Date(getCd());
		final SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.US);
		formatter.setTimeZone(Calendar.getInstance().getTimeZone());
		return formatter.format(date);
	}

	/**
	 * Returns a LatLon object containing the original coordinates of the object.
	 *
	 * @return The LatLon object with the position of the object.
	 */
	public LatLon getLatLon() {
		return latLon;
	}

	/**
	 * Returns the direction towards the image has been taken.
	 *
	 * @return The direction of the image (0 means north and goes clockwise).
	 */
	public double getMovingHe() {
		return movingHe;
	}

	/**
	 * Returns a LatLon object containing the current coordinates of the object.
	 * When you are dragging the image this changes.
	 *
	 * @return The LatLon object with the position of the object.
	 */
	public LatLon getMovingLatLon() {
		return movingLatLon;
	}

	/**
	 * Returns the sequence which contains this image. Never null.
	 *
	 * @return The StreetsideSequence object that contains this StreetsideImage.
	 */

	public StreetsideSequence getSequence() {
		synchronized (this) {
			if (sequence == null) {
				sequence = new StreetsideSequence();
				sequence.add(this);
			}
			return sequence;
		}
	}

	/**
	 * Returns the last fixed direction of the object.
	 *
	 * @return The last fixed direction of the object. 0 means north.
	 */
	public double getTempHe() {
		return tempHe;
	}

	/**
	 * Returns the last fixed coordinates of the object.
	 *
	 * @return A LatLon object containing.
	 */
	public LatLon getTempLatLon() {
		return tempLatLon;
	}

	/**
	 * Returns whether the object has been modified or not.
	 *
	 * @return true if the object has been modified; false otherwise.
	 */
	public boolean isModified() {
		return !getMovingLatLon().equals(latLon) || Math.abs(getMovingHe() - cd) > EPSILON;
	}

	/**
	 * Returns whether the image is visible on the map or not.
	 *
	 * @return True if the image is visible; false otherwise.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Moves the image temporally to another position
	 *
	 * @param x
	 *            The movement of the image in longitude units.
	 * @param y
	 *            The movement of the image in latitude units.
	 */
	public void move(final double x, final double y) {
		movingLatLon = new LatLon(tempLatLon.getY() + y, tempLatLon.getX() + x);
	}

	/**
	 * If the StreetsideImage belongs to a StreetsideSequence, returns the next
	 * image in the sequence.
	 *
	 * @return The following StreetsideImage, or null if there is none.
	 */
	public StreetsideAbstractImage next() {
		synchronized (this) {
			return getSequence().next(this);
		}
	}

	/**
	 * If the StreetsideImage belongs to a StreetsideSequence, returns the previous
	 * image in the sequence.
	 *
	 * @return The previous StreetsideImage, or null if there is none.
	 */
	public StreetsideAbstractImage previous() {
		synchronized (this) {
			return getSequence().previous(this);
		}
	}

	public void setHe(final double he) {
		this.he = he;
	}

	/**
	 * Sets the Epoch time when the picture was captured.
	 *
	 * @param cd
	 *            Epoch time when the image was captured.
	 */
	public synchronized void setCd(final long cd) {
		this.cd = cd;
	}

	public void setLatLon(final LatLon latLon) {
		if (latLon != null) {
			this.latLon = latLon;
		}
	}

	/**
	 * Sets the StreetsideSequence object which contains the StreetsideImage.
	 *
	 * @param sequence
	 *            The StreetsideSequence that contains the StreetsideImage.
	 * @throws IllegalArgumentException
	 *             if the image is not already part of the
	 *             {@link StreetsideSequence}. Call
	 *             {@link StreetsideSequence#add(StreetsideAbstractImage)} first.
	 */
	public void setSequence(final StreetsideSequence sequence) {
		synchronized (this) {
			if (sequence != null && !sequence.getImages().contains(this)) {
				throw new IllegalArgumentException();
			}
			this.sequence = sequence;
		}
	}

	/**
	 * Set's whether the image should be visible on the map or not.
	 *
	 * @param visible
	 *            true if the image is set to be visible; false otherwise.
	 */
	public void setVisible(final boolean visible) {
		this.visible = visible;
	}

	/**
	 * Called when the mouse button is released, meaning that the picture has
	 * stopped being dragged, so the temporal values are saved.
	 */
	public void stopMoving() {
		tempLatLon = movingLatLon;
		tempHe = movingHe;
	}

	/**
	 * Turns the image direction.
	 *
	 * @param ca
	 *            The angle the image is moving.
	 */
	public void turn(final double ca) {
		movingHe = tempHe + ca;
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

}