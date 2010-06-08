// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.JoinedWay;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.plugins.validator.OSMValidatorPlugin;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;

public class MultipolygonTest extends Test {

    protected static final int WRONG_MEMBER_TYPE = 1601;
    protected static final int WRONG_MEMBER_ROLE = 1602;
    protected static final int NON_CLOSED_WAY = 1603;
    protected static final int MISSING_OUTER_WAY = 1604;
    protected static final int INNER_WAY_OUTSIDE = 1605;
    protected static final int CROSSING_WAYS = 1606;
    protected static final int OUTER_STYLE_MISMATCH = 1607;
    protected static final int INNER_STYLE_MISMATCH = 1608;
    protected static final int NOT_CLOSED = 1609;
    protected static final int NO_STYLE = 1610;
    protected static final int NO_STYLE_POLYGON = 1611;

    private static ElemStyles.StyleSet styles;

    private final List<List<Node>> nonClosedWays = new ArrayList<List<Node>>();

    public MultipolygonTest() {
        super(tr("Multipolygon"),
                tr("This test checks if multipolygons are valid"));
    }

    @Override
    public void initialize(OSMValidatorPlugin plugin) throws Exception
    {
        styles = MapPaintStyles.getStyles().getStyleSet();
    }

    private List<List<Node>> joinWays(Collection<Way> ways) {
        List<List<Node>> result = new ArrayList<List<Node>>();
        List<Way> waysToJoin = new ArrayList<Way>();
        for (Way way: ways) {
            if (way.isClosed()) {
                result.add(way.getNodes());
            } else {
                waysToJoin.add(way);
            }
        }

        for (JoinedWay jw: Multipolygon.joinWays(waysToJoin)) {
            if (!jw.isClosed()) {
                nonClosedWays.add(jw.getNodes());
            } else {
                result.add(jw.getNodes());
            }
        }
        return result;
    }

    private GeneralPath createPath(List<Node> nodes) {
        GeneralPath result = new GeneralPath();
        result.moveTo((float)nodes.get(0).getCoor().lat(), (float)nodes.get(0).getCoor().lon());
        for (int i=1; i<nodes.size(); i++) {
            Node n = nodes.get(i);
            result.lineTo((float)n.getCoor().lat(), (float)n.getCoor().lon());
        }
        return result;
    }

    private List<GeneralPath> createPolygons(List<List<Node>> joinedWays) {
        List<GeneralPath> result = new ArrayList<GeneralPath>();
        for (List<Node> way: joinedWays) {
            result.add(createPath(way));
        }
        return result;
    }

    private Intersection getPolygonIntersection(GeneralPath outer, List<Node> inner) {
        boolean inside = false;
        boolean outside = false;

        for (Node n: inner) {
            boolean contains = outer.contains(n.getCoor().lat(), n.getCoor().lon());
            inside = inside | contains;
            outside = outside | !contains;
            if (inside & outside) {
                return Intersection.CROSSING;
            }
        }

        return inside?Intersection.INSIDE:Intersection.OUTSIDE;
    }

    @Override
    public void visit(Way w) {
        if (styles != null && !w.isClosed())
        {
            ElemStyle e = styles.getArea(w);
            if(e instanceof AreaElemStyle && !((AreaElemStyle)e).closed)
                errors.add( new TestError(this, Severity.WARNING, tr("Area style way is not closed"), NOT_CLOSED,  w));
        }
    }

