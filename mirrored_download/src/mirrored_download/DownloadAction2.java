// License: GPL. Copyright 2007 by Immanuel Scholz and others
package mirrored_download;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.Future;

import org.openstreetmap.josm.actions.JosmAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that opens a connection to the osm server and downloads map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadAction2 extends JosmAction {
    public DownloadAction2() {
        super(tr("Download from OSM mirror..."), null, tr("Download map data from the OSM server."),
                Shortcut.registerShortcut("mirror:download", tr("File: {0}", tr("Download from OSM mirror...")), KeyEvent.VK_D, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY), true);
        putValue("help", ht("/Action/MirroredDownload"));
    }

    public void actionPerformed(ActionEvent e) {
        DownloadDialog dialog = DownloadDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);
        if (! dialog.isCanceled()) {
            dialog.rememberSettings();
            Bounds area = dialog.getSelectedDownloadArea();
            if (dialog.isDownloadOsmData()) {
                DownloadOsmTask2 task = new DownloadOsmTask2();
                Future<?> future = task.download(dialog.isNewLayerRequired(), area, null);
                Main.worker.submit(new PostDownloadHandler(task, future));
            }
            if (dialog.isDownloadGpxData()) {
                DownloadGpsTask task = new DownloadGpsTask();
                Future<?> future = task.download(dialog.isNewLayerRequired(),area, null);
                Main.worker.submit(new PostDownloadHandler(task, future));
            }
        }
    }
}
