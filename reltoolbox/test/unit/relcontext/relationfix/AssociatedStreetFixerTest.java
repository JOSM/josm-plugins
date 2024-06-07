// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import relcontext.ChosenRelation;
import relcontext.actions.SortAndFixAction;

/**
 * Test class for {@link AssociatedStreetFixer}
 */
class AssociatedStreetFixerTest implements RelationFixerTest {
    private AssociatedStreetFixer instance;

    @BeforeEach
    void setup() {
        this.instance = new AssociatedStreetFixer();
        this.instance.setFixAction(new SortAndFixAction(new ChosenRelation()));
    }

    @Override
    public RelationFixer getInstance() {
        return this.instance;
    }

    @Override
    public Stream<Relation> getBadRelations() {
        final Relation relation = TestUtils.newRelation("type=associatedStreet name=FooBar",
                new RelationMember("street_misspelled", TestUtils.newWay("highway=residential name=Baz",
                        TestUtils.newNode(""), TestUtils.newNode(""))),
                new RelationMember("house_misspelled", TestUtils.newNode("")));
        final DataSet ds = new DataSet();
        ds.addPrimitiveRecursive(relation);
        return Stream.of(relation);
    }

    @Override
    public Stream<Relation> getGoodRelations() {
        final Relation relation = TestUtils.newRelation("type=associatedStreet name=FooBar",
                new RelationMember("street", TestUtils.newWay("highway=residential name=FooBar", TestUtils.newNode(""), TestUtils.newNode(""))),
                new RelationMember("house", TestUtils.newNode("")));
        final DataSet ds = new DataSet();
        ds.addPrimitiveRecursive(relation);
        return Stream.of(relation);
    }
}
