// License: GPL. For details, see LICENSE file.
package com.innovant.josm.plugin.routing.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

import com.innovant.josm.plugin.routing.RoutingLayer;
import com.innovant.josm.plugin.routing.RoutingModel;
import com.innovant.josm.plugin.routing.RoutingPlugin;
import com.innovant.josm.plugin.routing.gui.RoutingDialog;
import org.openstreetmap.josm.tools.Logging;

/**
 * Accounts for the selection or unselection of the routing tool in the tool bar,
 * and the mouse events when this tool is selected
 * @author Juangui
 * @author Jose Vidal
 *
 */
public class MoveRouteNodeAction extends MapMode {

    /**
     * Square of the distance radius where route nodes can be selected for dragging
     */
    private static final int DRAG_SQR_RADIUS = 100;

    /**
     * Index of dragged node
     */
    private int index;

    /**
     * Constructor
     */
    public MoveRouteNodeAction() {
        // TODO Use constructor with shortcut
        super(tr("Routing"), "move",
                tr("Click and drag to move destination"),
                ImageProvider.getCursor("normal", "move"));
    }

    @Override public void enterMode() {
        super.enterMode();
        MainApplication.getMap().mapView.addMouseListener(this);
    }

    @Override public void exitMode() {
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
    }

    @Override public void mousePressed(MouseEvent e) {
        // If left button is pressed
        if (e.getButton() == MouseEvent.BUTTON1 && MainApplication.getLayerManager().getActiveLayer() instanceof RoutingLayer) {
            requestFocusInMapView();
            RoutingLayer layer = (RoutingLayer) MainApplication.getLayerManager().getActiveLayer();
            RoutingModel routingModel = layer.getRoutingModel();
            // Search for the nearest node in the list
            List<Node> nl = routingModel.getSelectedNodes();
            index = -1;
            double dmax = DRAG_SQR_RADIUS; // maximum distance, in pixels
            for (int i = 0; i < nl.size(); i++) {
                Node node = nl.get(i);
                double d = MainApplication.getMap().mapView.getPoint(node).distanceSq(e.getPoint());
                if (d < dmax) {
                    dmax = d;
                    index = i;
                }
            }
            if (index >= 0)
                Logging.trace("Moved from node {0}", nl.get(index));
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        // If left button is released and a route node is being dragged
        if ((e.getButton() == MouseEvent.BUTTON1) && (index >= 0)) {
            searchAndReplaceNode(e.getPoint());
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
    }

    private void searchAndReplaceNode(Point point) {
        if (MainApplication.getLayerManager().getActiveLayer() instanceof RoutingLayer) {
            RoutingLayer layer = (RoutingLayer) MainApplication.getLayerManager().getActiveLayer();
            RoutingModel routingModel = layer.getRoutingModel();
            RoutingDialog routingDialog = RoutingPlugin.getInstance().getRoutingDialog();
            // Search for nearest highway node
            Node node = null;
            node = layer.getNearestHighwayNode(point);
            if (node == null) {
                Logging.trace("Didn't found a close node to move to.");
                return;
            }
            Logging.trace("Moved to node {0}", node);
            routingModel.removeNode(index);
            routingDialog.removeNode(index);
            routingModel.insertNode(index, node);
            routingDialog.insertNode(index, node);
            MainApplication.getLayerManager().getLayersOfType(RoutingLayer.class).forEach(RoutingLayer::invalidate);
        }
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof RoutingLayer;
    }
}
