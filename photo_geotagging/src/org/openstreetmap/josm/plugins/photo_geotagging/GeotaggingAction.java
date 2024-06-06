// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.photo_geotagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.imaging.ImagingOverflowException;
import org.apache.commons.io.FilenameUtils;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * The action to ask the user for confirmation and then do the tagging.
 */
class GeotaggingAction extends AbstractAction implements LayerAction {

    static final String KEEP_BACKUP = "plugins.photo_geotagging.keep_backup";
    static final String CHANGE_MTIME = "plugins.photo_geotagging.change-mtime";
    static final String MTIME_MODE = "plugins.photo_geotagging.mtime-mode";
    static final int MTIME_MODE_GPS = 1;
    static final int MTIME_MODE_PREVIOUS_VALUE = 2;

    public GeotaggingAction() {
        super(tr("Write coordinates to image header"), ImageProvider.get("geotagging"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {

        GeoImageLayer layer = getLayer();

        final List<ImageEntry> images = new ArrayList<>();
        int notSupportedFilesCount = 0;
        String notSupportedName = null;
        boolean hasTiff = false;

        final JPanel cont = new JPanel(new GridBagLayout());
        cont.add(new JLabel(tr("Write position information into the exif header of the following files:")), GBC.eol());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        DecimalFormat dFormatter = new DecimalFormat("###0.000000");

        for (ImageEntry e : layer.getImages()) {
             /* Only write lat/lon to the file, if the position is known and
                the GPS data changed. */
            if (e.getPos() != null && e.hasNewGpsData()) {
                String pth = e.getFile().getAbsolutePath();
                switch (FilenameUtils.getExtension(pth).toLowerCase(Locale.ENGLISH)) {
                    case "tif":
                    case "tiff":
                        hasTiff = true;
                        // fall through (this comment makes the compiler happy)
                    case "jpg":
                    case "jpeg":
                        images.add(e);
                        listModel.addElement(pth + " (" + dFormatter.format(e.getPos().lat()) + ","
                                + dFormatter.format(e.getPos().lon()) + ")");
                        break;
                    default:
                        notSupportedFilesCount++;
                        if (notSupportedName == null) {
                            notSupportedName = e.getFile().getName();
                        }
                        break;
                }
            }
        }

        JList<String> entryList = new JList<>(listModel);

        JScrollPane scroll = new JScrollPane(entryList);
        scroll.setPreferredSize(new Dimension(900, 250));
        cont.add(scroll, GBC.eol().fill(GridBagConstraints.BOTH));

        if (notSupportedFilesCount > 0) {
            JLabel warn = new JLabel(notSupportedFilesCount == 1
                    ? tr("The file \"{0}\" can not be updated. Only JPEG and TIFF images are supported.", notSupportedName)
                    : trn("{0} file can not be updated. Only JPEG and TIFF images are supported.",
                          "{0} files can not be updated. Only JPEG and TIFF images are supported.",
                    notSupportedFilesCount, notSupportedFilesCount));
            warn.setForeground(Color.RED);
            cont.add(warn, GBC.eol());
        }

        if (hasTiff) {
            JLabel warn = new JLabel(tr("Warning: Some metadata in TIFF files may be lost. Always keep a backup!"));
            warn.setForeground(Color.RED);
            cont.add(warn, GBC.eol());
        }

        final JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder(tr("settings")));
        cont.add(settingsPanel, GBC.eol().insets(3,10,3,0));

        final JCheckBox backups = new JCheckBox(tr("keep backup files"), Config.getPref().getBoolean(KEEP_BACKUP, true));
        settingsPanel.add(backups, GBC.eol().insets(3,3,0,0));

        final JCheckBox setMTime = new JCheckBox(tr("change file modification time:"), Config.getPref().getBoolean(CHANGE_MTIME, false));
        settingsPanel.add(setMTime, GBC.std().insets(3,3,5,3));

        final String[] mTimeModeArray = {"----", tr("to gps time"), tr("to previous value (unchanged mtime)")};
        final JComboBox<String> mTimeMode = new JComboBox<>(mTimeModeArray);
        {
            String mTimeModePref = Config.getPref().get(MTIME_MODE, null);
            int mTimeIdx = 0;
            if ("gps".equals(mTimeModePref)) {
                mTimeIdx = 1;
            } else if ("previous".equals(mTimeModePref)) {
                mTimeIdx = 2;
            }
            mTimeMode.setSelectedIndex(setMTime.isSelected() ? mTimeIdx : 0);
        }
        settingsPanel.add(mTimeMode, GBC.eol().insets(3,3,3,3));

        setMTime.addActionListener(e -> {
            if (setMTime.isSelected()) {
                mTimeMode.setEnabled(true);
            } else {
                mTimeMode.setSelectedIndex(0);
                mTimeMode.setEnabled(false);
            }
        });

        // Toggle the checkbox to fire actionListener
        setMTime.setSelected(!setMTime.isSelected());
        setMTime.doClick();

        int result = new ExtendedDialog(
                MainApplication.getMainFrame(),
                tr("Photo Geotagging Plugin"), tr("OK"), tr("Cancel"))
            .setButtonIcons("ok", "cancel")
            .setContent(cont)
            .setCancelButton(2)
            .setDefaultButton(1)
            .showDialog()
            .getValue();

        if (result != 1)
            return;

        final boolean keep_backup = backups.isSelected();
        final boolean change_mtime = setMTime.isSelected();
        Config.getPref().putBoolean(KEEP_BACKUP, keep_backup);
        Config.getPref().putBoolean(CHANGE_MTIME, change_mtime);
        if (change_mtime) {
            String mTimeModePref;
            switch (mTimeMode.getSelectedIndex()) {
            case 1:
                mTimeModePref = "gps";
                break;
            case 2:
                mTimeModePref = "previous";
                break;
            default:
                mTimeModePref = null;
            }
            Config.getPref().put(MTIME_MODE, mTimeModePref);
        }

        MainApplication.worker.execute(new GeoTaggingRunnable(images, keep_backup, mTimeMode.getSelectedIndex()));
    }

    static class GeoTaggingRunnable extends PleaseWaitRunnable {
        private final List<ImageEntry> images;
        private final boolean keep_backup;
        private final int mTimeMode;

        private boolean canceled = false;
        Boolean override_backup = null;

        private File fileFrom;
        private File fileTo;
        private File fileDelete;

        private int currentIndex;

        public GeoTaggingRunnable(List<ImageEntry> images, boolean keep_backup, int mTimeMode) {
            super(tr("Photo Geotagging Plugin"));
            this.images = images;
            this.keep_backup = keep_backup;
            this.mTimeMode = mTimeMode;
        }


        @Override
        protected void realRun() {
            List<ImageEntry> failedEntries = processEntries(images, false);
            if (!failedEntries.isEmpty()) {
                int ret = GuiHelper.runInEDTAndWaitAndReturn(() -> {
                    ExtendedDialog dlg = new ExtendedDialog(progressMonitor.getWindowParent(), tr("Warning"),
                            tr("Abort"), tr("Proceed"));

                    dlg.setButtonIcons("cancel", "dialogs/next")
                       .setIcon(JOptionPane.WARNING_MESSAGE);

                    StringBuilder sb = new StringBuilder(trn(
                            "The GPS tag could not be added to the following file because there is not enough free space in the EXIF section:",
                            "The GPS tag could not be added to the following files because there is not enough free space in the EXIF section:",
                            failedEntries.size()));

                    sb.append("<ul>");
                    for (int i = 0; i < failedEntries.size(); i++) {
                        sb.append("<li>");

                        if (i == 5 && failedEntries.size() > i + 1) {
                            int remaining = failedEntries.size() - i;
                            sb.append("<i>")
                              .append(trn("({0} more file)", "({0} more files)", remaining, remaining))
                              .append("</i></li>");
                            break;
                        } else {
                            sb.append(failedEntries.get(i).getFile().getName()).append("</li>");
                        }
                    }
                    sb.append("</ul><br>")
                      .append(tr("This can likely be fixed by rewriting the entire EXIF section, however some (rare) unknown tags may get lost in the process.<br>"
                              + "Would you like to proceed anyway?"));

                    dlg.setContent(sb.toString())
                       .setDefaultButton(2)
                       .showDialog();

                    return dlg.getValue();
                });
                if (ret == 2) {
                    processEntries(failedEntries, true);
                }
            }
        }

        List<ImageEntry> processEntries(List<ImageEntry> entries, boolean lossy) {
            progressMonitor.subTask(tr("Writing position information to image files..."));
            progressMonitor.setTicksCount(entries.size());
            progressMonitor.setTicks(0);

            final List<ImageEntry> exifFailedEntries = new ArrayList<>();

            final long startTime = System.currentTimeMillis();

            currentIndex = 0;
            while (currentIndex < entries.size()) {
                if (canceled)
                    return exifFailedEntries;
                ImageEntry e = entries.get(currentIndex);
                Logging.trace("photo_geotagging: GeotaggingAction: i: {0} {1} ", currentIndex, e.getFile().getName());
                try {
                    processEntry(e, lossy);
                } catch (final IOException ioe) {
                    Logging.trace(ioe);
                    restoreFile();
                    if (!lossy && ioe.getCause() instanceof ImagingOverflowException) {
                        exifFailedEntries.add(e);
                    } else {
                        int ret = GuiHelper.runInEDTAndWaitAndReturn(() -> {
                            ExtendedDialog dlg = new ExtendedDialog(progressMonitor.getWindowParent(),
                                    tr("Error"),
                                    tr("Abort"), tr("Retry"), tr("Ignore"));
                            dlg.setButtonIcons("cancel", "dialogs/refresh", "dialogs/next");

                            String msg;
                            if (ioe instanceof NoSuchFileException) {
                                msg = tr("File not found.");
                            } else {
                                msg = ioe.toString();
                            }

                            dlg.setIcon(JOptionPane.ERROR_MESSAGE)
                               .setContent(tr("Unable to process file ''{0}'':", e.getFile().toString()) + "<br/>" + msg)
                               .setDefaultButton(3)
                               .showDialog();

                            return dlg.getValue();
                        });

                        switch (ret) {
                            case 2: // retry
                                currentIndex--;
                                break;
                            case 3: // continue
                                break;
                            default: // abort
                                canceled = true;
                        }
                    }
                }
                progressMonitor.worked(1);

                float millisecondsPerFile = (float) (System.currentTimeMillis() - startTime) / (currentIndex + 1); // currentIndex starts at 0
                int filesLeft = entries.size() - currentIndex - 1;
                String timeLeft = Utils.getDurationString((long) Math.ceil(millisecondsPerFile * filesLeft));

                progressMonitor.subTask(tr("Writing position information to image files... Estimated time left: {0}", timeLeft));

                Logging.trace("photo_geotagging: GeotaggingAction: finished {0}", e.getFile());
                currentIndex++;
            }
            return exifFailedEntries;
        }

        private void processEntry(ImageEntry e, boolean lossy) throws IOException {
            fileFrom = null;
            fileTo = null;
            fileDelete = null;

            if (mTimeMode != 0) {
                testMTimeReadAndWrite(e.getFile());
            }

            Instant mTime = null;
            if (mTimeMode == MTIME_MODE_GPS) {
                // check GPS time fields, do nothing if all fails
                if (e.hasGpsTime()) {
                    mTime = e.getGpsInstant();
                } else if (e.hasExifGpsTime()) {
                    mTime = e.getExifGpsInstant();
                }
            }
            if ( mTimeMode == MTIME_MODE_PREVIOUS_VALUE
                 // this is also the fallback if one of the other
                 // modes failed to determine the modification time
                 || (mTimeMode != 0 && mTime == null)) {
                mTime = Instant.ofEpochMilli(e.getFile().lastModified());
                if (Instant.EPOCH.equals(mTime))
                    throw new IOException(tr("Could not read mtime."));
            }

            chooseFiles(e.getFile());
            if (canceled) return;
            ExifGPSTagger.setExifGPSTag(fileFrom, fileTo, e.getPos().lat(), e.getPos().lon(),
                    e.getGpsInstant(), e.getSpeed(), e.getElevation(), e.getExifImgDir(), lossy);

            if (mTime != null) {
                if (!fileTo.setLastModified(mTime.toEpochMilli()))
                    throw new IOException(tr("Could not write mtime."));
            }

            cleanupFiles();
            e.unflagNewGpsData();
        }

        private void chooseFiles(File file) throws IOException {
            Logging.trace("photo_geotagging: GeotaggingAction: f: "+file.getAbsolutePath());

            if (!keep_backup) {
                chooseFilesNoBackup(file);
                return;
            }

            File fileBackup = new File(file.getParentFile(),file.getName()+"_");
            if (fileBackup.exists()) {
                confirm_override();
                if (canceled)
                    return;

                if (override_backup) {
                    if (!fileBackup.delete())
                        throw new IOException(tr("File could not be deleted!"));
                } else {
                    chooseFilesNoBackup(file);
                    return;
                }
            }
            if (!file.renameTo(fileBackup))
                throw new IOException(tr("Could not rename file!"));

            fileFrom = fileBackup;
            fileTo = file;
            fileDelete = null;
        }

        private void chooseFilesNoBackup(File file) throws IOException {
            File fileTmp;
            //fileTmp = File.createTempFile("img", ".jpg", file.getParentFile());
            // on win32, file.renameTo(fileTmp) does not work when the destination file exists
            // see https://bugs.openjdk.java.net/browse/JDK-4017593
            // so we cannot use createTempFile(), which would create that "existing destination file"
            // instead, let's use new File(), which doesn't actually create a file
            // for getting a unique file name, we use UUID.randomUUID()
            do {
                fileTmp = new File(file.getParentFile(), "img" + UUID.randomUUID() + ".tmp");
            } while (fileTmp.exists());
            Logging.trace("photo_geotagging: GeotaggingAction: TMP: {0}", fileTmp.getAbsolutePath());
            try {
                Files.move(file.toPath(), fileTmp.toPath());
            } catch (IOException e) {
                Logging.error(tr("Could not rename file {0} to {1}!", file, fileTmp));
                throw e;
            }
            fileFrom = fileTmp;
            fileTo = file;
            fileDelete = fileTmp;
        }

        private void confirm_override() {
            if (override_backup != null)
                return;
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JLabel l = new JLabel(tr("<html><h3>There are old backup files in the image directory!</h3>"));
                    l.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                    int override = new ExtendedDialog(
                            progressMonitor.getWindowParent(),
                            tr("Override old backup files?"), tr("Cancel"), tr("Keep old backups and continue"),
                            tr("Override"))
                        .setButtonIcons("cancel", "ok", "dialogs/delete")
                        .setContent(l)
                        .setCancelButton(1)
                        .setDefaultButton(2)
                        .showDialog()
                        .getValue();
                    if (override == 2) {
                        override_backup = false;
                    } else if (override == 3) {
                        override_backup = true;
                    } else {
                        canceled = true;
                    }
                });
            } catch (Exception e) {
                Logging.error(e);
                canceled = true;
            }
        }

