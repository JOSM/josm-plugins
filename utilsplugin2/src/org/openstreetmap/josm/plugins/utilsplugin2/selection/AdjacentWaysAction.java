// License: GPL. Copyright 2011 by Alexei Kasatkin
package org.openstreetmap.josm.plugins.utilsplugin2.selection;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.*;

import org.openstreetmap.josm.tools.Shortcut;

/**
 *    Extends current selection
 */
public class AdjacentWaysAction extends JosmAction {

    public static final boolean treeMode = false;

    public AdjacentWaysAction() {
        super(tr("Adjacent ways"), "adjways",
                tr("Adjacent ways will be selected. Nodes will be deselected."),
                Shortcut.registerShortcut("tools:adjways", tr("Tool: {0}","Adjacent ways"),
                KeyEvent.VK_E, Shortcut.SHIFT), true);
        putValue("help", ht("/Action/AdjacentWays"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        Set<Node> selectedNodes = OsmPrimitive.getFilteredSet(selection, Node.class);

        Set<Way> selectedWays = OsmPrimitive.getFilteredSet(getCurrentDataSet().getSelected(), Way.class);

        // select ways attached to already selected ways
        Set<Way> newWays = new HashSet<>();
        NodeWayUtils.addWaysConnectedToWays(selectedWays, newWays);
        newWays.addAll(selectedWays);

        // selecting ways attached to selected nodes
        if(!selectedNodes.isEmpty()) {
            NodeWayUtils.addWaysConnectedToNodes(selectedNodes, newWays);
        }

//        System.out.printf("%d ways added to selection\n",newWays.size()-selectedWays.size());
        getCurrentDataSet().setSelected(newWays);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (selection == null) {
            setEnabled(false);
            return;
        }
        setEnabled(!selection.isEmpty());
    }



}
