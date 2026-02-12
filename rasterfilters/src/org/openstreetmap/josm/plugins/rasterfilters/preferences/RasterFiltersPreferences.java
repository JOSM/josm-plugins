package org.openstreetmap.josm.plugins.rasterfilters.preferences;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.EAST;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * This class draws subtab 'Image Filters' in the Preferences menu.
 *
 * @author Nipel-Crumple
 */
public class RasterFiltersPreferences implements SubPreferenceSetting {

    private AbstractTableModel model;
    private JPanel holder;

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        model = new FiltersTableModel();

        if (holder == null) {
            holder = new JPanel(new GridBagLayout());
            holder.setBorder(new EmptyBorder(10, 10, 10, 10));
            model.addTableModelListener(e -> {
                int row = e.getFirstRow();
                int col = e.getColumn();
                TableModel tmodel = (TableModel) e.getSource();
                ((FiltersTableModel) tmodel).filtersInfoList.get(row).setNeedToDownload((Boolean) tmodel.getValueAt(row, col));
            });

            JTable table = new JTable(model);
            table.getTableHeader().setReorderingAllowed(false);
            table.getColumnModel().getColumn(3).setMaxWidth(20);
            JScrollPane pane = new JScrollPane(table);

            holder.add(pane, GBC.eol().fill(BOTH));

            JButton download = new JButton(tr("Download"));
            download.addActionListener(new FiltersDownloader());
            holder.add(download, GBC.eol().anchor(EAST));
        }

        getTabPreferenceSetting(gui).addSubTab(this, tr("Image Filters"), holder);
    }

    @Override
    public boolean ok() {
        for (FilterInfo temp : ((FiltersTableModel) model).getFiltersInfoList()) {
            Config.getPref().putBoolean("rasterfilters." + temp.getMeta().getString("name"), temp.isNeedToDownload());
        }
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getImageryPreference();
    }

    private static class FiltersTableModel extends AbstractTableModel {

        private final String[] columnNames = {tr("Filter Name"), tr("Author"), tr("Description"), ""};
        private final Class<?>[] columnClasses = {String.class, String.class, String.class, Boolean.class};
        private final List<FilterInfo> filtersInfoList = FiltersDownloader.downloadFiltersInfoList();
        private final Object[][] data = new Object[filtersInfoList.size()][4];

        FiltersTableModel() {
            for (int i = 0; i < filtersInfoList.size(); i++) {
                data[i][0] = filtersInfoList.get(i).getName();
                data[i][1] = filtersInfoList.get(i).getOwner();
                data[i][2] = filtersInfoList.get(i).getDescription();
                data[i][3] = filtersInfoList.get(i).isNeedToDownload();
            }
        }

        @Override
        public int getRowCount() {
            return filtersInfoList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return filtersInfoList.get(rowIndex).getName();
                case 1:
                    return filtersInfoList.get(rowIndex).getOwner();
                case 2:
                    return filtersInfoList.get(rowIndex).getDescription();
                case 3:
                    return filtersInfoList.get(rowIndex).isNeedToDownload();
                default:
                    return null;
            }
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return columnClasses[col];
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 3;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 3) {
                filtersInfoList.get(row).setNeedToDownload((boolean) value);
                fireTableCellUpdated(row, col);
            }
        }

        public List<FilterInfo> getFiltersInfoList() {
            return filtersInfoList;
        }
    }
}
