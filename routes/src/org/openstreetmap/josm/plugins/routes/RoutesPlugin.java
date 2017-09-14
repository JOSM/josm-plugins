package org.openstreetmap.josm.plugins.routes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.routes.xml.Routes;
import org.openstreetmap.josm.plugins.routes.xml.RoutesXMLLayer;
import org.openstreetmap.josm.tools.Logging;

public class RoutesPlugin extends Plugin implements LayerChangeListener {

	private final List<RouteLayer> routeLayers = new ArrayList<>();

	public RoutesPlugin(PluginInformation info) {
		super(info);
		MainApplication.getLayerManager().addLayerChangeListener(this);

		File routesFile = new File(getPluginDir() + File.separator + "routes.xml");
		if (!routesFile.exists()) {
			Logging.info("File with route definitions doesn't exist, using default");

			try {
				routesFile.getParentFile().mkdir();
				try (
						OutputStream outputStream = new FileOutputStream(routesFile);
						InputStream inputStream = Routes.class.getResourceAsStream("routes.xml");
						) {
					byte[] b = new byte[512];
					int read;
					while ((read = inputStream.read(b)) != -1) {
						outputStream.write(b, 0, read);
					}
				}
			} catch (IOException e) {
				Logging.error(e);
			}
		}

		try {
			JAXBContext context = JAXBContext.newInstance(
					Routes.class.getPackage().getName(), Routes.class.getClassLoader());
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Routes routes = (Routes)unmarshaller.unmarshal(
					new FileInputStream(getPluginDir() + File.separator + "routes.xml"));
			for (RoutesXMLLayer layer:routes.getLayer()) {
				if (layer.isEnabled()) {
					routeLayers.add(new RouteLayer(layer));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		// Do nothing
	}

	@Override
	public void layerAdded(LayerAddEvent e) {
		Layer layer = e.getAddedLayer();
		if (layer instanceof OsmDataLayer) {
			LayerManager lm = e.getSource();
			for (RouteLayer routeLayer : routeLayers) {
				if (!lm.containsLayer(routeLayer)) {
					SwingUtilities.invokeLater(() -> {
						if (!lm.containsLayer(routeLayer)) {
							lm.addLayer(routeLayer);
						}
					});
				}
			}
		}
	}

	@Override
	public void layerRemoving(LayerRemoveEvent e) {
		for (Layer layer : e.getSource().getLayers()) {
			if (layer instanceof OsmDataLayer)  {
				return; /* at least one OSM layer left, do nothing */
			}
		}
		if(!e.isLastLayer()) {
			SwingUtilities.invokeLater(() -> {
				for (RouteLayer routeLayer : routeLayers) {
					if (e.getSource().containsLayer(routeLayer)) {
						e.getSource().removeLayer(routeLayer);
					}
				}
			});
		}
	}

	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Do nothing
	}
}
