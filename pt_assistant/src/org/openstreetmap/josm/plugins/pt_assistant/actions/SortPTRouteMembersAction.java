// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTStop;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopToWayAssigner;
import org.openstreetmap.josm.tools.Utils;

/**
 * Sorts the members of a PT route. It orders first the ways, then the stops
 * according to the assigned ways
 *
 * @author giacomo
 *
 */
public class SortPTRouteMembersAction extends JosmAction {

    private static final String ACTION_NAME = "Sort PT Route Members";

    /**
     * Creates a new SortPTRouteMembersAction
     */
    public SortPTRouteMembersAction() {
        super(ACTION_NAME, "icons/sortptroutemembers", ACTION_NAME, null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        for (Relation rel : getLayerManager().getEditDataSet().getSelectedRelations()) {
            if (rel.hasIncompleteMembers()) {
                if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(Main.parent,
                    tr("The relation has incomplete members. Do you want to download them and continue with the sorting?"),
                    tr("Incomplete Members"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, null, null)) {

                    List<Relation> incomplete = Collections.singletonList(rel);
                    Future<?> future = Main.worker.submit(new DownloadRelationMemberTask(
                            incomplete,
                            DownloadSelectedIncompleteMembersAction.buildSetOfIncompleteMembers(incomplete),
                            Main.getLayerManager().getEditLayer()));

                        Main.worker.submit(() -> {
                            try {
                                future.get();
                                continueAfterDownload(rel);
                            } catch (InterruptedException | ExecutionException e1) {
                                 Main.error(e1);
                                return;
                            }
                        });
                } else
                    return;
            } else
                continueAfterDownload(rel);
        }
    }

    private void continueAfterDownload(Relation rel) {
        Relation newRel = new Relation(rel);
        sortPTRouteMembers(newRel);
        Main.main.undoRedo.add(new ChangeCommand(rel, newRel));
    }

    /***
     * Sort the members of the PT route.
     *
     * @param rel route to be sorted
     */
    public static void sortPTRouteMembers(Relation rel) {
        if (!RouteUtils.isVersionTwoPTRoute(rel)) {
            return;
        }

        if (rel.hasTag("fixme:relation", "order members")) {
            rel.remove("fixme:relation");
        }

        List<RelationMember> members = new ArrayList<>();
        List<RelationMember> oldMembers = rel.getMembers();
        for (int i = 0; i < oldMembers.size(); i++) {
            RelationMember rm = oldMembers.get(i);
            if (!PTStop.isPTPlatform(rm) && !PTStop.isPTStopPosition(rm))
                members.add(new RelationMember("", rm.getMember()));
            else
                members.add(rm);
            rel.removeMember(0);
        }
        members = new RelationSorter().sortMembers(members);

        List<RelationMember> stops = new ArrayList<>();
        List<RelationMember> wayMembers = new ArrayList<>();
        List<Way> ways = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            RelationMember rm = members.get(i);
            if (PTStop.isPTPlatform(rm) || PTStop.isPTStopPosition(rm))
                stops.add(rm);
            else {
                wayMembers.add(rm);
                if (rm.getType() == OsmPrimitiveType.WAY)
                    ways.add(rm.getWay());
            }
        }

        Map<String, PTStop> stopsByName = new HashMap<>();
        List<PTStop> unnamed = new ArrayList<>();
        stops.forEach(rm -> {
            String name = getStopName(rm.getMember());
            if (name != null) {
                if (!stopsByName.containsKey(name))
                    stopsByName.put(name, new PTStop(rm));
                else
                    stopsByName.get(name).addStopElement(rm);
            } else {
                unnamed.add(new PTStop(rm));
            }
        });

        StopToWayAssigner assigner = new StopToWayAssigner(ways);
        List<PTStop> ptstops = new ArrayList<>(stopsByName.values());
        Map<Way, List<PTStop>> wayStop = new HashMap<>();
        ptstops.forEach(stop -> {
            Way way = assigner.get(stop);
            if (way == null) {
                addStopToRelation(rel, stop);
            }
            if (!wayStop.containsKey(way))
                wayStop.put(way, new ArrayList<PTStop>());
            wayStop.get(way).add(stop);
        });

        unnamed.forEach(stop -> {
            Way way = assigner.get(stop);
            if (way == null) {
                addStopToRelation(rel, stop);
            }
            if (!wayStop.containsKey(way))
                wayStop.put(way, new ArrayList<PTStop>());
            wayStop.get(way).add(stop);
        });

        for (int i = 0; i < wayMembers.size(); i++) {
            RelationMember wm = wayMembers.get(i);
            Way prev = null;
            Way next = null;
            if (i > 0) {
                RelationMember wmp = wayMembers.get(i-1);
                if (wmp.getType() == OsmPrimitiveType.WAY)
                    prev = wmp.getWay();
            }
            if (i < wayMembers.size() - 1) {
                RelationMember wmn = wayMembers.get(i+1);
                if (wmn.getType() == OsmPrimitiveType.WAY)
                    next = wmn.getWay();
            }

            if (wm.getType() == OsmPrimitiveType.WAY) {
                Way curr = wm.getWay();
                List<PTStop> stps = wayStop.get(curr);
                if (stps != null) {
                    if (stps.size() > 1)
                        stps = sortSameWayStops(stps, curr, prev, next);
                    stps.forEach(stop -> {
                        if (stop != null) {
                            addStopToRelation(rel, stop);
                        }
                    });
                }
            }
        }

        wayMembers.forEach(rel::addMember);
    }

