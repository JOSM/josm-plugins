package reverter;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class ReverterPlugin extends Plugin {
    static UndeletedObjectsStorage undeletedObjects = new UndeletedObjectsStorage();
    public ReverterPlugin(PluginInformation info) {
        super(info);
        JMenu historyMenu = Main.main.menu.addMenu(marktr("History"), KeyEvent.VK_R,
                Main.main.menu.defaultMenuPos,ht("/Plugin/Reverter"));
        //MainMenu.add(historyMenu, new ObjectsHistoryAction());       
        MainMenu.add(historyMenu, new RevertChangesetAction());
        MapView.addLayerChangeListener(undeletedObjects);
        UploadAction.registerUploadHook(new ReverterUploadHook(undeletedObjects));        
    }
}
