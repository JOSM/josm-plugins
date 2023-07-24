// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import relcontext.ChosenRelation;
import relcontext.actions.SortAndFixAction;

/**
 * Test class for {@link PublicTransportFixer}
 */
class PublicTransportFixerTest implements RelationFixerTest {
    private PublicTransportFixer instance;

    @BeforeEach
    void setup() {
        this.instance = new PublicTransportFixer();
        this.instance.setFixAction(new SortAndFixAction(new ChosenRelation()));
    }

    @Override
    public RelationFixer getInstance() {
        return this.instance;
    }

    @Override
    public Stream<Relation> getBadRelations() {
        final Relation badWay = TestUtils.newRelation("type=public_transport",
                new RelationMember("", TestUtils.newWay("public_transport=platform")));
        final Relation badNode = TestUtils.newRelation("type=public_transport",
                new RelationMember("", TestUtils.newNode("public_transport=stop_position")));
        final Relation[] relations = {badWay, badNode};
        for (Relation relation : relations) {
            final DataSet ds = new DataSet();
            ds.addPrimitiveRecursive(relation);
        }
        return Arrays.stream(relations);
    }

    @Override
    public Stream<Relation> getGoodRelations() {
        final Relation way = TestUtils.newRelation("type=public_transport",
                new RelationMember("platform", TestUtils.newWay("public_transport=platform")));
        final Relation node = TestUtils.newRelation("type=public_transport",
                new RelationMember("stop", TestUtils.newNode("public_transport=stop_position")));
        final Relation[] relations = {way, node};
        for (Relation relation : relations) {
            final DataSet ds = new DataSet();
            ds.addPrimitiveRecursive(relation);
        }
        return Arrays.stream(relations);
    }
}
