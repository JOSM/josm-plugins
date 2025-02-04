// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Merge address points with a building
 */
public class MergeAddrPointsAction extends JosmAction {

    private static class MultiConflict {
        private final int multi;
        private final int conflicts;
        public MultiConflict(int multi, int conflict) {
            this.multi = multi;
            this.conflicts = conflict;
        }
    }

    /**
     * Merge the address point with the building
     */
    public MergeAddrPointsAction() {
        super(tr("Merge address points"), "mergeaddr",
                tr("Move tags from address nodes inside buildings to building ways"),
                Shortcut.registerShortcut("edit:mergeaddrpoints", tr("Data: {0}", tr("Merge address points")),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();
        if (selection.isEmpty()) {
            new Notification(tr("Select both address nodes and building ways to merge"))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE).show();
            return;
        }
        List<Node> addrNodes = new LinkedList<>();
        List<Way> buildings = new LinkedList<>();
        if (!generateAddressesAndBuildings(selection, addrNodes, buildings)) {
            return;
        }

        Set<Way> overlappingWays = removeNodesInMoreThanOneBuilding(addrNodes, buildings);

        List<Command> cmds = new LinkedList<>();
        List<Pair<Node, Way>> replaced = new ArrayList<>();
        Set<Relation> modifiedRelations = new HashSet<>();

        MultiConflict multiConflict = parseBuildingWays(cmds, replaced, modifiedRelations, buildings, addrNodes);
        parseRelations(cmds, replaced, modifiedRelations);
        if (!replaced.isEmpty()) {
            final Command deleteCommand = DeleteCommand.delete(replaced.stream().map(p -> p.a).collect(Collectors.toList()));
            if (deleteCommand != null) {
                cmds.add(deleteCommand);
            }
        }

        generateFinalMessages(cmds, overlappingWays, multiConflict);
        if (!cmds.isEmpty())
            UndoRedoHandler.getInstance().add(new SequenceCommand("Merge addresses", cmds));
    }

    /**
     * Find the addresses and buildings from the selection
     * @param selection The selection to look through
     * @param addrNodes The collection to add address nodes to
     * @param buildings The collection to add buildings to
     * @return {@code true} if we can continue on
     */
    private static boolean generateAddressesAndBuildings(Collection<OsmPrimitive> selection, List<Node> addrNodes, List<Way> buildings) {
        for (OsmPrimitive p : selection) {
            // Don't use nodes if they're referenced by ways
            if (p.getType() == OsmPrimitiveType.NODE
                    && p.getReferrers().stream().map(IPrimitive::getType).noneMatch(OsmPrimitiveType.WAY::equals)) {
                for (String key : p.getKeys().keySet()) {
                    if (key.startsWith("addr:")) {
                        addrNodes.add((Node) p); // Found address node
                        break;
                    }
                }
            } else if (p.getType() == OsmPrimitiveType.WAY && p.getKeys().containsKey("building"))
                buildings.add((Way) p);
        }
        if (addrNodes.isEmpty()) {
            new Notification(tr("No address nodes found in the selection"))
                    .setIcon(JOptionPane.ERROR_MESSAGE).show();
            return false;
        }
        if (buildings.isEmpty()) {
            new Notification(tr("No building ways found in the selection"))
                    .setIcon(JOptionPane.ERROR_MESSAGE).show();
            return false;
        }
        return true;
    }

    /**
     * Remove nodes that are in more than one building
     * @param addrNodes The address nodes to look through
     * @param buildings The buildings to look through
     * @return The overlapping ways
     */
    private static Set<Way> removeNodesInMoreThanOneBuilding(List<Node> addrNodes, List<Way> buildings) {
        // find nodes covered by more than one building, see #17625
        Map<Node, Way> nodeToWayMap = new HashMap<>();
        Set<Way> overlappingWays = new HashSet<>();
        for (Way w : buildings) {
            for (Node n : addrNodes) {
                if (Geometry.nodeInsidePolygon(n, w.getNodes())) {
                    Way old = nodeToWayMap.put(n, w);
                    if (old != null) {
                        overlappingWays.add(w);
                        overlappingWays.add(old);
                    }
                }
            }
        }
        buildings.removeAll(overlappingWays);
        return overlappingWays;
    }

    private static MultiConflict parseBuildingWays(List<Command> cmds, List<Pair<Node, Way>> replaced,
                                          Set<Relation> modifiedRelations, List<Way> buildings,
                                          List<Node> addrNodes) {
        int multi = 0;
        int conflicts = 0;
        for (Way w : buildings) {
            Node mergeNode = null;
            int oldMulti = multi;
            for (Node n : addrNodes) {
                if (Geometry.nodeInsidePolygon(n, w.getNodes())) {
                    if (mergeNode != null) {
                        multi++;
                        // Multiple address nodes inside one building --
                        // skipping
                        break;
                    } else {
                        mergeNode = n;
                    }
                }
            }
            if (oldMulti != multi)
                continue;
            if (mergeNode != null) {
                conflicts += checkForConflicts(cmds, replaced, modifiedRelations, w, mergeNode);
            }
        }
        return new MultiConflict(multi, conflicts);
    }

    private static int checkForConflicts(List<Command> cmds, List<Pair<Node, Way>> replaced,
                                         Set<Relation> modifiedRelations, Way w, Node mergeNode) {
        boolean hasConflicts = false;
        int conflicts = 0;
        Map<String, String> tags = new HashMap<>();
        for (Map.Entry<String, String> entry : mergeNode.getKeys().entrySet()) {
            String newValue = entry.getValue();
            if (newValue == null)
                continue;
            String oldValue = w.getKeys().get(entry.getKey());
            if (!newValue.equals(oldValue)) {
                if (oldValue == null) {
                    tags.put(entry.getKey(), newValue);
                } else
                    hasConflicts = true;
            }
        }
        if (hasConflicts)
            conflicts++;
        if (!tags.isEmpty())
            cmds.add(new ChangePropertyCommand(Collections.singleton(w), tags));
        if (!hasConflicts) {
            replaced.add(Pair.create(mergeNode, w));
            modifiedRelations.addAll(mergeNode.referrers(Relation.class).collect(Collectors.toList()));
        }
        return conflicts;
    }

    private static void parseRelations(List<Command> cmds, List<Pair<Node, Way>> replaced, Set<Relation> modifiedRelations) {
        for (Relation r : modifiedRelations) {
            List<RelationMember> members = new ArrayList<>(r.getMembers());
            boolean modified = false;
            for (Pair<Node, Way> repl : replaced) {
                for (int i = 0; i < members.size(); i++) {
                    RelationMember member = members.get(i);
                    if (repl.a.equals(member.getMember())) {
                        members.set(i, new RelationMember(member.getRole(), repl.b));
                        modified = true;
                    }
                }
            }
            if (modified) {
                cmds.add(new ChangeMembersCommand(r, members));
            }
        }
    }

    private static void generateFinalMessages(List<Command> cmds, Set<Way> overlappingWays, MultiConflict multiConflict) {
        final int multi = multiConflict.multi;
        final int conflicts = multiConflict.conflicts;
        if (multi != 0)
            new Notification(trn("There is {0} building with multiple address nodes inside",
                    "There are {0} buildings with multiple address nodes inside", multi, multi))
                    .setIcon(JOptionPane.WARNING_MESSAGE).show();
        if (conflicts != 0)
            new Notification(trn("There is {0} building with address conflicts",
                    "There are {0} buildings with address conflicts", conflicts, conflicts))
                    .setIcon(JOptionPane.WARNING_MESSAGE).show();
        if (!overlappingWays.isEmpty())
            new Notification(tr("There are {0} buildings covering the same address node", overlappingWays.size()))
                    .setIcon(JOptionPane.WARNING_MESSAGE).show();
        if (cmds.isEmpty() && multi == 0 && conflicts == 0 && overlappingWays.isEmpty())
            new Notification(tr("No address nodes inside buildings found"))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE).show();
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
    }
}
