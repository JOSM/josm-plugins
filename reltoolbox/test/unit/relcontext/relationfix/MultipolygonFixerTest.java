// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import relcontext.ChosenRelation;
import relcontext.actions.SortAndFixAction;

/**
 * Test class for {@link MultipolygonFixer}
 */
class MultipolygonFixerTest implements RelationFixerTest {
    private MultipolygonFixer instance;

    @BeforeEach
    void setup() {
        this.instance = new MultipolygonFixer();
        this.instance.setFixAction(new SortAndFixAction(new ChosenRelation()));
    }

    @Override
    public RelationFixer getInstance() {
        return this.instance;
    }

    @Override
    public Stream<Relation> getBadRelations() {
        final Relation wayRelation = TestUtils.newRelation("type=multipolygon",
                new RelationMember("outer_misspelled", TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""))));
        final DataSet ds = new DataSet();
        ds.addPrimitiveRecursive(wayRelation);
        // Ensure that the boundary is enclosed
        final Node tNode = TestUtils.newNode("");
        wayRelation.getDataSet().addPrimitive(tNode);
        final Way way2 = TestUtils.newWay("", wayRelation.getMember(0).getWay().firstNode(),
                tNode, wayRelation.getMember(0).getWay().lastNode());
        wayRelation.getDataSet().addPrimitive(way2);
        wayRelation.addMember(new RelationMember("outer_misspelled", way2));
        return Stream.of(wayRelation);
    }

    @Override
    public Stream<Relation> getGoodRelations() {
        final Relation wayRelation = TestUtils.newRelation("type=multipolygon",
                new RelationMember("outer", TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""))));
        final DataSet ds = new DataSet();
        ds.addPrimitiveRecursive(wayRelation);
        return Stream.of(wayRelation);
    }
}
