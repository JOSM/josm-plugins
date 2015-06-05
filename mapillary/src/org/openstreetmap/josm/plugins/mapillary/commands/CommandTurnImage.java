package org.openstreetmap.josm.plugins.mapillary.commands;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;

public class CommandTurnImage extends MapillaryCommand {
	private List<MapillaryImage> images;
	private double ca;

	public CommandTurnImage(List<MapillaryImage> images, double ca) {
		this.images = new ArrayList<>(images);
		this.ca = ca;
	}


	@Override
	public void undo() {
		for (MapillaryImage image : images) {
			image.turn(-ca);
			image.stopMoving();
		}
		Main.map.repaint();
	}

	@Override
	public void redo() {
		for (MapillaryImage image : images) {
			image.turn(ca);
			image.stopMoving();
		}
		Main.map.repaint();
	}

}
