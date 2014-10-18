/*
 * Copyright (C) 2008 Innovant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,USA.
 *
 * For more information, please contact:
 *
 *  Innovant
 *   juangui@gmail.com
 *   vidalfree@gmail.com
 *
 *  http://public.grupoinnovant.com/blog
 *
 */

package com.innovant.josm.jrt.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import com.innovant.josm.jrt.osm.OsmEdge;
import com.innovant.josm.plugin.routing.RoutingLayer;
import com.innovant.josm.plugin.routing.RoutingModel;

/**
 * Class utility to work with graph routers.
 *
 * @author Juangui
 * @author Jose Vidal
 * @author Hassan S
 */
public class RoutingGraph {

    /**
     * Routing Profile
     */
    private final RoutingProfile routingProfile;

    /**
     * Diferent algorithms to apply to the graph.
     */
    public enum Algorithm {
        ROUTING_ALG_DIJKSTRA, ROUTING_ALG_BELLMANFORD
    };

    /**
     * Search criteria for the route.
     */
    public enum RouteType {FASTEST,SHORTEST};

    /**
     *
     */
    private RouteType routeType;

    /**
     * Associated Osm DataSet
     */
    private final DataSet data;

    /**
     * Logger.
     */
    static Logger logger = Logger.getLogger(RoutingGraph.class);

    private static Collection<String> excludedHighwayValues = Arrays.asList(new String[]{
        "bus_stop", "traffic_signals", "street_lamp", "stop", "construction", 
        "platform", "give_way", "proposed", "milestone", "speed_camera", "abandoned"
    });
    
    /**
     * Graph state
     * <code>true</code> Graph in memory.
     * <code>false</code> Graph not created.
     */
    //  public boolean graphState;

    /**
     * OSM Graph.
     */
    //  private DirectedWeightedMultigraph<Node, OsmEdge> graph;
    //  private WeightedMultigraph<Node, OsmEdge> graph;
    private Graph<Node, OsmEdge> graph;
    private RoutingGraphDelegator rgDelegator=null;


    /**
     * Graph getter
     */
    public Graph<Node, OsmEdge> getGraph(){
        return graph;

    }


    private void addEdgeBidirectional( Way way, Node from, Node to){
        addEdge(way,from,to);
        addEdge(way,to,from);
    }

    private void addEdgeReverseOneway( Way way, Node from, Node to){
        addEdge(way,to,from);
    }

    private void addEdgeNormalOneway( Way way, Node from, Node to){
        addEdge(way,from,to);
    }

    /**
     * Speeds
     */
    private Map<String,Double> waySpeeds;

    /**
     * Default Constructor.
     */
    public RoutingGraph(DataSet data) {
        //      this.graphState = false;
        this.graph = null;
        this.data = data;
        routeType=RouteType.SHORTEST;
        routingProfile=new RoutingProfile("default");
        routingProfile.setOnewayUse(true); // Don't ignore oneways by default
        this.setWaySpeeds(routingProfile.getWaySpeeds());
        logger.debug("Created RoutingGraph");
    }

