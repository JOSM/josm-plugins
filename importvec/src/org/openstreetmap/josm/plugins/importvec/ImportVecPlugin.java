package org.openstreetmap.josm.plugins.importvec;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class ImportVecPlugin extends Plugin {

    public ImportVecPlugin(PluginInformation info) {
        super(info);
        Main.main.menu.fileMenu.insert(new ImportVectorAction(), 3);
    }

}
