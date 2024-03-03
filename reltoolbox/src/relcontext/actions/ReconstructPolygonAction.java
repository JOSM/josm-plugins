// License: GPL. For details, see LICENSE file.
package relcontext.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

import relcontext.ChosenRelation;
import relcontext.ChosenRelationListener;

/**
 * Make a single polygon out of the multipolygon relation. The relation must have only outer members.
 * @author Zverik
 */
public class ReconstructPolygonAction extends JosmAction implements ChosenRelationListener {
    private final transient ChosenRelation rel;

    private static final List<String> IRRELEVANT_KEYS = Arrays.asList("source", "created_by", "note");

    /**
     * Reconstruct one or more polygons from multipolygon relation.
     * @param rel the multipolygon relation
     */
    public ReconstructPolygonAction(ChosenRelation rel) {
        super(tr("Reconstruct polygon"), "dialogs/filter", tr("Reconstruct polygon from multipolygon relation"),
                Shortcut.registerShortcut("reltoolbox:reconstructpoly", tr("Relation Toolbox: {0}",
                        tr("Reconstruct polygon from multipolygon relation")),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false, false);
        this.rel = rel;
        rel.addChosenRelationListener(this);
        setEnabled(isSuitableRelation(rel.get()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation r = rel.get();
        boolean relationReused = false;
        List<Way> ways = new ArrayList<>();
        boolean wont = false;
        for (RelationMember m : r.getMembers()) {
            if (m.isWay()) {
                ways.add(m.getWay());
            } else {
                wont = true;
            }
        }
        if (wont) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Multipolygon must consist only of ways"),
                    tr("Reconstruct polygon"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        MultipolygonBuilder mpc = new MultipolygonBuilder();
        String error = mpc.makeFromWays(ways);
        if (error != null) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), error);
            return;
        }

        rel.clear();
        List<Command> commands = new ArrayList<>();
        Command relationDeleteCommand = DeleteCommand.delete(Collections.singleton(r), true, true);
        if (relationDeleteCommand == null)
            return;

        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        for (JoinedPolygon p : mpc.outerWays) {

            ArrayList<JoinedPolygon> myInnerWays = new ArrayList<>();
            for (JoinedPolygon i : mpc.innerWays) {
                // if the first point of any inner ring is contained in this
                // outer ring, then this inner ring belongs to us. This
                // assumption only works if multipolygons have valid geometries
                EastNorth en = i.ways.get(0).firstNode().getEastNorth();
                if (p.area.contains(en.east(), en.north())) {
                    myInnerWays.add(i);
                }
            }

            if (!myInnerWays.isEmpty()) {
                // this ring has inner rings, so we leave a multipolygon in
                // place and don't reconstruct the rings.
                List<RelationMember> members = new ArrayList<>();
                Relation n;
                for (Way w : p.ways) {
                    members.add(new RelationMember("outer", w));
                }
                for (JoinedPolygon i : myInnerWays) {
                    for (Way w : i.ways) {
                        members.add(new RelationMember("inner", w));
                    }
                }
                if (relationReused) {
                    n = new Relation();
                    n.setKeys(r.getKeys());
                    n.setMembers(members);
                    commands.add(new AddCommand(ds, n));
                } else {
                    relationReused = true;
                    commands.add(new ChangeMembersCommand(r, members));
                }
                continue;
            }

            // move all tags from relation and common tags from ways
            // start with all tags from first way but only if area tags are present
            Map<String, String> tags = p.ways.get(0).getKeys();
            if (!p.ways.get(0).hasAreaTags()) {
                tags.clear();
            }
            List<OsmPrimitive> relations = p.ways.get(0).getReferrers();
            Set<String> noTags = new HashSet<>(r.keySet());
            for (int i = 1; i < p.ways.size(); i++) {
                Way w = p.ways.get(i);
                for (String key : w.keySet()) {
                    String value = w.get(key);
                    if (!noTags.contains(key) && tags.containsKey(key) && !tags.get(key).equals(value)) {
                        tags.remove(key);
                        noTags.add(key);
                    }
                }
                List<OsmPrimitive> referrers = w.getReferrers();
                relations.removeIf(osmPrimitive -> !referrers.contains(osmPrimitive));
            }
            tags.putAll(r.getKeys());
            tags.remove("type");

            // then delete ways that are not relevant (do not take part in other relations or have strange tags)
            Way candidateWay = null;
            for (Way w : p.ways) {
                if (w.getReferrers().size() == 1) {
                    // check tags that remain
                    Set<String> keys = new HashSet<>(w.keySet());
                    keys.removeAll(tags.keySet());
                    IRRELEVANT_KEYS.forEach(keys::remove);
                    if (keys.isEmpty()) {
                        if (candidateWay == null) {
                            candidateWay = w;
                        } else {
                            if (candidateWay.isNew() && !w.isNew()) {
                                // prefer ways that are already in the database
                                Way tmp = w;
                                w = candidateWay;
                                candidateWay = tmp;
                            }
                            commands.add(new DeleteCommand(w));
                        }
                    }
                }
            }

            // take the first way, put all nodes into it, making it a closed polygon
            Way result = candidateWay == null ? new Way() : new Way(candidateWay);
            result.setNodes(p.nodes);
            result.addNode(result.firstNode());
            result.setKeys(tags);
            commands.add(candidateWay == null ? new AddCommand(ds, result) : new ChangeCommand(candidateWay, result));
        }

        // only delete the relation if it hasn't been re-used
        if (!relationReused) {
            // The relation needs to be deleted first, so that undo/redo continue to work properly
            commands.add(0, relationDeleteCommand);
        }
        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Reconstruct polygons from relation {0}",
                r.getDisplayName(DefaultNameFormatter.getInstance())), commands));
        Collection<? extends OsmPrimitive> newSelection = UndoRedoHandler.getInstance().getLastCommand().getParticipatingPrimitives();
        newSelection.removeIf(p -> p.isDeleted());
        ds.setSelected(newSelection);
    }

    @Override
    public void chosenRelationChanged(Relation oldRelation, Relation newRelation) {
        setEnabled(isSuitableRelation(newRelation));
    }

    private static boolean isSuitableRelation(Relation newRelation) {
        return newRelation != null && "multipolygon".equals(newRelation.get("type")) && newRelation.getMembersCount() != 0;
    }
}
