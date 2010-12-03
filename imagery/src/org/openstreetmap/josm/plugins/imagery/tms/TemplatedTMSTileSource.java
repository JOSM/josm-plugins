package org.openstreetmap.josm.plugins.imagery.tms;

import org.openstreetmap.gui.jmapviewer.OsmTileSource;

public class TemplatedTMSTileSource extends OsmTileSource.AbstractOsmTileSource {
    private int maxZoom;
    
    public TemplatedTMSTileSource(String name, String url, int maxZoom) {
        super(name, url);
        this.maxZoom = maxZoom;
    }

    public String getTileUrl(int zoom, int tilex, int tiley) {
        return this.BASE_URL
        .replaceAll("\\{zoom\\}", Integer.toString(zoom))
        .replaceAll("\\{x\\}", Integer.toString(tilex))
        .replaceAll("\\{y\\}", Integer.toString(tiley));
        
    }

    @Override
    public int getMaxZoom() {
        return (maxZoom == 0) ? super.getMaxZoom() : maxZoom;
    }

    @Override
    public TileUpdate getTileUpdate() {
        return TileUpdate.IfNoneMatch;
    }
}