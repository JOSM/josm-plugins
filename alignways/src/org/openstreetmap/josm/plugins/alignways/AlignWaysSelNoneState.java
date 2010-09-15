/**
 * 
 */
package org.openstreetmap.josm.plugins.alignways;

import org.openstreetmap.josm.Main;

/**
 * @author tilusnet <tilusnet@gmail.com>
 * 
 */
public class AlignWaysSelNoneState extends AlignWaysState {

    @Override
    public void leftClick(AlignWaysMode alignWaysMode) {
        // Reference way segment selected successfully
        alignWaysMode.setCurrentState(alignWaysMode.getAligneeSelected());

    }

    @Override
    public void ctrlLClick(AlignWaysMode alignWaysMode) {
        // Reference way segment selected successfully
        alignWaysMode.setCurrentState(alignWaysMode.getReferenceSelected());
    }

    @Override
    public void setHelpText() {
        Main.map.statusLine
        .setHelpText("Ctrl-click: select reference way segment; Click: select way segment to be aligned");
    }

}
