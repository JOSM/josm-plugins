// License: GPL. For details, see LICENSE file.
package com.innovant.josm.plugin.routing;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

import com.innovant.josm.jrt.osm.OsmEdge;
import org.openstreetmap.josm.tools.Logging;

/**
 * A JOSM layer that encapsulates the representation of the shortest path.
 * @author juangui
 * @author Jose Vidal
 */
public class RoutingLayer extends Layer {

    /**
     * Preference keys for routing
     */
    public enum PreferencesKeys {
        KEY_ACTIVE_ROUTE_COLOR(marktr("routing active route")),
        KEY_INACTIVE_ROUTE_COLOR(marktr("routing inactive route")),
        KEY_ROUTE_WIDTH("routing.route.width"),
        KEY_ROUTE_SELECT("routing.route.select");

        private final String key;
        PreferencesKeys(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private static final String ROUTING = "routing";

    /**
     * Constant
     */
    private static final double ARROW_PHI = Math.toRadians(20);

    /**
     * Routing Model
     */
    private final RoutingModel routingModel;

    /**
     * Start, Middle and End icons
     */
    private final Icon startIcon;
    private final Icon middleIcon;
    private final Icon endIcon;

    /**
     * Associated OSM layer
     */
    private final OsmDataLayer dataLayer;

    /**
     * Default constructor
     * @param name Layer name.
     * @param dataLayer The datalayer to use for routing
     */
    public RoutingLayer(String name, OsmDataLayer dataLayer) {
        super(name);
        Logging.trace("Creating Routing Layer...");
        this.startIcon = ImageProvider.get(ROUTING, "startflag");
        this.middleIcon = ImageProvider.get(ROUTING, "middleflag");
        this.endIcon = ImageProvider.get(ROUTING, "endflag");
        this.dataLayer = dataLayer;
        this.routingModel = new RoutingModel(dataLayer.data);
        Logging.trace("Routing Layer created.");

        this.routingModel.routingGraph.createGraph();    /* construct the graph right after we create the layer */
        invalidate();                            /* update MapView */
    }

    /**
     * Getter Routing Model.
     * @return the routingModel
     */
    public RoutingModel getRoutingModel() {
        return this.routingModel;
    }

    /**
     * Gets associated data layer
     * @return OsmDataLayer associated to the RoutingLayer
     */
    public OsmDataLayer getDataLayer() {
        return dataLayer;
    }

    /**
     * Gets nearest node belonging to a highway tagged way
     * @param p Point on the screen
     * @return The nearest highway node, in the range of the snap distance
     */
    public final Node getNearestHighwayNode(Point p) {
        Node nearest = null;
        int snapDistance = NavigatableComponent.PROP_SNAP_DISTANCE.get();
        double minDist = 0;
        for (Way w : dataLayer.data.getWays()) {
            if (w.isDeleted() || w.isIncomplete() || w.get("highway") == null) continue;
            for (Node n : w.getNodes()) {
                if (n.isDeleted() || n.isIncomplete()) continue;

                Point point = MainApplication.getMap().mapView.getPoint(n);
                double dist = p.distance(point);
                if (dist < snapDistance && ((nearest == null) || (dist < minDist))) {
                    nearest = n;
                    minDist = dist;
                }
            }
        }
        return nearest;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "routing_small");
    }

    @Override
    public Object getInfoComponent() {
        return "<html>"
                + "<body>"
                +"Graph Vertex: "+this.routingModel.routingGraph.getVertexCount()+"<br/>"
                +"Graph Edges: "+this.routingModel.routingGraph.getEdgeCount()+"<br/>"
                + "</body>"
                + "</html>";
    }

    @Override
    public Action[] getMenuEntries() {
        Collection<Action> components = new ArrayList<>();
        components.add(LayerListDialog.getInstance().createShowHideLayerAction());
        //        components.add(new JMenuItem(new LayerListDialog.ShowHideMarkerText(this)));
        components.add(LayerListDialog.getInstance().createDeleteLayerAction());
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new RenameLayerAction(getAssociatedFile(), this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new LayerListPopup.InfoAction(this));
        return components.toArray(new Action[0]);
    }

