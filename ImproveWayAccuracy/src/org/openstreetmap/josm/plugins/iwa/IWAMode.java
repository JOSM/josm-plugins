/*
 * This file is part of ImproveWayAccuracy plugin for JOSM.
 * http://wiki.openstreetmap.org/wiki/JOSM/Plugins/ImproveWayAccuracy
 *
 * Licence: GPL v2 or later
 * Author:  Alexander Kachkaev <alexander@kachkaev.ru>, 2011
 */
package org.openstreetmap.josm.plugins.iwa;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

public class IWAMode extends MapMode implements MapViewPaintable,
        SelectionChangedListener, AWTEventListener {

    enum State {
        selecting, improving
    }

    private State state;

    private MapView mv;

    private static final long serialVersionUID = 42L;

    private Way targetWay;
    private Node candidateNode = null;
    private WaySegment candidateSegment = null;

    private Point mousePos = null;
    private boolean dragging = false;

    final private Cursor cursorSelect;
    final private Cursor cursorSelectHover;
    final private Cursor cursorImprove;
    final private Cursor cursorImproveAdd;
    final private Cursor cursorImproveAddLock;
    final private Cursor cursorImproveLock;

    private boolean shift = false;
    private boolean ctrl = false;
    private boolean oldShift = false;
    private boolean oldCtrl = false;

    private final Color guideColor;
    private final BasicStroke selectTargetWayStroke;
    private final BasicStroke moveNodeStroke;
    private final BasicStroke addNodeStroke;

    private boolean selectionChangedBlocked = false;

    protected String oldModeHelpText;

    public IWAMode(MapFrame mapFrame) {
        super(tr("ImproveWayAccuracy"), "button.png",
                tr("Improve Way Accuracy mode"), Shortcut.registerShortcut(
                        "mapmode:ImproveWayAccuracy",
                        tr("Mode: {0}", tr("Improve Way Accuracy")),
                        KeyEvent.VK_K, Shortcut.GROUP_EDIT), mapFrame, Cursor
                        .getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        cursorSelect = ImageProvider.getCursor("normal", null);
        cursorSelectHover = ImageProvider.getCursor("hand", null);
        cursorImprove = ImageProvider.getCursor("crosshair", null);
        cursorImproveAdd = ImageProvider.getCursor("crosshair", "add_node");
        cursorImproveAddLock = ImageProvider.getCursor("crosshair",
                "add_node_lock");
        cursorImproveLock = ImageProvider.getCursor("crosshair", "lock");

        guideColor = PaintColors.HIGHLIGHT.get();
        selectTargetWayStroke = new BasicStroke(2, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);
        float dash1[] = { 4.0f };
        moveNodeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
        addNodeStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER);
    }

    // -------------------------------------------------------------------------
    // Mode methods
    // -------------------------------------------------------------------------

    @Override
    public void enterMode() {
        if (!isEnabled())
            return;
        super.enterMode();

        mv = Main.map.mapView;
        mousePos = null;
        oldModeHelpText = "";

        if (getCurrentDataSet() == null)
            return;

        updateStateByCurrentSelection();

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.addTemporaryLayer(this);
        DataSet.addSelectionListener(this);

        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this,
                    AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();

        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.removeTemporaryLayer(this);
        DataSet.removeSelectionListener(this);

        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
        }

        Main.map.mapView.repaint();
    }

    @Override
    protected void updateStatusLine() {
        String newModeHelpText = getModeHelpText();
        if (!newModeHelpText.equals(oldModeHelpText)) {
            oldModeHelpText = newModeHelpText;
            Main.map.statusLine.setHelpText(newModeHelpText);
            Main.map.statusLine.repaint();
        }
    }

    @Override
    public String getModeHelpText() {
        if (state == State.selecting) {
            if (targetWay != null)
                return tr("Click on the way to start improving its shape.");
            else
                return tr("Select a way that you want to make more accurate.");
        } else {
            if (!ctrl)
                return tr("Click to move the highlighted node. Hold Ctrl to add new nodes.");
            else
                return tr("Click to add a new node. Release Ctrl to move existing nodes.");
        }
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
        // setEnabled(Main.main.getActiveLayer() instanceof OsmDataLayer);
    }

    // -------------------------------------------------------------------------
    // MapViewPaintable methods
    // -------------------------------------------------------------------------

    /**
     * Redraws temporary layer. Highlights targetWay in select mode. Draws
     * preview lines in improve mode and highlights the candidateNode
     */
    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        if (mousePos == null)
            return;

        g.setColor(guideColor);

        if (state == State.selecting && targetWay != null) {
            // Highlighting the targetWay in Selecting state
            // Non-native highlighting is used, because sometimes highlighted
            // segments are covered with others, which is bad.
            g.setStroke(selectTargetWayStroke);

            List<Node> nodes = targetWay.getNodes();

            GeneralPath b = new GeneralPath();
            Point p0 = mv.getPoint(nodes.get(0));
            Point pn;
            b.moveTo(p0.x, p0.y);

            for (Node n : nodes) {
                pn = mv.getPoint(n);
                b.lineTo(pn.x, pn.y);
            }
            if (targetWay.isClosed())
                b.lineTo(p0.x, p0.y);

            g.draw(b);

        } else if (state == State.improving) {
            // Drawing preview lines and highlighting the node
            // that is going to be moved.
            // Non-native highlighting is used here as well.

            // Finding endpoints
            Point p1 = null, p2 = null;
            if (candidateSegment != null) {
                g.setStroke(addNodeStroke);
                p1 = mv.getPoint(candidateSegment.getFirstNode());
                p2 = mv.getPoint(candidateSegment.getSecondNode());
            } else if (candidateNode != null) {
                g.setStroke(moveNodeStroke);
                List<Pair<Node, Node>> wpps = targetWay.getNodePairs(false);
                for (Pair<Node, Node> wpp : wpps) {
                    if (wpp.a == candidateNode)
                        p1 = mv.getPoint(wpp.b);
                    if (wpp.b == candidateNode)
                        p2 = mv.getPoint(wpp.a);
                    if (p1 != null && p2 != null)
                        break;
                }
            }

            // Drawing preview lines
            GeneralPath b = new GeneralPath();
            if (p1 != null) {
                b.moveTo(mousePos.x, mousePos.y);
                b.lineTo(p1.x, p1.y);
            }
            if (p2 != null) {
                b.moveTo(mousePos.x, mousePos.y);
                b.lineTo(p2.x, p2.y);
            }
            g.draw(b);

            // Highlighting candidateNode
            if (candidateNode != null) {
                p1 = mv.getPoint(candidateNode);
                g.fillRect(p1.x - 2, p1.y - 2, 6, 6);
            }

        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @Override
    public void eventDispatched(AWTEvent event) {
        if (Main.map == null || Main.map.mapView == null
                || !Main.map.mapView.isActiveLayerDrawable())
            return;
        updateKeyModifiers((InputEvent) event);
        updateCursorDependentObjectsIfNeeded();
        updateCursor();
        updateStatusLine();
        Main.map.mapView.repaint();
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (selectionChangedBlocked)
            return;
        updateStateByCurrentSelection();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dragging = true;
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isEnabled())
            return;

        mousePos = e.getPoint();

        updateKeyModifiers(e);
        updateCursorDependentObjectsIfNeeded();
        updateCursor();
        updateStatusLine();
        Main.map.mapView.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1)
            return;

        updateKeyModifiers(e);
        mousePos = e.getPoint();

        if (state == State.selecting) {
            if (targetWay != null) {
                getCurrentDataSet().setSelected(targetWay.getPrimitiveId());
                updateStateByCurrentSelection();
            }
        } else if (state == State.improving && mousePos != null) {
            // Checking if the new coordinate is outside of the world
            if (mv.getLatLon(mousePos.x, mousePos.y).isOutSideWorld()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Cannot place a node outside of the world."),
                        tr("Warning"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (ctrl && candidateSegment != null) {
                // Adding a new node to the highlighted segment
                // Important: If there are other ways containing the same
                // segment, a node must added to all of that ways.
                Collection<Command> virtualCmds = new LinkedList<Command>();

                // Creating a new node
                Node virtualNode = new Node(mv.getEastNorth(mousePos.x,
                        mousePos.y));
                virtualCmds.add(new AddCommand(virtualNode));

                // Looking for candidateSegment copies in ways that are
                // referenced
                // by candidateSegment nodes
                List<Way> firstNodeWays = OsmPrimitive.getFilteredList(
                        candidateSegment.getFirstNode().getReferrers(),
                        Way.class);
                List<Way> secondNodeWays = OsmPrimitive.getFilteredList(
                        candidateSegment.getFirstNode().getReferrers(),
                        Way.class);

                Collection<WaySegment> virtualSegments = new LinkedList<WaySegment>();
                for (Way w : firstNodeWays) {
                    List<Pair<Node, Node>> wpps = w.getNodePairs(true);
                    for (Way w2 : secondNodeWays) {
                        if (!w.equals(w2))
                            continue;
                        // A way is referenced in both nodes.
                        // Checking if there is such segment
                        int i = -1;
                        for (Pair<Node, Node> wpp : wpps) {
                            ++i;
                            if ((wpp.a.equals(candidateSegment.getFirstNode())
                                    && wpp.b.equals(candidateSegment
                                            .getSecondNode()) || (wpp.b
                                    .equals(candidateSegment.getFirstNode()) && wpp.a
                                    .equals(candidateSegment.getSecondNode())))) {
                                virtualSegments.add(new WaySegment(w, i));
                            }
                        }
                    }
                }

                // Adding the node to all segments found
                for (WaySegment virtualSegment : virtualSegments) {
                    Way w = virtualSegment.way;
                    Way wnew = new Way(w);
                    wnew.addNode(virtualSegment.lowerIndex + 1, virtualNode);
                    virtualCmds.add(new ChangeCommand(w, wnew));
                }

                // Finishing the sequence command
                String text = trn("Add and a new node to way",
                        "Add and a new node to {0} ways",
                        virtualSegments.size(), virtualSegments.size());

                Main.main.undoRedo.add(new SequenceCommand(text, virtualCmds));

            } else if (candidateNode != null) {
                // Moving the highlighted node
                EastNorth nodeEN = candidateNode.getEastNorth();
                EastNorth cursorEN = mv.getEastNorth(mousePos.x, mousePos.y);

                Main.main.undoRedo.add(new MoveCommand(candidateNode, cursorEN
                        .east() - nodeEN.east(), cursorEN.north()
                        - nodeEN.north()));
            }
        }

        mousePos = null;
        updateCursor();
        updateStatusLine();
        Main.map.mapView.repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!isEnabled())
            return;

        if (!dragging) {
            mousePos = null;
        }
        Main.map.mapView.repaint();
    }

    // -------------------------------------------------------------------------
    // Custom methods
    // -------------------------------------------------------------------------

    /**
     * Updates shift and ctrl key states
     */
    private void updateKeyModifiers(InputEvent e) {
        oldCtrl = ctrl;
        oldShift = shift;
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
    }

    /**
     * Sets new cursor depending on state, mouse position
     */
    private void updateCursor() {
        if (!isEnabled()) {
            mv.setNewCursor(null, this);
            return;
        }

        if (state == State.selecting) {
            mv.setNewCursor(targetWay == null ? cursorSelect
                    : cursorSelectHover, this);
        } else if (state == State.improving) {
            mv.setNewCursor(ctrl ? (shift || dragging ? cursorImproveAddLock
                    : cursorImproveAdd)
                    : (shift || dragging ? cursorImproveLock : cursorImprove),
                    this);
        }
    }

    /**
     * Updates these objects under cursor: targetWay, candidateNode,
     * candidateSegment
     */
    public void updateCursorDependentObjectsIfNeeded() {
        if (state == State.improving && (shift || dragging)
                && (ctrl == oldCtrl)
                && !(candidateNode == null && candidateSegment == null))
            return;

        if (mousePos == null) {
            candidateNode = null;
            candidateSegment = null;
            return;
        }

        if (state == State.selecting) {
            targetWay = IWATargetWayHelper.findWay(mv, mousePos);
        } else if (state == State.improving) {
            if (ctrl) {
                candidateSegment = IWATargetWayHelper.findCandidateSegment(mv,
                        targetWay, mousePos);
                candidateNode = null;
            } else {
                candidateNode = IWATargetWayHelper.findCandidateNode(mv,
                        targetWay, mousePos);
                candidateSegment = null;
            }
        }
    }

    /**
     * Switches to Selecting state
     */
    public void startSelecting() {
        state = State.selecting;

        targetWay = null;
        if (getCurrentDataSet() != null) {
            getCurrentDataSet().clearSelection();
        }

        mv.repaint();
        updateStatusLine();
    }

    /**
     * Switches to Improving state
     *
     * @param targetWay
     *            Way that is going to be improved
     */
    public void startImproving(Way targetWay) {
        state = State.improving;

        List<OsmPrimitive> currentSelection = (List<OsmPrimitive>) getCurrentDataSet()
                .getSelected();
        if (currentSelection.size() != 1
                || !currentSelection.get(0).equals(targetWay)) {
            selectionChangedBlocked = true;
            getCurrentDataSet().clearSelection();
            getCurrentDataSet().setSelected(targetWay.getPrimitiveId());
            selectionChangedBlocked = false;
        }

        this.targetWay = targetWay;
        this.candidateNode = null;
        this.candidateSegment = null;

        mv.repaint();
        updateStatusLine();
    }

    /**
     * Updates the state according to the current selection. Goes to Improve
     * state if a single way or node is selected. Extracts a way by a node in
     * the second case.
     *
     */
    private void updateStateByCurrentSelection() {
        final ArrayList<Node> nodeList = new ArrayList<Node>();
        final ArrayList<Way> wayList = new ArrayList<Way>();
        final Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();

        // Collecting nodes and ways from the selection
        for (OsmPrimitive p : sel) {
            if (p instanceof Way)
                wayList.add((Way) p);
            if (p instanceof Node)
                nodeList.add((Node) p);
        }

        if (wayList.size() == 1) {
            // Starting improving the single selected way
            startImproving(wayList.get(0));
            return;
        } else if (nodeList.size() > 0) {
            // Starting improving the only way of the single selected node
            if (nodeList.size() == 1) {
                List<OsmPrimitive> r = nodeList.get(0).getReferrers();
                if (r.size() == 1 && (r.get(0) instanceof Way)) {
                    startImproving((Way) r.get(0));
                    return;
                }
            }
        }

        // Starting selecting by default
        startSelecting();
    }
}
