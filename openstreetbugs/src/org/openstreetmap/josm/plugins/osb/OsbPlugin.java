/* Copyright (c) 2008, Henrik Niehaus
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its 
 *    contributors may be used to endorse or promote products derived from this 
 *    software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.plugins.osb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.UploadAction.UploadHook;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.osb.api.DownloadAction;
import org.openstreetmap.josm.plugins.osb.gui.OsbDialog;

/**
 * Shows issues from OpenStreetBugs 
 *
 * @author Henrik Niehaus (henrik dot niehaus at gmx dot de)
 */
public class OsbPlugin extends Plugin implements LayerChangeListener {

	private DataSet dataSet;
	
	private UploadHook uploadHook;
	
	private OsbDialog dialog;
	
	private OsbLayer layer;
	
	public static boolean active = false;
	
	private DownloadAction download = new DownloadAction();
	
	public OsbPlugin() {
		initConfig();
		dataSet = new DataSet();
		uploadHook = new OsbUploadHook();
		dialog = new OsbDialog(this);
		OsbLayer.listeners.add(dialog);
		OsbLayer.listeners.add(this);
	}
	
	private void initConfig() {
		String debug = Main.pref.get(ConfigKeys.OSB_API_DISABLED);
		if(debug == null || debug.length() == 0) {
			debug = "false";
			Main.pref.put(ConfigKeys.OSB_API_DISABLED, debug);
		}
		
		String uri = Main.pref.get(ConfigKeys.OSB_API_URI_EDIT);
		if(uri == null || uri.length() == 0) {
			uri = "http://openstreetbugs.appspot.com/editPOIexec";
			Main.pref.put(ConfigKeys.OSB_API_URI_EDIT, uri);
		}
		
		uri = Main.pref.get(ConfigKeys.OSB_API_URI_CLOSE);
		if(uri == null || uri.length() == 0) {
			uri = "http://openstreetbugs.appspot.com/closePOIexec";
			Main.pref.put(ConfigKeys.OSB_API_URI_CLOSE, uri);
		}
		
		uri = Main.pref.get(ConfigKeys.OSB_API_URI_DOWNLOAD);
		if(uri == null || uri.length() == 0) {
			uri = "http://openstreetbugs.appspot.com/getBugs";
			Main.pref.put(ConfigKeys.OSB_API_URI_DOWNLOAD, uri);
		}
		
		uri = Main.pref.get(ConfigKeys.OSB_API_URI_NEW);
		if(uri == null || uri.length() == 0) {
			uri = "http://openstreetbugs.appspot.com/addPOIexec";
			Main.pref.put(ConfigKeys.OSB_API_URI_NEW, uri);
		}
		
		String auto_download = Main.pref.get(ConfigKeys.OSB_AUTO_DOWNLOAD);
		if(auto_download == null || auto_download.length() == 0) {
			auto_download = "true";
			Main.pref.put(ConfigKeys.OSB_AUTO_DOWNLOAD, auto_download);
		}
	}

	/**
	 * Determines the bounds of the current selected layer
	 * @return
	 */
	protected Bounds bounds(){
		MapView mv = Main.map.mapView;
		return new Bounds(
			mv.getLatLon(0, mv.getHeight()),
			mv.getLatLon(mv.getWidth(), 0));
	}
	
	public void updateData() {
		// determine the bounds of the currently visible area
		Bounds bounds = bounds();
			
		try {
			// download the data
			download.execute(dataSet, bounds);

			// display the parsed data
			if(!dataSet.nodes.isEmpty()) {
				updateGui();
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Main.parent, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void updateGui() {
		// update dialog
		dialog.update(dataSet);
		
		// create a new layer if necessary
		updateLayer(dataSet);
		
		// repaint view, so that changes get visible
		Main.map.mapView.repaint();
	}
	
	private void updateLayer(DataSet osbData) {
		if(layer == null) {
			layer = new OsbLayer(osbData, "OpenStreetBugs");
			Main.main.addLayer(layer);
		}
	}

	@Override
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		if (oldFrame==null && newFrame!=null) { // map frame added
			// add the dialog
			newFrame.addToggleDialog(dialog);
			
			// add the upload hook
			LinkedList<UploadHook> hooks = ((UploadAction) Main.main.menu.upload).uploadHooks;
			hooks.add(0, uploadHook);
			
			// add a listener to the plugin toggle button
			final JToggleButton toggle = (JToggleButton) dialog.action.button;
			active = toggle.isSelected();
			toggle.addActionListener(new ActionListener() {
				private boolean download = true;

				public void actionPerformed(ActionEvent e) {
					active = toggle.isSelected();
					if (toggle.isSelected() && download) {
						Main.worker.execute(new Runnable() {
							public void run() {
								updateData();
							}
						});
						download = false;
					}
				}
			});
		} else if (oldFrame!=null && newFrame==null ) { // map frame removed
			
		}
	}
	
	public static ImageIcon loadIcon(String name) {
		URL url = OsbPlugin.class.getResource("/images/".concat(name));
		return new ImageIcon(url);
	}

	public void activeLayerChange(Layer oldLayer, Layer newLayer) {}

	public void layerAdded(Layer newLayer) {
		if(newLayer instanceof OsmDataLayer) {
			active = ((JToggleButton)dialog.action.button).isSelected();
			
			// start the auto download loop
			OsbDownloadLoop.getInstance().setPlugin(this);
		}
	}

	public void layerRemoved(Layer oldLayer) {
		if(oldLayer == layer) {
			layer = null;
		}
	}

	public OsbLayer getLayer() {
		return layer;
	}

	public void setLayer(OsbLayer layer) {
		this.layer = layer;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}
}
