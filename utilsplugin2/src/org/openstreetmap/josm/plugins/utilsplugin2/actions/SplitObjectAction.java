// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygon;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygonCreationException;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a closed way (polygon) into two closed ways or a multipolygon into two separate multipolygons.
 *
 * The closed ways are just split at the selected nodes (which must be exactly two).
 * The nodes remain in their original order.
 *
 * This is similar to SplitWayAction with the addition that the split ways are closed
 * immediately.
 */
public class SplitObjectAction extends JosmAction {
    private static final String ALLOW_INV_MP_SPLIT_KEY = "utilsplugin2.split-object.allowInvalidMultipolygonSplit";

    /**
     * Create a new SplitObjectAction.
     */
    public SplitObjectAction() {
        super(tr("Split Object"), "splitobject", tr("Split an object at the selected nodes."),
                Shortcut.registerShortcut("tools:splitobject", tr("More tools: {0}", tr("Split Object")), KeyEvent.VK_X, Shortcut.ALT),
                true);
        putValue("help", ht("/Action/SplitObject"));
    }

    /**
     * Called when the action is executed.
     *
     * This method performs an expensive check whether the selection clearly defines one
     * of the split actions outlined above, and if yes, calls the splitObject method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (!checkSelection(ds.getSelected())) {
            showWarningNotification(tr("The current selection cannot be used for splitting."));
            return;
        }

        List<Node> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
        List<Way> selectedWays = new ArrayList<>(ds.getSelectedWays());
        List<Relation> selectedRelations = new ArrayList<>(ds.getSelectedRelations());

        Way selectedWay = null;
        Way splitWay = null;

        if (selectedNodes.size() != 2) {            // if not exactly 2 nodes are selected - try to find split way
            selectedNodes.clear();                  // empty selected nodes (see #8237)
            for (Way selWay : selectedWays) {       // we assume not more 2 ways in the list
                if (selWay != null &&               // If one of selected ways is not closed we have it to get split points
                        selWay.isUsable() &&
                        selWay.getNodesCount() > 0 &&
                        !selWay.isClosed() &&
                        selWay.getKeys().isEmpty()) {
                    selectedNodes.add(selWay.firstNode());
                    selectedNodes.add(selWay.lastNode());
                    splitWay = selWay;
                } else {
                    selectedWay = selWay;           // use another way as selected way
                }
            }
        } else if (selectedWays.size() == 1) {
            selectedWay = selectedWays.get(0);      // two nodes and a way is selected, so use this selected way
        }

        if (selectedRelations.size() > 1) {
            showWarningNotification(tr("Only one multipolygon can be selected for splitting"));
            return;
        }

        if ((selectedRelations.size() == 1) && selectedRelations.get(0).isMultipolygon()) {
            Relation selectedMultipolygon = selectedRelations.get(0);
            if (splitWay == null) {
                showWarningNotification(tr("Splitting multipolygons requires a split way to be selected"));
                return;
            }

            boolean allowInvalidMpSplit = Config.getPref().getBoolean(ALLOW_INV_MP_SPLIT_KEY, false);

            splitMultipolygonAtWayChecked(selectedMultipolygon, splitWay, allowInvalidMpSplit);
            return;
        }

        // If only nodes are selected, try to guess which way to split. This works if there
        // is exactly one way that all nodes are part of.
        if (selectedWay == null && !selectedNodes.isEmpty()) {
            Map<Way, Integer> wayOccurenceCounter = new HashMap<>();
            for (Node n : selectedNodes) {
                for (Way w : n.getParentWays()) {
                    if (!w.isUsable()) {
                        continue;
                    }
                    // Only closed ways with at least four distinct nodes
                    // (i.e. five members since the first/last is listed twice)
                    // can be split into two objects
                    if (w.getNodesCount() < 5 || !w.isClosed()) {
                        continue;
                    }
                    for (Node wn : w.getNodes()) {
                        if (n.equals(wn)) {
                            Integer old = wayOccurenceCounter.get(w);
                            wayOccurenceCounter.put(w, (old == null) ? 1 : old + 1);
                            break;
                        }
                    }
                }
            }
            if (wayOccurenceCounter.isEmpty()) {
                showWarningNotification(
                        trn("The selected node is not in the middle of any way.",
                                "The selected nodes are not in the middle of any way.",
                                selectedNodes.size()));
                return;
            }

            for (Map.Entry<Way, Integer> entry : wayOccurenceCounter.entrySet()) {
                if (entry.getValue().equals(selectedNodes.size())) {
                    if (selectedWay != null) {
                        showWarningNotification(
                                trn("There is more than one way using the node you selected. Please select the way as well.",
                                        "There is more than one way using the nodes you selected. Please select the way as well.",
                                        selectedNodes.size())
                                );
                        return;
                    }
                    selectedWay = entry.getKey();
                }
            }

            if (selectedWay == null) {
                showWarningNotification(tr("The selected nodes do not share the same way."));
                return;
            }

            // If a way and nodes are selected, verify that the nodes
            // are part of the way and that the way is closed.
        } else if (selectedWay != null && !selectedNodes.isEmpty()) {
            if (!selectedWay.isClosed()) {
                showWarningNotification(tr("The selected way is not closed."));
                return;
            }
            HashSet<Node> nds = new HashSet<>(selectedNodes);
            nds.removeAll(selectedWay.getNodes());
            if (!nds.isEmpty()) {
                showWarningNotification(
                        trn("The selected way does not contain the selected node.",
                                "The selected way does not contain all the selected nodes.",
                                selectedNodes.size()));
                return;
            }
        } else if (selectedWay != null && selectedNodes.isEmpty()) {
            showWarningNotification(
                    tr("The selected way is not a split way, please select split points or split way too."));
            return;
        }

        // we're guaranteed to have two nodes
        Node node1 = selectedNodes.get(0);
        int nodeIndex1 = -1;
        Node node2 = selectedNodes.get(1);
        int nodeIndex2 = -1;
        int i = 0;
        for (Node wn : selectedWay.getNodes()) {
            if (nodeIndex1 == -1 && wn.equals(node1)) {
                nodeIndex1 = i;
            } else if (nodeIndex2 == -1 && wn.equals(node2)) {
                nodeIndex2 = i;
            }
            i++;
        }
        // both nodes aren't allowed to be consecutive
        if ((splitWay == null || splitWay.getNodesCount() == 2)
                && (nodeIndex1 == nodeIndex2 + 1 || nodeIndex2 == nodeIndex1 + 1 ||
                // minus 2 because we've a circular way where
                // the penultimate node is the last unique one
                        (nodeIndex1 == 0 && nodeIndex2 == selectedWay.getNodesCount() - 2)
                        || (nodeIndex2 == 0 && nodeIndex1 == selectedWay.getNodesCount() - 2))) {
            showWarningNotification(
                    tr("The selected nodes can not be consecutive nodes in the object."));
            return;
        }
        splitWayChecked(selectedNodes, selectedWay, splitWay);
    }

    private void splitWayChecked(List<Node> selectedNodes, Way selectedWay, Way splitWay) {
        List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(selectedWay, selectedNodes);
        if (wayChunks != null) {
            // close the chunks
            // update the logic - if we have splitWay not null, we have to add points from it to both chunks (in the correct direction)
            if (splitWay == null) {
                for (List<Node> wayChunk : wayChunks) {
                    wayChunk.add(wayChunk.get(0));
                }
            } else {
                for (List<Node> wayChunk : wayChunks) {
                    // check direction of the chunk and add splitWay nodes in the correct order
                    List<Node> way = splitWay.getNodes();
                    if (wayChunk.get(0).equals(splitWay.firstNode())) {
                        // add way to the end in the opposite direction.
                        way.remove(way.size()-1); // remove the last node
                        Collections.reverse(way);
                    } else {
                        // add way to the end in the given direction, remove the first node
                        way.remove(0);
                    }
                    wayChunk.addAll(way);
                }
            }
            SplitWayCommand result = SplitWayCommand.splitWay(
                    selectedWay, wayChunks, Collections.<OsmPrimitive>emptyList());
            if (splitWay != null) {
                result.executeCommand();
                DeleteCommand delCmd = new DeleteCommand(splitWay);
                delCmd.executeCommand();
                UndoRedoHandler.getInstance().add(new SplitObjectCommand(Arrays.asList(result, delCmd)), false);
            } else {
                UndoRedoHandler.getInstance().add(result);
            }
            getLayerManager().getEditDataSet().setSelected(result.getNewSelection());
        }

    }

    /**
     * Splits a multipolygon into two separate multipolygons along a way using {@link #splitMultipolygonAtWay}
     * if the resulting multipolygons are valid.
     * Inner polygon rings are automatically assigned to the appropriate multipolygon relation based on their location.
     * Performs a complete check of the resulting multipolygons using {@link MultipolygonTest} and aborts + displays
     * warning messages to the user if errors are encountered.
     * @param mpRelation the multipolygon relation to split.
     * @param splitWay the way along which the multipolygon should be split.
     * Must start and end on the outer ways and must not intersect with or connect to any of the multipolygon inners.
     * @param allowInvalidSplit allow multipolygon splits that result in invalid multipolygons.
     * @return the new multipolygon relations after splitting + the executed commands
     * (already executed and added to the {@link UndoRedoHandler}).
     * Relation and command lists are empty if split did not succeed.
     */
    public static Pair<List<Relation>, List<Command>> splitMultipolygonAtWayChecked(
            Relation mpRelation, Way splitWay, boolean allowInvalidSplit) {

        CheckParameterUtil.ensureParameterNotNull(mpRelation, "mpRelation");
        CheckParameterUtil.ensureParameterNotNull(splitWay, "splitWay");
        CheckParameterUtil.ensureThat(mpRelation.isMultipolygon(), "mpRelation.isMultipolygon");

        try {
            Pair<List<Relation>, List<Command>> splitResult = splitMultipolygonAtWay(mpRelation, splitWay, allowInvalidSplit);
            List<Relation> mpRelations = splitResult.a;
            List<Command> commands = splitResult.b;

            List<TestError> mpErrorsPostSplit = new ArrayList<>();
            for (Relation mp : mpRelations) {
                MultipolygonTest mpTestPostSplit = new MultipolygonTest();

                mpTestPostSplit.visit(mp);

                List<TestError> severeErrors = mpTestPostSplit.getErrors().stream()
                    .filter(e -> e.getSeverity().getLevel() <= Severity.ERROR.getLevel())
                    .collect(Collectors.toList());

                mpErrorsPostSplit.addAll(severeErrors);
            }

            // Commands were already executed. Either undo them on error or add them to the UndoRedoHandler
            if (!mpErrorsPostSplit.isEmpty()) {
                if (!allowInvalidSplit) {
                    showWarningNotification(tr("Multipolygon split would create invalid multipolygons! Split was not performed."));
                    for (TestError testError : mpErrorsPostSplit) {
                        showWarningNotification(testError.getMessage());
                    }
                    for (int i = commands.size()-1; i >= 0; --i) {
                        commands.get(i).undoCommand();
                    }

                    return new Pair<>(new ArrayList<>(), new ArrayList<>());
                } else {
                    showWarningNotification(tr("Multipolygon split created invalid multipolygons! Please review and fix these errors."));
                    for (TestError testError : mpErrorsPostSplit) {
                        showWarningNotification(testError.getMessage());
                    }
                }
            }
            if (commands.size() > 1)
                UndoRedoHandler.getInstance().add(new SplitObjectCommand(commands), false);
            else
                UndoRedoHandler.getInstance().add(commands.iterator().next(), false);

            mpRelation.getDataSet().setSelected(mpRelations);
            return splitResult;

        } catch (IllegalArgumentException e) {
            // Changes were already undone in splitMultipolygonAtWay
            showWarningNotification(e.getMessage());
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Splits a multipolygon into two separate multipolygons along a way.
     * Inner polygon rings are automatically assigned to the appropriate multipolygon relation based on their location.
     * @param mpRelation the multipolygon relation to split.
     * @param splitWay the way along which the multipolygon should be split.
     * Must start and end on the outer ways and must not intersect with or connect to any of the multipolygon inners.
     * @param allowInvalidSplit allow multipolygon splits that result in invalid multipolygons.
     * @return the new multipolygon relations after splitting + the commands required for the split
     * (already executed, but not yet added to the {@link UndoRedoHandler}).
     * @throws IllegalArgumentException if the multipolygon has errors and/or the splitWay is unsuitable for
     * splitting the multipolygon (e.g. because it crosses inners and {@code allowInvalidSplit == false}).
     */
    public static Pair<List<Relation>, List<Command>> splitMultipolygonAtWay(Relation mpRelation,
                                                                             Way splitWay,
                                                                             boolean allowInvalidSplit) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(mpRelation, "mpRelation");
        CheckParameterUtil.ensureParameterNotNull(splitWay, "splitWay");
        CheckParameterUtil.ensureThat(mpRelation.isMultipolygon(), "mpRelation.isMultipolygon");

        List<Command> commands = new ArrayList<>();
        List<Relation> mpRelations = new ArrayList<>();
        mpRelations.add(mpRelation);

        Multipolygon mp = new Multipolygon(mpRelation);

        if (mp.isIncomplete()) {
            throw new IllegalArgumentException(tr("Cannot split incomplete multipolygon"));
        }

        /* Splitting multipolygons with multiple outer rings technically works, but assignment of parts is
         * unpredictable and could lead to unwanted fragmentation. */
        if (mp.getOuterPolygons().size() > 1) {
            throw new IllegalArgumentException(tr("Cannot split multipolygon with multiple outer polygons"));
        }

        if (mpRelation.getMembers().stream().filter(RelationMember::isWay).anyMatch(w -> w.getWay() == splitWay)) {
            throw new IllegalArgumentException(tr("Split ways must not be a member of the multipolygon"));
        }

        if (!mp.getOpenEnds().isEmpty()) {
            throw new IllegalArgumentException(tr("Multipolygon has unclosed rings"));
        }

        List<Way> outerWaysUnsplit = mp.getOuterWays();

        Node firstNode = splitWay.firstNode();
        Node lastNode = splitWay.lastNode();

        Set<Way> firstNodeWays = firstNode.getParentWays().stream().filter(outerWaysUnsplit::contains).collect(Collectors.toSet());
        Set<Way> lastNodeWays = lastNode.getParentWays().stream().filter(outerWaysUnsplit::contains).collect(Collectors.toSet());

        if (firstNodeWays.isEmpty() || lastNodeWays.isEmpty()) {
            throw new IllegalArgumentException(tr("The split way does not start/end on the multipolygon outer ways"));
        }

        commands.addAll(splitMultipolygonWaysAtNodes(mpRelation, Arrays.asList(firstNode, lastNode)));

        // Need to refresh the multipolygon members after splitting
        mp = new Multipolygon(mpRelation);

        List<JoinedPolygon> joinedOuter = null;
        try {
            joinedOuter = MultipolygonBuilder.joinWays(mp.getOuterWays());
        } catch (JoinedPolygonCreationException e) {
            for (int i = commands.size()-1; i >= 0; --i) {
                commands.get(i).undoCommand();
            }
            throw new IllegalArgumentException(tr("Error in multipolygon: {0}", e.getMessage()), e);
        }

        // Find outer subring that should be moved to the new multipolygon
        for (JoinedPolygon outerRing : joinedOuter) {
            int firstIndex = -1;
            int lastIndex = -1;

            if (outerRing.nodes.containsAll(Arrays.asList(firstNode, lastNode))) {
                for (int i = 0; i < outerRing.ways.size() && (firstIndex == -1 || lastIndex == -1); i++) {
                    Way w = outerRing.ways.get(i);
                    boolean reversed = outerRing.reversed.get(i);

                    Node cStartNode = reversed ? w.lastNode() : w.firstNode();
                    Node cEndNode = reversed ? w.firstNode() : w.lastNode();

                    if (cStartNode == firstNode) {
                        firstIndex = i;
                    }
                    if (cEndNode == lastNode) {
                        lastIndex = i;
                    }
                }
            }

            if (firstIndex != -1 && lastIndex != -1) {
                int startIt = -1;
                int endIt = -1;

                if (firstIndex <= lastIndex) {
                    startIt = firstIndex;
                    endIt = lastIndex + 1;
                } else {
                    startIt = lastIndex + 1;
                    endIt = firstIndex;
                }

                /* Found outer subring for new multipolygon, now create new mp relation and move
                 * members + close old and new mp with split way */
                List<Way> newOuterRingWays = outerRing.ways.subList(startIt, endIt);

                RelationMember splitWayMember = new RelationMember("outer", splitWay);

                List<RelationMember> mpMembers = mpRelation.getMembers();
                List<RelationMember> newMpMembers = mpMembers.stream()
                    .filter(m -> m.isWay() && newOuterRingWays.contains(m.getWay()))
                    .collect(Collectors.toList());

                mpMembers.removeAll(newMpMembers);
                mpMembers.add(splitWayMember);

                Relation newMpRelation = new Relation(mpRelation, true, false);
                newMpMembers.add(splitWayMember);
                newMpRelation.setMembers(newMpMembers);

                Multipolygon newMp = new Multipolygon(newMpRelation);

                // Check if inners need to be moved to new multipolygon
                for (PolyData inner : mp.getInnerPolygons()) {
                    for (PolyData newOuter : newMp.getOuterPolygons()) {
                        Intersection intersection = newOuter.contains(inner.get());
                        switch (intersection) {
                            case INSIDE:
                                Collection<Long> innerWayIds = inner.getWayIds();
                                List<RelationMember> innerWayMembers = mpMembers.stream()
                                  .filter(m -> m.isWay() && innerWayIds.contains(m.getWay().getUniqueId()))
                                  .collect(Collectors.toList());

                                mpMembers.removeAll(innerWayMembers);
                                for (RelationMember innerWayMember : innerWayMembers) {
                                    newMpRelation.addMember(innerWayMember);
                                }

                                break;
                            case CROSSING:
                                if (!allowInvalidSplit) {
                                    for (int i = commands.size()-1; i >= 0; --i) {
                                        commands.get(i).undoCommand();
                                    }

                                    throw new IllegalArgumentException(tr("Split way crosses inner polygon"));
                                }

                                break;
                            default:
                                break;
                        }
                    }
                }

                List<Command> mpCreationCommands = new ArrayList<>();
                mpCreationCommands.add(new ChangeMembersCommand(mpRelation, mpMembers));
                mpCreationCommands.add(new AddCommand(mpRelation.getDataSet(), newMpRelation));
                mpCreationCommands.forEach(Command::executeCommand);
                commands.addAll(mpCreationCommands);

                mpRelations.add(newMpRelation);
            }
        }

        return new Pair<>(mpRelations, commands);
    }

    /**
     * Splits all ways of the multipolygon at the given nodes
     * @param mpRelation the multipolygon relation whose ways should be split
     * @param splitNodes the nodes at which the multipolygon ways should be split
     * @return a list of (already executed) commands for the split ways
     */
    public static List<SplitWayCommand> splitMultipolygonWaysAtNodes(Relation mpRelation, Collection<Node> splitNodes) {
        CheckParameterUtil.ensureParameterNotNull(mpRelation, "mpRelation");
        CheckParameterUtil.ensureParameterNotNull(splitNodes, "splitNodes");

        Set<Way> mpWays = mpRelation.getMembers().stream()
            .filter(RelationMember::isWay)
            .map(RelationMember::getWay)
            .collect(Collectors.toSet());

        List<SplitWayCommand> splitCmds = new ArrayList<>();
        for (Way way : mpWays) {
            List<Node> containedNodes = way.getNodes().stream()
                .filter(n -> splitNodes.contains(n) &&
                    (way.isClosed() || (n != way.firstNode() && n != way.lastNode())))
                .collect(Collectors.toList());

            if (!containedNodes.isEmpty()) {
                List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(way, containedNodes);

                if (wayChunks != null) {
                    SplitWayCommand result = SplitWayCommand.splitWay(
                                    way, wayChunks, Collections.<OsmPrimitive>emptyList());
                    result.executeCommand(); // relation members are overwritten/broken if there are multiple unapplied splits
                    splitCmds.add(result);
                }
            }
        }

        return splitCmds;
    }

    /**
     * Checks if the selection consists of something we can work with.
     * Checks only if the number and type of items selected looks good;
     * does not check whether the selected items are really a valid
     * input for splitting (this would be too expensive to be carried
     * out from the selectionChanged listener).
     * @param selection the selection
     * @return true if the selection is usable
     */
    private static boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
        int node = 0;
        int ways = 0;
        int multipolygons = 0;
        for (OsmPrimitive p : selection) {
            if (p instanceof Way) {
                ways++;
            } else if (p instanceof Node) {
                node++;
            } else if (p.isMultipolygon()) {
                multipolygons++;
            } else
                return false;
        }
        return (node == 2 || ways == 1 || ways == 2) || //only 2 nodes selected. one split-way selected. split-way + way to split.
               (multipolygons == 1 && ways == 1);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (selection == null) {
            setEnabled(false);
            return;
        }
        setEnabled(checkSelection(selection));
    }

    private static void showWarningNotification(String msg) {
        new Notification(msg)
        .setIcon(JOptionPane.WARNING_MESSAGE).show();
    }

    private static class SplitObjectCommand extends SequenceCommand {
        SplitObjectCommand(Collection<Command> sequenz) {
            super(tr("Split Object"), sequenz, true);
            setSequenceComplete(true);
        }
    }
}
