// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.time.Instant;
import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * A StreetsideImage object represents each of the images stored in Streetside.
 *
 * @param id The unique id for the cubemap
 * @param lat The latitude of the image
 * @param lon The longitude of the image
 * @param heading The direction of the images in degrees, meaning 0 north (not yet supported)
 * @param pitch The pitch of the image
 * @param roll The roll of the image
 * @param vintageEnd The date/time that the image was taken
 * @param vintageStart The date/time that the image was taken
 * @param logo The logo to show for this image
 * @param copyright The copyright to show for this image
 * @param zoomMax The maximum zoom for this image
 * @param zoomMin The minimum zoom for this image
 * @param imageHeight The height for this image tiles
 * @param imageWidth The width for this image tiles
 * @param imageUrlSubdomains The subdomains for this image
 *
 * @author nokutu
 * @author renerr18
 *
 * @see StreetsideData
 */
public record StreetsideImage(String id, double lat, double lon, double heading, double pitch, double roll,
                              Instant vintageStart, Instant vintageEnd, String logo, String copyright,
                              int zoomMin, int zoomMax, int imageHeight, int imageWidth,
                              List<String> imageUrlSubdomains) implements StreetsideAbstractImage {
    public StreetsideImage {
        if (lat > 90 || lat < -90) throw new IllegalArgumentException("Invalid latitude: " + lat);
        if (lon > 180 || lon < -180) throw new IllegalArgumentException("Invalid longitude: " + lon);
        if (pitch > 360 || pitch < -360) throw new IllegalArgumentException("Invalid pitch: " + pitch); // Is this radians or degrees?
        if (roll > 360 || roll < -360) throw new IllegalArgumentException("Invalid roll: " + roll); // Is this radians or degrees?
    }

    @Override
    public int compareTo(@Nonnull StreetsideAbstractImage o) {
        if (o instanceof StreetsideImage other) {
            if (this.vintageStart.compareTo(other.vintageStart) != 0) {
                return this.vintageStart.compareTo(other.vintageStart);
            }
            if (this.vintageEnd.compareTo(other.vintageEnd) != 0) {
                return this.vintageEnd.compareTo(other.vintageEnd);
            }
            if (this.id.compareTo(o.id()) != 0) {
                return this.id.compareTo(o.id());
            }
            // Fine. Fall back to all the doubles.
            return (int) Math.round((this.lat + this.lon + this.heading + this.pitch + this.roll) -
                    (other.lat + other.lon + other.heading + other.pitch + other.roll));
        }
        return this.hashCode() - o.hashCode();
    }
}