    @Override
    public String getToolTipText() {
        return this.routingModel.routingGraph.getVertexCount() + " vertices, "
                + this.routingModel.routingGraph.getEdgeCount() + " edges";
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void mergeFrom(Layer from) {
        // This layer is not mergable, so do nothing
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bounds) {
        boolean isActiveLayer = Objects.equals(mv.getLayerManager().getActiveLayer(), this);
        // Get routing nodes (start, middle, end)
        List<Node> nodes = routingModel.getSelectedNodes();

        // Get path stroke color from preferences
        // Color is different for active and inactive layers
        Color color;
        if (isActiveLayer) {
            color = new NamedColorProperty(PreferencesKeys.KEY_ACTIVE_ROUTE_COLOR.getKey(), Color.RED).get();
        } else {
            color = new NamedColorProperty(PreferencesKeys.KEY_INACTIVE_ROUTE_COLOR.getKey(), Color.decode("#dd2222")).get();
        }

        // Get path stroke width from preferences
        String widthString = Config.getPref().get(PreferencesKeys.KEY_ROUTE_WIDTH.getKey());
        if (widthString.isEmpty()) {
            widthString = "2";                        /* I think 2 is better  */
            // FIXME add after good width is found: Config.getPref().put(KEY_ROUTE_WIDTH, widthString);
        }
        int width = Integer.parseInt(widthString);


        // draw our graph
        if (isActiveLayer && routingModel.routingGraph != null && routingModel.routingGraph.getGraph() != null) {
            Set<OsmEdge> graphEdges = routingModel.routingGraph.getGraph().edgeSet();
            if (!graphEdges.isEmpty()) {
                Color color2 = ColorHelper.html2color("#00ff00");        /* just green for now  */
                OsmEdge firstedge = (OsmEdge) graphEdges.toArray()[0];
                Point from = mv.getPoint(firstedge.fromEastNorth());
                g.drawRect(from.x - 4, from.y + 4, from.x + 4, from.y - 4);
                for (OsmEdge edge : graphEdges) {
                    drawGraph(g, mv, edge, color2, width);
                }
            }
        }


        if (nodes == null || nodes.isEmpty()) return;

        // Paint routing path
        List<OsmEdge> routeEdges = routingModel.getRouteEdges();
        if (routeEdges != null) {
            for (OsmEdge edge : routeEdges) {
                drawEdge(g, mv, edge, color, width, true);
            }
        }

        // paint start icon
        Node node = nodes.get(0);
        Point screen = mv.getPoint(node);
        startIcon.paintIcon(mv, g, screen.x - startIcon.getIconWidth()/2,
                screen.y - startIcon.getIconHeight());

        // paint middle icons
        for (int index = 1; index < nodes.size() - 1; ++index) {
            node = nodes.get(index);
            screen = mv.getPoint(node);
            middleIcon.paintIcon(mv, g, screen.x - startIcon.getIconWidth()/2,
                    screen.y - middleIcon.getIconHeight());
        }
        // paint end icon
        if (nodes.size() > 1) {
            node = nodes.get(nodes.size() - 1);
            screen = mv.getPoint(node);
            endIcon.paintIcon(mv, g, screen.x - startIcon.getIconWidth()/2,
                    screen.y - endIcon.getIconHeight());
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (Node node : routingModel.getSelectedNodes()) {
            v.visit(node);
        }
    }

    @Override
    public synchronized void destroy() {
        routingModel.reset();
        //      layerAdded = false;
    }

    /**
     * Draw a line with the given color.
     */
    private static void drawEdge(Graphics g, MapView mv, OsmEdge edge, Color col, int width,
            boolean showDirection) {
        g.setColor(col);
        Point from;
        Point to;
        from = mv.getPoint(edge.fromEastNorth());
        to = mv.getPoint(edge.toEastNorth());

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Anti-alias!
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(width)); // thickness
        g2d.drawLine(from.x, from.y, to.x, to.y);
        if (showDirection) {
            double t = Math.atan2(to.y - (double) from.y, to.x - (double) from.x) + Math.PI;
            g.drawLine(to.x, to.y, (int) (to.x + 10*Math.cos(t-ARROW_PHI)), (int) (to.y + 10*Math.sin(t-ARROW_PHI)));
            g.drawLine(to.x, to.y, (int) (to.x + 10*Math.cos(t+ARROW_PHI)), (int) (to.y + 10*Math.sin(t+ARROW_PHI)));
        }
        g2d.setStroke(oldStroke);
    }

    private static void drawGraph(Graphics g, MapView mv, OsmEdge edge, Color col, int width) {
        g.setColor(col);
        Point from;
        Point to;
        from = mv.getPoint(edge.fromEastNorth());
        to = mv.getPoint(edge.toEastNorth());

        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(width)); // thickness
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Anti-alias!
        g2d.drawLine(from.x, from.y, to.x, to.y);
        g2d.drawRect(to.x - 2, to.y - 2, 4, 4);

        g2d.setStroke(oldStroke);
    }

}
