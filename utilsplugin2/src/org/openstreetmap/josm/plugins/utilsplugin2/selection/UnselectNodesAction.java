// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.selection;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Unselect all nodes.
 */
public class UnselectNodesAction extends JosmAction {

    /**
     * Constructs a new {@code UnselectNodesAction}.
     */
    public UnselectNodesAction() {
        super(tr("Unselect nodes"), "unsnodes",
                tr("Removes all nodes from selection"),
                Shortcut.registerShortcut("tools:unsnodes", tr("Selection: {0}", tr("Unselect nodes")),
                        KeyEvent.VK_U, Shortcut.SHIFT), true);
        putValue("help", ht("/Action/UnselectNodes"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getLayerManager().getActiveDataSet().clearSelection(getLayerManager().getActiveDataSet().getSelectedNodes());
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
