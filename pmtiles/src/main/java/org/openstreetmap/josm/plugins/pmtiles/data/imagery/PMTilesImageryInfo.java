// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.data.imagery;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;

import jakarta.json.JsonObject;

/**
 * An {@link ImageryInfo} object for PMTiles
 */
public class PMTilesImageryInfo extends ImageryInfo {
    /** The PMTiles header for this info */
    private final Header header;
    /** The metadata for this info */
    private final JsonObject meta;

    /**
     * Create a new {@link PMTilesImageryInfo} object from a PMTiles {@link Header}
     * @param header The header to use
     */
    public PMTilesImageryInfo(Header header) {
        Objects.requireNonNull(header);
        this.setBounds(new ImageryBounds(Double.toString(header.minLatitude()) + ',' + header.minLongitude()
                + ',' + header.maxLatitude() + ',' + header.maxLongitude(), ","));
        // Do NOT set the URL to the file -- the MapBoxVectorTileLayer will try to read a JSON from it (in its entirety!),
        // which doesn't work well.
        this.setUrl("");
        this.minZoom = header.minZoom();
        this.maxZoom = header.maxZoom();
        this.setDefaultMaxZoom(this.maxZoom);
        this.setDefaultMinZoom(this.minZoom);
        this.header = header;
        try {
            this.meta = PMTiles.readMetadata(header);
            this.setName(meta.getString("name", null));
            this.description = meta.getString("description", null);
            this.setAttributionText(meta.getString("attribution", null));
            if ("overlay".equals(meta.getString("type", null))) {
                this.setOverlay(true);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get the PMTiles header
     * @return The header
     */
    public Header header() {
        return this.header;
    }

    /**
     * Get the PMTiles metadata
     * @return The metadata
     */
    public JsonObject metadata() {
        return this.meta;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + this.header.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && this.header.equals(((PMTilesImageryInfo) o).header);
    }
}
