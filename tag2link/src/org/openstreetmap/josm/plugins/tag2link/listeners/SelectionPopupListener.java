//    JOSM tag2link plugin.
//    Copyright (C) 2011-2012 Don-vip & FrViPofm
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.tag2link.listeners;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.gui.MapFrame;

public class SelectionPopupListener extends AbstractIPrimitivePopupListener {

    public SelectionPopupListener(MapFrame frame) {
        super(frame);
    }

    @Override
    protected IPrimitive getFirstSelectedPrimitive() {
        return frame.selectionListDialog.getSelectedPrimitives().iterator().next();
    }
}
