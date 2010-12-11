/**
 * Terracer: A JOSM Plugin for terraced houses.
 *
 * Copyright 2009 CloudMade Ltd.
 *
 * Released under the GPLv2, see LICENSE file for details.
 */
package terracer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Terraces a quadrilateral, closed way into a series of quadrilateral,
 * closed ways. If two ways are selected and one of them can be identified as
 * a street (highway=*, name=*) then the given street will be added
 * to the 'associatedStreet' relation.
 *
 *
 * At present it only works on quadrilaterals, but there is no reason
 * why it couldn't be extended to work with other shapes too. The
 * algorithm employed is naive, but it works in the simple case.
 *
 * @author zere
 */
public final class TerracerAction extends JosmAction {

    // smsms1 asked for the last value to be remembered to make it easier to do
    // repeated terraces. this is the easiest, but not necessarily nicest, way.
    // private static String lastSelectedValue = "";

    Collection<Command> commands;

    public TerracerAction() {
        super(tr("Terrace a building"), "terrace",
                tr("Creates individual buildings from a long building."),
                Shortcut.registerShortcut("tools:Terracer", tr("Tool: {0}",
                        tr("Terrace a building")), KeyEvent.VK_T,
                        Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
    }

    /**
     * Checks that the selection is OK. If not, displays error message. If so
     * calls to terraceBuilding(), which does all the real work.
     */
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        Way outline = null;
        Way street = null;
        Node init = null;

        class InvalidUserInputException extends Exception {
            InvalidUserInputException(String message) {
                super(message);
            }
            InvalidUserInputException() {
                super();
            }
        }

        try {
            for (OsmPrimitive osm : sel) {
                if (osm instanceof Node) {
                    if (init != null)
                        throw new InvalidUserInputException();
                    init = (Node) osm;
                } else if (osm instanceof Way) {
                    if (osm.hasKey("highway")) {
                        if (street != null)
                            throw new InvalidUserInputException();
                        street = (Way) osm;
                        if (!street.hasKey("name"))
                            throw new InvalidUserInputException();
                    } else {
                        if (outline != null)
                            throw new InvalidUserInputException();
                        outline = (Way) osm;
                    }
                }
            }
            if (outline == null)
                throw new InvalidUserInputException();
            
            if (init != null && !init.getReferrers().contains(outline))
                throw new InvalidUserInputException();

            if (outline.getNodesCount() < 5)
                throw new InvalidUserInputException();

            if (!outline.isClosed())
                throw new InvalidUserInputException();
        } catch (InvalidUserInputException ex) {
            new ExtendedDialog(Main.parent, tr("Invalid selection"), new String[] {"OK"})
                .setButtonIcons(new String[] {"ok"}).setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setContent(tr("Select a single, closed way of at least four nodes. " +
                    "(Optionally you can also select a street for the addr:street tag " +
                    "and a node to mark the start of numbering.)"))
                .showDialog();
            return;
        }

        // If we have a street, try to find a associatedStreet relation that could be reused.
        Relation associatedStreet = null;
        if (street != null) {
            outer:for (OsmPrimitive osm : Main.main.getCurrentDataSet().allNonDeletedPrimitives()) {
                if (!(osm instanceof Relation)) continue;
                Relation rel = (Relation) osm;
                if ("associatedStreet".equals(rel.get("type")) && street.get("name").equals(rel.get("name"))) {
                    List<RelationMember> members = rel.getMembers();
                    for (RelationMember m : members) {
                        if ("street".equals(m.getRole()) && m.isWay() && m.getMember().equals(street)) {
                            associatedStreet = rel;
                            break outer;
                        }
                    }
                }
            }
        }

        String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
        // show input dialog.
        new HouseNumberInputHandler(this, outline, init, street, associatedStreet, title);
    }

    public Integer getNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Terraces a single, closed, quadrilateral way.
     *
     * Any node must be adjacent to both a short and long edge, we naively
     * choose the longest edge and its opposite and interpolate along them
     * linearly to produce new nodes. Those nodes are then assembled into
     * closed, quadrilateral ways and left in the selection.
     *
     * @param outline The closed, quadrilateral way to terrace.
     * @param init The node that hints at which side to start the numbering
     * @param street The street, the buildings belong to (may be null)
     * @param associatedStreet
     * @param From
     * @param To
     * @param streetName the name of a street (may be null). Used if not null and street is null.
     * @param handleRelations If the user likes to add a relation or extend an existing relation
     * @param deleteOutline If the outline way should be deleted, when done
     */
    public void terraceBuilding(Way outline,
                Node init,
                Way street,
                Relation associatedStreet,
                Integer segments,
                String From,
                String To,
                int step,
                String streetName,
                boolean handleRelations,
                boolean deleteOutline) {
        final int nb;

        Integer to, from;
        to = getNumber(To);
        from = getNumber(From);
        if (to != null && from != null) {
            nb = 1 + (to.intValue() - from.intValue()) / step;
        } else if (segments != null) {
            nb = segments.intValue();
        } else {
            // if we get here, there is is a bug in the input validation.
            throw new TerracerRuntimeException(
                    "Could not determine segments from parameters, this is a bug. "
                            + "Parameters were: segments " + segments
                            + " from " + from + " to " + to + " step " + step);
        }

        // new nodes array to hold all intermediate nodes
        Node[][] new_nodes = new Node[2][nb + 1];

        this.commands = new LinkedList<Command>();
        Collection<Way> ways = new LinkedList<Way>();

        // Should this building be terraced (i.e. is there more then one section?)
        if (nb > 1) {
            // create intermediate nodes by interpolating.

            // now find which is the longest side connecting the first node
            Pair<Way, Way> interp = findFrontAndBack(outline);

            boolean swap = false;
            if (init != null) {
                if (interp.a.lastNode().equals(init) || interp.b.lastNode().equals(init)) {
                    swap = true;
                }
            }

            final double frontLength = wayLength(interp.a);
            final double backLength = wayLength(interp.b);

            for (int i = 0; i <= nb; ++i) {
                int i_dir = swap ? nb - i : i;
                new_nodes[0][i] = interpolateAlong(interp.a, frontLength * i_dir / nb);
                new_nodes[1][i] = interpolateAlong(interp.b, backLength * i_dir / nb);
                this.commands.add(new AddCommand(new_nodes[0][i]));
                this.commands.add(new AddCommand(new_nodes[1][i]));
            }

            // assemble new quadrilateral, closed ways
            for (int i = 0; i < nb; ++i) {
                Way terr = new Way();
                terr.addNode(new_nodes[0][i]);
                terr.addNode(new_nodes[0][i + 1]);
                terr.addNode(new_nodes[1][i + 1]);
                terr.addNode(new_nodes[1][i]);
                terr.addNode(new_nodes[0][i]);

                // add the tags of the outline to each building (e.g. source=*)
                TagCollection.from(outline).applyTo(terr);

                String number = null;
                if (from != null) {
                    number = Integer.toString(from + i * step);
                }
                terr = addressBuilding(terr, street, streetName, number);

                ways.add(terr);
                this.commands.add(new AddCommand(terr));
            }

            if (deleteOutline) {
                this.commands.add(DeleteCommand.delete(Main.main.getEditLayer(), Collections.singleton(outline), true, true));
            }
        } else {
            // Single section, just add the address details
            Way newOutline;
            newOutline = addressBuilding(outline, street, streetName, From);
            ways.add(newOutline);
            this.commands.add(new ChangeCommand(outline, newOutline));
        }

        if (handleRelations) { // create a new relation or merge with existing
            if (associatedStreet == null) {  // create a new relation
                associatedStreet = new Relation();
                associatedStreet.put("type", "associatedStreet");
                if (street != null) { // a street was part of the selection
                    associatedStreet.put("name", street.get("name"));
                    associatedStreet.addMember(new RelationMember("street", street));
                } else {
                    associatedStreet.put("name", streetName);
                }
                for (Way w : ways) {
                    associatedStreet.addMember(new RelationMember("house", w));
                }
                this.commands.add(new AddCommand(associatedStreet));
            }
            else { // relation exists already - add new members
                Relation newAssociatedStreet = new Relation(associatedStreet);
                for (Way w : ways) {
                    newAssociatedStreet.addMember(new RelationMember("house", w));
                }
                this.commands.add(new ChangeCommand(associatedStreet, newAssociatedStreet));
            }
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Terrace"), commands));
        if (nb > 1) {
            // Select the new building outlines (for quick reversing)
            Main.main.getCurrentDataSet().setSelected(ways);
        } else if (street != null) {
            // Select the way (for quick selection of a new house (with the same way))
            Main.main.getCurrentDataSet().setSelected(street);
        }
    }

    /**
     * Adds address details to a single building
     *
     * @param outline The closed, quadrilateral way to add the address to.
     * @param street The street, the buildings belong to (may be null)
     * @param streetName the name of a street (may be null). Used if not null and street is null.
     * @param number The house number
     * @return the way with added address details
     */
    private Way addressBuilding(Way outline, Way street, String streetName, String number) {
        Way changedOutline = outline;
        if (number != null) {
            // only, if the user has specified house numbers
            this.commands.add(new ChangePropertyCommand(changedOutline, "addr:housenumber", number));
        }
        changedOutline.put("building", "yes");
        if (street != null) {
            this.commands.add(new ChangePropertyCommand(changedOutline, "addr:street", street.get("name")));
        } else if (streetName != null) {
            this.commands.add(new ChangePropertyCommand(changedOutline, "addr:street", streetName));
        }
        return changedOutline;
    }

    /**
     * Creates a node at a certain distance along a way, as calculated by the
     * great circle distance.
     *
     * Note that this really isn't an efficient way to do this and leads to
     * O(N^2) running time for the main algorithm, but its simple and easy
     * to understand, and probably won't matter for reasonable-sized ways.
     *
     * @param w The way to interpolate.
     * @param l The length at which to place the node.
     * @return A node at a distance l along w from the first point.
     */
    private Node interpolateAlong(Way w, double l) {
        List<Pair<Node,Node>> pairs = w.getNodePairs(false);
        for (int i = 0; i < pairs.size(); ++i) {
            Pair<Node,Node> p = pairs.get(i);
            final double seg_length = p.a.getCoor().greatCircleDistance(p.b.getCoor());
            if (l <= seg_length ||
                    i == pairs.size() - 1) {    // be generous on the last segment (numerical roudoff can lead to a small overshoot)
                return interpolateNode(p.a, p.b, l / seg_length);
            } else {
                l -= seg_length;
            }
        }
        // we shouldn't get here
        throw new IllegalStateException();
    }

    /**
     * Calculates the great circle length of a way by summing the great circle
     * distance of each pair of nodes.
     *
     * @param w The way to calculate length of.
     * @return The length of the way.
     */
    private double wayLength(Way w) {
        double length = 0.0;
        for (Pair<Node, Node> p : w.getNodePairs(false)) {
            length += p.a.getCoor().greatCircleDistance(p.b.getCoor());
        }
        return length;
    }

    /**
     * Given a way, try and find a definite front and back by looking at the
     * segments to find the "sides". Sides are assumed to be single segments
     * which cannot be contiguous.
     *
     * @param w The way to analyse.
     * @return A pair of ways (front, back) pointing in the same directions.
     */
    private Pair<Way, Way> findFrontAndBack(Way w) {
        // calculate the "side-ness" score for each segment of the way
        double[] sideness = calculateSideness(w);

        // find the largest two sidenesses which are not contiguous
        int[] indexes = sortedIndexes(sideness);
        int side1 = indexes[0];
        int side2 = indexes[1];
        // if side2 is contiguous with side1 then look further down the
        // list. we know there are at least 4 sides, as anything smaller
        // than a quadrilateral would have been rejected at an earlier
        // stage.
        if (indexDistance(side1, side2, indexes.length) < 2) {
            side2 = indexes[2];
        }
        if (indexDistance(side1, side2, indexes.length) < 2) {
            side2 = indexes[3];
        }

        // if the second side has a shorter length and an approximately equal
        // sideness then its better to choose the shorter, as with
        // quadrilaterals
        // created using the orthogonalise tool the sideness will be about the
        // same for all sides.
        if (sideLength(w, side1) > sideLength(w, side1 + 1)
                && Math.abs(sideness[side1] - sideness[side1 + 1]) < 0.001) {
            side1 = side1 + 1;
            side2 = (side2 + 1) % (w.getNodesCount() - 1);
        }

        // swap side1 and side2 into sorted order.
        if (side1 > side2) {
            int tmp = side2;
            side2 = side1;
            side1 = tmp;
        }

        Way front = new Way();
        Way back = new Way();
        for (int i = side2 + 1; i < w.getNodesCount() - 1; ++i) {
            front.addNode(w.getNode(i));
        }
        for (int i = 0; i <= side1; ++i) {
            front.addNode(w.getNode(i));
        }
        // add the back in reverse order so that the front and back ways point
        // in the same direction.
        for (int i = side2; i > side1; --i) {
            back.addNode(w.getNode(i));
        }

        return new Pair<Way, Way>(front, back);
    }

    /**
     * returns the distance of two segments of a closed polygon
     */
    private int indexDistance(int i1, int i2, int n) {
        return Math.min(positiveModulus(i1 - i2, n), positiveModulus(i2 - i1, n));
    }

    /**
     * return the modulus in the range [0, n)
     */
    private int positiveModulus(int a, int n) {
        if (n <=0)
            throw new IllegalArgumentException();
        int res = a % n;
        if (res < 0) {
            res += n;
        }
        return res;
    }

    /**
     * Calculate the length of a side (from node i to i+1) in a way. This assumes that
     * the way is closed, but I only ever call it for buildings.
     */
    private double sideLength(Way w, int i) {
        Node a = w.getNode(i);
        Node b = w.getNode((i + 1) % (w.getNodesCount() - 1));
        return a.getCoor().greatCircleDistance(b.getCoor());
    }

    /**
     * Given an array of doubles (but this could made generic very easily) sort
     * into order and return the array of indexes such that, for a returned array
     * x, a[x[i]] is sorted for ascending index i.
     *
     * This isn't efficient at all, but should be fine for the small arrays we're
     * expecting. If this gets slow - replace it with some more efficient algorithm.
     *
     * @param a The array to sort.
     * @return An array of indexes, the same size as the input, such that a[x[i]]
     * is in sorted order.
     */
    private int[] sortedIndexes(final double[] a) {
        class SortWithIndex implements Comparable<SortWithIndex> {
            public double x;
            public int i;

            public SortWithIndex(double a, int b) {
                x = a;
                i = b;
            }

            public int compareTo(SortWithIndex o) {
                return Double.compare(x, o.x);
            };
        }

        final int length = a.length;
        ArrayList<SortWithIndex> sortable = new ArrayList<SortWithIndex>(length);
        for (int i = 0; i < length; ++i) {
            sortable.add(new SortWithIndex(a[i], i));
        }
        Collections.sort(sortable);

        int[] indexes = new int[length];
        for (int i = 0; i < length; ++i) {
            indexes[i] = sortable.get(i).i;
        }

        return indexes;
    }

    /**
     * Calculate "sideness" metric for each segment in a way.
     */
    private double[] calculateSideness(Way w) {
        final int length = w.getNodesCount() - 1;
        double[] sideness = new double[length];

        sideness[0] = calculateSideness(w.getNode(length - 1), w.getNode(0), w
                .getNode(1), w.getNode(2));
        for (int i = 1; i < length - 1; ++i) {
            sideness[i] = calculateSideness(w.getNode(i - 1), w.getNode(i), w
                    .getNode(i + 1), w.getNode(i + 2));
        }
        sideness[length - 1] = calculateSideness(w.getNode(length - 2), w
                .getNode(length - 1), w.getNode(length), w.getNode(1));

        return sideness;
    }

    /**
     * Calculate sideness of a single segment given the nodes which make up that
     * segment and its previous and next segments in order. Sideness is calculated
     * for the segment b-c.
     */
    private double calculateSideness(Node a, Node b, Node c, Node d) {
        final double ndx = b.getCoor().getX() - a.getCoor().getX();
        final double pdx = d.getCoor().getX() - c.getCoor().getX();
        final double ndy = b.getCoor().getY() - a.getCoor().getY();
        final double pdy = d.getCoor().getY() - c.getCoor().getY();

        return (ndx * pdx + ndy * pdy)
                / Math.sqrt((ndx * ndx + ndy * ndy) * (pdx * pdx + pdy * pdy));
    }

    /**
     * Creates a new node at the interpolated position between the argument
     * nodes. Interpolates linearly in projected coordinates.
     *
     * @param a First node, at which f=0.
     * @param b Last node, at which f=1.
     * @param f Fractional position between first and last nodes.
     * @return A new node at the interpolated position.
     */
    private Node interpolateNode(Node a, Node b, double f) {
        Node n = new Node(a.getEastNorth().interpolate(b.getEastNorth(), f));
        return n;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null);
    }
}
