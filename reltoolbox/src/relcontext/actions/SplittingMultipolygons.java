package relcontext.actions;

import java.awt.geom.Area;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.*;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.*;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;

/**
 *
 * @author Zverik
 */
public class SplittingMultipolygons {
    private static final String PREF_MULTIPOLY = "reltoolbox.multipolygon.";

    public static boolean canProcess( Collection<Way> ways ) {
	List<Way> rings = new ArrayList<>();
	List<Way> arcs = new ArrayList<>();
	Area a = Main.main.getCurrentDataSet().getDataSourceArea();
	for( Way way : ways ) {
	    if( way.isDeleted() )
		return false;
	    for( Node n : way.getNodes() ) {
	        LatLon ll = n.getCoor();
    		if( n.isIncomplete() || (a != null && !a.contains(ll.getX(), ll.getY())) )
    		    return false;
	    }
	    if( way.isClosed() )
		rings.add(way);
	    else
		arcs.add(way);
	}
	
	// If there are more that one segment, check that they touch rings
	if( arcs.size() > 1 ) {
	    for( Way segment : arcs ) {
		boolean found = false;
		for( Way ring : rings )
		    if( ring.containsNode(segment.firstNode()) && ring.containsNode(segment.lastNode()) )
			found = true;
		if( !found )
		    return false;
	    }
	}

	if( rings.isEmpty() && arcs.isEmpty() )
	    return false;

	// check for non-containment of rings
	for( int i = 0; i < rings.size() - 1; i++ ) {
	    for( int j = i + 1; j < rings.size(); j++ ) {
		PolygonIntersection intersection = Geometry.polygonIntersection(rings.get(i).getNodes(), rings.get(j).getNodes());
		if( intersection == PolygonIntersection.FIRST_INSIDE_SECOND || intersection == PolygonIntersection.SECOND_INSIDE_FIRST )
		    return false;
	    }
	}

	return true;
    }
    
    public static List<Relation> process( Collection<Way> selectedWays ) {
//	System.out.println("---------------------------------------");
	List<Relation> result = new ArrayList<>();
	List<Way> rings = new ArrayList<>();
	List<Way> arcs = new ArrayList<>();
	for( Way way : selectedWays ) {
	    if( way.isClosed() )
		rings.add(way);
	    else
		arcs.add(way);
	}

	for( Way ring : rings ) {
	    List<Command> commands = new ArrayList<>();
	    Relation newRelation = SplittingMultipolygons.attachRingToNeighbours(ring, commands);
	    if( newRelation != null && !commands.isEmpty() ) {
		Main.main.undoRedo.add(commands.get(0));
		result.add(newRelation);
	    }
	}

	for( Way arc : arcs) {
	    List<Command> commands = new ArrayList<>();
	    Relation newRelation = SplittingMultipolygons.tryToCloseOneWay(arc, commands);
	    if( newRelation != null && !commands.isEmpty() ) {
		Main.main.undoRedo.add(commands.get(0));
		result.add(newRelation);
	    }
	}
	return result;
    }

    /**
     * Appends "append" to "base" so the closed polygon forms.
     */
    private static void closePolygon( List<Node> base, List<Node> append ) {
	if( append.get(0).equals(base.get(0)) && append.get(append.size() - 1).equals(base.get(base.size() - 1)) ) {
	    List<Node> ap2 = new ArrayList<>(append);
	    Collections.reverse(ap2);
	    append = ap2;
	}
	base.remove(base.size() - 1);
	base.addAll(append);
    }

    /**
     * Checks if a middle point between two nodes is inside a polygon. Useful to check if the way is inside.
     */
    private static boolean segmentInsidePolygon( Node n1, Node n2, List<Node> polygon ) {
	EastNorth en1 = n1.getEastNorth();
	EastNorth en2 = n2.getEastNorth();
	Node testNode = new Node(new EastNorth((en1.east() + en2.east()) / 2.0, (en1.north() + en2.north()) / 2.0));
	return Geometry.nodeInsidePolygon(testNode, polygon);
    }

