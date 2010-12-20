package imageryadjust;

import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class ImageryAdjustPlugin extends Plugin {

    public ImageryAdjustPlugin(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame == null) return;
        newFrame.addMapMode(new IconToggleButton(new ImageryAdjustMapMode(newFrame)));
    }
}
