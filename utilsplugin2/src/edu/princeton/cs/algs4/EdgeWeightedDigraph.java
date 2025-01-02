// License: GPL v3 or later courtesy of author Kevin Wayne
package edu.princeton.cs.algs4;

/*************************************************************************
 *  Compilation:  javac EdgeWeightedDigraph.java
 *  Execution:    java EdgeWeightedDigraph V E
 *  Dependencies: Bag.java DirectedEdge.java
 *
 *  An edge-weighted digraph, implemented using adjacency lists.
 *
 *************************************************************************/

/**
 *  The <tt>EdgeWeightedDigraph</tt> class represents an directed graph of vertices
 *  named 0 through V-1, where each edge has a real-valued weight.
 *  It supports the following operations: add an edge to the graph,
 *  iterate over all of edges leaving a vertex.
 *  Parallel edges and self-loops are permitted.
 *  <p>
 *  For additional documentation, see <a href="http://algs4.cs.princeton.edu/44sp">Section 4.4</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class EdgeWeightedDigraph {
    private final int V;
    private int E;
    private Bag<DirectedEdge>[] adj;

    /**
     * Create an empty edge-weighted digraph with V vertices.
     */
    @SuppressWarnings("unchecked")
    public EdgeWeightedDigraph(int V) {
        if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = V;
        this.E = 0;
        adj = new Bag[V];
        for (int v = 0; v < V; v++)
            adj[v] = new Bag<>();
    }

    /**
     * Create a edge-weighted digraph with V vertices and E edges.
     */
    public EdgeWeightedDigraph(int V, int E) {
        this(V);
        if (E < 0) throw new RuntimeException("Number of edges must be nonnegative");
        for (int i = 0; i < E; i++) {
            int v = (int) (Math.random() * V);
            int w = (int) (Math.random() * V);
            double weight = Math.round(100 * Math.random()) / 100.0;
            DirectedEdge e = new DirectedEdge(v, w, weight);
            addEdge(e);
        }
    }

    /**
     * Create an edge-weighted digraph from input stream.
     */
    //    public EdgeWeightedDigraph(In in) {
    //        this(in.readInt());
    //        int E = in.readInt();
    //        for (int i = 0; i < E; i++) {
    //            int v = in.readInt();
    //            int w = in.readInt();
    //            double weight = in.readDouble();
    //            addEdge(new DirectedEdge(v, w, weight));
    //        }
    //    }

    /**
     * Copy constructor.
     */
    public EdgeWeightedDigraph(EdgeWeightedDigraph G) {
        this(G.V());
        this.E = G.E();
        for (int v = 0; v < G.V(); v++) {
            // reverse so that adjacency list is in same order as original
            Stack<DirectedEdge> reverse = new Stack<>();
            for (DirectedEdge e : G.adj[v]) {
                reverse.push(e);
            }
            for (DirectedEdge e : reverse) {
                adj[v].add(e);
            }
        }
    }

    /**
     * Return the number of vertices in this digraph.
     */
    public int V() {
        return V;
    }

    /**
     * Return the number of edges in this digraph.
     */
    public int E() {
        return E;
    }

    /**
     * Add the edge e to this digraph.
     */
    public void addEdge(DirectedEdge ed) {
        int v = ed.from();
        adj[v].add(ed);
        E++;
    }

    /**
     * Return the edges leaving vertex ve as an Iterable.
     * To iterate over the edges leaving vertex ve, use foreach notation:
     * <tt>for (DirectedEdge e : graph.adj(ve))</tt>.
     */
    public Iterable<DirectedEdge> adj(int ve) {
        return adj[ve];
    }

    /**
     * Return all edges in this graph as an Iterable.
     * To iterate over the edges, use foreach notation:
     * <tt>for (DirectedEdge e : graph.edges())</tt>.
     */
    public Iterable<DirectedEdge> edges() {
        Bag<DirectedEdge> list = new Bag<>();
        for (int ve = 0; ve < V; ve++) {
            for (DirectedEdge ed : adj(ve)) {
                list.add(ed);
            }
        }
        return list;
    }

    /**
     * Return number of edges leaving ve.
     */
    public int outdegree(int ve) {
        return adj[ve].size();
    }

    /**
     * Return a string representation of this graph.
     */
    @Override
    public String toString() {
        String NEWLINE = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append(V + " " + E + NEWLINE);
        for (int v = 0; v < V; v++) {
            s.append(v + ": ");
            for (DirectedEdge e : adj[v]) {
                s.append(e + "  ");
            }
            s.append(NEWLINE);
        }
        return s.toString();
    }
}
