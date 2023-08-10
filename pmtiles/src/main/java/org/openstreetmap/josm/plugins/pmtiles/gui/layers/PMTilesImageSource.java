// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import jakarta.json.JsonObject;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;

/**
 * A source for images in PMTiles
 */
public class PMTilesImageSource extends AbstractTMSTileSource implements PMTilesTileSource {
    private final JsonObject metadata;
    private final Header header;
    private final OsmMercator osmMercator;

    /**
     * Create a new tile source
     * @param info The image raster source
     */
    public PMTilesImageSource(PMTilesImageryInfo info) {
        super(info);
        this.metadata = info.metadata();
        this.header = info.header();
        this.osmMercator = new OsmMercator(getTileSize());
    }

    @Override
    public JsonObject metadata() {
        return this.metadata;
    }

    @Override
    public Header header() {
        return this.header;
    }

    @Override
    public OsmMercator osmMercator() {
        return this.osmMercator;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        return PMTilesTileSource.super.getTileUrl(zoom, tilex, tiley);
    }
}
