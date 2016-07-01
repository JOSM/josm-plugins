package org.openstreetmap.josm.plugins.pt_assistant.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.pt_assistant.AbstractTest;

/**
 * Tests if the representation of a route relation is created correctly in the
 * pt_assistant plugin
 * 
 * @author darya
 *
 */
public class RouteRepresentationTest extends AbstractTest {
    
    @Test
    public void correctRouteTest() {
        
        /*-
         * Create a [correct] route which has:
         * stop1 (stop_position)
         * way1 (Way)
         * stop2(platform, Way)
         * way2 (Way)
         * stop3 (stop_area)
         * way3 (Relation that consists of Ways only)
         * stop4 (stop_position)
         * stop4 (platform, Node)
         * way4 (Way)
         * stop5 (platform_exit_only, Relation)
         * 
         */
        
        ArrayList<RelationMember> members = new ArrayList<>();
        
        // Create stops:
        Node n1 = new Node(); 
        n1.put("name", "Stop1");
        RelationMember rm1 = new RelationMember("stop", n1);
        members.add(rm1);
        Way w1 = new Way();
        w1.put("name", "Stop2");
        w1.put("highway", "platform");
        RelationMember rm2 = new RelationMember("platform", w1);
        members.add(rm2);
        Relation r1 = new Relation();
        r1.put("name", "Stop3");
        RelationMember rm3 = new RelationMember("stop_area", r1);
        members.add(rm3);
        Node n2 = new Node();
        n2.put("name", "Stop4");
        RelationMember rm4 = new RelationMember("stop", n2);
        members.add(rm4);
        Node n3 = new Node();
        n3.put("name", "Stop4");
        RelationMember rm5 = new RelationMember("platform", n3);
        members.add(rm5);
        Relation r2 = new Relation();
        r2.put("name", "Stop5");
        r2.put("highway", "platform_exit_only");
        RelationMember rm6 = new RelationMember("platform_exit_only", r2);
        members.add(rm6);
                
        // Create ways:
        Way w2 = new Way();
        RelationMember rm7 = new RelationMember("", w2);
        members.add(rm7);
        Way w3 = new Way();
        RelationMember rm8 = new RelationMember("", w3);
        members.add(rm8);
        Relation r3 = new Relation(); // nested relation
        Way w4 = new Way();
        Way w5 = new Way();
        Way w6 = new Way();
        r3.addMember(new RelationMember("", w4));
        r3.addMember(new RelationMember("", w5));
        r3.addMember(new RelationMember("", w6));
        RelationMember rm9 = new RelationMember("", r3);
        members.add(rm9);
        Way w7 = new Way();
        RelationMember rm10 = new RelationMember("", w7);
        members.add(rm10);
        

        
        Relation route = new Relation();
        route.setMembers(members);
        
        PTRouteDataManager manager = new PTRouteDataManager(route);
    
        assertEquals(manager.getPTStopCount(), 5);
        assertEquals(manager.getPTWayCount(), 4);
        
    }
    
