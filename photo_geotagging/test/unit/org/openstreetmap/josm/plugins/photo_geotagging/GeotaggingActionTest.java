package org.openstreetmap.josm.plugins.photo_geotagging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.plugins.photo_geotagging.GeotaggingAction.GeoTaggingRunnable;

public class GeotaggingActionTest {

    @Test
    public void testProcessEntries() throws IOException {
        File original = new File(TestUtils.getTestDataRoot(), "_DSC1234.jpg");
        assertTrue(original.exists());

        File copy = new File(TestUtils.getTestDataRoot(), "_DSC1234.copy.jpg");
        if (copy.exists()) copy.delete();
        //this method will actually override the file, so use a copy:
        Files.copy(original.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ImageEntry entry = new ImageEntry(copy);
        entry.setPos(new LatLon(1, 2));
        List<ImageEntry> list = Arrays.asList(entry);

        GeoTaggingRunnable runnable = new GeotaggingAction.GeoTaggingRunnable(list, true, 0);
        //this causes some warnings from the PleaseWaitRunnable because not all resources are available
        //but that's irrelevant to the test
        runnable.getProgressMonitor().beginTask("test");
        runnable.override_backup = true;
        assertEquals(1, runnable.processEntries(list, false).size());
        assertEquals(0, runnable.processEntries(list, true).size());
        //test if overriding backup works:
        assertEquals(0, runnable.processEntries(list, true).size());

        runnable = new GeotaggingAction.GeoTaggingRunnable(list, false, 0);
        runnable.getProgressMonitor().beginTask("test");
        //file is now "repaired" from operation above and lossless writing should work:
        assertEquals(0, runnable.processEntries(list, false).size());
        assertEquals(0, runnable.processEntries(list, true).size());

        File backup = new File(TestUtils.getTestDataRoot(), "_DSC1234.copy.jpg_");
        assertTrue(backup.exists());
        backup.delete();
        copy.delete();
    }
}