    @Override
    public void visit(Relation r) {
        nonClosedWays.clear();
        if ("multipolygon".equals(r.get("type"))) {
            checkMembersAndRoles(r);

            Multipolygon polygon = new Multipolygon(Main.map.mapView);
            polygon.load(r);

            if (polygon.getOuterWays().isEmpty()) {
                errors.add( new TestError(this, Severity.WARNING, tr("No outer way for multipolygon"), MISSING_OUTER_WAY,  r));
            }

            for (RelationMember rm: r.getMembers()) {
                if (!rm.getMember().isUsable()) {
                    return; // Rest of checks is only for complete multipolygons
                }
            }

            List<List<Node>> innerWays = joinWays(polygon.getInnerWays()); // Side effect - sets nonClosedWays
            List<List<Node>> outerWays = joinWays(polygon.getOuterWays());

            if(styles != null) {
                ElemStyle wayStyle = styles.get(r);

                // If area style was not found for relation then use style of ways
                if(!(wayStyle instanceof AreaElemStyle)) {
                    errors.add( new TestError(this, Severity.OTHER, tr("No style in multipolygon relation"),
                    NO_STYLE_POLYGON, r));
                    for (Way w : polygon.getOuterWays()) {
                        wayStyle = styles.getArea(w);
                        if(wayStyle != null) {
                            break;
                        }
                    }
                }

                if (wayStyle instanceof AreaElemStyle) {
                    for (Way wInner : polygon.getInnerWays())
                    {
                        ElemStyle innerStyle = styles.get(wInner);
                        if(wayStyle != null && wayStyle.equals(innerStyle)) {
                            List<OsmPrimitive> l = new ArrayList<OsmPrimitive>();
                            l.add(r);
                            l.add(wInner);
                            errors.add( new TestError(this, Severity.WARNING, tr("Style for inner way equals multipolygon"),
                            INNER_STYLE_MISMATCH, l, Collections.singletonList(wInner)));
                        }
                    }
                    for (Way wOuter : polygon.getOuterWays())
                    {
                        ElemStyle outerStyle = styles.get(wOuter);
                        if(outerStyle instanceof AreaElemStyle && !wayStyle.equals(outerStyle)) {
                            List<OsmPrimitive> l = new ArrayList<OsmPrimitive>();
                            l.add(r);
                            l.add(wOuter);
                            errors.add( new TestError(this, Severity.WARNING, tr("Style for outer way mismatches"),
                            OUTER_STYLE_MISMATCH, l, Collections.singletonList(wOuter)));
                        }
                    }
                }
                else
                    errors.add( new TestError(this, Severity.OTHER, tr("No style for multipolygon"),
                    NO_STYLE, r));
            }

            if (!nonClosedWays.isEmpty()) {
                errors.add( new TestError(this, Severity.WARNING, tr("Multipolygon is not closed"), NON_CLOSED_WAY,  Collections.singletonList(r), nonClosedWays));
            }

            // For painting is used Polygon class which works with ints only. For validation we need more precision
            List<GeneralPath> outerPolygons = createPolygons(outerWays);
            for (List<Node> pdInner: innerWays) {
                boolean outside = true;
                boolean crossing = false;
                List<Node> outerWay = null;
                for (int i=0; i<outerWays.size(); i++) {
                    GeneralPath outer = outerPolygons.get(i);
                    Intersection intersection = getPolygonIntersection(outer, pdInner);
                    outside = outside & intersection == Intersection.OUTSIDE;
                    if (intersection == Intersection.CROSSING) {
                        crossing = true;
                        outerWay = outerWays.get(i);
                    }
                }
                if (outside || crossing) {
                    List<List<Node>> highlights = new ArrayList<List<Node>>();
                    highlights.add(pdInner);
                    if (outside) {
                        errors.add(new TestError(this, Severity.WARNING, tr("Multipolygon inner way is outside"), INNER_WAY_OUTSIDE, Collections.singletonList(r), highlights));
                    } else if (crossing) {
                        highlights.add(outerWay);
                        errors.add(new TestError(this, Severity.WARNING, tr("Intersection between multipolygon ways"), CROSSING_WAYS, Collections.singletonList(r), highlights));
                    }
                }
            }
        }
    }

    private void checkMembersAndRoles(Relation r) {
        for (RelationMember rm: r.getMembers()) {
            if (rm.isWay()) {
                if (!("inner".equals(rm.getRole()) || "outer".equals(rm.getRole()) || !rm.hasRole())) {
                    errors.add( new TestError(this, Severity.WARNING, tr("No useful role for multipolygon member"), WRONG_MEMBER_ROLE, rm.getMember()));
                }
            } else {
                errors.add( new TestError(this, Severity.WARNING, tr("Non-Way in multipolygon"), WRONG_MEMBER_TYPE, rm.getMember()));
            }
        }
    }


}
