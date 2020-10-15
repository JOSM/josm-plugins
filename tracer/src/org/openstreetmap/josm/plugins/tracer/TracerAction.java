/**
 * Tracer - plugin for JOSM
 * Jan Bilak
 * This program is free software and licensed under GPL.
 */
package org.openstreetmap.josm.plugins.tracer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

class TracerAction extends MapMode implements MouseListener {

    private static final long serialVersionUID = 1L;
    protected boolean cancel;
    private boolean ctrl;
    private boolean alt;
    private boolean shift;
    protected TracerServer server = new TracerServer();

    public TracerAction() {
        super(tr("Tracer"), "tracer-sml", tr("Tracer."),
            Shortcut.registerShortcut("tools:tracer", tr("More tools: {0}", tr("Tracer")), KeyEvent.VK_T, Shortcut.DIRECT), getCursor());
    }

    @Override
    public void enterMode() {
        if (!isEnabled()) {
            return;
        }
        super.enterMode();
        MainApplication.getMap().mapView.setCursor(getCursor());
        MainApplication.getMap().mapView.addMouseListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
    }

    private static Cursor getCursor() {
        return ImageProvider.getCursor("crosshair", "tracer-sml");
    }

    protected void traceAsync(Point clickPoint) {
        cancel = false;
        /**
         * Positional data
         */
        final LatLon pos = MainApplication.getMap().mapView.getLatLon(clickPoint.x, clickPoint.y);

        try {
            PleaseWaitRunnable tracerTask = new PleaseWaitRunnable(tr("Tracing")) {

                @Override
                protected void realRun() throws SAXException {
                    traceSync(pos, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                }

                @Override
                protected void finish() {
                }

                @Override
                protected void cancel() {
                    TracerAction.this.cancel();
                }
            };
            Thread executeTraceThread = new Thread(tracerTask);
            executeTraceThread.start();
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    private void tagBuilding(Way way) {
        if(!alt) way.put("building", "yes");
        way.put("source", "cuzk:km");
    }

    private void traceSync(LatLon pos, ProgressMonitor progressMonitor) {
        Collection<Command> commands = new LinkedList<>();

        progressMonitor.beginTask(null, 3);
        try {
            ArrayList<LatLon> coordList = server.trace(pos);

            if (coordList.size() == 0) {
                return;
            }
            
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();

            // make nodes a way
            Way way = new Way();
            Node firstNode = null;
            for (LatLon coord : coordList) {
                Node node = new Node(coord);
                if (firstNode == null) {
                    firstNode = node;
                }
                commands.add(new AddCommand(ds, node));
                way.addNode(node);
            }
            way.addNode(firstNode);

            tagBuilding(way);
            commands.add(new AddCommand(ds, way));

            // connect to other buildings
            if (!ctrl) {
                commands.add(ConnectWays.connect(way));
                }

            if (!commands.isEmpty()) {
                   UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Tracer building"), commands));

                if (shift) {
                    ds.addSelected(way);
                } else {
                    ds.setSelected(way);
                }
            } else {
                System.out.println("Failed");
            }

        } finally {
            progressMonitor.finishTask();
        }
    }

    public void cancel() {
        cancel = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!MainApplication.getMap().mapView.isActiveLayerDrawable()) {
            return;
        }
        requestFocusInMapView();
        updateKeyModifiers(e);
        if (e.getButton() == MouseEvent.BUTTON1) {
            traceAsync(e.getPoint());
        }
    }

    @Override
    protected void updateKeyModifiers(MouseEvent e) {
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & (ActionEvent.ALT_MASK | InputEvent.ALT_GRAPH_MASK)) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}

