package net.simon04.comfort0;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * Copy the selected objects in the Level0L format.
 */
public class CopyLevel0LAction extends JosmAction {

    /**
     * Constructs a new {@link CopyLevel0LAction}.
     */
    public CopyLevel0LAction() {
        super(tr("Copy as Level0L"),
                "theta",
                tr("Copy the selected objects in the Level0L format"),
                null, false, "CopyLevel0LAction", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            final DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
            final String level0l = new OsmToLevel0L().visit(dataSet.getSelected()).toString();
            ClipboardUtils.copyString(level0l);
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection(true);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
