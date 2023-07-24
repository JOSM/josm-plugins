// License: GPL. For details, see LICENSE file.
package relcontext;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * The entry-point for the reltoolbox plugin
 */
public class RelContextPlugin extends Plugin {

    public RelContextPlugin(PluginInformation info) {
        super(info);
        DefaultNameFormatter.registerFormatHook(new ExtraNameFormatHook());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            //            if (dialog!=null) dialog.destroy();
            RelContextDialog dialog = new RelContextDialog();
            newFrame.addToggleDialog(dialog);
        }
    }
}
