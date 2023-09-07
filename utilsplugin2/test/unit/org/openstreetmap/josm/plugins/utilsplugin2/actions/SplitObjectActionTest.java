package org.openstreetmap.josm.plugins.utilsplugin2.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Utils;
import org.opentest4j.AssertionFailedError;

/**
 * Test class for {@link SplitObjectAction}
 */
@Main
@Projection
class SplitObjectActionTest {
    private SplitObjectAction action;
    private DataSet dataSet;

    @AfterEach
    void tearDown() {
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
    }

    @BeforeEach
    void setup() {
        action = new SplitObjectAction();
        dataSet = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(dataSet, "SplitObjectActionTest", null));
    }

    @Test
    void testNonRegression23159() {
        final Way inner = TestUtils.newWay("", new Node(new LatLon(0.25, 0.25)),
                new Node(new LatLon(0.25, 0.75)), new Node(new LatLon(0.75, 0.75)),
                new Node(new LatLon(0.75, 0.25)));
        inner.setOsmId(1, 1);
        final Way outer = TestUtils.newWay("", new Node(new LatLon(0, 0)),
                new Node(new LatLon(0, 1)), new Node(new LatLon(1, 1)),
                new Node(new LatLon(1, 0)));
        outer.setOsmId(2, 1);
        final Relation relation = TestUtils.newRelation("type=multipolygon landuse=orchard",
                new RelationMember("inner", inner), new RelationMember("outer", outer));
        relation.setOsmId(3, 1);
        this.dataSet.addPrimitiveRecursive(relation);
        inner.addNode(inner.firstNode());
        outer.addNode(outer.firstNode());
        final Way newWay = TestUtils.newWay("", outer.firstNode(), new Node(new LatLon(-0.5, -0.5)), outer.getNode(1));
        this.dataSet.addPrimitive(newWay.getNode(1));
        this.dataSet.addPrimitive(newWay);

        this.dataSet.setSelected(outer.firstNode(), outer.getNode(1));
        new SplitWayAction().actionPerformed(null);

        final Way innerSplitWay = this.dataSet.getSelectedWays().stream().filter(w -> w.getNodesCount() == 2)
                .findFirst().orElseThrow(() -> new AssertionFailedError("Could not find correct split way"));
        this.dataSet.setSelected(newWay, this.dataSet.getSelectedWays().stream().filter(w -> w.getNodesCount() == 4)
                .findFirst().orElseThrow(() -> new AssertionFailedError("Could not find correct split way")));

        DeleteCommand.setDeletionCallback(new AlwaysDeleteCallback());

        Config.getPref().putBoolean("message.combine_tags", false);
        Config.getPref().putInt("message.combine_tags.value", JOptionPane.YES_OPTION);
        new CombineWayAction().actionPerformed(null);
        GuiHelper.runInEDTAndWait(() -> { /* sync edt */ });

        assertAll(relation.getMemberPrimitives().stream()
                .map(prim -> () -> assertFalse(prim.isDeleted(), prim.toString())));

        this.dataSet.setSelected(innerSplitWay);
        this.action.actionPerformed(null);

        assertEquals(3, relation.getMembersCount());

        assertAll(relation.getMemberPrimitives().stream()
                .map(prim -> () -> assertFalse(prim.isDeleted(), prim.toString())));

        this.dataSet.setSelected(dataSet.getNodes().stream()
                .filter(node -> Utils.equalsEpsilon(node.lat(), -0.5) && Utils.equalsEpsilon(node.lon(), -0.5))
                .map(n -> n.getParentWays().get(0)).findFirst().orElseThrow(AssertionFailedError::new));

        /* Copied from PropertiesDialog */
        List<RelationMember> members = relation.getMembers();
        for (OsmPrimitive primitive: OsmDataManager.getInstance().getInProgressSelection()) {
            members.removeIf(rm -> rm.getMember() == primitive);
        }
        // This is where the problem occurs.
        assertDoesNotThrow(() -> UndoRedoHandler.getInstance().add(new ChangeMembersCommand(relation, members)));
        /* End copy from PropertiesDialog */

        assertEquals(1, dataSet.getRelations().size());
        assertEquals(2, relation.getMembersCount());
    }

    private static class AlwaysDeleteCallback implements DeleteCommand.DeletionCallback {

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
    }
}