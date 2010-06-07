/**
 * 
 */
package org.openstreetmap.josm.plugins.alignways;

import org.openstreetmap.josm.Main;

/**
 * @author tilusnet <tilusnet@gmail.com>
 * 
 */
public class AlignWaysSelAlgnState extends AlignWaysState {

	@Override
	public void leftClick(AlignWaysMode alignWaysMode) {
		// No state change, nothing to do
	}

	@Override
	public void ctrlLClick(AlignWaysMode alignWaysMode) {
		alignWaysMode.setCurrentState(alignWaysMode.getBothSelected());
	}

	@Override
	public void setHelpText() {
		Main.map.statusLine
				.setHelpText("Ctrl-Click: select reference way segment; Alt-click: Clear selection");
	}

}
