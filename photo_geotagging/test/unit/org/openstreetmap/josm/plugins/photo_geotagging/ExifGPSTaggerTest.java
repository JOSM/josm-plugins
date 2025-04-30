// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.photo_geotagging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;

class ExifGPSTaggerTest {

    @TempDir
    File tempFolder;

    private static ImageEntry newImageEntry(String file, Double lat, Double lon, Instant exifTime,
                                            Double speed, Double elevation, Double imgDir) {
        ImageEntry entry = new ImageEntry(new File(file));
        entry.setPos(new LatLon(lat, lon));
        entry.setExifTime(exifTime);
        entry.setSpeed(speed);
        entry.setElevation(elevation);
        entry.setExifImgDir(imgDir);
        return entry;
    }

    @Test
    void testTicket11757() {
        final File in = new File(TestUtils.getTestDataRoot(), "_DSC1234.jpg");
        final File out = new File(tempFolder, in.getName());
        final ImageEntry image = newImageEntry("test", 12d, 34d, Instant.now(), 12.34d, Math.E, Math.PI);
        assertDoesNotThrow(() -> ExifGPSTagger.setExifGPSTag(in, out, image, true));
    }

    @Test
    void testTicket11757WriteWithoutChange() throws Exception {
        final File in = new File(TestUtils.getTestDataRoot(), "_DSC1234.jpg");
        final long lastModified = in.lastModified();
        final TiffImageMetadata exif = ((JpegImageMetadata) Imaging.getMetadata(in)).getExif();
        final TiffOutputSet outputSet = exif.getOutputSet();
        new ExifRewriter().updateExifMetadataLossless(in, new ByteArrayOutputStream(), outputSet);
        assertEquals(lastModified, in.lastModified());
    }

    @Test
    void testTicket11902() throws Exception {
        final File in = new File(TestUtils.getTestDataRoot(), "IMG_7250_small.JPG");
        final File out = new File(tempFolder, in.getName());
        final ImageEntry image = newImageEntry("test", 12d, 34d, Instant.now(), 12.34d, Math.E, Math.PI);
        ExifGPSTagger.setExifGPSTag(in, out, image, true);
        try {
            final Process jhead = Runtime.getRuntime().exec(new String[]{"jhead", out.getAbsolutePath()});
            assertEquals(jhead.getErrorStream().available(), 0);
        } catch (IOException e) { /* jhead not installed */
            System.out.println(e);
            Assumptions.assumeTrue(false);
        }
    }

    @Test
    public void testTicket24278() {
        final File in = new File(TestUtils.getTestDataRoot(), "_DSC1234.jpg");
        final File out = new File(tempFolder, in.getName());
        final ImageEntry image = newImageEntry("test", 12d, 34d, Instant.now(), 12.34d, Math.E, Math.PI);
        image.setExifGpsTrack(Math.PI);
        image.setGpsDiffMode(2);
        image.setGps2d3dMode(3);
        image.setExifGpsProcMethod("GPS");
        image.setExifHPosErr(1.2d);
        image.setExifGpsDop(2.5d);
        image.setExifGpsDatum("WGS84");
        assertDoesNotThrow(() -> ExifGPSTagger.setExifGPSTag(in, out, image, true));
        /* TODO read temp file and assertEquals EXIF metadata values */
    }
}
