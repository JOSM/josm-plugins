// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.export;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.tools.Logging;

/**
 * Writes the images from the queue in the file system.
 *
 * @author nokutu
 * @see StreetsideExportManager
 */
public class StreetsideExportWriterThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(StreetsideExportWriterThread.class.getCanonicalName());

    private final ArrayBlockingQueue<BufferedImage> queue;
    private final ArrayBlockingQueue<StreetsideAbstractImage> queueImages;
    private final int amount;
    private final ProgressMonitor monitor;

    /**
     * Main constructor.
     *
     * @param ignored    Path to write the pictures.
     * @param queue     Queue of {@link StreetsideAbstractImage} objects.
     * @param queueImages Queue of {@link BufferedImage} objects.
     * @param amount    Amount of images that are going to be exported.
     * @param monitor   Progress monitor.
     */
    public StreetsideExportWriterThread(String ignored, ArrayBlockingQueue<BufferedImage> queue,
            ArrayBlockingQueue<StreetsideAbstractImage> queueImages, int amount, ProgressMonitor monitor) {
        this.queue = queue;
        this.queueImages = queueImages;
        this.amount = amount;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        monitor.setCustomText("Downloaded 0/" + amount);
        BufferedImage img;
        StreetsideAbstractImage mimg;
        var finalPath = "";
        for (var i = 0; i < amount; i++) {
            try {
                img = queue.take();
                mimg = queueImages.take();

                // Transforms the image into a byte array.
                final var outputStream = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                // Write EXIF tags
                final var outputSet = new TiffOutputSet();
                TiffOutputDirectory exifDirectory;
                TiffOutputDirectory gpsDirectory;

                exifDirectory = outputSet.getOrCreateExifDirectory();
                gpsDirectory = outputSet.getOrCreateGpsDirectory();

                gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF);
                gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF,
                        GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION_REF_VALUE_TRUE_NORTH);

                gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION);
                gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION, RationalNumber.valueOf(mimg.heading()));

                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

                outputSet.setGpsInDegrees(mimg.lon(), mimg.lat());
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(finalPath + ".jpg"))) {
                    new ExifRewriter().updateExifMetadataLossless(imageBytes, os, outputSet);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Streetside export cancelled");
                return;
            } catch (IOException e) {
                LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
            }

            // Increases the progress bar.
            monitor.worked(PleaseWaitProgressMonitor.PROGRESS_BAR_MAX / amount);
            monitor.setCustomText("Downloaded " + (i + 1) + "/" + amount);
        }
    }
}
