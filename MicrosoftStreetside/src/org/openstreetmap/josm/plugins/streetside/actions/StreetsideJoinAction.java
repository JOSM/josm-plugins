// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.mode.JoinMode;
import org.openstreetmap.josm.plugins.streetside.mode.SelectMode;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * Changes the mode of the Layer, from Select mode to Join mode and vice versa.
 *
 * @author nokutu
 *
 */
public class StreetsideJoinAction extends JosmAction {

  private static final long serialVersionUID = -7082300908202843706L;

  /**
   * Main constructor.
   */
  public StreetsideJoinAction() {
    super(tr("Join mode"), new ImageProvider("mapmode", "mapillary-join").setSize(ImageSizes.DEFAULT),
        tr("Join/unjoin pictures"), null, false, "mapillaryJoin", true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    if (StreetsideLayer.getInstance().mode instanceof SelectMode) {
      StreetsideLayer.getInstance().setMode(new JoinMode());
    } else {
      StreetsideLayer.getInstance().setMode(new SelectMode());
    }
  }

  @Override
  protected boolean listenToSelectionChange() {
    return false;
  }

  /**
   * Enabled when mapillary layer is the active layer
   */
  @Override
  protected void updateEnabledState() {
    super.updateEnabledState();
    setEnabled(MainApplication.getLayerManager().getActiveLayer() instanceof StreetsideLayer);
  }
}
