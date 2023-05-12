// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JMenu;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The entry point for the buildings tools plugin
 */
public class BuildingsToolsPlugin extends Plugin {
    public static final Projection MERCATOR = Projections.getProjectionByCode("EPSG:3857"); // Mercator

    /**
     * Convert a latlon to east north
     * @param p The latlon to convert
     * @return The east-north ({@link #MERCATOR})
     */
    public static EastNorth latlon2eastNorth(ILatLon p) {
        return MERCATOR.latlon2eastNorth(p);
    }

    /**
     * Convert an east-north to a latlon
     * @param p The east north to convert (from {@link #MERCATOR})
     * @return The latlon
     */
    public static LatLon eastNorth2latlon(EastNorth p) {
        return MERCATOR.eastNorth2latlon(p);
    }

    public BuildingsToolsPlugin(PluginInformation info) {
        super(info);
        JMenu moreToolsMenu = MainApplication.getMenu().moreToolsMenu;
        if (moreToolsMenu.getMenuComponentCount() > 0) {
            moreToolsMenu.addSeparator();
        }
        MainMenu.add(moreToolsMenu, new DrawBuildingAction());
        JMenu optionMenu = new JMenu(tr("Draw buildings modes"));
        optionMenu.setIcon(ImageProvider.get("preference_small", ImageProvider.ImageSizes.MENU));
        moreToolsMenu.add(optionMenu);
        MainMenu.add(optionMenu, new BuildingSizeAction());
        MainMenu.add(optionMenu, new BuildingCircleAction());
        MainMenu.add(optionMenu, new BuildingRectangleAction());
        MainMenu.add(optionMenu, new MergeAddrPointsAction());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            newFrame.addMapMode(new IconToggleButton(new DrawBuildingAction()));
        }
    }
}
