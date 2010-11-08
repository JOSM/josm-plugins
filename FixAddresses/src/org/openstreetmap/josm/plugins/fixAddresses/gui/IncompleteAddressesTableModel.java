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
package org.openstreetmap.josm.plugins.fixAddresses.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.plugins.fixAddresses.AddressEditContainer;
import org.openstreetmap.josm.plugins.fixAddresses.OSMAddress;
import org.openstreetmap.josm.plugins.fixAddresses.IOSMEntity;

public class IncompleteAddressesTableModel extends AddressEditTableModel  {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5951629033395186324L;
	
	// TODO: Add "state" colum, if required
	private static final int NUMBER_OF_COLUMNS = 5;
	private static final String[] COLUMN_NAMES = new String[]{tr("Country"), tr("City"), tr("Postcode"), tr("Street"), tr("Number")}; 
	private static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{
		String.class, String.class, String.class, String.class, String.class, String.class};
	
	
	/**
	 * Instantiates a new incomplete addresses table model.
	 *
	 * @param addressContainer the address container used for display
	 */
	public IncompleteAddressesTableModel(AddressEditContainer addressContainer) {
		super(addressContainer);
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return NUMBER_OF_COLUMNS;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		if (addressContainer == null || addressContainer.getIncompleteAddresses() == null) {
			return 0;
		}
		return addressContainer.getNumberOfIncompleteAddresses();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int row, int column) {		
		OSMAddress aNode = (OSMAddress) getEntityOfRow(row);
		
		if (aNode == null) {
			return null;
		}
		
		switch (column) {
		case 0:
			return aNode.getCountry();
		case 1:
			return aNode.getCity();
		case 2:
			return aNode.getPostCode();
		case 3:
			return aNode.getStreetName();			
		case 4:
			return aNode.getHouseNumber();
		default:
			throw new RuntimeException("Invalid column index: " + column);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int arg0) {
		return COLUMN_CLASSES[arg0];
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.josm.plugins.fixAddresses.gui.AddressEditTableModel#getEntityOfRow(int)
	 */
	@Override
	public IOSMEntity getEntityOfRow(int row) {
		if (addressContainer == null || addressContainer.getIncompleteAddresses() == null) {
			return null;
		}
		if (row < 0 || row >= addressContainer.getNumberOfIncompleteAddresses()) {
			return null;
		}
		return addressContainer.getIncompleteAddresses().get(row);
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.josm.plugins.fixAddresses.gui.AddressEditTableModel#getRowOfEntity(org.openstreetmap.josm.plugins.fixAddresses.IOSMEntity)
	 */
	@Override
	public int getRowOfEntity(IOSMEntity entity) {
		if (addressContainer == null || addressContainer.getIncompleteAddresses() == null) {
			return -1;
		}
		
		return addressContainer.getIncompleteAddresses().indexOf(entity);
	}
}