    @Test
    public void nestedRelationTest() {
        
        // Same as above, but the nested Relation has a Node (only ways are allowed)
        
        ArrayList<RelationMember> members = new ArrayList<>();
        
        // Create stops:
        Node n1 = new Node(); 
        n1.put("name", "Stop1");
        RelationMember rm1 = new RelationMember("stop", n1);
        members.add(rm1);
        Way w1 = new Way();
        w1.put("name", "Stop2");
        w1.put("highway", "platform");
        RelationMember rm2 = new RelationMember("platform", w1);
        members.add(rm2);
        Relation r1 = new Relation();
        r1.put("name", "Stop3");
        RelationMember rm3 = new RelationMember("stop_area", r1);
        members.add(rm3);
        Node n2 = new Node();
        n2.put("name", "Stop4");
        RelationMember rm4 = new RelationMember("stop", n2);
        members.add(rm4);
        Node n3 = new Node();
        n3.put("name", "Stop4");
        RelationMember rm5 = new RelationMember("platform", n3);
        members.add(rm5);
        Relation r2 = new Relation();
        r2.put("name", "Stop5");
        r2.put("highway", "platform_exit_only");
        RelationMember rm6 = new RelationMember("platform_exit_only", r2);
        members.add(rm6);
                
        // Create ways:
        Way w2 = new Way();
        RelationMember rm7 = new RelationMember("", w2);
        members.add(rm7);
        Way w3 = new Way();
        RelationMember rm8 = new RelationMember("", w3);
        members.add(rm8);
        Relation r3 = new Relation(); // nested relation
        Way w4 = new Way();
        Node wrongNode = new Node(); // CHANGED COMPARED TO PREVIOUS TEST
        Way w6 = new Way();
        r3.addMember(new RelationMember("", w4));
        r3.addMember(new RelationMember("platform", wrongNode));
        r3.addMember(new RelationMember("", w6));
        RelationMember rm9 = new RelationMember("", r3);
        members.add(rm9);
        Way w7 = new Way();
        RelationMember rm10 = new RelationMember("", w7);
        members.add(rm10);
        
        
        Relation route = new Relation();
        route.setMembers(members);
        
        boolean thrown = false;
        String message = "";
        try {
            /*PTRouteDataManager manager =*/ new PTRouteDataManager(route);
        } catch(IllegalArgumentException e) {
            thrown = true;
            message = e.getMessage();
        }
        
        assertTrue(thrown);
        assertEquals(message, "A route relation member of OsmPrimitiveType.RELATION can only have ways as members");
        
    }
    
    
    @Test
    public void multipleStopElementTest() {
        
        // Same as correctRouteTest(), but 
        
        ArrayList<RelationMember> members = new ArrayList<>();
        
        // Create stops:
        Node n1 = new Node(); 
        n1.put("name", "Stop1");
        RelationMember rm1 = new RelationMember("stop", n1);
        members.add(rm1);
        Way w1 = new Way();
        w1.put("name", "Stop2");
        w1.put("highway", "platform");
        RelationMember rm2 = new RelationMember("platform", w1);
        members.add(rm2);
        Relation r1 = new Relation();
        r1.put("name", "Stop3");
        RelationMember rm3 = new RelationMember("stop_area", r1);
        members.add(rm3);
        Node n2 = new Node();
        n2.put("name", "Stop4");
        RelationMember rm4 = new RelationMember("stop", n2);
        members.add(rm4);
        Node n3 = new Node();
        n3.put("name", "Stop4");
        RelationMember rm5 = new RelationMember("platform", n3);
        members.add(rm5);
        Relation r2 = new Relation();
        r2.put("name", "Stop5");
        r2.put("highway", "platform_exit_only");
        RelationMember rm6 = new RelationMember("platform_exit_only", r2);
        members.add(rm6);
                
        // Create ways:
        Way w2 = new Way();
        RelationMember rm7 = new RelationMember("", w2);
        members.add(rm7);
        Way w3 = new Way();
        RelationMember rm8 = new RelationMember("", w3);
        members.add(rm8);
        Relation r3 = new Relation(); // nested relation
        Way w4 = new Way();
        Way w5 = new Way();
        Way w6 = new Way();
        r3.addMember(new RelationMember("", w4));
        r3.addMember(new RelationMember("", w5));
        r3.addMember(new RelationMember("", w6));
        RelationMember rm9 = new RelationMember("", r3);
        members.add(rm9);
        Way w7 = new Way();
        RelationMember rm10 = new RelationMember("", w7);
        members.add(rm10);
        

        
        Relation route = new Relation();
        route.setMembers(members);
        
        PTRouteDataManager manager = new PTRouteDataManager(route);
    
        assertEquals(manager.getPTStopCount(), 5);
        assertEquals(manager.getPTWayCount(), 4);
        
    }
    

}
