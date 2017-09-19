// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;

public class MenuActionBoundaries extends JosmAction {

    public static final String NAME = "Administrative boundary";

    private static final long serialVersionUID = 1L;
    private WMSLayer wmsLayer = null;

    /**
     * Constructs a new {@code MenuActionBoundaries}.
     */
    public MenuActionBoundaries() {
        super(tr(NAME), "cadastre_small", tr("Extract commune boundary"), null, false);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        wmsLayer = WMSDownloadAction.getLayer();
        if (wmsLayer != null) {
            if (wmsLayer.isRaster()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Only on vectorized layers"), tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            DownloadSVGTask.download(wmsLayer);
        }
    }
}