    /**
     * Create OSM graph for routing
     *
     * @return
     */
    public void createGraph() {

        logger.debug("Creating Graph...");
        graph = new DirectedWeightedMultigraph<>(OsmEdge.class);
        rgDelegator=new RoutingGraphDelegator(graph);
        rgDelegator.setRouteType(this.routeType);
        // iterate all ways and segments for all nodes:
        for (Way way : data.getWays()) {

            // skip way if not suitable for routing.
            if (way == null || way.isDeleted() || !this.isvalidWay(way)
                    || way.getNodes().size() < 1) continue;

            // INIT
            Node from = null;
            Node to = null;
            List<Node> nodes = way.getNodes();
            int nodes_count = nodes.size();

            /*
             * Assume node is A B C D E. The procedure should be
             * 
             *  case 1 - bidirectional ways:
             *  1) Add vertex A B C D E
             *  2) Link A<->B, B<->C, C<->D, D<->E as Edges
             * 
             *  case 2 - oneway reverse:
             *  1) Add vertex A B C D E
             *  2) Link B->A,C->B,D->C,E->D as Edges. result: A<-B<-C<-D<-E
             * 
             *  case 3 - oneway normal:
             *  1) Add vertex A B C D E
             *  2) Link A->B, B->C, C->D, D->E as Edges. result: A->B->C->D->E
             * 
             * 
             */

            String oneway_val = way.get("oneway");   /*   get (oneway=?) tag for this way.   */
            String junction_val = way.get("junction");   /*   get (junction=?) tag for this way.   */

            from = nodes.get(0);                   /*   1st node A  */
            graph.addVertex(from);                 /*   add vertex A */

            for (int i = 1; i < nodes_count; i++) { /*   loop from B until E */

                to = nodes.get(i);                   /*   2nd node B   */

                if (to != null && !to.isDeleted()) {
                    graph.addVertex(to);               /*   add vertex B */


                    //this is where we link the vertices
                    if (!routingProfile.isOnewayUsed()) {
                        //"Ignore oneways" is selected
                        addEdgeBidirectional(way, from, to);

                    } else if (oneway_val == null && junction_val == "roundabout") {
                        //Case (roundabout): oneway=implicit yes
                        addEdgeNormalOneway(way, from, to);

                    } else if (oneway_val == null || oneway_val == "false" || oneway_val == "no" || oneway_val == "0") {
                        //Case (bi-way): oneway=false OR oneway=unset OR oneway=0 OR oneway=no
                        addEdgeBidirectional(way, from, to);

                    } else if (oneway_val == "-1") {
                        //Case (oneway reverse): oneway=-1
                        addEdgeReverseOneway(way, from, to);

                    } else if (oneway_val == "1" || oneway_val == "yes" || oneway_val == "true") {
                        //Case (oneway normal): oneway=yes OR 1 OR true
                        addEdgeNormalOneway(way, from, to);

                    }

                    from = to;                         /*   we did A<->B, next loop we will do B<->C, so from=B,to=C for next loop. */
                }

            } // end of looping thru nodes
        } // end of looping thru ways

        logger.debug("End Create Graph");
        logger.debug("Vertex: "+graph.vertexSet().size());
        logger.debug("Edges: "+graph.edgeSet().size());
    }

    /**
     * Compute weight and add edge to the graph
     * @param way
     * @param from
     * @param to
     */
    private void addEdge(Way way,Node from, Node to) {
        LatLon fromLL = from.getCoor();
        LatLon toLL = from.getCoor();
        if (fromLL == null || toLL == null) {
            return;
        }
        double length = fromLL.greatCircleDistance(toLL);

        OsmEdge edge = new OsmEdge(way, from, to);
        edge.setSpeed(12.1);
        graph.addEdge(from, to, edge);
        // weight = getWeight(way);
        double weight = getWeight(way, length);
        setWeight(edge, length);
        logger.debug("edge for way " + way.getId()
                + "(from node " + from.getId() + " to node "
                + to.getId() + ") has weight: " + weight);
        ((DirectedWeightedMultigraph<Node,OsmEdge>)graph).setEdgeWeight(edge, weight);
    }

    /**
     * Set the weight for the given segment depending on the highway type
     * and the length of the segment. The higher the value, the less it is used
     * in routing.
     *
     * @param way
     *            the way.
     * @return
     */
    private void setWeight(OsmEdge osmedge, double length) {

        osmedge.setLength(length);
        if (this.waySpeeds.containsKey(osmedge.getWay().get("highway")))
            osmedge.setSpeed(this.waySpeeds.get(osmedge.getWay().get("highway")));

    }

    /**
     * Returns the weight for the given segment depending on the highway type
     * and the length of the segment. The higher the value, the less it is used
     * in routing.
     *
     * @param way
     *            the way.
     * @return
     */
    private double getWeight(Way way, double length) {
        // Default speed if no setting is found
        double speed = 1;

        switch (routeType) {
        case SHORTEST:
            // Same speed for all types of ways
            if (this.waySpeeds.containsKey("residential"))
                speed=this.waySpeeds.get("residential");
            break;
        case FASTEST:
            // Each type of way may have a different speed
            if (this.waySpeeds.containsKey(way.get("highway")))
                speed=this.waySpeeds.get(way.get("highway"));
            logger.debug("Speed="+speed);
            break;
        default:
            break;
        }
        // Return the time spent to traverse the way
        return length / speed;
    }

