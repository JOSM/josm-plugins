package mappaint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;

/**
 * A visitor that paints the OSM components in a map-like style.
 */

public class MapPaintVisitor extends SimplePaintVisitor {

	/**
	 * Draw a small rectangle.
	 * White if selected (as always) or red otherwise.
	 *
	 * @param n The node to draw.
	 */
	// Altered from SimplePaintVisitor
	@Override public void visit(Node n) {
		ElemStyle nodeStyle = MapPaintPlugin.elemStyles.getStyle(n);
		if(nodeStyle!=null && Main.map.mapView.zoom()>=nodeStyle.getMinZoom()){
			if(nodeStyle instanceof IconElemStyle) {
				drawNode(n, ((IconElemStyle)nodeStyle).getIcon());
			} else {
				// throw some sort of exception
			}
		} else {
			drawNode(n, n.selected ? getPreferencesColor("selected",
									Color.YELLOW)
				: getPreferencesColor("node", Color.RED));
		}
	}

	/**
	 * Draw just a line between the points.
	 * White if selected (as always) or grey otherwise.
	 * Want to make un-wayed segments stand out less than ways.
	 */
	@Override public void visit(Segment ls) {
		drawSegment(ls, getPreferencesColor("untagged",Color.GRAY));
	}

	/**
	 * Draw a line for all segments, according to tags.
	 * @param w The way to draw.
	 */
	// Altered from SimplePaintVisitor
	@Override public void visit(Way w) {
		Color colour = getPreferencesColor("untagged",Color.GRAY);
		int width=2;
		boolean area=false;
		ElemStyle wayStyle = MapPaintPlugin.elemStyles.getStyle(w);
		if(wayStyle!=null)
		{
			if(wayStyle instanceof LineElemStyle)
			{
				colour = ((LineElemStyle)wayStyle).getColour();
				width = ((LineElemStyle)wayStyle).getWidth();
			}
			else if (wayStyle instanceof AreaElemStyle)
			{
				colour = ((AreaElemStyle)wayStyle).getColour();
				area = true;
			}
		}

		if(area)
		{
			drawWayAsArea(w,colour);
		}
		else
		{
			for (Segment ls : w.segments)
			{
				if (!ls.selected) // selected already in good color
					drawSegment(ls, w.selected ?
						getPreferencesColor("selected", Color.YELLOW) : colour,
						width);
			}
		}
	}

	// This assumes that all segments are aligned in the same direction!
	protected void drawWayAsArea(Way w, Color colour)
	{
		Polygon polygon = new Polygon();
		Point p;
		boolean first=true;
		for (Segment ls : w.segments)
		{
		    if (ls.incomplete) continue;
			if(first)
			{
				p = nc.getPoint(ls.from.eastNorth);
				polygon.addPoint(p.x,p.y);
				first=false;
			}
			p = nc.getPoint(ls.to.eastNorth);
			polygon.addPoint(p.x,p.y);
		}

		g.setColor( w.selected ?
						getPreferencesColor("selected", Color.YELLOW) : colour);

		if(Main.pref.getBoolean("mappaint.fillareas", true))
			g.fillPolygon(polygon);
		else
		{
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(2));
			g.drawPolygon(polygon);
			g2d.setStroke(new BasicStroke(1));
		}
	}

	// NEW
	protected void drawNode(Node n, ImageIcon icon) {
		Point p = nc.getPoint(n.eastNorth);
		int w = icon.getIconWidth(), h=icon.getIconHeight();
		icon.paintIcon ( Main.map.mapView, g, p.x-w/2, p.y-h/2 );
		String name = (n.keys==null) ? null : n.keys.get("name");
		if (name!=null)
		{
			g.setColor( getPreferencesColor ("text", Color.WHITE));
			g.setFont (new Font("Helvetica", Font.PLAIN, 8));
			g.drawString (name, p.x+w/2+2, p.y+h/2+2);
		}
		if (n.selected)
		{
			g.setColor (  getPreferencesColor("selected", Color.YELLOW) );
			g.drawRect (p.x-w/2-2,p.y-w/2-2, w+4, h+4);
		}
	}

	/**
	 * Draw a line with the given color.
	 */
	// Altered - now specify width
	@Override protected void drawSegment(Segment ls, Color col) {
			drawSegment(ls,col,1);
	}

	// Altered - now specify width
	private void drawSegment (Segment ls, Color col, int width) {
		Graphics2D g2d = (Graphics2D)g;
		if (ls.incomplete)
			return;
		if (ls.selected)
			col = getPreferencesColor("selected", Color.YELLOW);
		g.setColor(col);
		//g.setWidth(width);
		g2d.setStroke(new BasicStroke(width));
		Point p1 = nc.getPoint(ls.from.eastNorth);
		Point p2 = nc.getPoint(ls.to.eastNorth);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);

		if (Main.pref.getBoolean("draw.segment.direction")) {
			double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
	        g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
	        g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
		}
	}


	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	@Override public void drawNode(Node n, Color color) {
		Point p = nc.getPoint(n.eastNorth);
		g.setColor(color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}

	// NW 111106 Overridden from SimplePaintVisitor in josm-1.4-nw1
	// Shows areas before non-areas
	public void visitAll(DataSet data) {

		for (final OsmPrimitive osm : data.ways) {
			if(!osm.deleted && MapPaintPlugin.elemStyles.isArea(osm))
				osm.visit(this);
		}

		for (final OsmPrimitive osm : data.segments)
			if (!osm.deleted)
				osm.visit(this);


		for (final OsmPrimitive osm : data.ways) {
			if(!osm.deleted && !MapPaintPlugin.elemStyles.isArea(osm))
				osm.visit(this);
		}

		for (final OsmPrimitive osm : data.nodes)
			if (!osm.deleted)
				osm.visit(this);

		for (final OsmPrimitive osm : data.getSelected())
			if (!osm.deleted)
				osm.visit(this);
	}
}
