package mirrored_download;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;

import org.xml.sax.SAXException;

public class UrlSelectionDialog
{
  private JDialog jDialog = null;
  private JTabbedPane tabbedPane = null;
  private JComboBox cbSelectUrl = null;

  public UrlSelectionDialog() {
    Frame frame = JOptionPane.getFrameForComponent(Main.parent);
    jDialog = new JDialog(frame, tr("Select OSM mirror URL"), false);
    tabbedPane = new JTabbedPane();
    JPanel tabSettings = new JPanel();
    tabbedPane.addTab(tr("Settings"), tabSettings);
    tabbedPane.setEnabledAt(0, true);
    jDialog.add(tabbedPane);

    //Settings Tab
    JPanel contentPane = tabSettings;
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints layoutCons = new GridBagConstraints();
    contentPane.setLayout(gridbag);

    JLabel label = new JLabel(tr("Base URL"));

    layoutCons.gridx = 0;
    layoutCons.gridy = 0;
    layoutCons.gridwidth = 2;
    layoutCons.weightx = 0.0;
    layoutCons.weighty = 0.0;
    layoutCons.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(label, layoutCons);
    contentPane.add(label);

    cbSelectUrl = new JComboBox();
    cbSelectUrl.setEditable(true);

    String preferredUrl = Main.pref.get("plugin.mirrored_download.preferred-url");
    if (preferredUrl != null && !"".equals(preferredUrl))
      cbSelectUrl.addItem(preferredUrl);

    for (String url: getURLs()) {
      cbSelectUrl.addItem(url);
    }

    cbSelectUrl.setActionCommand("selectURL");
    cbSelectUrl.addActionListener(new UrlChangedAction());

    layoutCons.gridx = 0;
    layoutCons.gridy = 1;
    layoutCons.gridwidth = 1;
    layoutCons.weightx = 0.0;
    layoutCons.weighty = 0.0;
    layoutCons.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(cbSelectUrl, layoutCons);
    contentPane.add(cbSelectUrl);

    jDialog.pack();
    jDialog.setLocationRelativeTo(frame);
  }

  private Collection<String> getURLs() {
    // List can be edited at http://josm.openstreetmap.de/wiki/MirroredDownloadInfo
    String src = Main.pref.get("plugin.mirrored_download.url-src", "http://josm.openstreetmap.de/mirrored_download_info");
    Collection<String> urls = new ArrayList<String>();
    InputStream in = null;
    try {
      in = new MirroredInputStream(src, 24*60*60);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.equals("")) {
          urls.add(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    Utils.close(in);
    for (String url : Main.pref.getCollection("plugin.mirrored_download.custom-urls")) {
      urls.add(url);
    }
    return urls;
  }

  public class UrlChangedAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      MirroredDownloadPlugin.setDownloadUrl(cbSelectUrl.getSelectedItem().toString());
      Main.pref.put("plugin.mirrored_download.preferred-url",
          cbSelectUrl.getSelectedItem().toString());
    }

  }

  public void setVisible(boolean visible) {
    jDialog.setVisible(visible);
  }

  private static UrlSelectionDialog singleton = null;

  public static UrlSelectionDialog getInstance() {

    if (singleton == null)
      singleton = new UrlSelectionDialog();

    return singleton;
  }
}
