package smed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.swing.JMenuItem;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import smed.plug.SmedPluginApp;
import smed.plug.ifc.SmedPluggable;
import smed.plug.util.JARFileFilter;
import smed.plug.util.SmedPluginLoader;
import smed.tabs.SmedTabAction;

public class Smed extends Plugin{

    private JMenuItem item;
    private SmedTabAction SmedTab;
	private List<SmedPluggable> plugins = null;

    public Smed(PluginInformation info) {
        super(info);

        String os = "";
        String userHome = "";

        File pluginDir = Main.pref.getPluginsDirectory();
        String pluginDirName = pluginDir.getAbsolutePath();
        SmedFile splugDir = new SmedFile(pluginDirName + "/splug");

        if(!splugDir.exists()) splugDir.mkdir();

        File[] jars = splugDir.listFiles(new JARFileFilter());

        // build smed_ifc.jar from smed.jar
        JarEntry e = null;
        BufferedInputStream inp = null;
        String eName = null;
        byte[] buffer = new byte[16384];
        int len;

        try {
            JarFile file = new JarFile(pluginDirName  + "/smed.jar");
            FileOutputStream fos = new FileOutputStream(pluginDirName + "/splug/smed_ifc.jar");
            JarOutputStream jos = new JarOutputStream(fos);
            BufferedOutputStream oos = new BufferedOutputStream( jos);

            // extract *.jar to splug
            Enumeration<JarEntry> ent = file.entries();
            while(ent.hasMoreElements()) {
                e = ent.nextElement();
                eName = e.getName();
                if(eName.endsWith(".jar")) {
                    if(splugDir.needUpdate(jars,eName)) {
                        FileOutputStream pfos = new FileOutputStream(pluginDirName + "/splug/" + eName);
                        BufferedOutputStream pos = new BufferedOutputStream(pfos);
                        inp = new BufferedInputStream(file.getInputStream( e ));

                        while ((len = inp.read(buffer)) > 0) {
                            pos.write(buffer, 0, len);
                        }

                        pos.flush();
                        pos.close();
                        inp.close();
                        pfos.close();
                    }
                }
            }



            // write smed_ifc.jar to splug
            e = file.getJarEntry("smed/plug/ifc/SmedPluggable.class");
            inp = new BufferedInputStream(file.getInputStream( e ));
            eName = e.getName();

            jos.putNextEntry(new JarEntry(eName));

            while ((len = inp.read(buffer)) > 0) {
                oos.write(buffer, 0, len);
            }

            oos.flush();
            inp.close();

            e = file.getJarEntry("smed/plug/ifc/SmedPluginManager.class");
            inp = new BufferedInputStream(file.getInputStream( e ));
            eName = e.getName();
            jos.putNextEntry(new JarEntry(eName));

            while ((len = inp.read(buffer)) > 0) {
                oos.write(buffer, 0, len);
            }

            oos.flush();
            oos.close();
            fos.flush();
            fos.close();
            inp.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // add smed_ifc.jar to classpath (josm need this archive, or perhaps only the interface)
        File f = new java.io.File(pluginDirName + "/splug/smed_ifc.jar");
        ClassLoader myClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            addUrlMethod.setAccessible(true);
            addUrlMethod.invoke(myClassLoader, f.toURI().toURL());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SmedTab = new SmedTabAction();
        item = Main.main.menu.toolsMenu.add(SmedTab);

        item.setEnabled(false);

    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            item.setEnabled(true);
        } else {
            item.setEnabled(false);
            // SmpDialog.CloseDialog();
        }
    }

    public void setPlugins(List<SmedPluggable> plugins)  {
    	this.plugins = plugins;
    	
    }

}
