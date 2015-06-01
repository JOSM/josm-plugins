package org.openstreetmap.josm.plugins.mapillary;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.apache.commons.jcs.access.CacheAccess;
import org.openstreetmap.josm.plugins.mapillary.downloads.MapillaryDownloader;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.Action;
import javax.swing.Icon;

import java.util.List;
import java.util.ArrayList;

public class MapillaryLayer extends AbstractModifiableLayer implements
		MouseListener, DataSetListener, EditLayerChangeListener {

	public static Boolean INSTANCED = false;
	public static MapillaryLayer INSTANCE;
	public static CacheAccess<String, BufferedImageCacheEntry> CACHE;
	public static MapillaryImage BLUE;
	public static MapillaryImage RED;

	private final MapillaryData mapillaryData;
	private List<Bounds> bounds;
	private MapillaryToggleDialog tgd;

	public MapillaryLayer() {
		super(tr("Mapillary Images"));
		mapillaryData = MapillaryData.getInstance();
		bounds = new ArrayList<>();
		init();
	}

	/**
	 * Initializes the Layer.
	 */
	private void init() {
		INSTANCED = true;
		MapillaryLayer.INSTANCE = this;
		try {
			CACHE = JCSCacheManager.getCache("Mapillary");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Main.map != null && Main.map.mapView != null) {
			Main.map.mapView.addMouseListener(this);
			Main.map.mapView.addLayer(this);
			MapView.addEditLayerChangeListener(this, false);
			Main.map.mapView.getEditLayer().data.addDataSetListener(this);
			if (tgd == null) {
				if (MapillaryToggleDialog.INSTANCE == null) {
					tgd = MapillaryToggleDialog.getInstance();
					Main.map.addToggleDialog(tgd, false);
				} else
					tgd = MapillaryToggleDialog.getInstance();
			}
		}
		MapillaryPlugin.setMenuEnabled(MapillaryPlugin.EXPORT_MENU, true);
		download();
	}

	public static MapillaryLayer getInstance() {
		if (MapillaryLayer.INSTANCE == null)
			MapillaryLayer.INSTANCE = new MapillaryLayer();
		return MapillaryLayer.INSTANCE;
	}

	/**
	 * Downloads all images of the area covered by the OSM data.
	 */
	protected void download() {
		for (Bounds bounds : Main.map.mapView.getEditLayer().data
				.getDataSourceBounds()) {
			if (!this.bounds.contains(bounds)) {
				this.bounds.add(bounds);
				new MapillaryDownloader(mapillaryData).getImages(
						bounds.getMin(), bounds.getMax());
			}
		}
	}

	/**
	 * Returs the MapillaryData object, which acts as the database of the Layer.
	 * 
	 * @return
	 */
	public MapillaryData getMapillaryData() {
		return mapillaryData;
	}

	/**
	 * Method invoqued when the layer is destroyed.
	 */
	@Override
	public void destroy() {
		MapillaryToggleDialog.getInstance().mapillaryImageDisplay
				.setImage(null);
		INSTANCED = false;
		MapillaryLayer.INSTANCE = null;
		MapillaryPlugin.setMenuEnabled(MapillaryPlugin.EXPORT_MENU, false);
		MapillaryData.deleteInstance();
		Main.map.mapView.removeMouseListener(this);
		MapView.removeEditLayerChangeListener(this);
		if (Main.map.mapView.getEditLayer() != null)
			Main.map.mapView.getEditLayer().data.removeDataSetListener(this);
		super.destroy();
	}

	/**
	 * Returns true any of the images from the database has been modified.
	 */
	@Override
	public boolean isModified() {
		for (MapillaryImage image : mapillaryData.getImages()) {
			if (image.isModified()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Paints the database in the map.
	 */
	@Override
	public void paint(Graphics2D g, MapView mv, Bounds box) {
		synchronized (this) {
			// Draw colored lines
			MapillaryLayer.BLUE = null;
			MapillaryLayer.RED = null;
			MapillaryToggleDialog.getInstance().blueButton.setEnabled(false);
			MapillaryToggleDialog.getInstance().redButton.setEnabled(false);
			if (mapillaryData.getSelectedImage() != null) {
				MapillaryImage[] closestImages = getClosestImagesFromDifferentSequences();
				Point selected = mv.getPoint(mapillaryData.getSelectedImage()
						.getLatLon());
				if (closestImages[0] != null) {
					MapillaryLayer.BLUE = closestImages[0];
					g.setColor(Color.BLUE);
					g.drawLine(mv.getPoint(closestImages[0].getLatLon()).x,
							mv.getPoint(closestImages[0].getLatLon()).y,
							selected.x, selected.y);
					MapillaryToggleDialog.getInstance().blueButton
							.setEnabled(true);
				}
				if (closestImages[1] != null) {
					MapillaryLayer.RED = closestImages[1];
					g.setColor(Color.RED);
					g.drawLine(mv.getPoint(closestImages[1].getLatLon()).x,
							mv.getPoint(closestImages[1].getLatLon()).y,
							selected.x, selected.y);
					MapillaryToggleDialog.getInstance().redButton
							.setEnabled(true);
				}
			}
			g.setColor(Color.WHITE);
			for (MapillaryImage image : mapillaryData.getImages()) {
				Point p = mv.getPoint(image.getLatLon());
				Point nextp;
				if (image.getSequence() != null
						&& image.getSequence().next(image) != null) {
					nextp = mv.getPoint(image.getSequence().next(image)
							.getLatLon());
					g.drawLine(p.x, p.y, nextp.x, nextp.y);
				}
				ImageIcon icon;
				if (!mapillaryData.getMultiSelectedImages().contains(image))
					icon = MapillaryPlugin.MAP_ICON;
				else
					icon = MapillaryPlugin.MAP_ICON_SELECTED;
				Image imagetemp = icon.getImage();
				BufferedImage bi = (BufferedImage) imagetemp;
				int width = icon.getIconWidth();
				int height = icon.getIconHeight();
				
				// Rotate the image
				double rotationRequired = Math.toRadians(image.getCa());
				double locationX = width / 2;
				double locationY = height / 2;
				AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
				
				g.drawImage(op.filter(bi, null), p.x - (width / 2), p.y - (height / 2),
						Main.map.mapView);
				
			}
		}
	}

	@Override
	public Icon getIcon() {
		return MapillaryPlugin.ICON16;
	}

	@Override
	public boolean isMergable(Layer other) {
		return false;
	}

	@Override
	public void mergeFrom(Layer from) {
		throw new UnsupportedOperationException(
				"Notes layer does not support merging yet");
	}

	@Override
	public Action[] getMenuEntries() {
		List<Action> actions = new ArrayList<>();
		actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
		actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
		actions.add(new LayerListPopup.InfoAction(this));
		return actions.toArray(new Action[actions.size()]);
	}

	private MapillaryImage[] getClosestImagesFromDifferentSequences() {
		MapillaryImage[] ret = new MapillaryImage[2];
		double[] distances = { 100, 100 };
		LatLon selectedCoords = mapillaryData.getSelectedImage().getLatLon();
		double maxJumpDistance = 100;
		for (MapillaryImage image : mapillaryData.getImages()) {
			if (image.getLatLon().greatCircleDistance(selectedCoords) < maxJumpDistance
					&& mapillaryData.getSelectedImage().getSequence() != image
							.getSequence()) {
				if ((ret[0] == null && ret[1] == null)
						|| (image.getLatLon().greatCircleDistance(
								selectedCoords) < distances[0] && (ret[1] == null || image
								.getSequence() != ret[1].getSequence()))) {
					ret[0] = image;
					distances[0] = image.getLatLon().greatCircleDistance(
							selectedCoords);
				} else if ((ret[1] == null || image.getLatLon()
						.greatCircleDistance(selectedCoords) < distances[1])
						&& image.getSequence() != ret[0].getSequence()) {
					ret[1] = image;
					distances[1] = image.getLatLon().greatCircleDistance(
							selectedCoords);
				}
			}
		}
		return ret;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		if (Main.map.mapView.getActiveLayer() != this) {
			return;
		}
		Point clickPoint = e.getPoint();
		double snapDistance = 10;
		double minDistance = Double.MAX_VALUE;
		MapillaryImage closest = null;
		for (MapillaryImage image : mapillaryData.getImages()) {
			Point imagePoint = Main.map.mapView.getPoint(image.getLatLon());
			// move the note point to the center of the icon where users are
			// most likely to click when selecting
			imagePoint.setLocation(imagePoint.getX(), imagePoint.getY());
			double dist = clickPoint.distanceSq(imagePoint);
			if (minDistance > dist
					&& clickPoint.distance(imagePoint) < snapDistance) {
				minDistance = dist;
				closest = image;
			}
		}
		if (e.getModifiers() == (MouseEvent.BUTTON1_MASK | MouseEvent.CTRL_MASK))
			mapillaryData.addMultiSelectedImage(closest);
		else
			mapillaryData.setSelectedImage(closest);
	}

	@Override
	public Object getInfoComponent() {
		StringBuilder sb = new StringBuilder();
		sb.append(tr("Mapillary layer"));
		sb.append("\n");
		sb.append(tr("Total images:"));
		sb.append(" ");
		sb.append(this.size());
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public String getToolTipText() {
		return this.size() + " " + tr("images");
	}

	private int size() {
		return mapillaryData.getImages().size();
	}

	// MouseListener
	@Override
	public void visitBoundingBox(BoundingXYVisitor v) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	// EditDataLayerChanged
	@Override
	public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
	}

	/**
	 * When more data is downloaded, a delayed update is thrown, in order to
	 * wait for the data bounds to be set.
	 * 
	 * @param event
	 */
	@Override
	public void dataChanged(DataChangedEvent event) {
		Main.worker.submit(new delayedDownload());
	}

	private class delayedDownload extends Thread {

		@Override
		public void run() {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MapillaryLayer.getInstance().download();
		}

	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent event) {
	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent event) {
	}

	@Override
	public void tagsChanged(TagsChangedEvent event) {
	}

	@Override
	public void nodeMoved(NodeMovedEvent event) {
	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent event) {
	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent event) {
	}

	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent event) {
	}
}
