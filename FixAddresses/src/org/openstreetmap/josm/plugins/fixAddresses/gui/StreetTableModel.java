// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fixAddresses.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;

import org.openstreetmap.josm.plugins.fixAddresses.AddressEditContainer;
import org.openstreetmap.josm.plugins.fixAddresses.IOSMEntity;
import org.openstreetmap.josm.plugins.fixAddresses.OSMStreet;

@SuppressWarnings("serial")
public class StreetTableModel extends AddressEditTableModel {
    private static final int NUMBER_OF_COLUMNS = 3;
    private static final String[] COLUMN_NAMES = new String[]{tr("Type"), tr("Name"), tr("Addresses")};
    private static final Class<?>[] COLUMN_CLASSES = new Class<?>[]{String.class, String.class, Integer.class};
    /**
     * @param addressContainer
     */
    public StreetTableModel(AddressEditContainer addressContainer) {
        super(addressContainer);
    }

    @Override
    public int getColumnCount() {
        // TODO Auto-generated method stub
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex];
    }

    @Override
    public int getRowCount() {
        if (addressContainer == null || addressContainer.getStreetList() == null) {
            return 0;
        }
        return addressContainer.getNumberOfStreets();
    }

    @Override
    public Object getValueAt(int row, int column) {
        OSMStreet sNode = (OSMStreet) getEntityOfRow(row);

        if (sNode == null) {
            return null;
        }

        switch (column) {
        case 0:
            return sNode.getType();
        case 1:
            return sNode.getName();
        case 2:
            return sNode.getNumberOfSegments();
        case 3:
            return sNode.getNumberOfAddresses();
        case 4:
            return sNode.hasAssociatedStreetRelation();
        default:
            throw new RuntimeException("Invalid column index: " + column);
        }

    }

    @Override
    public boolean isCellEditable(int row, int column) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IOSMEntity getEntityOfRow(int row) {
        if (addressContainer == null || addressContainer.getStreetList() == null) {
            return null;
        }
        if (row < 0 || row >= addressContainer.getNumberOfStreets()) {
            return null;
        }
        return addressContainer.getStreetList().get(row);
    }

    @Override
    public int getRowOfEntity(IOSMEntity entity) {
        if (addressContainer == null || addressContainer.getStreetList() == null) {
            return -1;
        }

        return addressContainer.getStreetList().indexOf(entity);
    }

    @Override
    protected void sortByColumn(int column, boolean ascending) {
        Collections.sort(addressContainer.getStreetList(), new StreetModelSorter(column, ascending));
    }

    /**
     * Internal class StreetModelSorter.
     */
    class StreetModelSorter extends ColumnSorter<OSMStreet> {

        public StreetModelSorter(int column, boolean asc) {
            super(column, asc);
        }

        public int compare(OSMStreet arg0, OSMStreet arg1) {
            if (arg0 == null || arg1 == null) return 0;

            switch (getColumn()) {
            case 0:
                if (arg0.getType() != null) {
                    return arg0.getType().compareTo(arg1.getType());
                } else {
                    return arg1.hasName() ? -1 : 0;
                }
            case 1:
                if (arg0.hasName()) {
                    return arg0.getName().compareTo(arg1.getName());
                } else {
                    return arg1.hasName() ? -1 : 0;
                }
            case 2:
                return new Integer(arg0.getNumberOfAddresses()).
                                compareTo(new Integer(arg1.getNumberOfAddresses()));
            default:
            }
            return 0;
        }
    }
}