    private static void addStopToRelation(Relation rel, PTStop stop) {
        if (stop.getStopPositionRM() != null)
            rel.addMember(stop.getStopPositionRM());
        if (stop.getPlatformRM() != null)
            rel.addMember(stop.getPlatformRM());
    }

    private static List<PTStop> sortSameWayStops(List<PTStop> stps, Way way, Way prev, Way next) {
        Map<Node, List<PTStop>> closeNodes = new HashMap<>();
        List<PTStop> noLocationStops = new ArrayList<>();
        List<Node> nodes = way.getNodes();
        for (PTStop stop : stps) {
            Node closest = findClosestNode(stop, nodes);
            if (closest == null) {
                noLocationStops.add(stop);
                continue;
            }
            if (!closeNodes.containsKey(closest)) {
                closeNodes.put(closest, new ArrayList<>());
            }
            closeNodes.get(closest).add(stop);
        }

        boolean reverse = false;

        if (prev != null) {
            reverse = prev.firstNode().equals(way.lastNode())
                        || prev.lastNode().equals(way.lastNode());
        } else if (next != null) {
            reverse = next.firstNode().equals(way.firstNode())
                    || next.lastNode().equals(way.firstNode());
        }

        if (reverse)
            Collections.reverse(nodes);

        List<PTStop> ret = getSortedStops(nodes, closeNodes);
        ret.addAll(noLocationStops);
        return ret;
    }

    private static List<PTStop> getSortedStops(List<Node> nodes,
            Map<Node, List<PTStop>> closeNodes) {

        List<PTStop> ret = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            Node prevNode = i > 0 ? nodes.get(i - 1) : n;
            List<PTStop> stops = closeNodes.get(n);
            if (stops != null) {
                if (stops.size() > 1) {
                    stops.sort((s1, s2) -> {
                        Double d1 = stopEastNorth(s1).distance(prevNode.getEastNorth());
                        Double d2 = stopEastNorth(s2).distance(prevNode.getEastNorth());
                        return d1.compareTo(d2);
                    });
                }
                stops.forEach(ret::add);
            }
        }

        return ret;
    }

    private static Node findClosestNode(PTStop stop, List<Node> nodes) {
        EastNorth stopEN = stopEastNorth(stop);
        if (stopEN == null)
            return null;
        double minDist = Double.MAX_VALUE;
        Node closest = null;
        for (Node node : nodes) {
            double dist = node.getEastNorth().distance(stopEN);
            if (dist < minDist) {
                minDist = dist;
                closest = node;
            }
        }
        return closest;
    }

    private static EastNorth stopEastNorth(PTStop stop) {
        if (stop.getStopPosition() != null)
            return stop.getStopPosition().getEastNorth();
        OsmPrimitive prim = stop.getPlatform();
        if (prim.getType() == OsmPrimitiveType.WAY)
            return ((Way) prim).firstNode().getEastNorth();
        else if (prim.getType() == OsmPrimitiveType.NODE)
            return ((Node) prim).getEastNorth();
        else
            return null;
    }

    private static String getStopName(OsmPrimitive p) {
        for (Relation ref : Utils.filteredCollection(p.getReferrers(), Relation.class)) {
            if (ref.hasTag("type", "public_transport")
                    && ref.hasTag("public_transport", "stop_area")
                    && ref.getName() != null) {
                return ref.getName();
            }
        }
        return p.getName();
    }

    @Override
    protected void updateEnabledState(
            Collection<? extends OsmPrimitive> selection) {
        if (selection.isEmpty()) {
            setEnabled(false);
            return;
        }

        for (OsmPrimitive sel : selection) {
            if (sel.getType() != OsmPrimitiveType.RELATION || !RouteUtils.isPTRoute((Relation) sel)) {
                setEnabled(false);
                return;
            }
        }

        setEnabled(true);
    }
}
