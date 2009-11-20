package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.*;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;

/**
 * Check coastlines for errors
 *
 * @author frsantos
 * @author Teemu Koskinen
 */
public class Coastlines extends Test
{
    protected static int UNORDERED_COASTLINE = 901;
    protected static int REVERSED_COASTLINE = 902;
    protected static int UNCONNECTED_COASTLINE = 903;

    private List<Way> coastlines;

    private Area downloadedArea = null;

    /**
     * Constructor
     */
    public Coastlines()
    {
        super(tr("Coastlines."),
              tr("This test checks that coastlines are correct."));
    }

    @Override
    public void startTest(ProgressMonitor monitor)
    {
    	super.startTest(monitor);

        OsmDataLayer layer = Main.map.mapView.getEditLayer();

        if (layer != null)
            downloadedArea = layer.data.getDataSourceArea();

        coastlines = new LinkedList<Way>();
    }

    @Override
    public void endTest()
    {
        for (Way c1 : coastlines) {
            Node head = c1.firstNode();
            Node tail = c1.lastNode();

            if (head.equals(tail))
                continue;

            int headWays = 0;
            int tailWays = 0;
            boolean headReversed = false;
            boolean tailReversed = false;
            boolean headUnordered = false;
            boolean tailUnordered = false;
            Way next = null;
            Way prev = null;

            for (Way c2 : coastlines) {
                if (c1 == c2)
                    continue;

                if (c2.containsNode(head)) {
                    headWays++;
                    next = c2;

                    if (head.equals(c2.firstNode()))
                        headReversed = true;
                    else if (!head.equals(c2.lastNode()))
                        headUnordered = true;
                }

                if (c2.containsNode(tail)) {
                    tailWays++;
                    prev = c2;

                    if (tail.equals(c2.lastNode()))
                        tailReversed = true;
                    else if (!tail.equals(c2.firstNode()))
                        tailUnordered = true;
                }
            }


            List<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
            primitives.add(c1);

            if (headWays == 0 || tailWays == 0) {
                List<OsmPrimitive> highlight = new ArrayList<OsmPrimitive>();

                System.out.println("Unconnected coastline: " + c1.getId());
                if (headWays == 0 && (downloadedArea == null || downloadedArea.contains(head.getCoor()))) {
                    System.out.println("headways: " +headWays+ " node: " + head.toString());
                    highlight.add(head);
                }
                if (tailWays == 0 && (downloadedArea == null || downloadedArea.contains(tail.getCoor()))) {
                    System.out.println("tailways: " +tailWays+ " tail: " + tail.toString());
                    highlight.add(tail);
                }

                if (highlight.size() > 0)
                    errors.add(new TestError(this, Severity.ERROR, tr("Unconnected coastline"),
                                             UNCONNECTED_COASTLINE, primitives, highlight));
            }

            boolean unordered = false;
            boolean reversed = false;

            if (headWays == 1 && headReversed && tailWays == 1 && tailReversed)
                reversed = true;

            if (headWays > 1 || tailWays > 1)
                unordered = true;
            else if (headUnordered || tailUnordered)
                unordered = true;
            else if (reversed && next == prev)
                unordered = true;

            if (unordered) {
                List<OsmPrimitive> highlight = new ArrayList<OsmPrimitive>();

                System.out.println("Unordered coastline: " + c1.toString());
                if (headWays > 1 || headUnordered || reversed) {
                    System.out.println("head: " + head.toString());
                    highlight.add(head);
                }
                if (tailWays > 1 || tailUnordered || reversed) {
                    System.out.println("tail: " + tail.toString());
                    highlight.add(tail);
                }

                errors.add(new TestError(this, Severity.ERROR, tr("Unordered coastline"),
                                         UNORDERED_COASTLINE, primitives, highlight));
            }
            else if (reversed) {
                errors.add(new TestError(this, Severity.ERROR, tr("Reversed coastline"),
                                         REVERSED_COASTLINE, primitives));
            }
        }

        coastlines = null;
        downloadedArea = null;

        super.endTest();
    }

    @Override
    public void visit(Way way)
    {
        if (!way.isUsable())
            return;

        String natural = way.get("natural");
        if (natural == null || !natural.equals("coastline"))
            return;

        coastlines.add(way);
    }

    @Override
    public Command fixError(TestError testError) {
        if (isFixable(testError)) {
            Way way = (Way) testError.getPrimitives().iterator().next();
            Way newWay = new Way(way);

            List<Node> nodesCopy = newWay.getNodes();
            Collections.reverse(nodesCopy);
            newWay.setNodes(nodesCopy);

            return new ChangeCommand(way, newWay);
        }

        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getTester() instanceof Coastlines) {
            return (testError.getCode() == REVERSED_COASTLINE);
        }

        return false;
    }
}
