package org.openstreetmap.josm.plugins.pt_assistant.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMember;

public class PTStop extends RelationMember {

	private Node stopPosition = null;
	private OsmPrimitive platform = null;

	private String name = "";

	public PTStop(RelationMember other) throws IllegalArgumentException {

		super(other);

		// if ((other.hasRole("stop") || other.hasRole("stop_entry_only") ||
		// other.hasRole("stop_exit_only"))
		// && other.getType().equals(OsmPrimitiveType.NODE)) {

		if (other.getMember().hasTag("public_transport", "stop_position")) {

			this.stopPosition = other.getNode();
			this.name = stopPosition.get("name");

			// } else if (other.getRole().equals("platform") ||
			// other.getRole().equals("platform_entry_only")
			// || other.getRole().equals("platform_exit_only")) {
		} else if (other.getMember().hasTag("highway", "bus_stop")
				|| other.getMember().hasTag("public_transport", "platform")
				|| other.getMember().hasTag("highway", "platform") || other.getMember().hasTag("railway", "platform")) {

			this.platform = other.getMember();
			this.name = platform.get("name");

		} else {
			throw new IllegalArgumentException(
					"The RelationMember type does not match its role " + other.getMember().getName());
		}

	}

	/**
	 * Adds the given element to the stop after a check
	 * 
	 * @param member
	 *            Element to add
	 * @return true if added successfully, false otherwise. A false value
	 *         indicates either that the OsmPrimitiveType of the given
	 *         RelationMember does not match its role or that this PTStop
	 *         already has an attribute with that role.
	 */
	protected boolean addStopElement(RelationMember member) {

		// each element is only allowed once per stop

		// add stop position:
		// if (member.hasRole("stop") || member.hasRole("stop_entry_only") ||
		// member.hasRole("stop_exit_only")) {
		if (member.getMember().hasTag("public_transport", "stop_position")) {
			if (member.getType().equals(OsmPrimitiveType.NODE) && stopPosition == null) {
				this.stopPosition = member.getNode();
				return true;
			}
		}

		// add platform:
		// if (member.getRole().equals("platform") ||
		// member.getRole().equals("platform_entry_only")
		// || member.getRole().equals("platform_exit_only")) {
		if (member.getMember().hasTag("highway", "bus_stop")
				|| member.getMember().hasTag("public_transport", "platform")
				|| member.getMember().hasTag("highway", "platform")
				|| member.getMember().hasTag("railway", "platform")) {
			if (platform == null) {
				platform = member.getMember();
				return true;
			}
		}

		return false;

	}

	/**
	 * Returns the stop_position for this PTstop. If the stop_position is not
	 * available directly, the method searches for a stop_area relation
	 * 
	 * @return
	 */
	public Node getStopPosition() {

		return this.stopPosition;
	}

	/**
	 * Returns platform (including platform_entry_only and platform_exit_only)
	 * 
	 * @return
	 */
	public OsmPrimitive getPlatform() {
		return this.platform;
	}

	protected String getName() {
		return this.name;
	}

	public void setStopPosition(Node newStopPosition) {

		this.stopPosition = newStopPosition;

	}

	/**
	 * Finds potential stop_positions of the platform of this PTStop. It only
	 * makes sense to call this method if the stop_position attribute is null.
	 * The stop_positions are potential because they may refer to a different
	 * route, which this method does not check.
	 * 
	 * @return List of potential stop_positions for this PTStop
	 */
	public List<Node> findPotentialStopPositions() {

		ArrayList<Node> potentialStopPositions = new ArrayList<>();

		if (platform == null) {
			return potentialStopPositions;
		}

		// Look for a stop position within 100 m (around 0.002 degrees) of this
		// platform:

		LatLon platformCenter = platform.getBBox().getCenter();
		Double ax = platformCenter.getX() - 0.002;
		Double bx = platformCenter.getX() + 0.002;
		Double ay = platformCenter.getY() - 0.002;
		Double by = platformCenter.getY() + 0.002;
		BBox platformBBox = new BBox(ax, ay, bx, by);

		// Collection<Node> allNodes =
		// Main.getLayerManager().getEditDataSet().getNodes();
		Collection<Node> allNodes = platform.getDataSet().getNodes();
		for (Node currentNode : allNodes) {
			if (platformBBox.bounds(currentNode.getBBox()) && currentNode.hasTag("public_transport", "stop_position")) {
				potentialStopPositions.add(currentNode);
			}
		}

		return potentialStopPositions;
	}

	/**
	 * Checks if this stop equals to other by comparing if they have the same
	 * stop_position or a platform
	 * 
	 * @param other PTStop to be compared
	 * @return true if equal, false otherwise
	 */
	public boolean equalsStop(PTStop other) {

		if (other == null) {
			return false;
		}

		if (this.stopPosition != null && (this.stopPosition == other.getStopPosition() || this.stopPosition == other.getPlatform())) {
			return true;
		}
		
		if (this.platform != null && (this.platform == other.getPlatform() || this.platform == other.getStopPosition())) {
			return true;
		}

		return false;
	}
	
}
