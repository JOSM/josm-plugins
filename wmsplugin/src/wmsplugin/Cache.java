package wmsplugin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.*;

import org.openstreetmap.josm.Main;

public class Cache {
    final File dir;
    // Last 5 days
    private final long expire = Main.pref.getInteger("wmsplugin.cache.expire", 1000*60*60*24*5);
    // 40 MBytes
    private final long maxsize = Main.pref.getInteger("wmsplugin.cache.size", 1000*1000*40);
    private boolean disabled = false;
    // If the cache is full, we don't want to delete just one file
    private final int cleanUpThreshold = 20;

    Cache() {
        this(Main.pref.get("wmsplugin.cache.path", WMSPlugin.getPrefsPath() + "cache"));
    }

    Cache(String working_dir) {
        // Override default working directory
        this.dir = new File(working_dir);
        try {
            this.dir.mkdirs();
        } catch(Exception e) {
            // We probably won't be able to do anything anyway
            disabled = true;
        }

        if(expire <= 0 || maxsize <= 0)
            disabled = true;
    }

    public BufferedImage getImg(String ident) {
        if(disabled) return null;
        try {
            File img = getPath(ident);
            if(!img.exists()) {
                //System.out.println("Miss");
                return null;
            }
            if(img.lastModified() < (new Date().getTime() - expire)) {
                img.delete();
                //System.out.println("Miss");
                return null;
            }
            //System.out.println("Hit");
            return ImageIO.read(img);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        //System.out.println("Miss");
        return null;
    }

    public void saveImg(String ident, BufferedImage image) {
        if(disabled) return;
        try {
            ImageIO.write(image, "png", getPath(ident));
        } catch(Exception e){
            System.out.println(e.getMessage());
        }

        // Clean up must be called manually
    }

    public BufferedImage saveImg(String ident, BufferedImage image, boolean passThrough) {
        saveImg(ident, image);
        return image;
    }

    public void cleanUp() {
        if(disabled) return;

        TreeMap<Long, File> modtime = new TreeMap<Long, File>();
        long time = new Date().getTime() - expire;
        long dirsize = 0;

        for(File f : getFiles()) {
            if(f.lastModified() < time)
                f.delete();
            else {
                dirsize += f.length();
                modtime.put(f.lastModified(), f);
            }
        }

        if(dirsize < maxsize) return;

        Set keySet = modtime.keySet();
        Iterator it = keySet.iterator();
        int i=0;
        while (it.hasNext()) {
            i++;
            ((File)modtime.get(it.next())).delete();

            // Delete a couple of files, then check again
            if(i % cleanUpThreshold == 0 && getDirSize() < maxsize)
                return;
        }
    }

    private long getDirSize() {
        long dirsize = 0;

        for(File f : getFiles())
            dirsize += f.length();
        return dirsize;
    }

    private File[] getFiles() {
        return dir.listFiles(
            new FileFilter() {
                public boolean accept(File file) {
                    return file.getName().endsWith(".png");
                }
            }
        );
    }

    private String clean(String ident) {
        return ident.replaceAll("[^a-zA-Z0-9]", "");
    }

    private File getPath(String ident) {
        return new File(dir, clean(ident) + ".png");
    }
}