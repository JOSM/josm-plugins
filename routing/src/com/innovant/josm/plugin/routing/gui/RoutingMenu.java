/*
 * Copyright (C) 2008 Innovant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,USA.
 *
 * For more information, please contact:
 *
 *  Innovant
 *   juangui@gmail.com
 *   vidalfree@gmail.com
 *
 *  http://public.grupoinnovant.com/blog
 *
 */

package com.innovant.josm.plugin.routing.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.openstreetmap.josm.Main;

import com.innovant.josm.jrt.core.RoutingGraph.RouteType;
import com.innovant.josm.plugin.routing.RoutingLayer;
import com.innovant.josm.plugin.routing.RoutingModel;
import com.innovant.josm.plugin.routing.RoutingPlugin;

/**
 * The menu bar from this plugin
 * @author jvidal
 *
 */
public class RoutingMenu extends JMenu {

	/**
	 * Default serial version UID
	 */
	private static final long serialVersionUID = 3559922048225708480L;

	/**
	 * @param s
	 */
	public RoutingMenu(final String name) {
		super(name);
		final RoutingLayer routingLayer = RoutingPlugin.getInstance().getRoutingLayer();
		final RoutingModel routingModel = routingLayer.getRoutingModel();

		JMenuItem mi;
		JMenu m;

		mi = new JMenuItem(tr("Start routing"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!routingLayer.isLayerAdded()) {
					routingLayer.setLayerAdded();
					Main.main.addLayer(routingLayer);
				}
			}
		});
		this.add(mi);

		this.addSeparator();
		ButtonGroup group = new ButtonGroup();

		m = new JMenu(tr("Criteria"));

		JRadioButtonMenuItem rshorter = new JRadioButtonMenuItem(tr("Shortest"));
		rshorter.setSelected(true);
		rshorter.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					routingModel.routingGraph.setTypeRoute(RouteType.SHORTEST);
				} else {
					routingModel.routingGraph.setTypeRoute(RouteType.FASTEST);
				}
				routingModel.routingGraph.resetGraph();
				routingModel.routingGraph.createGraph();
				//TODO: Change this way
				//FIXME: do not change node but recalculate routing.
				routingModel.setNodesChanged();
				Main.map.repaint();
			}

		});

		JRadioButtonMenuItem rfaster = new JRadioButtonMenuItem(tr("Fastest"));

		group.add(rshorter);
		group.add(rfaster);
		m.add(rshorter);
		m.add(rfaster);

		m.addSeparator();
		JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem("Ignore oneways");
		cbmi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED)
					routingModel.routingGraph.getRoutingProfile().setOnewayUse(false);
				else
					routingModel.routingGraph.getRoutingProfile().setOnewayUse(true);
				routingModel.setNodesChanged();
				Main.map.repaint();
			}
		});
		m.add(cbmi);
		this.add(m);

		this.addSeparator();
		mi = new JMenuItem(tr("Reverse route"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				routingModel.reverseNodes();
				Main.map.repaint();
			}
		});
		this.add(mi);

		mi = new JMenuItem(tr("Clear route"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Reset routing nodes and paths
				routingModel.reset();
				RoutingPlugin.getInstance().getRoutingDialog().clearNodes();
				Main.map.repaint();
			}
		});
		this.add(mi);

	}


}
