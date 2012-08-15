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

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.tag2link.Tag2LinkRuleChecker;
import org.openstreetmap.josm.plugins.tag2link.data.Link;

public abstract class AbstractIPrimitivePopupListener extends AbstractPopupListener {

    protected AbstractIPrimitivePopupListener(MapFrame frame) {
        super(frame);
    }

    protected abstract IPrimitive getFirstSelectedPrimitive();
    
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        IPrimitive p = getFirstSelectedPrimitive();
        if (p != null) {
            JPopupMenu popup = (JPopupMenu) e.getSource();
            for (Link link : Tag2LinkRuleChecker.getLinks(p)) {
                addLink(popup, link);
            }
        }
    }
}
