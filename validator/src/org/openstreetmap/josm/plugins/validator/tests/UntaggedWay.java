package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;

/**
 * Checks for untagged ways
 *
 * @author frsantos
 */
public class UntaggedWay extends Test
{
    /** Empty way error */
    protected static final int EMPTY_WAY    = 301;
    /** Untagged way error */
    protected static final int UNTAGGED_WAY = 302;
    /** Unnamed way error */
    protected static final int UNNAMED_WAY  = 303;
    /** One node way error */
    protected static final int ONE_NODE_WAY = 304;
    /** Unnamed junction error */
    protected static final int UNNAMED_JUNCTION  = 305;

    private LinkedList<Way> multipolygonways;

    /** Ways that must have a name */
    public static final Set<String> NAMED_WAYS = new HashSet<String>();
    static
    {
        NAMED_WAYS.add( "motorway" );
        NAMED_WAYS.add( "trunk" );
        NAMED_WAYS.add( "primary" );
        NAMED_WAYS.add( "secondary" );
        NAMED_WAYS.add( "tertiary" );
        NAMED_WAYS.add( "residential" );
        NAMED_WAYS.add( "pedestrian" ); ;
    }

    /**
     * Constructor
     */
    public UntaggedWay()
    {
        super(tr("Untagged, empty and one node ways."),
              tr("This test checks for untagged, empty and one node ways."));
    }

    @Override
    public void visit(Way w)
    {
        if (!w.isUsable()) return;

        Map<String, String> tags = w.getKeys();
        if( tags.size() != 0 )
        {
            String highway = tags.get("highway");
            if(highway != null && NAMED_WAYS.contains(highway))
            {
                if( !tags.containsKey("name") && !tags.containsKey("ref") )
                {
                    boolean isRoundabout = false;
                    boolean hasName = false;
                    for( String key : w.keySet())
                    {
                        hasName = key.startsWith("name:") || key.endsWith("_name") || key.endsWith("_ref");
                        if( hasName )
                            break;
                        if(key.equals("junction"))
                        {
                            isRoundabout = w.get("junction").equals("roundabout");
                            break;
                        }
                    }

                    if( !hasName && !isRoundabout)
                        errors.add( new TestError(this, Severity.WARNING, tr("Unnamed ways"), UNNAMED_WAY, w) );
		    else if(isRoundabout)
                        errors.add( new TestError(this, Severity.WARNING, tr("Unnamed junction"), UNNAMED_JUNCTION, w) );
                }
            }
        }

        if(!w.isTagged() && !multipolygonways.contains(w))
        {
            errors.add( new TestError(this, Severity.WARNING, tr("Untagged ways"), UNTAGGED_WAY, w) );
        }

        if( w.getNodesCount() == 0 )
        {
            errors.add( new TestError(this, Severity.ERROR, tr("Empty ways"), EMPTY_WAY, w) );
        }
        else if( w.getNodesCount() == 1 )
        {
            errors.add( new TestError(this, Severity.ERROR, tr("One node ways"), ONE_NODE_WAY, w) );
        }

    }

    @Override
    public void startTest()
    {
        multipolygonways = new LinkedList<Way>();
        for (final Relation r : Main.main.getCurrentDataSet().relations)
        {
            if(r.isUsable() && "multipolygon".equals(r.get("type")))
            {
                for (RelationMember m : r.getMembers())
                {
                    if(m.getMember() != null && m.getMember() instanceof Way &&
                    m.getMember().isUsable() && !m.getMember().isTagged())
                        multipolygonways.add((Way)m.getMember());
                }
            }
        }
    }

    @Override
    public void endTest()
    {
        multipolygonways = null;
    }

    @Override
    public boolean isFixable(TestError testError)
    {
        if( testError.getTester() instanceof UntaggedWay )
        {
            return testError.getCode() == EMPTY_WAY
                || testError.getCode() == ONE_NODE_WAY;
        }

        return false;
    }

    @Override
    public Command fixError(TestError testError)
    {
        return DeleteCommand.delete(Main.map.mapView.getEditLayer(), testError.getPrimitives());
    }
}
