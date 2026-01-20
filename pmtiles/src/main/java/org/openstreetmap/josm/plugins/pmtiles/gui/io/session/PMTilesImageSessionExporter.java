// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.io.session;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.io.session.AbstractSessionExporter;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesImageLayer;
import org.openstreetmap.josm.tools.GBC;
import org.w3c.dom.Element;

/**
 * Session exporter for {@link PMTilesImageLayer}.
 * @since 36468
 */
public class PMTilesImageSessionExporter extends AbstractSessionExporter<PMTilesImageLayer> {

    /**
     * Constructs a new {@code PMTilesImageSessionExporter}.
     * @param layer PMTiles image layer to export
     */
    public PMTilesImageSessionExporter(PMTilesImageLayer layer) {
        super(layer);
    }

    @Override
    public Component getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        export.setSelected(true);
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEADING);
        lbl.setToolTipText(layer.getToolTipText());
        lbl.setLabelFor(export);
        p.add(export, GBC.std());
        p.add(lbl, GBC.std());
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        return p;
    }

    @Override
    public Element export(ExportSupport support) throws IOException {
        Element layerElem = support.createElement("layer");
        layerElem.setAttribute("type", "pmtiles-image");
        layerElem.setAttribute("version", "0.1");

        // Store the URI to the PMTiles file
        Element fileElem = support.createElement("file");
        String uri = layer.getInfo().header().location().toString();
        fileElem.appendChild(support.createTextNode(uri));
        layerElem.appendChild(fileElem);

        // Store the layer name if different from default
        String name = layer.getName();
        if (name != null && !name.isEmpty()) {
            Element nameElem = support.createElement("name");
            nameElem.appendChild(support.createTextNode(name));
            layerElem.appendChild(nameElem);
        }

        return layerElem;
    }
}
