// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit test for the {@link TurnRestrictionLegEditor}
 */
@BasicPreferences
class TurnRestrictionLegEditorUnitTest {
    private DataSet ds;
    private OsmDataLayer layer;
    private TurnRestrictionEditorModel model;

    @BeforeEach
    public void setUp() {
        ds = new DataSet();
        layer = new OsmDataLayer(ds, "test", null);
        model = new TurnRestrictionEditorModel(layer, new NavigationControler() {
            @Override
            public void gotoBasicEditor(BasicEditorFokusTargets focusTarget) {
            }

            @Override
            public void gotoBasicEditor() {
            }

            @Override
            public void gotoAdvancedEditor() {
            }
        });
    }

    @Test
    void testConstructor1() {
        TurnRestrictionLegEditor editor = new TurnRestrictionLegEditor(model, TurnRestrictionLegRole.FROM);
        assertEquals(model, editor.getModel());
        assertEquals(TurnRestrictionLegRole.FROM, editor.getRole());
    }

    @Test
    void testConstructor2() {
        assertThrows(IllegalArgumentException.class, () -> new TurnRestrictionLegEditor(null, TurnRestrictionLegRole.FROM));
    }

    @Test
    void testConstructor3() {
        assertThrows(IllegalArgumentException.class, () -> new TurnRestrictionLegEditor(model, null));
    }
}