        private void restoreFile() {
            if (fileFrom != null && fileFrom.exists()) {
                if (fileTo != null && fileTo.exists()) {
                    fileTo.delete();
                }
                fileFrom.renameTo(fileTo);
            }
        }

        private void cleanupFiles() throws IOException {
            if (fileDelete != null) {
                if (!fileDelete.delete())
                    throw new IOException(tr("Could not delete temporary file!"));
            }
        }

        boolean testMTimeReadAndWriteDone = false;

        private void testMTimeReadAndWrite(File file) throws IOException {
            if (testMTimeReadAndWriteDone)  // do this only once
                return;
            File fileTest = File.createTempFile("geo", ".txt", file.getParentFile());
            long mTimeTest = fileTest.lastModified();
            if (mTimeTest == 0L)
                throw new IOException(tr("Test failed: Could not read mtime."));
            if (!fileTest.setLastModified(mTimeTest))
                throw new IOException(tr("Test failed: Could not write mtime."));
            if (!fileTest.delete())
                throw new IOException(tr("Could not delete temporary file!"));

            testMTimeReadAndWriteDone = true;
        }

        @Override
        protected void finish() {
        }

        @Override
        protected void cancel() {
            canceled = true;
        }
    }

    private GeoImageLayer getLayer() {
        return (GeoImageLayer)LayerListDialog.getInstance().getModel().getSelectedLayers().get(0);
    }

    /**
     * Check if there is any suitable image.
     * @param layer geo image layer
     * @return {@code true} if there is any suitable image
     */
    private boolean enabled(GeoImageLayer layer) {
        for (ImageEntry e : layer.getImages()) {
            if (e.getPos() != null && e.hasNewGpsData())
                return true;
        }
        return false;
    }

    @Override
    public Component createMenuComponent() {
        JMenuItem geotaggingItem = new JMenuItem(this);
        geotaggingItem.setEnabled(enabled(getLayer()));
        return geotaggingItem;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof GeoImageLayer;
    }
}
