// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;

import jakarta.json.JsonObject;

/**
 * A source for images in PMTiles
 */
public class PMTilesImageSource extends AbstractTMSTileSource implements PMTilesTileSource {
    /** The metadata for this source */
    private final JsonObject metadata;
    /** The PMTiles header */
    private final Header header;
    /** The {@link OsmMercator} instance for the specified tile size */
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
