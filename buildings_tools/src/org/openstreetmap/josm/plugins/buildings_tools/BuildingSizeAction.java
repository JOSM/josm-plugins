// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action to open the {@link BuildingSizeDialog}
 */
public class BuildingSizeAction extends JosmAction {
    private static final String SET_BUILDINGS_SIZE = marktr("Set buildings size");
    /**
     * Create a new action for setting building sizes
     */
    public BuildingSizeAction() {
        super(tr(SET_BUILDINGS_SIZE), "mapmode/building", tr(SET_BUILDINGS_SIZE),
                Shortcut.registerShortcut("edit:buildingsdialog", tr("Data: {0}", tr(SET_BUILDINGS_SIZE)),
                KeyEvent.VK_B, Shortcut.ALT_CTRL),
                true, "edit:buildingsdialog", false);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        BuildingSizeDialog dlg = new BuildingSizeDialog();
        if (dlg.getValue() == 1) {
            dlg.saveSettings();
        }
    }
}
