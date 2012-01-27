package utilsplugin2.dumbutils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.*;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Replaces already existing object (id>0) with a new object (id<0).
 *
 * @author Zverik
 */
public class ReplaceGeometryAction extends JosmAction {
    private static final String TITLE = tr("Replace Geometry");

    public ReplaceGeometryAction() {
        super(TITLE, "dumbutils/replacegeometry", tr("Replace geometry of selected object with a new one"),
                Shortcut.registerShortcut("tools:replacegeometry", tr("Tool: {0}", tr("Replace Geometry")), KeyEvent.VK_G, Shortcut.GROUP_HOTKEY, Shortcut.SHIFT_DEFAULT)
                , true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getCurrentDataSet() == null) {
            return;
        }

        // There must be two ways selected: one with id > 0 and one new.
        List<OsmPrimitive> selection = new ArrayList<OsmPrimitive>(getCurrentDataSet().getSelected());
        if (selection.size() != 2) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("This tool replaces geometry of one object with another, and so requires exactly two objects to be selected."),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        OsmPrimitive firstObject = selection.get(0);
        OsmPrimitive secondObject = selection.get(1);

        if (firstObject instanceof Way && secondObject instanceof Way) {
            replaceWayWithWay(Arrays.asList((Way) firstObject, (Way) secondObject));
        } else if (firstObject instanceof Node && secondObject instanceof Node) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("To replace a node with a node, use the node merge tool."),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        } else if (firstObject instanceof Node) {
            replaceNode((Node) firstObject, secondObject);
        } else if (secondObject instanceof Node) {
            replaceNode((Node) secondObject, firstObject);
        } else {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("This tool can only replace a node with a way, a node with a multipolygon, or a way with a way."),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }

