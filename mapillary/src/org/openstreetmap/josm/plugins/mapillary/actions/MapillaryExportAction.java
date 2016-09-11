// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImportedImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.MapillaryPlugin;
import org.openstreetmap.josm.plugins.mapillary.gui.MapillaryExportDialog;
import org.openstreetmap.josm.plugins.mapillary.io.export.MapillaryExportManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that launches a MapillaryExportDialog and lets you export the images.
 *
 * @author nokutu
 *
 */
public class MapillaryExportAction extends JosmAction {

  private static final long serialVersionUID = 6009490043174837948L;

  private MapillaryExportDialog dialog;

  /**
   * Main constructor.
   */
  public MapillaryExportAction() {
    super(tr("Export pictures"), MapillaryPlugin.getProvider("icon24.png"),
        tr("Export pictures"), Shortcut.registerShortcut("Export Mapillary",
            tr("Export Mapillary pictures"), KeyEvent.CHAR_UNDEFINED,
            Shortcut.NONE), false, "mapillaryExport", false);
    this.setEnabled(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JOptionPane pane = new JOptionPane();

    JButton ok = new JButton("Ok");
    ok.addActionListener(new OKAction(pane));
    JButton cancel = new JButton(tr("Cancel"));
    cancel.addActionListener(new CancelAction(pane));

    this.dialog = new MapillaryExportDialog(ok);
    pane.setMessage(this.dialog);
    pane.setOptions(new JButton[] {ok, cancel});

    JDialog dlg = pane.createDialog(Main.parent, tr("Export images"));
    dlg.setMinimumSize(new Dimension(400, 150));
    dlg.setVisible(true);

    // Checks if the inputs are correct and starts the export process.
    if (pane.getValue() != null
        && (int) pane.getValue() == JOptionPane.OK_OPTION
        && this.dialog.chooser != null) {
      if (this.dialog.group.isSelected(this.dialog.all.getModel())) {
        export(MapillaryLayer.getInstance().getData().getImages());
      } else if (this.dialog.group.isSelected(this.dialog.sequence.getModel())) {
        Set<MapillaryAbstractImage> images = new ConcurrentSkipListSet<>();
        for (MapillaryAbstractImage image : MapillaryLayer.getInstance().getData().getMultiSelectedImages()) {
          if (image instanceof MapillaryImage) {
            if (!images.contains(image)) {
              images.addAll(((MapillaryImage) image).getSequence().getImages());
            }
          } else {
            images.add(image);
          }
        }
        export(images);
      } else if (this.dialog.group.isSelected(this.dialog.selected.getModel())) {
        export(MapillaryLayer.getInstance().getData().getMultiSelectedImages());
      }
      // This option ignores the selected directory.
    } else if (this.dialog.group.isSelected(this.dialog.rewrite.getModel())) {
      ArrayList<MapillaryImportedImage> images = new ArrayList<>();
      for (MapillaryAbstractImage image : MapillaryLayer.getInstance().getData().getImages()) {
        if (image instanceof MapillaryImportedImage) {
          images.add((MapillaryImportedImage) image);
        }
      }
      try {
        Main.worker.submit(new MapillaryExportManager(images));
      } catch (IOException e1) {
        Main.error(e1);
      }
    }
    dlg.dispose();
  }

  /**
   * Exports the given images from the database.
   *
   * @param images
   *          The set of images to be exported.
   */
  public void export(Set<MapillaryAbstractImage> images) {
    Main.worker.submit(new MapillaryExportManager(images,
        this.dialog.chooser.getSelectedFile().toString()));
  }

  private static class OKAction implements ActionListener {
    private final JOptionPane pane;

    public OKAction(JOptionPane pane) {
      this.pane = pane;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      this.pane.setValue(JOptionPane.OK_OPTION);
    }
  }

  private static class CancelAction implements ActionListener {
    private final JOptionPane pane;

    public CancelAction(JOptionPane pane) {
      this.pane = pane;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      this.pane.setValue(JOptionPane.CANCEL_OPTION);
    }
  }
}
