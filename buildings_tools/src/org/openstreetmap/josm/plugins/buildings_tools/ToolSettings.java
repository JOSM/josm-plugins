// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * A holder for plugin settings
 */
public final class ToolSettings {

    private ToolSettings() {
        // Hide default constructor for utils classes
    }

    public static final BooleanProperty PROP_USE_ADDR_NODE = new BooleanProperty("buildings_tools.addrNode", false);

    /**
     * The shapes that users can create
     */
    public enum Shape {
            CIRCLE, RECTANGLE
    }

    private static Shape shape = loadShape();
    private static double width;
    private static double lenstep;
    private static boolean useAddr;
    private static final Map<String, String> TAGS = new HashMap<>();

    /**
     * Get the current shape that will be created
     * @return The shape
     */
    public static Shape getShape() {
        loadShape();
        return shape;
    }

    /**
     * Set whether to use the address dialog
     * @param useAddr {@code true} if the address dialog should be used
     */
    public static void setAddrDialog(boolean useAddr) {
        ToolSettings.useAddr = useAddr;
    }

    /**
     * Set some constraints for new buildings
     * @param newwidth The max width ({@link Shape#RECTANGLE}) or max diameter ({@link Shape#CIRCLE})
     * @param newlenstep The step sizes to use when setting the length of a {@link Shape#RECTANGLE}
     */
    public static void setSizes(double newwidth, double newlenstep) {
        width = newwidth;
        lenstep = newlenstep;
    }

    /**
     * Get the width/diameter
     * @return The max width ({@link Shape#RECTANGLE}) or max diameter ({@link Shape#CIRCLE})
     */
    public static double getWidth() {
        return width;
    }

    /**
     * Get the step length for {@link Shape#RECTANGLE}
     * @return The amount to increase the length of a {@link Shape#RECTANGLE}.
     */
    public static double getLenStep() {
        return lenstep;
    }

    /**
     * Check if we want to show the user an address dialog
     * @return {@code true} if the user should be shown the address dialog
     */
    public static boolean isUsingAddr() {
        return useAddr;
    }

    /**
     * Get the tags that the user wants to set on all new objects
     * @return The tag map
     */
    public static Map<String, String> getTags() {
        loadTags();
        return Collections.unmodifiableMap(TAGS);
    }

    /**
     * Set the tags to put on all new objects
     * @param tags The tags to set
     */
    public static void saveTags(Map<String, String> tags) {
        TAGS.clear();
        TAGS.putAll(tags);
        ArrayList<String> values = new ArrayList<>(TAGS.size() * 2);
        for (Map.Entry<String, String> entry : TAGS.entrySet()) {
            values.add(entry.getKey());
            values.add(entry.getValue());
        }
        Config.getPref().putList("buildings_tools.tags", values);
    }

    /**
     * Load tags from preferences
     */
    private static void loadTags() {
        TAGS.clear();
        Collection<String> values = Config.getPref().getList("buildings_tools.tags",
                Arrays.asList("building", "yes"));
        try {
            for (Iterator<String> iterator = values.iterator(); iterator.hasNext();) {
                TAGS.put(iterator.next(), iterator.next());
            }
        } catch (NoSuchElementException e) {
            Logging.warn(e);
        }
    }

    /**
     * Set the shape to use
     * @param shape The shape
     */
    public static void saveShape(Shape shape) {
        Config.getPref().put("buildings_tool.shape", shape.name());
    }

    /**
     * Load the shape to use from preferences
     * @return The shape to use
     */
    private static Shape loadShape() {
        String shape = Config.getPref().get("buildings_tool.shape");
        if (ToolSettings.Shape.CIRCLE.name().equals(shape)) {
            ToolSettings.shape = Shape.CIRCLE;
            return Shape.CIRCLE;
        } else {
            ToolSettings.shape = Shape.RECTANGLE;
            return Shape.RECTANGLE;
        }
    }

    /**
     * Set the Big buildings mode
     * @param bbmode {@code true} if big building mode should be used
     */
    public static void setBBMode(boolean bbmode) {
        Config.getPref().putBoolean("buildings_tools.bbmode", bbmode);
    }

    /**
     * Get the Big buildings mode
     * @return {@code true} if big building mode should be used
     */
    public static boolean isBBMode() {
        return Config.getPref().getBoolean("buildings_tools.bbmode", false);
    }

    public static void setSoftCursor(boolean softCursor) {
        Config.getPref().putBoolean("buildings_tools.softcursor", softCursor);
    }

    public static boolean isSoftCursor() {
        return Config.getPref().getBoolean("buildings_tools.softcursor", false);
    }

    public static boolean isAutoSelect() {
        return Config.getPref().getBoolean("buildings_tools.autoselect", false);
    }

    public static void setAutoSelect(boolean autoSelect) {
        Config.getPref().putBoolean("buildings_tools.autoselect", autoSelect);
    }

    public static boolean isAutoSelectReplaceSelection() {
        return Config.getPref().getBoolean("buildings_tools.autoselect_replace_selection", false);
    }

    public static void setAutoSelectReplaceSelection(boolean autoSelectReplace) {
        Config.getPref().putBoolean("buildings_tools.autoselect_replace_selection", autoSelectReplace);
    }

    /**
     * Check if we are toggling between {@link Shape} types if the user toggles the mapmode with a keypress
     * @return {@code true} if we want to change the shape type
     */
    public static boolean isTogglingBuildingTypeOnRepeatedKeyPress() {
        return Config.getPref().getBoolean("buildings_tools.toggle_building_type", false);
    }

    /**
     * Set whether or not we are toggling between {@link Shape} types if the user toggles the mapmode with a keypress
     * @param toggle {@code true} if we want to change the shape type
     */
    public static void setTogglingBuildingTypeOnRepeatedKeyPress(boolean toggle) {
        Config.getPref().putBoolean("buildings_tools.toggle_building_type", toggle);
    }

    public static boolean isNoClickAndDrag() {
        return Config.getPref().getBoolean("buildings_tools.noclickdrag", false);
    }

    public static void setNoClickAndDrag(boolean noClickDrag) {
        Config.getPref().putBoolean("buildings_tools.noclickdrag", noClickDrag);
    }
}
