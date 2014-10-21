// License: GPL. Copyright (C) 2012 Russell Edwards
package org.openstreetmap.josm.plugins.gpsblam;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;

class GPSBlamMode extends MapMode implements LayerChangeListener, MouseWheelListener, AWTEventListener {

    Point pointPressed;
    MapFrame frame;
    private Point oldP2;
    int radius;
    MouseWheelListener[] mapViewWheelListeners;
    GPSBlamLayer currentBlamLayer;

    GPSBlamMode(MapFrame mapFrame, String name, String desc) {
        super(name, "gpsblam_mode.png", desc, mapFrame, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        radius = 10;
    }

    @Override
    public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.addMouseWheelListener(this);
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Main.error(ex);
        }

        MapView.addLayerChangeListener(this);
    }

    @Override
    public void eventDispatched(AWTEvent e) {
        if (e instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent)e;
            if (ke.getKeyCode()==KeyEvent.VK_UP)
                changeRadiusBy(1);
            else if (ke.getKeyCode()==KeyEvent.VK_DOWN)
                changeRadiusBy(-1);
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.removeMouseWheelListener(this);
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            pointPressed = new Point(e.getPoint());
            // gain exclusive use of mouse wheel for now
            mapViewWheelListeners = Main.map.mapView.getMouseWheelListeners();
            for (MouseWheelListener l : mapViewWheelListeners) {
                if (l != this)
                    Main.map.mapView.removeMouseWheelListener(l);
            }
            paintBox(pointPressed, radius);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ( (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) ==  InputEvent.BUTTON1_DOWN_MASK) {
            //if button1 is hold, draw the line
            paintBox(e.getPoint(), radius);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        // give mapView back its mouse wheel
        for (MouseWheelListener l : mapViewWheelListeners) {
            if (l != this)
                Main.map.mapView.addMouseWheelListener(l);
        }

        xorDrawBox(pointPressed, oldP2, radius); // clear box

        GPSBlamInputData inputData = new GPSBlamInputData(pointPressed, e.getPoint(), radius);
        if (!inputData.isEmpty()) {
            if(currentBlamLayer == null) {
                currentBlamLayer = new GPSBlamLayer(tr("GPSBlam"));
                Main.main.addLayer(currentBlamLayer);
            }
            currentBlamLayer.addBlamMarker(new GPSBlamMarker(inputData));
            Main.map.mapView.repaint();
        }

        pointPressed = oldP2 = null;
    }

    private void changeRadiusBy(int delta) {
        if (pointPressed != null) {
            int newRadius = radius + delta;
            if (newRadius < 1)
                newRadius = 1;
            paintBox(oldP2, newRadius);
            radius = newRadius;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) ==  InputEvent.BUTTON1_DOWN_MASK) {
            changeRadiusBy(-e.getWheelRotation());
        }
    }

    private void xorDrawBox(Point p1, Point p2, int radius){
        if (frame != null) {
            Graphics2D g = (Graphics2D)Main.map.mapView.getGraphics();
            g.setXORMode(Color.BLACK);
            g.setColor(Color.WHITE);
            // AA+XOR broken in some versions of Java
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setStroke(new BasicStroke(2.0f));
            if (p2==null)
                p2 = p1;
            double length = Math.sqrt((p2.x-p1.x)*(p2.x-p1.x)+(p2.y-p1.y)*(p2.y-p1.y));
            if (length > 0) {
                double dirX = (p2.x-p1.x)/length, dirY = (p2.y-p1.y)/length; // unit direction vector from p1.x,p1.y to p2.x, p2.y
                double perpdirX = dirY, perpdirY = -dirX; // unit vector 90deg CW from direction vector
                double angle = Math.atan2(-perpdirY, perpdirX); // polar angle of perpdir
                double ofsX = radius * perpdirX, ofsY = radius * perpdirY; // radius vector, 90deg CW from dir vector

                Path2D path = new Path2D.Double();
                path.append(new Line2D.Double(p1.x+ofsX, p1.y+ofsY,p2.x+ofsX, p2.y+ofsY), false);
                path.append(new Arc2D.Double(p2.x-radius, p2.y-radius,radius*2, radius*2,
                        Math.toDegrees(angle), -180, Arc2D.OPEN), true);
                path.append(new Line2D.Double(p2.x-ofsX, p2.y-ofsY,p1.x-ofsX, p1.y-ofsY), true);
                path.append(new Arc2D.Double(p1.x-radius, p1.y-radius,radius*2, radius*2,
                        Math.toDegrees(angle)-180, -180, Arc2D.OPEN), true);
                path.closePath();
                g.draw(path);
            } else {
                try {
                    g.setXORMode(Color.BLACK);
                    g.setColor(Color.WHITE);
                    g.drawOval(Math.round(p2.x-radius), Math.round(p2.y-radius),
                    Math.round(radius*2), Math.round(radius*2));
                } catch (InternalError e) {
                    // Robustness against Java bug https://bugs.openjdk.java.net/browse/JDK-8041647
                    Main.error(e);
                    Main.error("Java bug JDK-8041647 occured. To avoid this bug, please consult https://bugs.openjdk.java.net/browse/JDK-8041647."
                            +" If the bug is fixed, please update your Java Runtime Environment.");
                }
            }
        }
    }

    private void paintBox(Point p2, int newRadius) {
        if (frame != null) {
            if (oldP2 != null) {
                xorDrawBox(pointPressed, oldP2, radius); // clear old box
            }
            xorDrawBox(pointPressed, p2, newRadius); // draw new box
            oldP2 = p2;
        }
    }

    public void setFrame(MapFrame mapFrame) {
        frame = mapFrame;
    }

    @Override
    public void activeLayerChange(Layer arg0, Layer arg1) {
        // Do nothing
    }

    @Override
    public void layerAdded(Layer arg0) {
        // Do nothing
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof GPSBlamLayer) {
            currentBlamLayer = null;
            if(Main.map.mapMode instanceof GPSBlamMode)
                Main.map.selectSelectTool(false);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        MapView.removeLayerChangeListener(this);
    }
}
