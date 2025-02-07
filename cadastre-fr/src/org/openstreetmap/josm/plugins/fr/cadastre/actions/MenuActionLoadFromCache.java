// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre.actions;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.fr.cadastre.CadastrePlugin;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.CacheControl;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.CacheFileLambert4ZoneFilter;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.CacheFileLambert9ZoneFilter;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.CacheFileUTM20NFilter;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.WMSLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * Load location from cache (only if cache is enabled)
 */
public class MenuActionLoadFromCache extends JosmAction {
    private static final long serialVersionUID = 1L;
    private static final String ERROR = marktr("Error");

    private static final String ACTION_NAME = marktr("Load layer from cache");

    /**
     * Constructs a new {@code MenuActionLoadFromCache}.
     */
    public MenuActionLoadFromCache() {
        super(tr(ACTION_NAME), "cadastre_small",
                tr("Load location from cache (only if cache is enabled)"), null, false, "cadastrefr/loadfromcache", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = createAndOpenFileChooser();
        if (fc == null)
            return;

        File[] files = fc.getSelectedFiles();
        int layoutZone = CadastrePlugin.getCadastreProjectionLayoutZone();
        for (File file : files) {
            if (file.exists()) {
                String filename = file.getName();
                String ext = getExtension(filename);
                if ((extIsLambertCC9Z(ext) && !CadastrePlugin.isLambert_cc9())
                    || (extIsUTM20N(ext) && !CadastrePlugin.isUtm_france_dom())
                    || (ext.length() == 1 && !CadastrePlugin.isLambert())) {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                                tr("{0} not allowed with the current projection", filename),
                                tr("Error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    actionPerformed(file, filename, ext, layoutZone);
                }
            }
        }
    }

    private static String getExtension(String filename) {
        return (filename.lastIndexOf('.') == -1) ? "" : filename.substring(filename.lastIndexOf('.')+1);
    }

    private static String simplifyExtension(String ext) {
        if (extIsLambertCC9Z(ext))
            return ext.substring(2);
        else if (extIsUTM20N(ext))
            return ext.substring(3);
        return ext;
    }

    private static void actionPerformed(File file, String filename, String ext, int layoutZone) {
        String location = filename.substring(0, filename.lastIndexOf('.'));
        // check the extension and its compatibility with current projection
        ext = simplifyExtension(ext);
        try {
            int cacheZone = Integer.parseInt(ext) - 1;
            if (cacheZone >= 0 && cacheZone <= 9) {
                if (cacheZone != layoutZone) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("Cannot load cache {0} which is not compatible with current projection zone", filename),
                            tr(ERROR), JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    Logging.info("Load cache " + filename);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Selected file {0} is not a cache file from this plugin (invalid extension)", filename),
                    tr(ERROR), JOptionPane.ERROR_MESSAGE);
            return;
        }
        // check if the selected cache is not already displayed
        if (MainApplication.getMap() != null) {
            for (Layer l : MainApplication.getLayerManager().getLayers()) {
                if (l instanceof WMSLayer && l.getName().equals(location)) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("The location {0} is already on screen. Cache not loaded.", filename),
                            tr(ERROR), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        // create layer and load cache
        WMSLayer wmsLayer = new WMSLayer("", "", Integer.parseInt(ext)-1);
        if (wmsLayer.grabThread.getCacheControl().loadCache(file, layoutZone)) {
            CadastrePlugin.addWMSLayer(wmsLayer);
        }
    }

    private static boolean extIsLambertCC9Z(String ext) {
        return ext.length() == 3 && ext.startsWith(CacheControl.C_LAMBERT_CC_9Z);
    }

    private static boolean extIsUTM20N(String ext) {
        return ext.length() == 4 && ext.startsWith(CacheControl.C_UTM20N);
    }

    protected static JFileChooser createAndOpenFileChooser() {
        JFileChooser fc = new JFileChooser(new File(CadastrePlugin.cacheDir));
        fc.setMultiSelectionEnabled(true);
        int layoutZone = CadastrePlugin.getCadastreProjectionLayoutZone();
        if (layoutZone != -1) {
            if (CadastrePlugin.isLambert())
                fc.addChoosableFileFilter(CacheFileLambert4ZoneFilter.filters[layoutZone]);
            else if (CadastrePlugin.isLambert_cc9())
                fc.addChoosableFileFilter(CacheFileLambert9ZoneFilter.filters[layoutZone]);
            else if (CadastrePlugin.isUtm_france_dom())
                fc.addChoosableFileFilter(CacheFileUTM20NFilter.filters[layoutZone]);
        }
        fc.setAcceptAllFileFilterUsed(false);

        int answer = fc.showOpenDialog(MainApplication.getMainFrame());
        if (answer != JFileChooser.APPROVE_OPTION)
            return null;

        return fc;
    }

}
