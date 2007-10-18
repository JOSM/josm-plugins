package UtilsPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

import javax.swing.AbstractAction;

class MergeNodeWayAction extends JosmAction {
	public MergeNodeWayAction() {
	    super(tr("Join node to way"), "mergenodeway",
			tr("Join a node into the nearest way segments"), 0, 0, true);
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.size() != 1 || !(sel.iterator().next() instanceof Node)) return;
		Node node = (Node) sel.iterator().next();

		List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(
			Main.map.mapView.getPoint(node.eastNorth));
		HashMap<Way, List<Integer>> insertPoints = new HashMap<Way, List<Integer>>();
		for (WaySegment ws : wss) {
			List<Integer> is;
			if (insertPoints.containsKey(ws.way)) {
				is = insertPoints.get(ws.way);
			} else {
				is = new ArrayList<Integer>();
				insertPoints.put(ws.way, is);
			}

			if (ws.way.nodes.get(ws.lowerIndex) != node
					&& ws.way.nodes.get(ws.lowerIndex+1) != node) {
				is.add(ws.lowerIndex);
			}
		}

		Collection<Command> cmds = new LinkedList<Command>();
		for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
			Way w = insertPoint.getKey();
			Way wnew = new Way(w);
			List<Integer> is = insertPoint.getValue();
			pruneSuccsAndReverse(is);
			for (int i : is) wnew.nodes.add(i+1, node);
			cmds.add(new ChangeCommand(w, wnew));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Join Node and Line"), cmds));
		Main.map.repaint();
	}

	private static void pruneSuccsAndReverse(List<Integer> is) {
		//if (is.size() < 2) return;

		HashSet<Integer> is2 = new HashSet<Integer>();
		for (int i : is) {
			if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
				is2.add(i);
			}
		}
		is.clear();
		is.addAll(is2);
		Collections.sort(is);
		Collections.reverse(is);
	}
}
