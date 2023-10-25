// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.terracer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Tool to reverse the house numbers in a terrace.
 * <p>
 * Useful for when you're using the Terracer tool and the house numbers come out
 * in the wrong direction, or when someone has added house numbers in the wrong
 * direction anyway.
 * <p>
 * Finds all connected ways which have a building=* and addr tag on them in order (breadth
 * first search) and then changes the tags to be the reverse of the order in which
 * they were found.
 */
public class ReverseTerraceAction extends JosmAction {
    private static final String ADDR_HOUSENUMBER = "addr:housenumber";

    /**
     * Create a new action for reversing a terrace
     */
    public ReverseTerraceAction() {
        super(tr("Reverse a terrace"),
            "reverse_terrace",
            tr("Reverses house numbers on a terrace."),
            Shortcut.registerShortcut("tools:ReverseTerrace",
                    tr("More tools: {0}", tr("Reverse a terrace")),
                    KeyEvent.VK_V, Shortcut.ALT_CTRL_SHIFT),
                        true);
    }

    /**
     * Breadth-first searches based on the selection while the selection is a way
     * with a building=* tag and then applies the addr:housenumber tag in reverse order.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<Way> selectedWays = MainApplication.getLayerManager().getEditDataSet().getSelectedWays();
        reverseTerracedAddresses(selectedWays);
    }

    static void reverseTerracedAddresses(Collection<Way> selectedWays) {
        // Set to keep track of all the nodes that have been visited - that is: if
        // we encounter them again we will not follow onto the connected ways.
        Set<Node> visitedNodes = new HashSet<>();

        // Set to keep track of the ways the algorithm has seen, but not yet visited.
        // Since when a way is visited all of its nodes are marked as visited, there
        // is no need to keep a visitedWays set.
        final Deque<Way> front = findFirstWay(selectedWays);

        if (front.isEmpty()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Cannot reverse!"));
            return;
        }


        // This is like a visitedWays set, but in a linear order.
        LinkedList<Way> orderedWays = new LinkedList<>();

        // And the tags to reverse on the orderedWays.
        LinkedList<String> houseNumbers = new LinkedList<>();

        while (!front.isEmpty()) {
            // Java apparently doesn't have useful methods to get single items from sets...
            Way w = front.pop();

            // Visit all the nodes in the way, adding the building's they're members of
            // to the front.
            for (Node n : w.getNodes()) {
                if (!visitedNodes.contains(n)) {
                    for (OsmPrimitive prim : n.getReferrers()) {
                        if (prim.hasKey("building") && prim.hasKey(ADDR_HOUSENUMBER)
                                && prim instanceof Way && !front.contains(prim)) {
                            front.add((Way) prim);
                        }
                    }
                    visitedNodes.add(n);
                }
            }

            // We've finished visiting this way, so record the attributes we're interested
            // in for re-writing.
            orderedWays.addLast(w);
            houseNumbers.addFirst(w.get(ADDR_HOUSENUMBER));
        }

        Collection<Command> commands = new LinkedList<>();
        for (int i = 0; i < orderedWays.size(); ++i) {
            commands.add(new ChangePropertyCommand(
                    orderedWays.get(i),
                    ADDR_HOUSENUMBER,
                    houseNumbers.get(i)));
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Reverse Terrace"), commands));
        MainApplication.getLayerManager().getEditDataSet().setSelected(orderedWays);
    }

    private static Deque<Way> findFirstWay(Collection<Way> selectedWays) {
        // Find the first or last way from the terraced houses.
        // It should be connected to exactly one other way.
        for (Way w : selectedWays) {
            int conn = 0;
            for (Way v : selectedWays) {
                if (!w.equals(v) && !Collections.disjoint(w.getNodes(), v.getNodes())) {
                    ++conn;
                    if (conn > 1) {
                        break;
                    }
                }
            }
            if (conn == 1) {
                return new ArrayDeque<>(Collections.singletonList(w));
            }
        }
        return new ArrayDeque<>();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
    }
}