    /**
     * Splits a way with regard to containing relations. This modifies the way and the relation, be prepared.
     * @param w The way.
     * @param n The node to split at.
     * @param commands List of commands to add way/relation changing to. If null, never mind.
     * @return Newly created ways. <b>Warning:</b> if commands is no not, newWays contains {@code w}.
     */
    public static List<Way> splitWay( Way w, Node n1, Node n2, List<Command> commands ) {
	List<Node> nodes = new ArrayList<>(w.getNodes());
	if( w.isClosed() )
	    nodes.remove(nodes.size() - 1);
	int index1 = nodes.indexOf(n1);
	int index2 = n2 == null ? -1 : nodes.indexOf(n2);
	if( index1 > index2 ) {
	    int tmp = index1;
	    index1 = index2;
	    index2 = tmp;
	}
	// right now index2 >= index1
	if( index2 < 1 || index1 >= w.getNodesCount() - 1 || index2 >= w.getNodesCount() )
	    return Collections.emptyList();
	if( w.isClosed() && (index1 < 0 || index1 == index2 || index1 + w.getNodesCount() == index2) )
	    return Collections.emptyList();
	
	// todo: download parent relations!

	// make a list of segments
	List<List<Node>> chunks = new ArrayList<>(2);
	List<Node> chunk = new ArrayList<>();
	for( int i = 0; i < nodes.size(); i++ ) {
	    chunk.add(nodes.get(i));
	    if( (w.isClosed() || chunk.size() > 1) && (i == index1 || i == index2) ) {
		chunks.add(chunk);
		chunk = new ArrayList<>();
		chunk.add(nodes.get(i));
	    }
	}
	chunks.add(chunk);

	// for closed way ignore the way boundary
	if( w.isClosed() ) {
	    chunks.get(chunks.size() - 1).addAll(chunks.get(0));
	    chunks.remove(0);
	} else if( chunks.get(chunks.size() - 1).size() < 2 )
	    chunks.remove(chunks.size() - 1);

	// todo remove debug: show chunks array contents
	/*for( List<Node> c1 : chunks ) {
	for( Node cn1 : c1 )
	System.out.print(cn1.getId() + ",");
	System.out.println();
	}*/

	// build a map of referencing relations
	Map<Relation, Integer> references = new HashMap<>();
	List<Command> relationCommands = new ArrayList<>();
	for( OsmPrimitive p : w.getReferrers() ) {
	    if( p instanceof Relation ) {
		Relation rel = commands == null ? (Relation)p : new Relation((Relation)p);
		if( commands != null )
		    relationCommands.add(new ChangeCommand((Relation)p, rel));
		for( int i = 0; i < rel.getMembersCount(); i++ )
		    if( rel.getMember(i).getMember().equals(w) )
			references.put(rel, Integer.valueOf(i));
	    }
	}

	// build ways
	List<Way> result = new ArrayList<>();
	Way updatedWay = commands == null ? w : new Way(w);
	updatedWay.setNodes(chunks.get(0));
	if( commands != null ) {
	    commands.add(new ChangeCommand(w, updatedWay));
	    result.add(updatedWay);
	}

	for( int i = 1; i < chunks.size(); i++ ) {
	    List<Node> achunk = chunks.get(i);
	    Way newWay = new Way();
	    newWay.setKeys(w.getKeys());
	    result.add(newWay);
	    for( Relation rel : references.keySet() ) {
		int relIndex = references.get(rel);
		rel.addMember(relIndex + 1, new RelationMember(rel.getMember(relIndex).getRole(), newWay));
	    }
	    newWay.setNodes(achunk);
	    if( commands != null )
		commands.add(new AddCommand(newWay));
	}
	if( commands != null )
	    commands.addAll(relationCommands);
	return result;
    }

    public static List<Way> splitWay( Way w, Node n1, Node n2 ) {
	return splitWay(w, n1, n2, null);
    }

