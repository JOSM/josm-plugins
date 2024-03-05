// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.gui.layers;

import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.plugins.pmtiles.data.imagery.PMTilesImageryInfo;
import org.openstreetmap.josm.tools.TextUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * A common interface for layers using PMTiles as a source
 */
interface PMTilesLayer extends MapViewPaintable {

    /**
     * Returns imagery info.
     * @return imagery info
     */
    PMTilesImageryInfo getInfo();

    /**
     * Get the source tag for the layer
     * @return The source tag
     */
    default String getChangesetSourceTag() {
        final var sb = new StringBuilder();
        final var info = getInfo();
        if (info.hasAttribution()) {
            sb.append(getInfo().getAttributionText(0, null, null)
                    .replaceAll("<a [^>]*>|</a>", "")
                    .replaceAll("  +", " "));
        }
        if (info.getName() != null) {
            if (!sb.isEmpty()) {
                sb.append(" - ");
            }
            sb.append(info.getName());
        }
        if (sb.isEmpty()) {
            final var location = info.header().location().toString();
            if (Utils.isLocalUrl(location)) {
                final String userName = getSystemProperty("user.name");
                final String userNameAlt = "<user.name>";
                sb.append(location.replace(userName, userNameAlt));
            } else {
                sb.append(TextUtils.stripUrl(location));
            }
        }
        return sb.toString();
    }

    /**
     * Get info information
     * @return The information to add to the info panel
     */
    default String[][] getInfoContent() {
        final var info = getInfo();
        final var content = new String[3][];
        content[0] = new String[] {"Maximum zoom", String.valueOf(info.getMaxZoom())};
        content[1] = new String[] {"Minimum zoom", String.valueOf(info.getMinZoom())};
        content[2] = new String[] {"Bounds", info.getBounds().toBBox().toStringCSV(",")};
        return content;
    }

    @Override
    default void paint(Graphics2D g, MapView mv, Bounds box) {
        final var info = getInfo();
        g.setStroke(new BasicStroke());
        g.setColor(Color.DARK_GRAY);
        if (info.getBounds().getShapes().isEmpty()) {
            final var lowerLeft = mv.getPoint(info.getBounds().getMin());
            final var upperRight = mv.getPoint(info.getBounds().getMax());
            g.drawRect(lowerLeft.x, upperRight.y, upperRight.x - lowerLeft.x, lowerLeft.y - upperRight.y);
        } else {
            for (var shape : info.getBounds().getShapes()) {
                Point last = null;
                for (ICoordinate coord : shape.getPoints()) {
                    final var point = mv.getPoint(new LatLon(coord.getLat(), coord.getLon()));
                    if (last != null) {
                        g.drawLine(last.x, last.y, point.x, point.y);
                    }
                    last = point;
                }
            }
        }
    }

    /**
     * Visits the content bounds of this layer. The behavior of this method depends on the layer,
     * but each implementation should attempt to cover the relevant content of the layer in this method.
     * @param v The visitor that gets notified about the contents of this layer.
     * @see org.openstreetmap.josm.gui.layer.Layer#visitBoundingBox
     */
    default void visitBoundingBox(BoundingXYVisitor v) {
        v.visit(this.getInfo().getBounds());
    }
}
