/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.openstreetmap.josm.plugins.fixAddresses.gui.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.plugins.fixAddresses.AddressEditContainer;
import org.openstreetmap.josm.plugins.fixAddresses.OSMStreet;
import org.openstreetmap.josm.plugins.fixAddresses.gui.AddressEditSelectionEvent;

@SuppressWarnings("serial")
public class ConvertAllToRelationAction extends ConvertToRelationAction {
	public ConvertAllToRelationAction() {
		super(tr("Convert ALL streets."), "convert2rel_24",
			tr("Create relation between street and related addresses for ALL streets in the current layer."),
			"fixaddresses/convertalltorelation");
	}

	@Override
	public void addressEditActionPerformed(AddressEditSelectionEvent ev) {
		// nothing to do
	}

	@Override
	public void addressEditActionPerformed(AddressEditContainer container) {
		if (container != null) {
			for (OSMStreet street : container.getStreetList()) {
				createRelationForStreet(street);
			}
		}
	}

	@Override
	protected void updateEnabledState(AddressEditContainer container) {
		setEnabled(hasStreetsToConvert());
	}

	@Override
	protected void updateEnabledState(AddressEditSelectionEvent event) {
		setEnabled(hasStreetsToConvert());
	}

	/**
	 * Checks for streets to convert to a relation.
	 *
	 * @return true, if successful
	 */
	private boolean hasStreetsToConvert() {
		if (container != null) {
			for (OSMStreet street : container.getStreetList()) {
				if (street.hasAddresses() && !street.hasAssociatedStreetRelation()) {
					return true;
				}
			}
		}
		return false;
	}

}
