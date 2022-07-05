// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.IWaySegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static org.openstreetmap.josm.plugins.buildings_tools.BuildingsToolsPlugin.latlon2eastNorth;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

public class DrawBuildingAction extends MapMode implements MapViewPaintable, DataSelectionListener,
        KeyPressReleaseListener, ModifierExListener {
    // We need to avoid opening many file descriptors on Linux under Wayland -- see JOSM #21929. This will probably also
    // improve performance, since we aren't creating cursors all the time.
    private static final Cursor CURSOR_SILO = ImageProvider.getCursor("crosshair", "silo");
    private static final Cursor CURSOR_BUILDING = ImageProvider.getCursor("crosshair", "building");

    private enum Mode {
        None, Drawing, DrawingWidth, DrawingAngFix
    }

    private final Cursor cursorJoinNode;
    private final Cursor cursorJoinWay;
    private Cursor customCursor;

    private Mode mode = Mode.None;
    private Mode nextMode = Mode.None;

    private Color selectedColor = Color.red;
    private Point drawStartPos;
    private Point mousePos;
    /** Force a reasonable dimension */
    private static final double MIN_LEN_WIDTH = 1E-6;

    final transient Building building = new Building();

    private final PreferenceChangedListener shapeChangeListener = event -> updCursor();

    public DrawBuildingAction() {
        super(tr("Draw buildings"), "building", tr("Draw buildings"),
                Shortcut.registerShortcut("mapmode:buildings",
                        tr("Mode: {0}", tr("Draw buildings")),
                        KeyEvent.VK_B, Shortcut.DIRECT),
                // Set super.cursor to crosshair without overlay because super.cursor is final,
                // but we use two different cursors with overlays for rectangular and circular buildings
                // the actual cursor is drawn in enterMode()
                ImageProvider.getCursor("crosshair", null));

        cursorJoinNode = ImageProvider.getCursor("crosshair", "joinnode");
        cursorJoinWay = ImageProvider.getCursor("crosshair", "joinway");
    }

    private static Cursor getCursor() {
        try {
            if (ToolSettings.Shape.CIRCLE == ToolSettings.getShape()) {
                return CURSOR_SILO;
            } else {
                return CURSOR_BUILDING;
            }
        } catch (Exception e) {
            Logging.error(e);
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Displays the given cursor instead of the normal one.
     *
     * @param c One of the available cursors
     */
    private void setCursor(final Cursor c) {
        MainApplication.getMap().mapView.setNewCursor(c, this);
    }

    private void showAddrDialog(Way w) {
        AddressDialog dlg = new AddressDialog();
        if (!alt) {
            dlg.showDialog();
            if (dlg.getValue() != 1)
                return;
        }
        dlg.saveValues();
        String tmp = dlg.getHouseNum();
        if (tmp != null && !tmp.isEmpty())
            w.put("addr:housenumber", tmp);
        tmp = dlg.getStreetName();
        if (tmp != null && !tmp.isEmpty())
            w.put("addr:street", tmp);
    }

    @Override
    public void enterMode() {
        super.enterMode();

        MapFrame map = MainApplication.getMap();
        selectedColor = new NamedColorProperty(marktr("selected"), selectedColor).get();
        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        map.mapView.addTemporaryLayer(this);
        map.keyDetector.addKeyListener(this);
        map.keyDetector.addModifierExListener(this);
        SelectionEventManager.getInstance().addSelectionListener(this);
        Config.getPref().addKeyPreferenceChangeListener("buildings_tool.shape", shapeChangeListener);

        updateSnap(getLayerManager().getEditDataSet().getSelected());
        // super.enterMode() draws the basic cursor. Overwrite it with the cursor for the current building mode.
        updCursor();
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
        map.mapView.removeTemporaryLayer(this);
        map.keyDetector.removeKeyListener(this);
        map.keyDetector.removeModifierExListener(this);
        SelectionEventManager.getInstance().removeSelectionListener(this);
        Config.getPref().removeKeyPreferenceChangeListener("buildings_tool.shape", shapeChangeListener);

        if (mode != Mode.None)
            map.mapView.repaint();
        mode = Mode.None;
    }

    public final void cancelDrawing() {
        mode = Mode.None;
        MapFrame map = MainApplication.getMap();
        if (map == null || map.mapView == null)
            return;
        map.statusLine.setHeading(-1);
        map.statusLine.setAngle(-1);
        building.reset();
        map.mapView.repaint();
        updateStatusLine();
    }

    @Override
    public void modifiersExChanged(int modifiers) {
        boolean oldCtrl = ctrl;
        boolean oldShift = shift;
        updateKeyModifiersEx(modifiers);
        if (ctrl != oldCtrl || shift != oldShift) {
            processMouseEvent(null);
            updCursor();
            if (mode != Mode.None)
                MainApplication.getMap().mapView.repaint();
        }
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (mode != Mode.None)
                e.consume();

            cancelDrawing();
        }
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
    }

    private EastNorth getEastNorth() {
        if (!ctrl) {
            Node n = MainApplication.getMap().mapView.getNearestNode(mousePos, OsmPrimitive::isUsable);
            if (n != null)
                return latlon2eastNorth(n);
            IWaySegment<Node, Way> ws = MainApplication.getMap().mapView.getNearestWaySegment(mousePos,
                    OsmPrimitive::isSelectable);
            if (ws != null && ws.getWay().get("building") != null) {
                EastNorth p1 = latlon2eastNorth(ws.getFirstNode());
                EastNorth p2 = latlon2eastNorth(ws.getSecondNode());
                EastNorth enX = Geometry.closestPointToSegment(p1, p2,
                        latlon2eastNorth(MainApplication.getMap().mapView.getLatLon(mousePos.x, mousePos.y)));
                if (enX != null) {
                    return enX;
                }
            }
        }
        return latlon2eastNorth(MainApplication.getMap().mapView.getLatLon(mousePos.x, mousePos.y));
    }

    private boolean isRectDrawing() {
        return building.isRectDrawing() && (!shift || ToolSettings.isBBMode())
                && ToolSettings.Shape.RECTANGLE == ToolSettings.getShape();
    }

    private Mode modeDrawing() {
        EastNorth p = getEastNorth();
        if (isRectDrawing()) {
            building.setPlaceRect(p);
            if (Math.abs(building.getLength()) < MIN_LEN_WIDTH)
                return Mode.Drawing;
            return shift ? Mode.DrawingAngFix : Mode.None;
        } else if (ToolSettings.Shape.CIRCLE == ToolSettings.getShape()) {
            if (ToolSettings.getWidth() != 0) {
                building.setPlaceCircle(p, ToolSettings.getWidth(), shift);
            } else {
                building.setPlace(p, ToolSettings.getWidth(), ToolSettings.getLenStep(), shift);
            }
            if (building.getLength() < MIN_LEN_WIDTH)
                return Mode.Drawing;
            MainApplication.getMap().statusLine.setDist(building.getLength());
            return Mode.None;
        } else {
            building.setPlace(p, ToolSettings.getWidth(), ToolSettings.getLenStep(), shift);
            if (building.getLength() < MIN_LEN_WIDTH)
                return Mode.Drawing;
            MainApplication.getMap().statusLine.setDist(building.getLength());
            return ToolSettings.getWidth() == 0 ? Mode.DrawingWidth : Mode.None;
        }
    }

    private Mode modeDrawingWidth() {
        building.setWidth(getEastNorth());
        double width = Math.abs(building.getWidth());
        MainApplication.getMap().statusLine.setDist(width);
        return width < MIN_LEN_WIDTH ? Mode.DrawingWidth : Mode.None;
    }

    private Mode modeDrawingAngFix() {
        building.angFix(getEastNorth());
        return Mode.None;
    }

    private void processMouseEvent(MouseEvent e) {
        if (e != null) {
            mousePos = e.getPoint();
            updateKeyModifiers(e);
        }
        if (mode == Mode.None) {
            nextMode = Mode.None;
            return;
        }

        if (mode == Mode.Drawing) {
            nextMode = modeDrawing();
        } else if (mode == Mode.DrawingWidth) {
            nextMode = modeDrawingWidth();
        } else if (mode == Mode.DrawingAngFix) {
            nextMode = modeDrawingAngFix();
        } else {
            throw new AssertionError("Invalid drawing mode");
        }
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        if (mode == Mode.None || building.getLength() == 0) {
            return;
        }

        g.setColor(selectedColor);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        building.paint(g, mv);

        g.setStroke(new BasicStroke(1));

    }

    private void drawingStart(MouseEvent e) {
        mousePos = e.getPoint();
        drawStartPos = mousePos;
        EastNorth en = getEastNorth();
        building.setBase(en);
        mode = Mode.Drawing;
        updateStatusLine();
    }

    private void drawingAdvance(MouseEvent e) {
        processMouseEvent(e);
        if (this.mode != Mode.None && this.nextMode == Mode.None) {
            drawingFinish();
        } else {
            mode = this.nextMode;
            updateStatusLine();
        }
    }

    private void drawingFinish() {
        if (building.getLength() != 0) {
            Way w;
            if (ToolSettings.Shape.CIRCLE == ToolSettings.getShape()) {
                w = building.createCircle();
            } else {
                w = building.createRectangle(ctrl);
            }
            if (w != null) {
                if (!alt || ToolSettings.isUsingAddr())
                    for (Map.Entry<String, String> kv : ToolSettings.getTags().entrySet()) {
                        w.put(kv.getKey(), kv.getValue());
                    }
                if (ToolSettings.isUsingAddr())
                    showAddrDialog(w);
                if (ToolSettings.isAutoSelect()
                        && (getLayerManager().getEditDataSet().getSelected().isEmpty() || shift ||
                            ToolSettings.isAutoSelectReplaceSelection())) {
                    getLayerManager().getEditDataSet().setSelected(w);
                }
            }
        }
        cancelDrawing();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if (!MainApplication.getMap().mapView.isActiveLayerDrawable())
            return;

        requestFocusInMapView();

        if (mode == Mode.None)
            drawingStart(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        processMouseEvent(e);
        updCursor();
        if (mode != Mode.None)
            MainApplication.getMap().mapView.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if (!MainApplication.getMap().mapView.isActiveLayerDrawable())
            return;
        boolean dragged = true;
        if (drawStartPos != null) {
            dragged = e.getPoint().distance(drawStartPos) > 10;
            drawStartPos = null;
            if (ToolSettings.isNoClickAndDrag()) {
                return;
            }
        }

        if (mode == Mode.Drawing && !dragged)
            return;
        if (mode == Mode.None)
            return;

        drawingAdvance(e);
    }

    private void updCursor() {
        if (!MainApplication.isDisplayingMapView())
            return;

        if (!ctrl && (mousePos != null)) {
            Node n = MainApplication.getMap().mapView.getNearestNode(mousePos, OsmPrimitive::isSelectable);
            if (n != null) {
                setCursor(cursorJoinNode);
                return;
            } else {
                Way w = MainApplication.getMap().mapView.getNearestWay(mousePos, OsmPrimitive::isSelectable);
                if (w != null && w.get("building") != null) {
                    setCursor(cursorJoinWay);
                    return;
                }
            }
        }
        if (customCursor != null && (!ctrl || isRectDrawing()))
            setCursor(customCursor);
        else
            setCursor(getCursor());

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!MainApplication.getMap().mapView.isActiveLayerDrawable())
            return;
        processMouseEvent(e);
        updCursor();
        if (mode != Mode.None)
            MainApplication.getMap().mapView.repaint();
    }

    @Override
    public String getModeHelpText() {
        if (mode == Mode.None)
            return tr("Point on the corner of the building to start drawing");
        if (mode == Mode.Drawing)
            return tr("Point on opposite end of the building");
        if (mode == Mode.DrawingWidth)
            return tr("Set width of the building");
        return "";
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return isEditableDataLayer(l);
    }

    public final void updateSnap(Collection<? extends OsmPrimitive> newSelection) {
        building.clearAngleSnap();
        // update snap only if selection isn't too big
        if (newSelection.size() <= 10) {
            LinkedList<Node> nodes = new LinkedList<>();
            LinkedList<Way> ways = new LinkedList<>();

            for (OsmPrimitive p : newSelection) {
                switch (p.getType()) {
                case NODE:
                    nodes.add((Node) p);
                    break;
                case WAY:
                    ways.add((Way) p);
                    break;
                default:
                    break;
                }
            }

            building.addAngleSnap(nodes.toArray(new Node[0]));
            for (Way w : ways) {
                building.addAngleSnap(w);
            }
        }
        updateCustomCursor();
    }

    private void updateCustomCursor() {
        Double angle = building.getDrawingAngle();
        if (angle == null || !ToolSettings.isSoftCursor()) {
            customCursor = null;
            return;
        }
        final int R = 9; // crosshair outer radius
        final int r = 3; // crosshair inner radius
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        GeneralPath b = new GeneralPath();
        b.moveTo(16 - Math.cos(angle) * R, 16 - Math.sin(angle) * R);
        b.lineTo(16 - Math.cos(angle) * r, 16 - Math.sin(angle) * r);
        b.moveTo(16 + Math.cos(angle) * R, 16 + Math.sin(angle) * R);
        b.lineTo(16 + Math.cos(angle) * r, 16 + Math.sin(angle) * r);
        b.moveTo(16 + Math.sin(angle) * R, 16 - Math.cos(angle) * R);
        b.lineTo(16 + Math.sin(angle) * r, 16 - Math.cos(angle) * r);
        b.moveTo(16 - Math.sin(angle) * R, 16 + Math.cos(angle) * R);
        b.lineTo(16 - Math.sin(angle) * r, 16 + Math.cos(angle) * r);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(b);

        g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.BLACK);
        g.draw(b);

        customCursor = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(16, 16), "custom crosshair");

        updCursor();
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        updateSnap(event.getSelection());
    }
}
