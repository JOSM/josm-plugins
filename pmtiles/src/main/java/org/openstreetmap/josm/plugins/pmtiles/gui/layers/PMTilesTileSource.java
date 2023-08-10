// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import java.awt.Image;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.Projected;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileRange;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.PMTiles;

/**
 * The tile source for PMTiles
 */
public interface PMTilesTileSource extends TileSource {
    /**
     * The metdata for the source
     * @return The metadata
     */
    JsonObject metadata();

    /**
     * The header for the source
     * @return The header
     */
    Header header();

    /**
     * A mercator object for calculations
     * @return The mercator object
     */
    OsmMercator osmMercator();

    @Override
    default boolean requiresAttribution() {
        return this.metadata().containsKey("attribution");
    }

    @Override
    default String getAttributionText(int zoom, ICoordinate topLeft, ICoordinate botRight) {
        return this.metadata().getString("attribution", null);
    }

    @Override
    default String getAttributionLinkURL() {
        return null;
    }

    @Override
    default Image getAttributionImage() {
        return null;
    }

    @Override
    default String getAttributionImageURL() {
        return null;
    }

    @Override
    default String getTermsOfUseText() {
        return null;
    }

    @Override
    default String getTermsOfUseURL() {
        return null;
    }

    @Override
    default int getMaxZoom() {
        return this.header().maxZoom();
    }

    @Override
    default int getMinZoom() {
        return this.header().minZoom();
    }

    @Override
    default String getName() {
        return this.metadata().getString("name", null);
    }

    @Override
    default String getId() {
        return this.header().location().toString();
    }

    @Override
    default String getTileUrl(int zoom, int tilex, int tiley) {
        return header().location().toString() + '/' + getTileId(this.header(), zoom, tilex, tiley);
    }

    @Override
    default String getTileId(int zoom, int tilex, int tiley) {
        return getTileId(this.header(), zoom, tilex, tiley);
    }

    /**
     * Get the id for this tile
     * @param header The PMTile header
     * @param zoom The zoom level
     * @param tilex The tilex
     * @param tiley The tiley
     * @return The id for the tile
     */
    static String getTileId(Header header, int zoom, int tilex, int tiley) {
        final String extension = switch (header.tileType()) {
            case MVT -> ".mvt";
            case PNG -> ".png";
            case JPEG -> ".jpg";
            case WEBP -> ".webp";
            case AVIF -> ".avif";
            case UNKNOWN -> throw new IllegalArgumentException("Unknown format: " + header.location());
        };
        return PMTiles.convertToHilbert(zoom, tilex, tiley) + extension;
    }

    @Override
    default int getTileSize() {
        return this.getDefaultTileSize();
    }

    @Override
    default int getDefaultTileSize() {
        return 512;
    }

    @Override
    default double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return osmMercator().getDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    default Point latLonToXY(double lat, double lon, int zoom) {
        return new Point(
                (int) Math.round(osmMercator().lonToX(lon, zoom)),
                (int) Math.round(osmMercator().latToY(lat, zoom))
        );
    }

    @Override
    default Point latLonToXY(ICoordinate point, int zoom) {
        return this.latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    default ICoordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    default ICoordinate xyToLatLon(int x, int y, int zoom) {
        return new Coordinate(
                osmMercator().yToLat(y, zoom),
                osmMercator().xToLon(x, zoom)
        );
    }

    @Override
    default TileXY latLonToTileXY(double lat, double lon, int zoom) {
        return new TileXY(
                osmMercator().lonToX(lon, zoom) / getTileSize(),
                osmMercator().latToY(lat, zoom) / getTileSize()
        );
    }

    @Override
    default TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    default ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return this.tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    default ICoordinate tileXYToLatLon(Tile tile) {
        return this.tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    default ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        return new Coordinate(
                osmMercator().yToLat((long) y * getTileSize(), zoom),
                osmMercator().xToLon((long) x * getTileSize(), zoom)
        );
    }

    @Override
    default int getTileXMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    default int getTileXMin(int zoom) {
        return 0;
    }

    @Override
    default int getTileYMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    default int getTileYMin(int zoom) {
        return 0;
    }

    /**
     * Get the maximum number of tiles on an axis for a specified zoom level
     * @param zoom The zoom level to find the tiles for
     * @return The number of tiles on an axis
     */
    private static int getTileMax(int zoom) {
        return (int) Math.pow(2.0, zoom) - 1;
    }

    @Override
    default boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content) {
        return content.length == 0;
    }

    @Override
    default Map<String, String> getMetadata(Map<String, List<String>> headers) {
        return Collections.emptyMap();
    }

    @Override
    default IProjected tileXYtoProjected(int x, int y, int zoom) {
        final var mercatorWidth = 2 * Math.PI * OsmMercator.EARTH_RADIUS;
        final var f = mercatorWidth * getTileSize() / osmMercator().getMaxPixels(zoom);
        return new Projected(f * x - mercatorWidth / 2, -(f * y - mercatorWidth / 2));
    }

    @Override
    default TileXY projectedToTileXY(IProjected p, int zoom) {
        final var mercatorWidth = 2 * Math.PI * OsmMercator.EARTH_RADIUS;
        final var f = mercatorWidth * getTileSize() / osmMercator().getMaxPixels(zoom);
        return new TileXY((p.getEast() + mercatorWidth / 2) / f, (-p.getNorth() + mercatorWidth / 2) / f);
    }

    @Override
    default boolean isInside(Tile inner, Tile outer) {
        final int dz = inner.getZoom() - outer.getZoom();
        if (dz < 0) return false;
        return outer.getXtile() == inner.getXtile() >> dz &&
                outer.getYtile() == inner.getYtile() >> dz;
    }

    @Override
    default TileRange getCoveringTileRange(Tile tile, int newZoom) {
        if (newZoom <= tile.getZoom()) {
            final int dz = tile.getZoom() - newZoom;
            final var xy = new TileXY(tile.getXtile() >> dz, tile.getYtile() >> dz);
            return new TileRange(xy, xy, newZoom);
        } else {
            final int dz = newZoom - tile.getZoom();
            final var t1 = new TileXY(tile.getXtile() << dz, tile.getYtile() << dz);
            final var t2 = new TileXY(t1.getX() + (1 << dz) - 1, t1.getY() + (1 << dz) - 1);
            return new TileRange(t1, t2, newZoom);
        }
    }

    @Override
    default String getServerCRS() {
        return "EPSG:3857";
    }
}
