// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * A class for checking common issues with the {@link RelationFixer} subclasses
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
interface RelationFixerTest {
	/**
	 * Get the instance to use for checking
	 * @return The fixer instance
	 */
	RelationFixer getInstance();

	/**
	 * A relation that should be fixed by the fixer
	 * @return The bad relation
	 */
	Stream<Relation> getBadRelations();

	/**
	 * A relation that should not be fixed by the fixer
	 * @return The good relation
	 */
	Stream<Relation> getGoodRelations();

	default Stream<Relation> getRelations() {
		return Stream.concat(getBadRelations(), getGoodRelations());
	}

	@ParameterizedTest
	@MethodSource("getRelations")
	default void testIsFixerApplicable(Relation relation) {
		final RelationFixer fixer = getInstance();
		assertTrue(fixer.isFixerApplicable(relation));
	}

	@Test
	default void testIsFixerApplicableEmptyRelation() {
		assertFalse(getInstance().isFixerApplicable(new Relation()));
	}

	@ParameterizedTest
	@MethodSource("getGoodRelations")
	default void testGoodRelationMatches(Relation goodRelation) {
		final RelationFixer fixer = getInstance();
		assertTrue(fixer.isFixerApplicable(goodRelation));
		assertFalse(fixer.isFixerApplicable(new Relation()));
	}

	@ParameterizedTest
	@MethodSource("getGoodRelations")
	default void testIsRelationGoodGoodRelation(Relation goodRelation) {
		assertTrue(getInstance().isRelationGood(goodRelation));
	}

	@ParameterizedTest
	@MethodSource("getBadRelations")
	default void testIsRelationGoodBadRelation(Relation badRelation) {
		assertFalse(getInstance().isRelationGood(badRelation));
	}

	@MethodSource("getBadRelations")
	@ParameterizedTest
	@Projection
	default void testFixBadRelation(Relation badRelation) {
		final DataSet ds = badRelation.getDataSet();
		final RelationFixer fixer = getInstance();
		final Command command = fixer.fixRelation(badRelation);
		assertNotNull(command);
		assertDoesNotThrow(command::executeCommand);
		final Relation relation = ds.getRelations().stream().filter(fixer::isFixerApplicable).findFirst().orElseThrow(AssertionError::new);
		assertAll(relation.getMemberPrimitives().stream()
				.map(member -> () -> assertSame(ds, member.getDataSet(), member + " does not have the same dataset as " + relation)));
	}

	@Projection
	@ParameterizedTest
	@MethodSource("getGoodRelations")
	default void testGoodRelationNotFixed(Relation goodRelation) {
		assertTrue(getInstance().isRelationGood(goodRelation));
		final DataSet ds = goodRelation.getDataSet();
		final RelationFixer fixer = getInstance();
		final Command command = fixer.fixRelation(goodRelation);
		assertNull(command);
	}

	@Projection
	@ParameterizedTest
	@MethodSource("getGoodRelations")
	default void testAddBadMember(Relation goodRelation) {
		// regression test for #23715
		final DataSet ds = goodRelation.getDataSet();
		assertTrue(getInstance().isRelationGood(goodRelation));
		Node n1 = new Node(new LatLon(2.0, 2.0));
		Node n2 = new Node(new LatLon(2.0, 3.0));
		Way w1 = TestUtils.newWay("", n1, n2);
		ds.addPrimitiveRecursive(w1);
		goodRelation.addMember(new RelationMember("", w1));
		final RelationFixer fixer = getInstance();
		final Command command = fixer.fixRelation(goodRelation);
		assertNull(command);
	}

}
