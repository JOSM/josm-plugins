/*
 * Licence: GPL v2 or later
 * Author:  Alexei Kasatkin, 2011
 * Thanks to authors of BuildingTools, ImproveWayAccuracy and LakeWalker
 * for good sample code
 */
package org.openstreetmap.josm.plugins.fastdraw;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.PasteTagsAction.TagPaster;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.TextTagParser;

import static org.openstreetmap.josm.tools.I18n.tr;

class FastDrawingMode extends MapMode implements MapViewPaintable,
        AWTEventListener {
    private static final String SIMPLIFYMODE_MESSAGE=
            tr("Q=Options, Enter=save, Ctrl-Enter=save with tags, Up/Down=tune");
    private static final String DRAWINGMODE_MESSAGE=
    tr("Click or Click&drag to continue, Ctrl-Click to add fixed node, Shift-Click to delete, Enter to simplify or save, Ctrl-Shift-Click to start new line");

    private FDSettings settings;

    private DrawnPolyLine line;
    private MapView mv;
    private String statusText;
    private boolean drawing;
    private boolean ctrl;
    private boolean shift;
    private double eps;
    private final Cursor cursorDraw;
    private final Cursor cursorCtrl;
    private final Cursor cursorShift;
    private final Cursor cursorReady;
    private final Cursor cursorNode;
    private final Cursor cursorDrawing;
    private boolean nearSomeNode;
    private LatLon highlighted;
    private int nearestIdx;
    private int dragNode=-1;
    private SequenceCommand delCmd;
    private List<Node> oldNodes;
    
    private final TreeSet<Integer> set = new TreeSet<Integer>();
    private Timer timer;
  
    private KeyEvent releaseEvent;
    private boolean lineWasSaved;
    private boolean deltaChanged;
    /**
     * used for skipping keyboard AWTTevents while dialogs are active
     */
    private boolean listenKeys = false;
    
    FastDrawingMode(MapFrame mapFrame) {
        super(tr("FastDrawing"), "turbopen.png", tr("Fast drawing mode"), 
		Shortcut.registerShortcut("mapmode:fastdraw", tr("Mode: {0}", tr("Fast drawing mode")), KeyEvent.VK_F, Shortcut.SHIFT)
		,mapFrame, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        line=new DrawnPolyLine();
        cursorDraw = ImageProvider.getCursor("crosshair", null);
        cursorCtrl = ImageProvider.getCursor("crosshair", "fixed");
        cursorShift = ImageProvider.getCursor("crosshair", "new");
        cursorReady = ImageProvider.getCursor("crosshair", "ready");
        cursorNode = ImageProvider.getCursor("crosshair", "joinnode");
        cursorDrawing = ImageProvider.getCursor("crosshair", "mode");
        //loadPrefs();
    }

// <editor-fold defaultstate="collapsed" desc="Event listeners">

    @Override
    public void enterMode() {
        if (!isEnabled()) return;
        super.enterMode();
        lineWasSaved=false;
        settings=new FDSettings();
        settings.loadPrefs();
        settings.savePrefs();

        eps=settings.startingEps;
        mv = Main.map.mapView;
        line.setMv(mv);
        
        if (getCurrentDataSet() == null) return;

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.addTemporaryLayer(this);

        //tryToLoadWay();
        
        timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 timer.stop();
                 if (set.remove(releaseEvent.getKeyCode())) {
                  doKeyReleaseEvent(releaseEvent);
                 }
            }
        });
        
        listenKeys = true;
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this,
                    AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        if (line.wasSimplified() && !lineWasSaved) saveAsWay(false);

        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);

        Main.map.mapView.removeTemporaryLayer(this);

        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
        }
        settings.savePrefs();
        Main.map.mapView.setCursor(cursorDraw);
        repaint();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    //////////    Event listener methods

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        LinkedList<LatLon> pts=line.getPoints();
        if (pts.isEmpty()) return;

        if (line.wasSimplified()) {
            // we are drawing simplified version, that exists
            g.setStroke(settings.simplifiedStroke);
        } else {
            g.setStroke(settings.normalStroke);
        }
        
        int bigDotSize = settings.bigDotSize;

        Point p1, p2;
        LatLon pp1, pp2;
        p1 = line.getPoint(pts.get(0));
        g.setColor(settings.COLOR_FIXED);
        g.fillOval(p1.x - bigDotSize/2, p1.y - bigDotSize/2, bigDotSize, bigDotSize);
        Color lineColor,initLineColor;
        initLineColor = line.wasSimplified() ? settings.COLOR_SIMPLIFIED: settings.COLOR_NORMAL;
        lineColor = initLineColor;
        int rp,dp;
        dp=line.wasSimplified() ? settings.bigDotSize : settings.dotSize;  rp=dp/2;
        if (pts.size() > 1) {
        Iterator<LatLon> it1,it2;
        it1=pts.listIterator(0);
        it2=pts.listIterator(1);
        for (int i = 0; i < pts.size() - 1; i++) {
                pp1 = it1.next();
                p1 = line.getPoint(pp1);
                pp2 = it2.next();
                p2 = line.getPoint(pp2);
                if (shift && highlighted==pp1 && nearestIdx<0) {lineColor=settings.COLOR_SELECTEDFRAGMENT;}
                if (!shift && line.isLastPoint(i)) { lineColor=settings.COLOR_EDITEDFRAGMENT; }
                g.setColor(lineColor);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
                if (line.isFixed(pp2)) {
                    lineColor=initLineColor;
                    g.setColor(settings.COLOR_FIXED);
                    g.fillOval(p2.x - bigDotSize/2, p2.y - bigDotSize/2, bigDotSize, bigDotSize);
                } else {
                    g.fillRect(p2.x - rp, p2.y - rp, dp, dp);
                }
                if (!drawing) {
                    if (!line.wasSimplified() && nearestIdx==i+1 ) {
                    if (shift) {
                        // highlight node to delete
                        g.setStroke(settings.deleteStroke);
                        g.setColor(settings.COLOR_DELETE);
                        g.drawLine(p2.x - 5, p2.y - 5,p2.x + 5, p2.y + 5);
                        g.drawLine(p2.x - 5, p2.y + 5,p2.x + 5, p2.y - 5);
                        g.setStroke(settings.normalStroke);
                    } else if (ctrl) {
                        // highlight node to toggle fixation
                        g.setStroke(settings.deleteStroke);
                        g.setColor( line.isFixed(pp2) ? settings.COLOR_NORMAL: settings.COLOR_FIXED);
                        g.fillOval(p2.x - bigDotSize/2-2, p2.y - bigDotSize/2-2, bigDotSize+4, bigDotSize+4);
                        g.setStroke(settings.normalStroke);
                    } 
                    }
                }
            }
        }
        if (settings.drawLastSegment && !drawing && dragNode<0  && !shift && 
                nearestIdx<=0 && !line.wasSimplified()) {
            // draw line to current point
            g.setColor(lineColor);
            Point lp=line.getLastPoint();
            Point mp=mv.getMousePosition();
            if (lp!=null && mp!=null) g.drawLine(lp.x,lp.y,mp.x,mp.y);
        }
        if (deltaChanged) {
            g.setColor(lineColor);
            Point lp=line.getLastPoint();
            int r=(int) settings.minPixelsBetweenPoints;
            if (lp!=null) g.drawOval(lp.x-r,lp.y-r,2*r,2*r);
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!listenKeys || Main.map == null || Main.map.mapView == null
                || !Main.map.mapView.isActiveLayerDrawable()) {
            return;
        }
        updateKeyModifiers((InputEvent) event);
        if (event instanceof KeyEvent) {
        KeyEvent e=(KeyEvent) event;
        
        if (event.getID() == KeyEvent.KEY_PRESSED) {
             if (timer.isRunning()) {
                  timer.stop();
                } else {
                  set.add((e.getKeyCode()));
                }
            doKeyEvent((KeyEvent) event);
        }
        if (event.getID() == KeyEvent.KEY_RELEASED) {
            if (timer.isRunning()) {
              timer.stop();
               if (set.remove(e.getKeyCode())) {
                  doKeyReleaseEvent(e);
               }
            } else {
              releaseEvent = e;
              timer.restart();
            }
        }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!isEnabled()) return;
        if (e.getButton() != MouseEvent.BUTTON1) return;

        int idx=line.findClosestPoint(e.getPoint(),settings.maxDist);
        if (idx==0 && !line.isClosed()) {
            line.closeLine();
            // the way should become closed
            drawing=false;
            dragNode=0;
            updateCursor();
            return;
        }
        autoCloseIfNeeded();

        if (ctrl && shift) {newDrawing();repaint();return;}
        if (!ctrl && shift) {
            if (idx>=0) {line.deleteNode(idx); nearestIdx=-1;}
            else line.tryToDeleteSegment(e.getPoint());
            return;
        }
        if (idx>=0) {
            if (ctrl) {
                // toggle fixed point
                line.toggleFixed(idx);
            }
            // node dragging
            dragNode=idx;
            return;
        }
        startDrawing(e.getPoint(),settings.fixedClick);
    }

    private void startDrawing(Point point, boolean fixFlag) {
        //if (line.isClosed()) { setStatusLine(tr(SIMPLIFYMODE_MESSAGE));return;  }
        drawing = true;
        if (line.wasSimplified()) { 
            // new line started after simplification
            // we need to save old line
            saveAsWay(false);
            newDrawing();
            //line.clearSimplifiedVersion();
        }

        LatLon p = mv.getLatLon(point.x, point.y);
        if (settings.snapNodes) { // find existing node near point and use it
            Node nd1 = getNearestNode(point, settings.maxDist);
            if (nd1!=null) {
                // found node, make it fixed point of the line
                //System.out.println("node "+nd1);
                p=nd1.getCoor();
                line.fixPoint(p);
            }
        }

        line.addLast(p);
        if (ctrl || fixFlag) line.fixPoint(p);
        
        setStatusLine(tr("Please move the mouse to draw new way"));
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        stopDrawing();
    }
    
    private void stopDrawing() {
        if (!isEnabled()) return;
        dragNode = -1;
        drawing = false;
        highlighted=null;
        if (!line.isClosed()) setStatusLine(DRAWINGMODE_MESSAGE);
        updateCursor();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isEnabled()) return;
        deltaChanged=false;
        Node nd1 = getNearestNode(e.getPoint(), settings.maxDist);
        boolean nearSomeNode2=nd1!=null;
        boolean needRepaint=false;
        if (nearSomeNode!=nearSomeNode2) {nearSomeNode=nearSomeNode2;updateCursor();needRepaint=true;}

        int nearestIdx2=line.findClosestPoint(e.getPoint(),settings.maxDist);
        if (nearestIdx != nearestIdx2) {nearestIdx=nearestIdx2; updateCursor();needRepaint=true;}
        if (settings.drawLastSegment) needRepaint=true;
        
        if (!drawing) {
            if (dragNode>=0) {
                line.moveNode(dragNode,getLatLon(e));
                repaint();
                return;
            }

            if (shift) {
                // find line fragment to highlight
                LatLon h2=line.findBigSegment(e.getPoint());
                if (highlighted!=h2) { highlighted=h2; repaint(); }
            } else if (needRepaint) {
                repaint();
            }
            return;
        }
        if (line.isClosed()) setStatusLine(SIMPLIFYMODE_MESSAGE);

        // do not draw points close to existing points - we do not want self-intersections
        if (nearestIdx>=0) { return; }

        Point lastP = line.getLastPoint(); // last point of line fragment being edited

        // free mouse-drawing
        if (nearSomeNode){
            if (settings.snapNodes && Math.hypot(e.getX() - lastP.x, e.getY() - lastP.y) > 1e-2) {
                line.addFixed(nd1.getCoor()); // snap to node coords
                repaint();
                return;
            }
        } else {
            if (Math.hypot(e.getX() - lastP.x, e.getY() - lastP.y) > settings.minPixelsBetweenPoints) {
                line.addLast(getLatLon(e)); // add new point
                repaint();
                return;
            }
        }
        autoCloseIfNeeded();
    }

    private void doKeyEvent(KeyEvent e) {
        if (getShortcut().isEvent(e)) { // repeated press
            tryToLoadWay();
            return;
        }
        switch(e.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
            if (line.wasSimplified()) {
                // return to line editing
                line.clearSimplifiedVersion();
                repaint();
                eps=settings.startingEps;
            }
            back();
        break;
        case KeyEvent.VK_ENTER:
            e.consume();
            // first Enter = simplify, second = save the way
            if (!line.wasSimplified()) {
                //line.simplify(eps);
                switch(settings.simplifyMode) {
                    case 0: //case 1:
                        eps = line.autoSimplify(settings.startingEps, settings.epsilonMult, 
                        settings.pkmBlockSize, settings.maxPointsPerKm);
                        break;
                    case 1: //case 2: case 3:
                        line.simplify(eps);
                        break;
                }
                if (settings.simplifyMode==2) {
                    // autosave
                    saveAsWay(true);
                } else {
                    repaint();
                    showSimplifyHint();
                }
            } else {saveAsWay(true);}
        break;
        case KeyEvent.VK_DOWN:
            if (ctrl || shift || alt) return;
            // more details
            e.consume();
            if (line.wasSimplified()) changeEpsilon(settings.epsilonMult);
            else changeDelta(1/1.1);
        break;
        case KeyEvent.VK_UP:
            if (ctrl || shift || alt) return;
            // less details
            e.consume();
            if (line.wasSimplified()) changeEpsilon(1/settings.epsilonMult);
            else changeDelta(1.1);
        break;
        case KeyEvent.VK_ESCAPE:
            e.consume();
            Point lastPoint = line.getLastPoint();
            if (!line.isClosed()) line.moveToTheEnd();
            if (lastPoint==null || lastPoint.equals(line.getLastPoint())) {
                 if (line.getPoints().size()>5) {
                    // no key events while the dialog is active!
                    listenKeys = false;
                    boolean answer = ConditionalOptionPaneUtil.showConfirmationDialog(
                       "delete_drawn_line", Main.parent,
                       tr("Are you sure you do not want to save the line containing {0} points?",
                            line.getPoints().size()), tr("Delete confirmation"),
                       JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_OPTION );
                    listenKeys = true;
                    if(!answer) break;
                }
                newDrawing(); // stop drawing
                exitMode();
                Main.map.selectSelectTool(false);
            }
        break;
        
        case KeyEvent.VK_I:
           listenKeys = false;
           JOptionPane.showMessageDialog(Main.parent,
                        tr("{0} m - length of the line\n{1} nodes\n{2} points per km (maximum)\n{3} points per km (average)",
                        line.getLength(),line.getPoints().size(),line.getNodesPerKm(settings.pkmBlockSize),
                        line.getNodesPerKm(1000000)),
                        tr("Line information"),JOptionPane.INFORMATION_MESSAGE);
           listenKeys = true;
        break;            
        case KeyEvent.VK_Q:
            // less details
            e.consume();
            try {
                listenKeys = false;
                new FastDrawConfigDialog(settings);
                if (line.wasSimplified()) {
                eps = line.autoSimplify(settings.startingEps, settings.epsilonMult, settings.pkmBlockSize,settings.maxPointsPerKm);
                showSimplifyHint();
                }
                //System.out.println("final eps="+eps);
                listenKeys = true;
            } catch (SecurityException ex) {  }
            repaint();
        break;
        case KeyEvent.VK_SPACE:
            e.consume();
            if (!drawing) {
                Point p = Main.map.mapView.getMousePosition();
                if (p!=null) startDrawing(p,settings.fixedSpacebar);
            }
        break;
        }
    }
    
    private void doKeyReleaseEvent(KeyEvent keyEvent) {
            //System.out.println("released "+keyEvent);
            if (keyEvent.getKeyCode()==KeyEvent.VK_SPACE) stopDrawing();
            updateCursor();
    }
    /**
     * Updates shift and ctrl key states
     */
    protected void updateKeyModifiers(InputEvent e) {
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        updateCursor();
    }

    @Override
    protected void updateStatusLine() {
        Main.map.statusLine.setHelpText(statusText);
        Main.map.statusLine.repaint();
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Different action helper methods">
    public void newDrawing() {
        if (delCmd!=null) delCmd.undoCommand();
        delCmd=null; oldNodes=null;
        eps=settings.startingEps;
        line.clear();
    }
    
    private void saveAsWay(boolean autoExit) {
        List<LatLon> pts=line.getPoints();
        int n = pts.size();
        if (n < 2) return; //do not save oversimplified lines
        if (line.isClosed() && n==2) return;
        if (line.isClosed() && n==3) pts.remove(2); // two-point way can not be closed

        Collection<Command> cmds = new LinkedList<Command>();
        int i = 0;
        Way w = new Way();
        LatLon first=pts.get(0);
        Node firstNode=null;

        for (LatLon p : pts) {
            Node nd=null;
 
            nd = Main.map.mapView.getNearestNode(line.getPoint(p), OsmPrimitive.isSelectablePredicate);
            // there may be a node with the same coords!
            
            if (nd!=null && p.greatCircleDistance(nd.getCoor())>0.01) nd=null;
            if (nd==null) {
                if (i>0 && p.equals(first)) nd=firstNode; else {
                    nd = new Node(p);
                    cmds.add(new AddCommand(nd));
                }
            }
            if (nd.getCoor().isOutSideWorld()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Cannot place node outside of the world."));
                return;
            }
            if (i==0) firstNode=nd;
            w.addNode(nd);
            i++;
        }
        if (ctrl) {
            // paste tags - from ctrl-shift-v
            Set <OsmPrimitive> ts = new HashSet<OsmPrimitive>();
            ts.add(w);
            TagPaster tp = new TagPaster(Main.pasteBuffer.getDirectlyAdded(), ts);
            List<Tag> execute = tp.execute();
            Map<String,String> tgs=new HashMap<String,String>();
            for (Tag t : execute) {
                w.put(t.getKey(), t.getValue());
            }
        }
        if (!settings.autoTags.isEmpty()) {
            Map<String, String> tags = TextTagParser.readTagsFromText(settings.autoTags);
            for (String k: tags.keySet()) {
                w.put(k, tags.get(k));
            }
        }
        if (delCmd!=null) {
            List<Node> nodes = w.getNodes();
            for (Node nd: oldNodes) {
                // node from old way but not in new way 
                if (!nodes.contains(nd)) {
                    List<OsmPrimitive> refs = nd.getReferrers();
                    // does someone need this node? if no-delete it.
                    if (refs.isEmpty() && !nd.isDeleted() && nd.isUsable()) cmds.add(new DeleteCommand(nd));                                       
                }
            }
            delCmd = null; // that is all with this command
            cmds.add(new AddCommand(w));
        } else cmds.add(new AddCommand(w));
        Command c = new SequenceCommand(tr("Draw the way by mouse"), cmds);
        if (!Main.main.hasEditLayer()) return;
        Main.main.undoRedo.add(c);
        lineWasSaved = true;
        newDrawing(); // stop drawing
        if (autoExit) {
            // Select this way and switch drawing mode off
            exitMode();
            getCurrentDataSet().setSelected(w);
            Main.map.selectSelectTool(false);
        }
    }

    public void back() {
        line.undo();
        repaint();
    }

    void changeEpsilon(double k) {
        //System.out.println(tr("Eps={0}", eps));
        eps*=k;
        line.simplify(eps);
        /* I18N: Eps = Epsilon, the tolerance parameter */ 
        showSimplifyHint();
        repaint();
    }

    void changeDelta(double k) {
        settings.minPixelsBetweenPoints*=k;
        deltaChanged=true;
        
        setStatusLine(tr("min distance={0} px ({1} m)",(int)settings.minPixelsBetweenPoints,
                mv.getDist100Pixel()/100*settings.minPixelsBetweenPoints));
        repaint();
    }


    /*private Node findClosestNode(LatLon p, double d) {
        Node nn=null;
        double dist,minD=1e10,x,y;
        Point pscreen=getPoint(p);   x=pscreen.x; y=pscreen.y;
        BBox b=new BBox(new LatLon(p.lat()-deltaLatLon,p.lon()-deltaLatLon),
                new LatLon(p.lat()+deltaLatLon,p.lon()+deltaLatLon));
        List<Node> nodes = getCurrentDataSet().searchNodes(b);
        for (Node n: nodes) {
            dist = Math.sqrt(getPoint(n.getCoor()).distanceSq(x,y));
            if (dist<d && dist<minD) {
                nn=n;
                minD=dist;
            };
        }
        return nn;
    }*/

    private void loadFromWay(Way w) {
        List<LatLon> pts=line.getPoints();

        Collection<Command> cmds = new LinkedList<Command>();
        
        Node firstNode=null;
        Object[] nodes = w.getNodes().toArray();
        int n=nodes.length;
        if (w.isClosed()) n--;
        for (int i=0;i<n;i++) {
            Node nd=(Node) nodes[i];
            line.addLast(nd.getCoor());
        }
        if (w.isClosed()) line.closeLine();
        oldNodes = w.getNodes();
        List <OsmPrimitive> wl = new ArrayList<OsmPrimitive>(); wl.add(w);
        
        Command c = DeleteCommand.delete(getEditLayer(), wl, false);
        if (c != null) cmds.add(c);
        delCmd = new SequenceCommand(tr("Convert way to FastDraw line"), cmds);
        Main.main.undoRedo.add(delCmd);
    }

    private void setStatusLine(String tr) {
        statusText=tr;
        updateStatusLine();
    }

    private void showSimplifyHint() {
            setStatusLine(tr("Eps={0}, {1} points, {2} p/km", 
                eps, line.getSimplePointsCount(),line.getNodesPerKm(settings.pkmBlockSize))+" "
            +SIMPLIFYMODE_MESSAGE);
    }
    
    private void updateCursor() {
        if (shift) Main.map.mapView.setCursor(cursorShift); else
        if (line.isClosed() || (nearestIdx==0)) Main.map.mapView.setCursor(cursorReady); else
        if (ctrl) Main.map.mapView.setCursor(cursorCtrl); else
        if (nearSomeNode && settings.snapNodes) Main.map.mapView.setCursor(cursorCtrl); else
        if (drawing) Main.map.mapView.setCursor(cursorDrawing); else
        Main.map.mapView.setCursor(cursorDraw);
    }

    private void repaint() {
        Main.map.mapView.repaint();
    }
    
    private void tryToLoadWay() {
        updateCursor();
        Collection<Way> selectedWays = Main.main.getCurrentDataSet().getSelectedWays();
        if (selectedWays!=null // if there is a selection
            && selectedWays.size()==1 // and one way is selected
            && line.getPoints().size()==0) /* and ther is no already drawn line */ {
            // we can start drawing new way starting from old one
            Way w = selectedWays.iterator().next();
            
            if (w.isNew()) loadFromWay(w);
        }
    }

    private void autoCloseIfNeeded() {
        if (settings.drawClosed && line.getPointCount()>1 && !line.isClosed()) {
            line.closeLine();
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Helper functions">

    private Node getNearestNode(Point point, double maxDist) {
       Node nd = Main.map.mapView.getNearestNode(point, OsmPrimitive.isSelectablePredicate);
       if (nd!=null && line.getPoint(nd.getCoor()).distance(point)<=maxDist) return nd;
       else return null;
    }

    LatLon getLatLon(MouseEvent e) {
        return mv.getLatLon(e.getX(), e.getY());
    }
// </editor-fold>

}