    /**
     * Find a way the tips of a segment, ensure it's in a multipolygon and try to close the relation.
     */
    public static Relation tryToCloseOneWay( Way segment, List<Command> resultingCommands ) {
	if( segment.isClosed() || segment.isIncomplete() )
	    return null;

	List<Way> ways = intersection(
		OsmPrimitive.getFilteredList(segment.firstNode().getReferrers(), Way.class),
		OsmPrimitive.getFilteredList(segment.lastNode().getReferrers(), Way.class));
	ways.remove(segment);
	for( Iterator<Way> iter = ways.iterator(); iter.hasNext(); ) {
	    boolean save = false;
	    for( OsmPrimitive ref : iter.next().getReferrers() )
		if( ref instanceof Relation && ((Relation)ref).isMultipolygon() && !ref.isDeleted() )
		    save = true;
	    if( !save )
		iter.remove();
	}
	if( ways.isEmpty() )
	    return null; // well...
	Way target = ways.get(0);

	// time to create a new multipolygon relation and a command stack
	List<Command> commands = new ArrayList<>();
	Relation newRelation = new Relation();
	newRelation.put("type", "multipolygon");
	newRelation.addMember(new RelationMember("outer", segment));
	Collection<String> linearTags = Main.pref.getCollection(PREF_MULTIPOLY + "lineartags", CreateMultipolygonAction.DEFAULT_LINEAR_TAGS);
	Way segmentCopy = new Way(segment);
	boolean changed = false;
	for( String key : segmentCopy.keySet() ) {
	    if( !linearTags.contains(key) ) {
		newRelation.put(key, segmentCopy.get(key));
		segmentCopy.remove(key);
		changed = true;
	    }
	}
	if( changed )
	    commands.add(new ChangeCommand(segment, segmentCopy));

	// now split the way, at last
	List<Way> newWays = new ArrayList<>(splitWay(target, segment.firstNode(), segment.lastNode(), commands));

	Way addingWay = null;
	if( target.isClosed() ) {
	    Way utarget = newWays.get(1);
	    Way alternate = newWays.get(0);
	    List<Node> testRing = new ArrayList<>(segment.getNodes());
	    closePolygon(testRing, utarget.getNodes());
	    addingWay = segmentInsidePolygon(alternate.getNode(0), alternate.getNode(1), testRing) ? alternate : utarget;
	} else {
	    for( Way w : newWays ) {
		if( (w.firstNode().equals(segment.firstNode()) && w.lastNode().equals(segment.lastNode()))
			|| (w.firstNode().equals(segment.lastNode()) && w.lastNode().equals(segment.firstNode())) ) {
		    addingWay = w;
		    break;
		}
	    }
	}
	newRelation.addMember(new RelationMember("outer", addingWay.getUniqueId() == target.getUniqueId() ? target : addingWay));
	commands.add(new AddCommand(newRelation));
	resultingCommands.add(new SequenceCommand(tr("Complete multipolygon for way {0}",
		DefaultNameFormatter.getInstance().format(segment)), commands));
	return newRelation;
    }

    /**
     * Returns all elements from {@code list1} that are in {@code list2}.
     */
    private static <T> List<T> intersection( Collection<T> list1, Collection<T> list2 ) {
	List<T> result = new ArrayList<>();
	for( T item : list1 )
	    if( list2.contains(item) )
		result.add(item);
	return result;
    }
    
    /**
     * Make a multipolygon out of the ring, but split it to attach to neighboring multipolygons.
     */
    public static Relation attachRingToNeighbours( Way ring, List<Command> resultingCommands ) {
	if( !ring.isClosed() || ring.isIncomplete() )
	    return null;
	Map<Way, Boolean> touchingWays = new HashMap<>();
	for( Node n : ring.getNodes() ) {
	    for( OsmPrimitive p : n.getReferrers() ) {
		if( p instanceof Way && !p.equals(ring) ) {
		    for( OsmPrimitive r : p.getReferrers() ) {
			if( r instanceof Relation && ((Relation)r).hasKey("type") && ((Relation)r).get("type").equals("multipolygon") ) {
			    if( touchingWays.containsKey((Way)p) )
				touchingWays.put((Way)p, Boolean.TRUE);
			    else
				touchingWays.put((Way)p, Boolean.FALSE);
			    break;
			}
		    }
		}
	    }
	}
	
	List<TheRing> otherWays = new ArrayList<>();
	for( Way w : touchingWays.keySet() )
	    if( touchingWays.get(w) ) {
		otherWays.add(new TheRing(w));
//		System.out.println("Touching ring: " + otherWays.get(otherWays.size()-1));
	    }
	
//	for( Iterator<Way> keys = touchingWays.keySet().iterator(); keys.hasNext(); ) {
//	    if( !touchingWays.get(keys.next()) )
//		keys.remove();
//	}
	
	// now touchingWays has only ways that touch the ring twice
	List<Command> commands = new ArrayList<>();
	TheRing theRing = new TheRing(ring); // this is actually useful
	
	for( TheRing otherRing : otherWays )
	    theRing.collide(otherRing);
	
	theRing.putSourceWayFirst();
	for( TheRing otherRing : otherWays )
	    otherRing.putSourceWayFirst();
	
	Map<Relation, Relation> relationCache = new HashMap<>();
	for( TheRing otherRing : otherWays )
	    commands.addAll(otherRing.getCommands(false, relationCache));
	commands.addAll(theRing.getCommands(relationCache));
	TheRing.updateCommandsWithRelations(commands, relationCache);
	resultingCommands.add(new SequenceCommand(tr("Complete multipolygon for way {0}",
		DefaultNameFormatter.getInstance().format(ring)), commands));
	return theRing.getRelation();
    }
}
