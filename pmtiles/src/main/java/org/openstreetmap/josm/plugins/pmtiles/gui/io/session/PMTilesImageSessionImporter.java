// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionLayerImporter;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.gui.layers.PMTilesImageLayer;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Session importer for {@link PMTilesImageLayer}.
 * @since 36468
 */
public class PMTilesImageSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        // Check against session file XML schema version
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for PMTiles layer is not supported. Expected: 0.1", version));
        }

        // Read the file URI
        String fileUri = getChildText(elem, "file");
        if (fileUri == null || fileUri.isEmpty()) {
            throw new IllegalDataException(tr("Missing file URI in PMTiles session data"));
        }

        // Read optional name
        String name = getChildText(elem, "name");

        try {
            URI uri = new URI(fileUri);
            Header header = PMTiles.readHeader(uri);
            PMTilesImageryInfo info = new PMTilesImageryInfo(header);
            PMTilesImageLayer layer = new PMTilesImageLayer(info);

            // Set custom name if provided
            if (name != null && !name.isEmpty()) {
                layer.setName(name);
            }

            return layer;
        } catch (URISyntaxException e) {
            throw new IllegalDataException(tr("Invalid PMTiles file URI: {0}", fileUri), e);
        }
    }

    /**
     * Get the text content of a child element with the given tag name.
     * @param parent the parent element
     * @param tagName the tag name to search for
     * @return the text content, or null if not found
     */
    private static String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}
