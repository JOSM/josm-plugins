package org.openstreetmap.josm.plugins.validator.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * A visitor that aggregates all primitives it visits.
 * <p>
 * The primitives are sorted according to their type: first nodes, then ways.
 *
 * @author frsantos
 */
public class AgregatePrimitivesVisitor implements Visitor
{
    /** Aggregated data */
    Collection<OsmPrimitive> aggregatedData;

    /**
     * Constructor
     */
    public AgregatePrimitivesVisitor()
    {
        aggregatedData = new LinkedList<OsmPrimitive>();
    }

    /**
     * Visits a collection of primitives
     * @param data The collection of primitives
     * @return The aggregated primitives
     */
    public Collection<OsmPrimitive> visit(Collection<OsmPrimitive> data)
    {
        for (OsmPrimitive osm : data)
        {
            osm.visit(this);
        }

        return aggregatedData;
    }

    public void visit(Node n)
    {
        if(!aggregatedData.contains(n))
            aggregatedData.add(n);
    }

    public void visit(Way w)
    {
        if(!aggregatedData.contains(w))
        {
            aggregatedData.add(w);
            for (Node n : w.nodes)
                visit(n);
        }
    }

    public void visit(Relation r) {
        if (!aggregatedData.contains(r)) {
            aggregatedData.add(r);
            for (RelationMember m : r.members) {
                m.member.visit(this);
            }
        }
    }
}
