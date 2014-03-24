/***************************************************************************
 *                                                                         *
 *   Copyright (C) 2011 Patrick "Petschge" Kilian, based on code           *
 *   (c) 2009 by Tomasz Stelmach                                           *
 *   http://www.stelmach-online.net/                                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package org.openstreetmap.josm.plugins.piclayer.actions.transform;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.piclayer.actions.GenericPicTransformAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class handles the input during shearing of the picture.
 */
@SuppressWarnings("serial")
public class ShearPictureAction extends GenericPicTransformAction {

    /**
     * Constructor
     */
    public ShearPictureAction(MapFrame frame) {
        super(tr("PicLayer shear"), tr("Sheared"), "shear", tr("Drag to shear the picture"), frame, ImageProvider.getCursor("crosshair", null));
    }

    @Override
    protected void doAction(MouseEvent e) {
        EastNorth eastNorth = Main.map.mapView.getEastNorth(e.getX(),e.getY());
        currentLayer.shearPictureBy(
            1000* (eastNorth.east() - prevEastNorth.east()),
            1000* (eastNorth.north() - prevEastNorth.north())
        );
    }

}
