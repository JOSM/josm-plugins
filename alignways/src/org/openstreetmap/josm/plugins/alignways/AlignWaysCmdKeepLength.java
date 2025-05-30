// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.alignways;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IWaySegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Executes one of the desired geometric actions
 * needed to align the ways.
 *
 * @author tilusnet &lt;tilusnet@gmail.com&gt;
 */
public class AlignWaysCmdKeepLength extends Command {

    enum AlignableStatus {
        ALGN_VALID,
        ALGN_INV_CONNECTED_UNSHARED_PIVOT,
        ALGN_INV_OUTSIDE_WORLD,
        ALGN_INV_TOOMANY_CONNECTED_WS,      // for AlignWaysCmdKeepAngles
        ALGN_INV_ANGLE_PRESERVING_CONFLICT, // for AlignWaysCmdKeepAngles
        ALGN_INV_XPOINT_FALLSOUT            // for AlignWaysCmdKeepAngles
    }

    final AlignWaysAlgnSegment algnSeg;

    /**
     * The nodes that will move as result of the aligning.
     */
    final Collection<Node> displaceableNodes;

    /**
     * Maps the alignee nodes with their calculated coordinates.
     * Useful for validation.
     */
    final Map<Node, EastNorth> calculatedNodes = new HashMap<>();

    /**
     * Set of nodes that were affected by the last command execution.
     */
    Collection<Node> lastAffectedNodes;

    /**
     * Pivot point
     */
    final EastNorth pivot;

    /**
     * Computed rotation angle to rotate the segment
     *
     */
    private final double rotationAngle;

    /**
     * List of all old states of the objects.
     */
    final Map<Node, Node> oldNodes = new HashMap<>();

    /**
     * Creates an AlignWaysRotateCommand.
     * TODO Limitation (for now): constructor assumes areSegsAlignable() returns true.
     * @param ds the data set. Must not be null.
     */
    public AlignWaysCmdKeepLength(DataSet ds) {
        super(ds);
        lastAffectedNodes = null;

        MapView mapView = MainApplication.getMap().mapView;
        algnSeg = AlignWaysSegmentMgr.getInstance(mapView).getAlgnSeg();
        IWaySegment<Node, Way> algnWS = algnSeg.getSegment();
        IWaySegment<Node, Way> refWS = AlignWaysSegmentMgr.getInstance(mapView).getRefSeg().getSegment();

        this.pivot = algnSeg.getCurrPivotCoord();
        this.displaceableNodes = algnSeg.getSegmentEndPoints();

        EastNorth enRefNode1 = refWS.getFirstNode().getEastNorth();
        EastNorth enRefNode2 = refWS.getSecondNode().getEastNorth();

        EastNorth enAlgnNode1 = algnWS.getFirstNode().getEastNorth();
        EastNorth enAlgnNode2 = algnWS.getSecondNode().getEastNorth();

        // Calculate the rotation angle
        double refAngle = Math.atan2(enRefNode1.north() - enRefNode2.north(),
                enRefNode1.east() - enRefNode2.east());
        double algnAngle = Math.atan2(
                enAlgnNode1.north() - enAlgnNode2.north(), enAlgnNode1.east()
                - enAlgnNode2.east());

        rotationAngle = normalise_angle(refAngle - algnAngle);

        // Rotate using the rotation matrix known in algebra:
        // [ x' ] = [ cos(Phi) -sin(Phi) ] [ x ]
        // [ y' ]   [ sin(Phi)  cos(Phi) ] [ y ]
        for (Node n : displaceableNodes) {
            double cosPhi = Math.cos(rotationAngle);
            double sinPhi = Math.sin(rotationAngle);
            EastNorth oldEastNorth = n.getEastNorth();
            double x = oldEastNorth.east() - pivot.east();
            double y = oldEastNorth.north() - pivot.north();
            double nx = cosPhi * x - sinPhi * y + pivot.east();
            double ny = sinPhi * x + cosPhi * y + pivot.north();

            calculatedNodes.put(n, new EastNorth(nx, ny));
        }

        /*
         * For debug only
         * String s = "Ref Angle: " + refAngle + " (" + Math.toDegrees(refAngle)
         * + ")\n";
         * s += "Algn Angle: " + algnAngle + " (" + Math.toDegrees(algnAngle)
         * + ")\n";
         * s += "Rotation angle: " + rotationAngle + " ("
         * + Math.toDegrees(rotationAngle) + ")";
         */
    }

