// License: GPL. For details, see LICENSE file.
package relcontext.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

import junit.framework.AssertionFailedError;
import mockit.Mock;
import mockit.MockUp;
import relcontext.ChosenRelation;

/**
 * Test class for {@link ReconstructPolygonAction}
 */
@Projection
@Main
class ReconstructPolygonActionTest {
    private DataSet ds;
    private ChosenRelation chosenRelation;
    private Way way1;
    private Way way2;
    private Way way3;
    private Relation relation;
    private ReconstructPolygonAction action;

    @BeforeEach
    void setup() {
        DeleteCommand.setDeletionCallback(new DeleteCommand.DeletionCallback() {
            @Override
            public boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives, Collection<? extends OsmPrimitive> ignore) {
                return true;
            }

            @Override
            public boolean confirmRelationDeletion(Collection<Relation> relations) {
                return true;
            }

            @Override
            public boolean confirmDeletionFromRelation(Collection<RelationToChildReference> references) {
                return true;
            }
        });
        ds = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "ReconstructPolygonActionTest#testNonRegression23170", null));
        chosenRelation = new ChosenRelation();
        way1 = TestUtils.newWay("", TestUtils.newNode("name=1"), TestUtils.newNode("name=2"));
        way2 = TestUtils.newWay("", way1.lastNode(), TestUtils.newNode("name=3"));
        way3 = TestUtils.newWay("", way2.lastNode(), way1.firstNode());
        relation = TestUtils.newRelation("type=multipolygon landuse=orchard",
                new RelationMember("outer", way1),
                new RelationMember("outer", way2),
                new RelationMember("outer", way3));
        ds.addPrimitiveRecursive(relation);
        chosenRelation.set(relation);
        action = new ReconstructPolygonAction(chosenRelation);
    }

    @AfterEach
    void tearDown() {
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
    }

    /**
     * Check that the reconstruct code works on a minimal level
     */
    @Test
    void testPolygonReconstructSimple() {
        assertDoesNotThrow(() -> action.actionPerformed(null));
        assertTrue(relation.isDeleted());
        assertEquals(2, Stream.of(way1, way2, way3).filter(Way::isDeleted).count());
        final Way keptWay = Stream.of(way1, way2, way3).filter(w -> !w.isDeleted()).findFirst().orElseThrow(AssertionFailedError::new);
        assertTrue(keptWay.isClosed());
        assertEquals(4, keptWay.getNodesCount());
        assertEquals(1, keptWay.getNodes().stream().distinct().filter(n -> "1".equals(n.get("name"))).count());
        assertEquals(1, keptWay.getNodes().stream().distinct().filter(n -> "2".equals(n.get("name"))).count());
        assertEquals(1, keptWay.getNodes().stream().distinct().filter(n -> "3".equals(n.get("name"))).count());
        assertEmpty(DatasetConsistencyTest.runTests(ds));
    }

    /**
     * Ensure that we bail if a way in the relation to be simplified will be deleted from another relation.
     */
    @Test
    void testPolygonReconstructComplex() {
        final AtomicReference<String> shownMessage = new AtomicReference<>();
        new MockUp<JOptionPane>() {
            @Mock
            void showMessageDialog(Component parentComponent,
                                   Object message, String title, int messageType) {
                shownMessage.set((String) message);
            }
        };
        final Relation otherRelation = TestUtils.newRelation("type=multipolygon landuse=retail",
                new RelationMember("outer", way1),
                new RelationMember("outer", way2),
                new RelationMember("outer", way3),
                new RelationMember("label", TestUtils.newNode("name=4")));
        ds.addPrimitiveRecursive(otherRelation);
        assertDoesNotThrow(() -> GuiHelper.runInEDTAndWait(() -> action.actionPerformed(null)));
        assertEmpty(DatasetConsistencyTest.runTests(ds));
        assertEquals(tr("Multipolygon must consist only of ways with one referring relation"), shownMessage.get());
    }

    /**
     * Check that a string is empty
     * @param string The string to check. Will be printed to log if not empty.
     */
    private static void assertEmpty(String string) {
        assertTrue(string.isEmpty(), string);
    }
}
