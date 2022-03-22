package org.openstreetmap.josm.plugins.photo_geotagging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Instant;
import java.util.Scanner;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openstreetmap.josm.TestUtils;

class ExifGPSTaggerTest {

    @TempDir
    File tempFolder;

    @Test
    void testTicket11757() {
        final File in = new File(TestUtils.getTestDataRoot(), "_DSC1234.jpg");
        final File out = new File(tempFolder, in.getName());
        assertDoesNotThrow(() -> ExifGPSTagger.setExifGPSTag(in, out, 12, 34, Instant.now(), 12.34, Math.E, Math.PI, true));
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
    @Disabled("To enable after https://josm.openstreetmap.de/ticket/11902 is fixed")
    void testTicket11902() throws Exception {
        final File in = new File(TestUtils.getTestDataRoot(), "IMG_7250_small.JPG");
        final File out = new File(tempFolder, in.getName());
        ExifGPSTagger.setExifGPSTag(in, out, 12, 34, Instant.now(), 12.34, Math.E, Math.PI, false);
        final Process jhead = Runtime.getRuntime().exec(new String[]{"jhead", out.getAbsolutePath()});
        final String stdout = new Scanner(jhead.getErrorStream()).useDelimiter("\\A").next();
        System.out.println(stdout);
        assertFalse(stdout.contains("Suspicious offset of first Exif IFD value"));
    }
}