    /**
     * Replace or upgrade a node to a way or multipolygon
     *
     * @param node
     * @param target
     */
    public void replaceNode(Node node, OsmPrimitive target) {
        if (!node.getReferrers().isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent, tr("Node belongs to way(s) or relation(s), cannot replace."),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (target instanceof Relation && !((Relation) target).isMultipolygon()) {
            JOptionPane.showMessageDialog(Main.parent, tr("Relation is not a multipolygon, cannot be used as a replacement."),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Node nodeToReplace = null;
        // see if we need to replace a node in the replacement way to preserve connection in history
        if (!node.isNew()) {
            // Prepare a list of nodes that are not important
            Collection<Node> nodePool = new HashSet<Node>();
            if (target instanceof Way) {
                nodePool.addAll(getUnimportantNodes((Way) target));
            } else if (target instanceof Relation) {
                for (RelationMember member : ((Relation) target).getMembers()) {
                    if ((member.getRole().equals("outer") || member.getRole().equals("inner"))
                            && member.isWay()) {
                        // TODO: could consider more nodes, such as nodes that are members of other ways,
                        // just need to replace occurences in all referrers
                        nodePool.addAll(getUnimportantNodes(member.getWay()));
                    }
                }
            } else {
                assert false;
            }
            nodeToReplace = findNearestNode(node, nodePool);
        }

        List<Command> commands = new ArrayList<Command>();
        AbstractMap<String, String> nodeTags = (AbstractMap<String, String>) node.getKeys();

        // merge tags
        Collection<Command> tagResolutionCommands = getTagConflictResolutionCommands(node, target);
        if (tagResolutionCommands == null) {
            // user canceled tag merge dialog
            return;
        }
        commands.addAll(tagResolutionCommands);

        // replace sacrificial node in way with node that is being upgraded
        if (nodeToReplace != null) {
            // node should only have one parent, a way
            Way parentWay = (Way) nodeToReplace.getReferrers().get(0);
            List<Node> wayNodes = parentWay.getNodes();
            int idx = wayNodes.indexOf(nodeToReplace);
            wayNodes.set(idx, node);
            if (idx == 0 && parentWay.isClosed()) {
                // node is at start/end of way
                wayNodes.set(wayNodes.size() - 1, node);
            }
            commands.add(new ChangeNodesCommand(parentWay, wayNodes));
            commands.add(new MoveCommand(node, nodeToReplace.getCoor()));
            commands.add(new DeleteCommand(nodeToReplace));

            // delete tags from node
            if (!nodeTags.isEmpty()) {
                for (String key : nodeTags.keySet()) {
                    commands.add(new ChangePropertyCommand(node, key, null));
                }

            }
        } else {
            // no node to replace, so just delete the original node
            commands.add(new DeleteCommand(node));
        }

        getCurrentDataSet().setSelected(target);

        Main.main.undoRedo.add(new SequenceCommand(
                tr("Replace geometry for node {0}", node.getDisplayName(DefaultNameFormatter.getInstance())),
                commands));
    }
    
    public void replaceWayWithWay(List<Way> selection) {
        // determine which way will be replaced and which will provide the geometry
        boolean overrideNewCheck = false;
        int idxNew = selection.get(0).isNew() ? 0 : 1;
        if( selection.get(1-idxNew).isNew() ) {
            // if both are new, select the one with all the DB nodes
            boolean areNewNodes = false;
            for (Node n : selection.get(0).getNodes()) {
                if (n.isNew()) {
                    areNewNodes = true;
                }
            }
            idxNew = areNewNodes ? 0 : 1;
            overrideNewCheck = true;
            for (Node n : selection.get(1 - idxNew).getNodes()) {
                if (n.isNew()) {
                    overrideNewCheck = false;
                }
            }
        }
        Way geometry = selection.get(idxNew);
        Way way = selection.get(1 - idxNew);
        
        if( !overrideNewCheck && (way.isNew() || !geometry.isNew()) ) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("Please select one way that exists in the database and one new way with correct geometry."),
                    TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }

        Area a = getCurrentDataSet().getDataSourceArea();
        if (!isInArea(way, a) || !isInArea(geometry, a)) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The ways must be entirely within the downloaded area."),
                    TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (hasImportantNode(way)) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The way to be replaced cannot have any nodes with properties or relation memberships."),
                    TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Command> commands = new ArrayList<Command>();
                
        // merge tags
        Collection<Command> tagResolutionCommands = getTagConflictResolutionCommands(geometry, way);
        if (tagResolutionCommands == null) {
            // user canceled tag merge dialog
            return;
        }
        commands.addAll(tagResolutionCommands);
        
        // Prepare a list of nodes that are not used anywhere except in the way
        Collection<Node> nodePool = getUnimportantNodes(way);

        // And the same for geometry, list nodes that can be freely deleted
        Set<Node> geometryPool = new HashSet<Node>();
        for( Node node : geometry.getNodes() ) {
            List<OsmPrimitive> referrers = node.getReferrers();
            if( node.isNew() && !node.isDeleted() && referrers.size() == 1
                    && referrers.get(0).equals(geometry) && !way.containsNode(node)
                    && !hasInterestingKey(node))
                geometryPool.add(node);
        }

        // Find new nodes that are closest to the old ones, remove matching old ones from the pool
        Map<Node, Node> nodeAssoc = new HashMap<Node, Node>();
        for( Node n : geometryPool ) {
            Node nearest = findNearestNode(n, nodePool);
            if( nearest != null ) {
                nodeAssoc.put(n, nearest);
                nodePool.remove(nearest);
            }
        }

        // Now that we have replacement list, move all unused new nodes to nodePool (and delete them afterwards)
        for( Node n : geometryPool )
            if( nodeAssoc.containsKey(n) )
                nodePool.add(n);

        // And prepare a list of nodes with all the replacements
        List<Node> geometryNodes = geometry.getNodes();
        for( int i = 0; i < geometryNodes.size(); i++ )
            if( nodeAssoc.containsKey(geometryNodes.get(i)) )
                geometryNodes.set(i, nodeAssoc.get(geometryNodes.get(i)));

        // Now do the replacement
        commands.add(new ChangeNodesCommand(way, geometryNodes));

        // Move old nodes to new positions
        for( Node node : nodeAssoc.keySet() )
            commands.add(new MoveCommand(nodeAssoc.get(node), node.getCoor()));

        // Remove geometry way from selection
        getCurrentDataSet().clearSelection(geometry);

        // And delete old geometry way
        commands.add(new DeleteCommand(geometry));

        // Delete nodes that are not used anymore
        if( !nodePool.isEmpty() )
            commands.add(new DeleteCommand(nodePool));

        // Two items in undo stack: change original way and delete geometry way
        Main.main.undoRedo.add(new SequenceCommand(
                tr("Replace geometry for way {0}", way.getDisplayName(DefaultNameFormatter.getInstance())),
                commands));
    }

    /**
     * Create a list of nodes that are not used anywhere except in the way.
     *
     * @param way
     * @return 
     */
    protected Collection<Node> getUnimportantNodes(Way way) {
        Set<Node> nodePool = new HashSet<Node>();
        for (Node n : way.getNodes()) {
            List<OsmPrimitive> referrers = n.getReferrers();
            if (!n.isDeleted() && referrers.size() == 1 && referrers.get(0).equals(way)
                    && !hasInterestingKey(n)) {
                nodePool.add(n);
            }
        }
        return nodePool;
    }
    
    /**
     * Checks if a way has at least one important node (e.g. interesting tag,
     * role membership), and thus cannot be safely modified.
     * 
     * @param way
     * @return 
     */
    protected boolean hasImportantNode(Way way) {
        for (Node n : way.getNodes()) {
            //TODO: if way is connected to other ways, warn or disallow?
            for (OsmPrimitive o : n.getReferrers()) {
                if (o instanceof Relation) {
                    return true;
                }
            }
            if (hasInterestingKey(n)) {
                return true;
            }
        }
        return false;
    }
    
    protected boolean hasInterestingKey(OsmPrimitive object) {
        for (String key : object.getKeys().keySet()) {
            if (!OsmPrimitive.isUninterestingKey(key)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isInArea(Node node, Area area) {
        if (node.isNewOrUndeleted() || area == null || area.contains(node.getCoor())) {
            return true;
        }
        return false;
    }
    
    protected static boolean isInArea(Way way, Area area) {
        if (area == null) {
            return true;
        }

        for (Node n : way.getNodes()) {
            if (!isInArea(n, area)) {
                return false;
            }
        }

        return true;
    }
    
     /**
     * Merge tags from source to target object, showing resolution dialog if
     * needed.
     *
     * @param source
     * @param target
     * @return
     */
    public List<Command> getTagConflictResolutionCommands(OsmPrimitive source, OsmPrimitive target) {
        Collection<OsmPrimitive> primitives = Arrays.asList(source, target);
        
        Set<RelationToChildReference> relationToNodeReferences = RelationToChildReference.getRelationToChildReferences(primitives);

        // build the tag collection
        TagCollection tags = TagCollection.unionOfAllPrimitives(primitives);
        TagConflictResolutionUtil.combineTigerTags(tags);
        TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing(tags, primitives);
        TagCollection tagsToEdit = new TagCollection(tags);
        TagConflictResolutionUtil.completeTagCollectionForEditing(tagsToEdit);

        // launch a conflict resolution dialog, if necessary
        CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
        dialog.getTagConflictResolverModel().populate(tagsToEdit, tags.getKeysWithMultipleValues());
        dialog.getRelationMemberConflictResolverModel().populate(relationToNodeReferences);
        dialog.setTargetPrimitive(target);
        dialog.prepareDefaultDecisions();

        // conflict resolution is necessary if there are conflicts in the merged tags
        // or if at least one of the merged nodes is referred to by a relation
        if (!tags.isApplicableToPrimitive() || relationToNodeReferences.size() > 1) {
            dialog.setVisible(true);
            if (dialog.isCanceled()) {
                return null;
            }
        }
        return dialog.buildResolutionCommands();
    }

    
    /**
     * Find node from the collection which is nearest to <tt>node</tt>. Max distance is taken in consideration.
     * @return null if there is no such node.
     */
    private Node findNearestNode( Node node, Collection<Node> nodes ) {
        if( nodes.contains(node) )
            return node;
        
        Node nearest = null;
        // TODO: use meters instead of degrees, but do it fast
        double distance = Double.parseDouble(Main.pref.get("utilsplugin2.replace-geometry.max-distance", "1"));
        Point2D coor = node.getCoor();

        for( Node n : nodes ) {
            double d = n.getCoor().distance(coor);
            if( d < distance ) {
                distance = d;
                nearest = n;
            }
        }
        return nearest;
    }

    @Override
    protected void updateEnabledState() {
        if( getCurrentDataSet() == null ) {
            setEnabled(false);
        }  else
            updateEnabledState(getCurrentDataSet().getSelected());
    }

    @Override
    protected void updateEnabledState( Collection<? extends OsmPrimitive> selection ) {
        setEnabled(selection != null && selection.size() >= 2 );
    }
}

