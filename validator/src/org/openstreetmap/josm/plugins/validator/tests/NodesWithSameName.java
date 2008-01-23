package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;

public class NodesWithSameName extends Test {
	private Map<String, List<Node>> namesToNodes;

	public NodesWithSameName() {
		super(tr("Nodes with same name"),
			tr("Find nodes that have the same name " +
				"(might be duplicates due to e.g. the OpenGeoDB import)"));
	}

	@Override public void startTest() {
		namesToNodes = new HashMap<String, List<Node>>();
	}

	@Override public void visit(Node n) {
		if (n.deleted || n.incomplete) return;

		String name = n.get("name");
		if (name == null) return;

		List<Node> nodes = namesToNodes.get(name);
		if (nodes == null)
			namesToNodes.put(name, nodes = new ArrayList<Node>());

		nodes.add(n);
	}

	@Override public void endTest() {
		for (List<Node> nodes : namesToNodes.values()) {
			if (nodes.size() > 1) {
				errors.add(new TestError(this, Severity.WARNING,
					tr("Nodes with same name"), nodes));
			}
		}

		namesToNodes = null;
	}
}
