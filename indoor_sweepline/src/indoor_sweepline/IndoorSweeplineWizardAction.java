// License: GPL. For details, see LICENSE file.
package indoor_sweepline;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class IndoorSweeplineWizardAction extends JosmAction {

    public IndoorSweeplineWizardAction() {
        super(tr("Concourse wizard ..."), null,
                tr("Opens up a wizard to create a concourse"), null, false);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Layer layer = MainApplication.getLayerManager().getActiveLayer();
        if (layer == null)
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MainApplication.getMainFrame()),
                    "No default layer found.");
        else if (!(layer instanceof OsmDataLayer))
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MainApplication.getMainFrame()),
                    "The default layer is not an OSM layer.");
        else if (MainApplication.getMap() == null)
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MainApplication.getMainFrame()),
                    "No map found.");
        else if (MainApplication.getMap().mapView == null)
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MainApplication.getMainFrame()),
                    "No map view found.");
        else
            new IndoorSweeplineController((OsmDataLayer) layer,
                    ProjectionRegistry.getProjection().eastNorth2latlon(MainApplication.getMap().mapView.getCenter()));
    }
}
