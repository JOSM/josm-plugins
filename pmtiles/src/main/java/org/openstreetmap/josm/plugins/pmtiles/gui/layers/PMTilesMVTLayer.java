// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTFile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorTileSource;
import org.openstreetmap.josm.gui.layer.imagery.MVTLayer;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;

/**
 * MVT layer that comes from PMTiles
 * @see PMTilesImageLayer for raster tiles (unfortunately it has a different inheritance tree)
 */
public class PMTilesMVTLayer extends MVTLayer implements PMTilesLayer {
    /**
     * Creates an instance of an MVT layer
     *
     * @param info ImageryInfo describing the layer
     */
    public PMTilesMVTLayer(PMTilesImageryInfo info) {
        super(info);
        if (info.getName() != null) {
            this.setName(info.getName());
        } else {
            this.setName(info.header().location().toString());
        }
    }

    @Override
    protected Class<? extends TileLoader> getTileLoaderClass() {
        return PMTilesLoader.class;
    }

    @Override
    protected void initTileSource(MapboxVectorTileSource tileSource) {
        super.initTileSource(tileSource);
        this.tileLoader = getTileLoaderFactory().makeTileLoader(this, getHeaders(tileSource), this.info.getMinimumTileExpire());
        if (this.tileLoader instanceof PMTilesLoader pmTilesLoader) {
            pmTilesLoader.setInfo((PMTilesImageryInfo) this.info);
        }
    }

    @Override
    protected String getCacheName() {
        return "PMTILES_IMAGE";
    }

    @Override
    public Collection<String> getNativeProjections() {
        // There is not information on what native projections are used.
        // However, there are references to "Web Mercator".
        return Collections.singleton(MVTFile.DEFAULT_PROJECTION);
    }

    @Override
    protected PMTilesMVTTileSource getTileSource() {
        return new PMTilesMVTTileSource((PMTilesImageryInfo) this.info);
    }
}