    /**
     * Check if a Way is correct.
     *
     * @param way
     *            The way.
     * @return <code>true</code> is valid. <code>false</code> is not valid.
     */
    public boolean isvalidWay(Way way) {
        //if (!way.isTagged())            <---not needed me thinks
        //    return false;
        
        String highway = way.get("highway");

        return (highway != null && !excludedHighwayValues.contains(highway)) || way.get("junction") != null
                || way.get("service") != null;

    }

    /**
     * Apply selected routing algorithm to the graph.
     *
     * @param nodes
     *            Nodes used to calculate path.
     * @param algorithm
     *            Algorithm used to compute the path,
     *            RoutingGraph.Algorithm.ROUTING_ALG_DIJKSTRA or
     *            RoutingGraph.Algorithm.ROUTING_ALG_BELLMANFORD
     * @return new path.
     */
    public List<OsmEdge> applyAlgorithm(List<Node> nodes, Algorithm algorithm) {
        List<OsmEdge> path = new ArrayList<>();
        Graph<Node,OsmEdge> g;
        double totalWeight = 0;
        RoutingLayer layer = (RoutingLayer)Main.map.mapView.getActiveLayer();
        RoutingModel routingModel = layer.getRoutingModel();

        if (graph == null || routingModel.getOnewayChanged())
            this.createGraph();
        logger.debug("apply algorithm between nodes ");

        for (Node node : nodes) {
            logger.debug(node.getId());
        }
        logger.debug("-----------------------------------");

        // Assign the graph to g
        g = graph;

        switch (algorithm) {
        case ROUTING_ALG_DIJKSTRA:
            logger.debug("Using Dijkstra algorithm");
            DijkstraShortestPath<Node, OsmEdge> routingk = null;
            for (int index = 1; index < nodes.size(); ++index) {
                routingk = new DijkstraShortestPath<>(g, nodes
                        .get(index - 1), nodes.get(index));
                if (routingk.getPathEdgeList() == null) {
                    logger.debug("no path found!");
                    break;
                }
                path.addAll(routingk.getPathEdgeList());
                totalWeight += routingk.getPathLength();
            }
            break;
        case ROUTING_ALG_BELLMANFORD:
            logger.debug("Using Bellman Ford algorithm");
            for (int index = 1; index < nodes.size(); ++index) {
                path = BellmanFordShortestPath.findPathBetween(rgDelegator, nodes
                        .get(index - 1), nodes.get(index));
                if (path == null) {
                    logger.debug("no path found!");
                    return null;
                }
            }
            break;
        default:
            logger.debug("Wrong algorithm");
            break;
        }

        logger.debug("shortest path found: " + path + "\nweight: "
                + totalWeight);
        return path;
    }

    /**
     * Return the number of vertices.
     * @return the number of vertices.
     */
    public int getVertexCount(){
        int value=0;
        if (graph!=null) value=graph.vertexSet().size();
        return value;
    }

    /**
     * Return the number of edges.
     * @return the number of edges.
     */
    public int getEdgeCount(){
        int value=0;
        if (graph!=null) value=graph.edgeSet().size();
        return value;
    }

    /**
     * @param routeType the routeType to set
     */
    public void setTypeRoute(RouteType routetype) {
        this.routeType = routetype;
        this.rgDelegator.setRouteType(routetype);
    }

    /**
     * @return the routeType
     */
    public RouteType getTypeRoute() {
        return routeType;
    }

    public Map<String, Double> getWaySpeeds() {
        return waySpeeds;
    }

    public void setWaySpeeds(Map<String, Double> waySpeeds) {
        this.waySpeeds = waySpeeds;
    }

    public void resetGraph() {
        graph=null;
    }

    public RoutingProfile getRoutingProfile() {
        return routingProfile;
    }
}
