// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.photo_geotagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffImageWriterLossy;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;

/**
 * Wrapper class for sanselan library
 */
public final class ExifGPSTagger {
    private ExifGPSTagger() {
        // Hide constructor
    }

    /**
     * Set the GPS values in JPEG EXIF metadata.
     * This is based on one of the examples of the sanselan project.
     *
     * @param imageFile A source image file.
     * @param dst The output file.
     * @param imageEntry the image object from Josm core
     * @param lossy whether to use lossy approach when writing metadata (overwriting unknown tags)
     * @throws IOException in case of I/O error
     * @since 36436 separate image parameters (lat, lon, gpsTime, speed, ele, imgDir), replaced by the whole ImageEntry object.
     */

    public static void setExifGPSTag(File imageFile, File dst, ImageEntry imageEntry,
            boolean lossy) throws IOException {
        try {
            setExifGPSTagWorker(imageFile, dst, imageEntry, lossy);
        } catch (ImagingException ire) {
            // This used to be two separate exceptions; ImageReadException and imageWriteException
            throw new IOException(tr("Read/write error: " + ire.getMessage()), ire);
        }
    }

    public static void setExifGPSTagWorker(File imageFile, File dst, ImageEntry imageEntry,
            boolean lossy) throws IOException {

        TiffOutputSet outputSet = null;
        ImageMetadata metadata = Imaging.getMetadata(imageFile);

        if (metadata instanceof JpegImageMetadata) {
            TiffImageMetadata exif = ((JpegImageMetadata) metadata).getExif();
            if (null != exif) {
                outputSet = exif.getOutputSet();
            }
        } else if (metadata instanceof TiffImageMetadata) {
            outputSet = ((TiffImageMetadata) metadata).getOutputSet();
        }

        if (null == outputSet) {
            outputSet = new TiffOutputSet();
        }

        TiffOutputDirectory gpsDirectory = outputSet.getOrCreateGpsDirectory();
        gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_VERSION_ID);
        gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_VERSION_ID, (byte) 2, (byte) 3, (byte) 0, (byte) 0);

        if (imageEntry.getGpsInstant() != null) {
            Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(imageEntry.getGpsInstant().toEpochMilli());

            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH) + 1;
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            final int hour = calendar.get(Calendar.HOUR_OF_DAY);
            final int minute = calendar.get(Calendar.MINUTE);
            final int second = calendar.get(Calendar.SECOND);

            DecimalFormat yearFormatter = new DecimalFormat("0000");
            DecimalFormat monthFormatter = new DecimalFormat("00");
            DecimalFormat dayFormatter = new DecimalFormat("00");

            final String yearStr = yearFormatter.format(year);
            final String monthStr = monthFormatter.format(month);
            final String dayStr = dayFormatter.format(day);
            final String dateStamp = yearStr+":"+monthStr+":"+dayStr;
            //System.err.println("date: "+dateStamp+"  h/m/s: "+hour+"/"+minute+"/"+second);

            // make sure to remove old value if present (this method will
            // not fail if the tag does not exist).
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_TIME_STAMP);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_TIME_STAMP,
                    RationalNumber.valueOf(hour),
                    RationalNumber.valueOf(minute),
                    RationalNumber.valueOf(second));

            // make sure to remove old value if present (this method will
            // not fail if the tag does not exist).
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP, dateStamp);
        }

        outputSet.setGpsInDegrees(imageEntry.getPos().lon(), imageEntry.getPos().lat());

        if (imageEntry.getSpeed() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_SPEED_REF);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_SPEED_REF,
                             GpsTagConstants.GPS_TAG_GPS_SPEED_REF_VALUE_KMPH);

            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_SPEED);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_SPEED, RationalNumber.valueOf(imageEntry.getSpeed()));
        }

        if (imageEntry.getElevation() != null) {
            byte eleRef = imageEntry.getElevation() >= 0 ? (byte) 0 : (byte) 1;
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF, eleRef);

            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(Math.abs(imageEntry.getElevation())));
        }

        if (imageEntry.getExifImgDir() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF,
                             GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF_VALUE_TRUE_NORTH);
            Double imgDir = checkAngle(imageEntry.getExifImgDir());
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION, RationalNumber.valueOf(imgDir));
        }

        if (imageEntry.getExifGpsTrack() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_TRACK_REF);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_TRACK_REF,
                             GpsTagConstants.GPS_TAG_GPS_TRACK_REF_VALUE_TRUE_NORTH);
            // make sure the value is in the range 0.0...<360.0
            Double gpsTrack = checkAngle(imageEntry.getExifGpsTrack());
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_TRACK);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_TRACK, RationalNumber.valueOf(gpsTrack));
        }

        if (imageEntry.getGpsDiffMode() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_DIFFERENTIAL);
            // make sure the gpsDiffMode value is 0 (no diffential) or 1 (differential)
            if (imageEntry.getGpsDiffMode().equals(0) || imageEntry.getGpsDiffMode().equals(1)) {
                gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_DIFFERENTIAL, imageEntry.getGpsDiffMode().shortValue());
            }
        }
        
        if (imageEntry.getGps2d3dMode() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_MEASURE_MODE);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_MEASURE_MODE, imageEntry.getGps2d3dMode().toString());
        }

        if (imageEntry.getExifGpsProcMethod() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_PROCESSING_METHOD);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_PROCESSING_METHOD, imageEntry.getExifGpsProcMethod());
        }

        if (imageEntry.getExifGpsDatum() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_MAP_DATUM);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_MAP_DATUM, imageEntry.getExifGpsDatum().toString());
        }
        
        if (imageEntry.getExifHPosErr() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_HOR_POSITIONING_ERROR);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_HOR_POSITIONING_ERROR, RationalNumber.valueOf(imageEntry.getExifHPosErr()));
        }

        if (imageEntry.getExifGpsDop() != null) {
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_DOP);
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_DOP, RationalNumber.valueOf(imageEntry.getExifGpsDop()));
        }

        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {
            if (metadata instanceof JpegImageMetadata) {
                if (lossy) {
                    new ExifRewriter().updateExifMetadataLossy(imageFile, os, outputSet);
                } else {
                    new ExifRewriter().updateExifMetadataLossless(imageFile, os, outputSet);
                }
            } else if (metadata instanceof TiffImageMetadata) {
                new TiffImageWriterLossy().write(os, outputSet);
            }
        }
    }

    /**
     * Normalizes an angle to the range [0.0, 360.0[ degrees.
     * This will fix any angle value <0 and >= 360 
     * @param angle the angle to normalize (in degrees)
     * @return the equivalent angle value in the range [0.0, 360.0[
     */
    private static Double checkAngle(Double angle) {
        if (angle < 0.0) {
            angle %= 360.0; // >-360.0...-0.0
            angle += 360.0; // >0.0...360.0
        }
        if (angle >= 360.0) {
            angle %= 360.0;
        }
        return angle;
    }
}
