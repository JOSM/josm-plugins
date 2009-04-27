/***************************************************************************
 *   Copyright (C) 2009 by Tomasz Stelmach                                 *
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

package org.openstreetmap.josm.plugins.piclayer;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;

/**
 * Action responsible for creation of a new layer based on
 * the content of the clipboard.
 */
public class NewLayerFromClipboardAction extends JosmAction {
	
	/**
	 * Constructor...
	 */
	public NewLayerFromClipboardAction() {
		super("New picture layer from clipboard", null, null, null, false);
	}

	/**
	 * Action handler
	 */
	public void actionPerformed(ActionEvent arg0) {
		// Create layer from clipboard
		PicLayerFromClipboard layer = new PicLayerFromClipboard();
		// Add layer only if successfully initialized
		try {
			layer.Initialize();
		}
		catch (IOException e) {
			// Failed
			System.out.println( "NewLayerFromClipboardAction::actionPerformed - " + e.getMessage() );
			JOptionPane.showMessageDialog(null, e.getMessage() );  
			return;
		}
		// Add layer
		Main.main.addLayer( layer );
	}
}
