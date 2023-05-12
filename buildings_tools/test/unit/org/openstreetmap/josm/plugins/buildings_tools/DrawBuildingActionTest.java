// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.KeyEvent;
import java.time.Instant;

import javax.swing.JLabel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link DrawBuildingAction}
 */
@BasicPreferences
@Main
@Projection
class DrawBuildingActionTest {
    private static DrawBuildingAction action;

    @BeforeEach
    void setup() {
        action = new DrawBuildingAction();
    }

    @AfterEach
    void tearDown() {
        action.destroy();
        action = null;
    }


    /**
     * Ensure that we are toggling the map mode properly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testToggle(boolean setToggle) {
        // Ensure we are showing the main map
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "testToggle", null));
        ToolSettings.setTogglingBuildingTypeOnRepeatedKeyPress(setToggle);
        assertEquals(setToggle, ToolSettings.isTogglingBuildingTypeOnRepeatedKeyPress());
        final ToolSettings.Shape shape = ToolSettings.getShape();
        final KeyStroke keyStroke = action.getShortcut().getKeyStroke();
        MainApplication.getMap().selectMapMode(action);
        action.doKeyPressed(getKeyEvent(keyStroke));
        assertNotEquals(setToggle, shape == ToolSettings.getShape());
        action.doKeyPressed(getKeyEvent(keyStroke));
        assertEquals(shape, ToolSettings.getShape());
    }

    /**
     * Get a key event to send the action
     * @param keyStroke The keystroke to use
     * @return The event
     */
    private KeyEvent getKeyEvent(KeyStroke keyStroke) {
        assertNotNull(keyStroke);
        return new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, Instant.now().toEpochMilli(),
                keyStroke.getModifiers(), keyStroke.getKeyCode(), keyStroke.getKeyChar());
    }
}
