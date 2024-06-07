// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import java.util.Arrays;
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
 * Test class for {@link BoundaryFixer}
 */
class BoundaryFixerTest implements RelationFixerTest {

    private BoundaryFixer instance;

    @BeforeEach
    void setup() {
        this.instance = new BoundaryFixer();
        this.instance.setFixAction(new SortAndFixAction(new ChosenRelation()));
    }

    @Override
    public RelationFixer getInstance() {
        return instance;
    }

    @Override
    public Stream<Relation> getBadRelations() {
        final Relation wayRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("outer_misspelled", TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""))));
        final Relation nodeRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("admin_centre_misspelled", TestUtils.newNode("")));
        final Relation relationRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("subarea_misspelled", TestUtils.newRelation("")));
        Relation[] relations = {nodeRelation, wayRelation, relationRelation};
        for (Relation relation : relations) {
            final DataSet ds = new DataSet();
            ds.addPrimitiveRecursive(relation);
        }
        // Ensure that the boundary is enclosed
        final Node tNode = TestUtils.newNode("");
        wayRelation.getDataSet().addPrimitive(tNode);
        final Way way2 = TestUtils.newWay("", wayRelation.getMember(0).getWay().firstNode(),
                tNode, wayRelation.getMember(0).getWay().lastNode());
        wayRelation.getDataSet().addPrimitive(way2);
        wayRelation.addMember(new RelationMember("outer_misspelled", way2));
        return Arrays.stream(relations);
    }

    @Override
    public Stream<Relation> getGoodRelations() {
        final Relation wayRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("outer", TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""))));
        final Relation nodeRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("admin_centre", TestUtils.newNode("place=city")));
        final Relation relationRelation = TestUtils.newRelation("type=boundary boundary=administrative",
                new RelationMember("subarea", TestUtils.newRelation("")));
        Relation[] relations = {nodeRelation, wayRelation, relationRelation};
        for (Relation relation : relations) {
            final DataSet ds = new DataSet();
            ds.addPrimitiveRecursive(relation);
        }
        return Arrays.stream(relations);
    }
}
