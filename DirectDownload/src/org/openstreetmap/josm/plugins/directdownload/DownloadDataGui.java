// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.directdownload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;

public class DownloadDataGui extends ExtendedDialog {

    private final NamedResultTableModel model;
    private final JTable tblSearchResults;

    public DownloadDataGui() {
        // Initalizes ExtendedDialog
        super(MainApplication.getMainFrame(),
          tr("Download Track"),
          new String[] {tr("Download Track"), tr("Cancel")},
          true
          );

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new NamedResultTableModel();
        tblSearchResults = new JTable(model);
        tblSearchResults.setAutoCreateRowSorter(true);
        tblSearchResults.setSelectionModel(selectionModel);
        tblSearchResults.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tblSearchResults);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        panel.add(scrollPane, BorderLayout.CENTER);

        model.setData(new UserTrackReader().getTrackList());

        setContent(panel);
        setupDialog();
    }

    static class NamedResultTableModel extends DefaultTableModel {
        private ArrayList<UserTrack> data;

        NamedResultTableModel() {
            data = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            if (data == null) return 0;
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (data == null) return null;
            UserTrack track =  data.get(row);
            switch (column) {
            case 0:
                return track.datetime;
            case 1:
                return track.filename;
            case 2:
                return track.description;
            case 3:
                return String.join(";", track.tags);
            default:
                return track;
            }
        }

        public void setData(List<UserTrack> data) {
            if (data == null) {
                this.data.clear();
            } else {
                this.data = new ArrayList<>(data);
            }
            fireTableDataChanged();
        }

        public List<UserTrack> getDataArrayList() {
            return data;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return tr("Date");
            case 1:
                return tr("Filename");
            case 2:
                return tr("Description");
            case 3:
                return tr("Tags");
            default:
                throw new IllegalArgumentException("Unknown column");
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
            case 1:
            case 2:
            case 3:
                return String.class;
            default:
                throw new IllegalArgumentException("Unknown column");
            }
        }
    }

    public List<UserTrack> getSelectedUserTracks() {
        List<UserTrack> dataArray = model.getDataArrayList();
        int[] selected = tblSearchResults.getSelectedRows();
        List<UserTrack> selectedTracks = new ArrayList<>(selected.length);

        for (int i : selected) {
            selectedTracks.add(dataArray.get(tblSearchResults.convertRowIndexToModel(i)));
        }

        return selectedTracks;
    }

    public UserTrack getSelectedUserTrack() {
        return model.getDataArrayList().get(tblSearchResults.convertRowIndexToModel(tblSearchResults.getSelectedRow()));
    }
}
