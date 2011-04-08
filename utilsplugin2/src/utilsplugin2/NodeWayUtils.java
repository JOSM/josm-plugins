// License: GPL. Copyright 2011 by Alexei Kasatkin
package utilsplugin2;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;

import static org.openstreetmap.josm.tools.I18n.tr;


/**
 * Class with some useful functions that are reused in extend selection actions
 *
 */
public final class NodeWayUtils {

    static final int maxLevel = Main.pref.getInteger("selection.maxrecursion", 5);
    static final int maxWays = Main.pref.getInteger("selection.maxfoundways", 2000);
    static final int maxWays1 = Main.pref.getInteger("selection.maxfoundwaysrec", 200);

    /**
     * Find the neighbours of node n on the way w and put them in given collection
     * @param w way on which the search goes
     * @param n node to find its neighbours
     * @param nodes collection to place the nodes we found
     */
    static void addNeighbours(Way w, Node n, Collection<Node> nodes) {
        List<Node> nodeList = w.getNodes();
        
        int idx = nodeList.indexOf(n);
        if (idx == -1) return;

        // add previous element
        if (idx > 0) {
            nodes.add(nodeList.get(idx - 1));
        }
        // add next element
        if (idx < nodeList.size() - 1) {
            nodes.add(nodeList.get(idx + 1));
        }
        if (w.isClosed()) {
            // cyclic neighbours detection
            if (idx == 0) {
                nodes.add(nodeList.get(nodeList.size() - 2));
            }
            if (idx == nodeList.size() - 1) {
                nodes.add(nodeList.get(1));
            }
        }
     }

    /**
     * Adds all ways attached to way to specified collection
     * @param w way to find attached ways
     * @param ways  collection to place the ways we found
     */
    static int addWaysConnectedToWay(Way w, Set<Way> ways) {
         int s = ways.size();
        List<Node> nodes = w.getNodes();
        for (Node n: nodes) {
            ways.addAll(OsmPrimitive.getFilteredList(n.getReferrers(), Way.class));
        }
        return ways.size() - s;
    }

    /**
     * Adds all ways attached to node to specified collection
     * @param n Node to find attached ways
     * @param ways  collection to place the ways we found
     */
    static int addWaysConnectedToNode(Node n, Set<Way> ways) {
        int s = ways.size();
        ways.addAll(OsmPrimitive.getFilteredList(n.getReferrers(), Way.class));
        return ways.size() - s;
    }

    /**
     * Adds all ways intersecting one way to specified set
     * @param ways collection of ways to search
     * @param w way to check intersections
     * @param newWays set to place the ways we found
     */
    static int addWaysIntersectingWay(Collection<Way> ways, Way w, Set<Way> newWays,Set<Way> excludeWays) {
        List<Pair<Node, Node>> nodePairs = w.getNodePairs(false);
        int count=0;
        for (Way anyway: ways) {
            if (anyway == w) continue;
            if (newWays.contains(anyway) || excludeWays.contains(anyway) ) continue;

            List<Pair<Node, Node>> nodePairs2 = anyway.getNodePairs(false);
            loop: for (Pair<Node,Node> p1 : nodePairs) {
                for (Pair<Node,Node> p2 : nodePairs2) {
                    if (null!=Geometry.getSegmentSegmentIntersection(
                            p1.a.getEastNorth(),p1.b.getEastNorth(),
                            p2.a.getEastNorth(),p2.b.getEastNorth())) {
                            newWays.add(anyway);
                            count++;
                            break loop;
                    }
                }
            }
        }
        return count;
    }


        static int addWaysIntersectingWay(Collection<Way> ways, Way w, Set<Way> newWays) {
        List<Pair<Node, Node>> nodePairs = w.getNodePairs(false);
        int count=0;
        for (Way anyway: ways) {
            if (anyway == w) continue;
            if (newWays.contains(anyway)) continue;
            List<Pair<Node, Node>> nodePairs2 = anyway.getNodePairs(false);
            loop: for (Pair<Node,Node> p1 : nodePairs) {
                for (Pair<Node,Node> p2 : nodePairs2) {
                    if (null!=Geometry.getSegmentSegmentIntersection(
                            p1.a.getEastNorth(),p1.b.getEastNorth(),
                            p2.a.getEastNorth(),p2.b.getEastNorth())) {
                            newWays.add(anyway);
                            count++;
                            break loop;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Adds all ways from allWays intersecting initWays way to specified set newWays
     * @param allWays collection of ways to search
     * @param initWays ways to check intersections
     * @param newWays set to place the ways we found
     */
    static int addWaysIntersectingWays(Collection<Way> allWays, Collection<Way> initWays, Set<Way> newWays) {
        int count=0;
        for (Way w : initWays){
            count+=addWaysIntersectingWay(allWays, w, newWays);
        }
        return count;
    }

    static int addWaysConnectedToNodes(Set<Node> selectedNodes, Set<Way> newWays) {
        int s = newWays.size();
        for (Node node: selectedNodes) {
            addWaysConnectedToNode(node, newWays);
        }
        return newWays.size() - s;
    }

    static int addNodesConnectedToWays(Set<Way> initWays, Set<Node> newNodes) {
        int s = newNodes.size();
        for (Way w: initWays) {
                newNodes.addAll(w.getNodes());
        }
        return newNodes.size()-s;
    }

    static void addWaysIntersectingWaysRecursively
            (Collection<Way> allWays, Collection<Way> initWays, Set<Way> newWays)
    {
            Set<Way> foundWays = new LinkedHashSet<Way>();
            foundWays.addAll(initWays);
            newWays.addAll(initWays);
            Set<Way> newFoundWays = new LinkedHashSet<Way>();

            int level=0,c;
            do {
                 c=0;
                 newFoundWays = new LinkedHashSet<Way>();
                 for (Way w : foundWays){
                      c+=addWaysIntersectingWay(allWays, w, newFoundWays,newWays);
                 }
                 foundWays = newFoundWays;
                 newWays.addAll(newFoundWays);
                 level++;
//                 System.out.printf("%d: %d ways added to selection intersectiong\n",level,c);
                 if (c>maxWays1) {
                       JOptionPane.showMessageDialog(Main.parent,
                                tr("Too many ways are added: {0}!",c),
                                        tr("Warning"),
                                        JOptionPane.WARNING_MESSAGE);
                       return;
                 }
            } while ( c >0 && level < maxLevel );
            return;
    }

 static void addWaysConnectedToWaysRecursively
            (Collection<Way> initWays, Set<Way> newWays)
    {
            int level=0,c;
            newWays.addAll(initWays);
            do {
                 c=0;
                 Set<Way> foundWays = new LinkedHashSet<Way>();
                 foundWays.addAll(newWays);
                 for (Way w : foundWays){
                      c+=addWaysConnectedToWay(w, newWays);
                 }
                 level++;
//                 System.out.printf("%d: %d ways added to selection\n",level,c);
                 if (c>maxWays) {
                       JOptionPane.showMessageDialog(Main.parent,
                                tr("Too many ways are added: {0}!",c),
                                        tr("Warning"),
                                        JOptionPane.WARNING_MESSAGE);
                       return;
                 }
            } while ( c >0 && level < maxLevel );
            return;
    }



}
