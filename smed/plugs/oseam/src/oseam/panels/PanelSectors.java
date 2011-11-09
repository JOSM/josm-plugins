package oseam.panels;

import java.awt.*;
import java.awt.event.*;
import java.util.EnumMap;

import javax.swing.*;
import javax.swing.table.*;

import oseam.Messages;
import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.*;

public class PanelSectors extends JFrame {

	private OSeaMAction dlg;
	private JPanel panel;
	private TableModel model;
	private JTable table;

	public JButton minusButton;
	private ActionListener alMinusButton = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if ((getSectorCount() > 1) && (table.getSelectedRow() != 0))
				deleteSector(table.getSelectedRow());
		}
	};
	public JButton plusButton;
	private ActionListener alPlusButton = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (table.getSelectedRow() < 0)
				addSector(table.getRowCount());
			else
				addSector(table.getSelectedRow()+1);
		}
	};
	public JComboBox colourBox;
	public EnumMap<Col, ImageIcon> colours = new EnumMap<Col, ImageIcon>(Col.class);
	public JComboBox visibilityBox;
	public EnumMap<Vis, String> visibilities = new EnumMap<Vis, String>(Vis.class);
	public JComboBox exhibitionBox;
	public EnumMap<Exh, String> exhibitions = new EnumMap<Exh, String>(Exh.class);

	public PanelSectors(OSeaMAction dia) {
		super("Sector Table");
		dlg = dia;
		this.setSize(900, 100);
		this.setVisible(true);
		this.setAlwaysOnTop(true);
		this.setLocation(450, 0);
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.setLayout(null);
		minusButton = new JButton(new ImageIcon(getClass().getResource("/images/MinusButton.png")));
		minusButton.setBounds(0, 0, 32, 34);
		minusButton.addActionListener(alMinusButton);
		this.add(minusButton);
		plusButton = new JButton(new ImageIcon(getClass().getResource("/images/PlusButton.png")));
		plusButton.setBounds(0, 34, 32, 34);
		plusButton.addActionListener(alPlusButton);
		this.add(plusButton);
		panel = new JPanel(new BorderLayout());
		panel.setBounds(40, 0, 860, 512);
		model = new SectorTable();
		table = new JTable(model);
		table.setBounds(0, 0, 860, 34);
		panel.add(new JScrollPane(table));
		this.getContentPane().add(panel);
		
		table.setDefaultRenderer(String.class, new CentreRenderer());
		table.getColumnModel().getColumn(1).setCellRenderer(new ColourCellRenderer());

		TableColumn colColumn = table.getColumnModel().getColumn(1);
		colourBox = new JComboBox();
		addColItem(new ImageIcon(getClass().getResource("/images/DelButton.png")), Col.UNKNOWN);
		addColItem(new ImageIcon(getClass().getResource("/images/WhiteButton.png")), Col.WHITE);
		addColItem(new ImageIcon(getClass().getResource("/images/RedButton.png")), Col.RED);
		addColItem(new ImageIcon(getClass().getResource("/images/GreenButton.png")), Col.GREEN);
		addColItem(new ImageIcon(getClass().getResource("/images/YellowButton.png")), Col.YELLOW);
		addColItem(new ImageIcon(getClass().getResource("/images/OrangeButton.png")), Col.ORANGE);
		addColItem(new ImageIcon(getClass().getResource("/images/AmberButton.png")), Col.AMBER);
		addColItem(new ImageIcon(getClass().getResource("/images/BlueButton.png")), Col.BLUE);
		addColItem(new ImageIcon(getClass().getResource("/images/VioletButton.png")), Col.VIOLET);
		colColumn.setCellEditor(new DefaultCellEditor(colourBox));
		
		TableColumn visColumn = table.getColumnModel().getColumn(12);
		visibilityBox = new JComboBox();
		addVisibItem("", Vis.UNKNOWN);
		addVisibItem(Messages.getString("Intensified"), Vis.INTEN);
		addVisibItem(Messages.getString("Unintensified"), Vis.UNINTEN);
		addVisibItem(Messages.getString("PartiallyObscured"), Vis.PARTOBS);
		visColumn.setCellEditor(new DefaultCellEditor(visibilityBox));
		
		TableColumn exhColumn = table.getColumnModel().getColumn(13);
		exhibitionBox = new JComboBox();
		addExhibItem("", Exh.UNKNOWN);
		addExhibItem(Messages.getString("24h"), Exh.H24);
		addExhibItem(Messages.getString("Day"), Exh.DAY);
		addExhibItem(Messages.getString("Night"), Exh.NIGHT);
		addExhibItem(Messages.getString("Fog"), Exh.FOG);
		exhColumn.setCellEditor(new DefaultCellEditor(exhibitionBox));
	}

	private class SectorTable extends AbstractTableModel {

		private String[] headings = { "Sector", "Colour", "Character", "Group", "Sequence", "Period",
				"Directional", "Start", "End", "Radius", "Height", "Range", "Visibility", "Exhibition" };

		public SectorTable() {
		}

		public String getColumnName(int col) {
			return headings[col];
		}

		public int getColumnCount() {
			return headings.length;
		}

		public int getRowCount() {
			return dlg.mark.getSectorCount();
		}

		public boolean isCellEditable(int row, int col) {
			return ((col > 0) && (row > 0));
		}

		public Class getColumnClass(int col) {
			switch (col) {
			case 1:
				return Col.class;
			case 6:
				return Boolean.class;
			default:
				return String.class;
			}
		}

		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				if (row == 0)
					return "Common";
				else
					return row;
			case 1:
				return dlg.mark.getLightAtt(Att.COL, row);
			case 6:
				return (dlg.mark.getLightAtt(Att.LIT, row) == Lit.DIR);
			case 7:
			case 8:
				if (dlg.mark.getLightAtt(Att.LIT, row) == Lit.DIR)
					return dlg.mark.getLightAtt(Att.ORT, row);
				else
					return dlg.mark.getLightAtt(col - 1, row);
			case 12:
				return visibilities.get(dlg.mark.getLightAtt(Att.VIS, row));
			case 13:
				return exhibitions.get(dlg.mark.getLightAtt(Att.EXH, row));
			default:
				return dlg.mark.getLightAtt(col - 1, row);
			}
		}

		public void setValueAt(Object value, int row, int col) {
			switch (col) {
			case 1:
				for (Col colour : colours.keySet()) {
					ImageIcon img = colours.get(colour);
					if (img == value)
						dlg.mark.setLightAtt(Att.COL, row, colour);
				}
				break;
			case 5:
			case 9:
			case 10:
			case 11:
				dlg.mark.setLightAtt(col - 1, row, dlg.mark.validDecimal((String) value));
				break;
			case 6:
				if ((Boolean) value == true) {
					dlg.mark.setLightAtt(Att.LIT, row, Lit.DIR);
				} else {
					dlg.mark.setLightAtt(Att.LIT, row, Lit.UNKNOWN);
					dlg.mark.setLightAtt(Att.ORT, row, "");
				}
				break;
			case 7:
			case 8:
				if (dlg.mark.getLightAtt(Att.LIT, row) == Lit.DIR) {
					dlg.mark.setLightAtt(Att.ORT, row, dlg.mark.validDecimal((String) value));
				} else {
					dlg.mark.setLightAtt(Att.LIT, row, Lit.UNKNOWN);
					dlg.mark.setLightAtt(col - 1, row, dlg.mark.validDecimal((String) value));
				}
				break;
			case 12:
				for (Vis vis : visibilities.keySet()) {
					String str = visibilities.get(vis);
					if (str.equals(value))
						dlg.mark.setLightAtt(Att.VIS, row, vis);
				}
				break;
			case 13:
				for (Exh exh : exhibitions.keySet()) {
					String str = exhibitions.get(exh);
					if (str.equals(value))
						dlg.mark.setLightAtt(Att.EXH, row, exh);
				}
				break;
			default:
				dlg.mark.setLightAtt(col - 1, row, value);
			}
		}
	}

	static class CentreRenderer extends DefaultTableCellRenderer {
		public CentreRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.CENTER);
		}
	}

	public class ColourCellRenderer extends JLabel implements TableCellRenderer {
		public ColourCellRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.CENTER);
		}
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
			setIcon(colours.get(value));
			return this;
		}
	}

	public int getSectorCount() {
		return model.getRowCount();
	}

	public void addSector(int idx) {
		dlg.mark.addLight(idx);
		table.setSize(860, ((table.getRowCount() * 16) + 18));
		if (table.getRowCount() > 3) {
			this.setSize(900, ((table.getRowCount() * 16) + 40));
		} else {
			this.setSize(900, 100);
		}
	}

	public void deleteSector(int idx) {
		dlg.mark.delLight(idx);
		table.setSize(860, ((table.getRowCount() * 16) + 18));
		if (table.getRowCount() > 3) {
			this.setSize(900, ((table.getRowCount() * 16) + 40));
		} else {
			this.setSize(900, 100);
		}
	}

	private void addColItem(ImageIcon img, Col col) {
		colours.put(col, img);
		colourBox.addItem(img);
	}

	private void addVisibItem(String str, Vis vis) {
		visibilities.put(vis, str);
		visibilityBox.addItem(str);
	}

	private void addExhibItem(String str, Exh exh) {
		exhibitions.put(exh, str);
		exhibitionBox.addItem(str);
	}

}
