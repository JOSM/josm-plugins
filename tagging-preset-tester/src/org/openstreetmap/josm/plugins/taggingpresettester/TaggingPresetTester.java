package org.openstreetmap.josm.plugins.taggingpresettester;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.tagging.TaggingCellRenderer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.xml.sax.SAXException;

public class TaggingPresetTester extends JFrame {

	private JComboBox taggingPresets;
	private final String[] args;
	private JPanel taggingPresetPanel = new JPanel(new BorderLayout());
	private JPanel panel = new JPanel(new BorderLayout());

	public void reload() {
		Vector<TaggingPreset> allPresets = new Vector<TaggingPreset>();
		for (String source : args) {
			InputStream in = null;
			try {
				if (source.startsWith("http") || source.startsWith("ftp") || source.startsWith("file"))
					in = new URL(source).openStream();
				else if (source.startsWith("resource://"))
					in = TaggingPresetTester.class.getResourceAsStream(source.substring("resource:/".length()));
				else
					in = new FileInputStream(source);
				allPresets.addAll(TaggingPreset.readAll(in));
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Could not read tagging preset source: "+source);
			} catch (SAXException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error parsing "+source+": "+e.getMessage());
			}

			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
		taggingPresets.setModel(new DefaultComboBoxModel(allPresets));
	}

	public void reselect() {
		taggingPresetPanel.removeAll();
		TaggingPreset preset = (TaggingPreset)taggingPresets.getSelectedItem();
		if (preset == null)
			return;
		JPanel p = preset.createPanel();
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		if (p != null)
			taggingPresetPanel.add(p, BorderLayout.NORTH);
		panel.validate();
		panel.repaint();
	}

	public TaggingPresetTester(String[] args) {
		super("Tagging Preset Tester");
		this.args = args;
		taggingPresets = new JComboBox();
		taggingPresets.setRenderer(new TaggingCellRenderer());
		reload();

		panel.add(taggingPresets, BorderLayout.NORTH);
		panel.add(taggingPresetPanel, BorderLayout.CENTER);
		taggingPresets.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				reselect();
			}
		});
		reselect();

		JButton b = new JButton("Reload");
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int i = taggingPresets.getSelectedIndex();
				reload();
				taggingPresets.setSelectedIndex(i);
			}
		});
		panel.add(b, BorderLayout.SOUTH);

		setContentPane(panel);
		setSize(300,500);
		setVisible(true);
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			JFileChooser c = new JFileChooser();
			if (c.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;
			args = new String[]{c.getSelectedFile().getPath()};
		}
		JFrame f = new TaggingPresetTester(args);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
