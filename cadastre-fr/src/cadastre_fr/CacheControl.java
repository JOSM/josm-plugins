// License: GPL. v2 and later. Copyright 2008-2009 by Pieren <pieren3@gmail.com> and others
package cadastre_fr;
/**
 * This class handles the WMS layer cache mechanism. The design is oriented for a good performance (no
 * wait status on GUI, fast saving even in big file). A separate thread is created for each WMS
 * layer to not suspend the GUI until disk I/O is terminated (a file for the cache can take
 * several MB's). If the cache file already exists, new images are just appended to the file
 * (performance). Since we use the ObjectStream methods, it is required to modify the standard
 * ObjectOutputStream in order to have objects appended readable (otherwise a stream header
 * is inserted before each append and an exception is raised at objects read).
 */

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.LambertCC9Zones;
import org.openstreetmap.josm.data.projection.UTM_20N_France_DOM;

public class CacheControl implements Runnable {
    
    public static final String cLambertCC9Z = "CC";

    public static final String cUTM20N = "UTM";

    public class ObjectOutputStreamAppend extends ObjectOutputStream {
        public ObjectOutputStreamAppend(OutputStream out) throws IOException {
            super(out);
        }
        protected void writeStreamHeader() throws IOException {
            reset();
        }
    }

    public static boolean cacheEnabled = true;

    public static int cacheSize = 500;


    public WMSLayer wmsLayer = null;

    private ArrayList<GeorefImage> imagesToSave = new ArrayList<GeorefImage>();
    private Lock imagesLock = new ReentrantLock();

    public CacheControl(WMSLayer wmsLayer) {
        cacheEnabled = Main.pref.getBoolean("cadastrewms.enableCaching", true);
        this.wmsLayer = wmsLayer;
        try {
            cacheSize = Integer.parseInt(Main.pref.get("cadastrewms.cacheSize", String.valueOf(CadastrePreferenceSetting.DEFAULT_CACHE_SIZE)));
        } catch (NumberFormatException e) {
            cacheSize = CadastrePreferenceSetting.DEFAULT_CACHE_SIZE;
        }
        File path = new File(CadastrePlugin.cacheDir);
        if (!path.exists())
            path.mkdirs();
        else // check directory capacity
            checkDirSize(path);
        new Thread(this).start();
    }

    private void checkDirSize(File path) {
        long size = 0;
        long oldestFileDate = Long.MAX_VALUE;
        int oldestFile = 0;
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            size += files[i].length();
            if (files[i].lastModified() <  oldestFileDate) {
                oldestFile = i;
                oldestFileDate = files[i].lastModified();
            }
        }
        if (size > cacheSize*1024*1024) {
            System.out.println("Delete oldest file  \""+ files[oldestFile].getName()
                    + "\" in cache dir to stay under the limit of " + cacheSize + " MB.");
            files[oldestFile].delete();
            checkDirSize(path);
        }
    }

    public boolean loadCacheIfExist() {
        try {
            String extension = String.valueOf((wmsLayer.getLambertZone() + 1));
            if (Main.proj instanceof LambertCC9Zones)
                extension = cLambertCC9Z + extension;
            else if (Main.proj instanceof UTM_20N_France_DOM)
                extension = cUTM20N + extension;
            File file = new File(CadastrePlugin.cacheDir + wmsLayer.getName() + "." + extension);
            if (file.exists()) {
                JOptionPane pane = new JOptionPane(
                        tr("Location \"{0}\" found in cache.\n"+
                        "Load cache first ?\n"+
                        "(No = new cache)", wmsLayer.getName()),
                        JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null);
                // this below is a temporary workaround to fix the "always on top" issue
                JDialog dialog = pane.createDialog(Main.parent, tr("Select Feuille"));
                CadastrePlugin.prepareDialog(dialog);
                dialog.setVisible(true);
                int reply = (Integer)pane.getValue();
                // till here

                if (reply == JOptionPane.OK_OPTION && loadCache(file, wmsLayer.getLambertZone())) {
                    return true;
                } else {
                    delete(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

    public void deleteCacheFile() {
        try {
            String extension = String.valueOf((wmsLayer.getLambertZone() + 1));
            if (Main.proj instanceof LambertCC9Zones)
                extension = cLambertCC9Z + extension;
            delete(new File(CadastrePlugin.cacheDir + wmsLayer.getName() + "." + extension));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    
    private void delete(File file) {
        System.out.println("Delete file "+file);
        if (file.exists())
            file.delete();
        while (file.exists()) // wait until file is really gone (otherwise appends to existing one)
            CadastrePlugin.safeSleep(500);
    }

    public boolean loadCache(File file, int currentLambertZone) {
        boolean successfulRead = false;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            successfulRead = wmsLayer.read(ois, currentLambertZone);
            ois.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            JOptionPane.showMessageDialog(Main.parent, tr("Error loading file.\nProbably an old version of the cache file."), tr("Error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (successfulRead && wmsLayer.isRaster()) {
            // serialized raster bufferedImage hangs-up on Java6. Recreate them here
            wmsLayer.images.get(0).image = RasterImageModifier.fixRasterImage(wmsLayer.images.get(0).image);
        }
        return successfulRead;
    }


    public synchronized void saveCache(GeorefImage image) {
        imagesLock.lock();
        this.imagesToSave.add(image);
        this.notify();
        imagesLock.unlock();
    }

    /**
     * Thread saving the grabbed images in background.
     */
    public synchronized void run() {
        for (;;) {
            imagesLock.lock();
            // copy locally the images to save for better performance
            ArrayList<GeorefImage> images = new ArrayList<GeorefImage>(imagesToSave);
            imagesToSave.clear();
            imagesLock.unlock();
            if (images != null && !images.isEmpty()) {
                String extension = String.valueOf((wmsLayer.getLambertZone() + 1));
                if (Main.proj instanceof LambertCC9Zones)
                    extension = cLambertCC9Z + extension;
                else if (Main.proj instanceof UTM_20N_France_DOM)
                    extension = cUTM20N + extension;
                File file = new File(CadastrePlugin.cacheDir + wmsLayer.getName() + "." + extension);
                try {
                    if (file.exists()) {
                        ObjectOutputStreamAppend oos = new ObjectOutputStreamAppend(
                                new BufferedOutputStream(new FileOutputStream(file, true)));
                        for (GeorefImage img : images) {
                            oos.writeObject(img);
                        }
                        oos.close();
                    } else {
                        ObjectOutputStream oos = new ObjectOutputStream(
                                new BufferedOutputStream(new FileOutputStream(file)));
                        wmsLayer.write(oos);
                        for (GeorefImage img : images) {
                            oos.writeObject(img);
                        }
                        oos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
            try {wait();} catch (InterruptedException e) {
                e.printStackTrace(System.out);
            }
        }
    }
}