    /**
     * Helper for actually rotating the nodes.
     *
     * @param setModified
     *            - true if rotated nodes should be flagged "modified"
     */
    private void rotateNodes(boolean setModified) {

        // "Backup" state
        lastAffectedNodes = new HashSet<>();
        for (Node n : this.displaceableNodes) {
            Node nodeBackup = new Node(n);
            // Set other fields that clone doesn't copy
            nodeBackup.setEastNorth(n.getEastNorth());
            oldNodes.put(n, nodeBackup);

            lastAffectedNodes.add(n);
        }

        // Physical change occurs here:
        for (Node n : displaceableNodes) {
            n.setEastNorth(calculatedNodes.get(n));
            if (setModified) {
                n.setModified(true);
            }
        }
        algnSeg.updatePivotsEndpoints();
    }

    /**
     * Make sure angle is in interval ( -Pi/2, Pi/2 ].
     * @param a angle
     * @return normalized angle
     */
    private double normalise_angle(double a) {
        while (a > Math.PI) {
            a -= 2 * Math.PI;
        }
        while (a <= -Math.PI) {
            a += 2 * Math.PI;
        }

        if (a > Math.PI / 2) {
            a -= Math.PI;
        } else if (a < -Math.PI / 2) {
            a += Math.PI;
        }
        return a;
    }

    @Override
    public String getDescriptionText() {
        return tr("Align way segment");
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("alignways");
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified,
            Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (OsmPrimitive osm : displaceableNodes) {
            modified.add(osm);
        }
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<>(displaceableNodes);
        prims.add(algnSeg.getSegment().getWay());
        return Collections.unmodifiableCollection(prims);
    }

    @Override
    public boolean executeCommand() {
        rotateNodes(true);
        return true;
    }

    @Override
    public void undoCommand() {
        for (Node n : displaceableNodes) {
            Node oldNode = oldNodes.get(n);
            n.setCoor(oldNode.getCoor());
        }
        algnSeg.updatePivotsEndpoints();
    }

    public Collection<Node> getPrevAffectedNodes() {
        return lastAffectedNodes;
    }

    /**
     * Returns true if the two selected segments are alignable.
     * They are not if they are connected *and* the pivot is not the connection
     * node.
     * @return alignable status
     */
    AlignableStatus areSegsAlignable() {
        Collection<Node> algnNodes = displaceableNodes;
        Collection<Node> refNodes = AlignWaysSegmentMgr
                .getInstance(MainApplication.getMap().mapView).getRefSeg()
                .getSegmentEndPoints();

        // First check if the pivot node of the alignee exists in the reference:
        // in this case the pivot is the shared node and alignment is possible
        for (Node nR : refNodes) {
            if (nR.getEastNorth().equals(pivot))
                return AlignableStatus.ALGN_VALID;
        }

        // Otherwise if the segments are connected, alignment is not possible
        for (Node nA : algnNodes) {
            for (Node nR : refNodes) {
                if (nA.equals(nR))
                    return AlignableStatus.ALGN_INV_CONNECTED_UNSHARED_PIVOT;
            }
        }

        // Deny action if the nodes would end up outside world
        for (EastNorth en : calculatedNodes.values()) {
            if (new Node(en).isOutSideWorld()) {
                return AlignableStatus.ALGN_INV_OUTSIDE_WORLD;
            }
        }

        // In all other cases alignment is possible
        return AlignableStatus.ALGN_VALID;
    }

    /**
     * Validates the circumstances of the alignment command to be executed.
     *
     * @return true if the aligning action can be done, false otherwise.
     */
    public boolean executable() {
        AlignableStatus stat = areSegsAlignable();

        if (stat != AlignableStatus.ALGN_VALID) {
            reportInvalidCommand(stat);
            return false;
        } else
            // Action valid
            return true;
    }

    /**
     * Reports invalid alignable statuses on screen in dialog boxes.
     *
     * @param stat The invalid status to report
     */
    void reportInvalidCommand(AlignableStatus stat) {
        String statMsg;

        switch (stat) {
        case ALGN_INV_CONNECTED_UNSHARED_PIVOT:
            statMsg = tr("Please select two segments that don''t share any nodes\n"
                    + " or put the pivot on their common node.\n");
            break;
        case ALGN_INV_OUTSIDE_WORLD:
            statMsg = tr("Aligning would result nodes ''outside the world''.\n"
                    + "Alignment not possible.\n");
            break;
        default:
            statMsg = tr("Undocumented problem occured.\n");
            break;
        }

        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                tr(statMsg),
                tr("AlignWayS: Alignment not possible"),
                JOptionPane.WARNING_MESSAGE
                );

    }

}
