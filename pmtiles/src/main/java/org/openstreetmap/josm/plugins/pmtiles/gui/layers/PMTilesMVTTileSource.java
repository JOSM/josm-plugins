// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorTileSource;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.style.MapboxVectorStyle;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * The tile source for MVT tiles in PMTiles
 */
public class PMTilesMVTTileSource extends MapboxVectorTileSource implements PMTilesTileSource {
    /** The metadata for the source */
    private final JsonObject metadata;
    /** The PMTiles header information */
    private final Header header;
    /** The style source for the vector tiles */
    private final MapboxVectorStyle styleSource;

    /**
     * Create a new tile source
     * @param info The MVT source
     */
    public PMTilesMVTTileSource(PMTilesImageryInfo info) {
        super(info);
        this.metadata = info.metadata();
        this.header = info.header();
        this.baseUrl = info.header().location().toString();

        // Check if there is a TileJSON specification file. JOSM doesn't (currently) understand it, but it may in the
        // future.
        if (info.metadata().containsKey("vector_layers") && info.metadata().get("vector_layers").getValueType() == JsonValue.ValueType.ARRAY) {
            final var tileJson = info.metadata().getJsonArray("vector_layers");
            final var builder = Json.createObjectBuilder().add("version", 8)
                    .add("sources", tileJson);
            if (metadata.containsKey("name")) {
                builder.add("name", metadata.getString("name"));
            }
            this.styleSource = new MapboxVectorStyle(builder.build());
        } else {
            this.styleSource = null;
        }
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
    public MapboxVectorStyle getStyleSource() {
        return this.styleSource;
    }
}
