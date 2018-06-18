// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.history.commands;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;

/**
* Abstract class for any Streetside command.
*
* @author nokutu
*
*/
public abstract class StreetsideCommand {
/** Set of {@link StreetsideAbstractImage} objects affected by the command */
public Set<StreetsideAbstractImage> images;

/**
* Main constructor.
*
* @param images
*          The images that are affected by the command.
*/
public StreetsideCommand(Set<StreetsideAbstractImage> images) {
 this.images = new ConcurrentSkipListSet<>(images);
}

/**
* Undoes the action.
*/
public abstract void undo();

/**
* Redoes the action.
*/
public abstract void redo();

/**
* If two equal commands are applied consecutively to the same set of images,
* they are summed in order to reduce them to just one command.
*
* @param command
*          The command to be summed to last command.
*/
public abstract void sum(StreetsideCommand command);

@Override
public abstract String toString();
}
