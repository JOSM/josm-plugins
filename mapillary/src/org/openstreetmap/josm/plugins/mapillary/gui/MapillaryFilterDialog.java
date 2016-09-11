// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryDataListener;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImportedImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.MapillarySign;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * ToggleDialog that lets you filter the images that are being shown.
 *
 * @author nokutu
 * @see MapillaryFilterChooseSigns
 */
public class MapillaryFilterDialog extends ToggleDialog implements MapillaryDataListener {

  private static final long serialVersionUID = -4192029663670922103L;

  private static MapillaryFilterDialog instance;

  private static final String[] TIME_LIST = {tr("All"), tr("Years"), tr("Months"), tr("Days")};

  /**
   * Spinner to choose the range of dates.
   */
  private final SpinnerNumberModel spinner;

  private final JCheckBox imported = new JCheckBox(tr("Imported images"));
  private final JCheckBox downloaded = new JCheckBox(new DownloadCheckBoxAction());
  private final JCheckBox onlySigns = new JCheckBox(new OnlySignsAction());
  private final JComboBox<String> time;
  private final JTextField user;

  private final SideButton updateButton = new SideButton(new UpdateAction());
  private final SideButton resetButton = new SideButton(new ResetAction());
  private final JButton signChooser = new JButton(new SignChooserAction());

  private final MapillaryFilterChooseSigns signFilter = MapillaryFilterChooseSigns.getInstance();

  /**
   * The list of sign names
   */
  private static final String[] SIGN_TAGS = {"prohibitory--maximum-speed-limit",
    "regulatory|priority--stop", "regulatory|priority--give_way|yield", "warning|mandatory--roundabout",
    "prohibitory|regulatory--no-entry|no-traffic-both-ways",
    "crossroads|junction", "mandatory--turn|straight", "uneven|slippery",
    "no-parking", "no_overtaking",
    "danger_pedestrian_crossing", "no_*_turn"};
  /**
   * The {@link JCheckBox} where the respective tag should be searched
   */
  private final JCheckBox[] SIGN_CHECKBOXES = {this.signFilter.maxSpeed,
    this.signFilter.stop, this.signFilter.giveWay,
    this.signFilter.roundabout, this.signFilter.access, this.signFilter.intersection,
    this.signFilter.direction, this.signFilter.uneven,
    this.signFilter.noParking, this.signFilter.noOvertaking,
    this.signFilter.crossing, this.signFilter.noTurn};

  private MapillaryFilterDialog() {
    super(tr("Mapillary filter"), "mapillary-filter.svg",
      tr("Open Mapillary filter dialog"), Shortcut.registerShortcut(
        tr("Mapillary filter"), tr("Open Mapillary filter dialog"),
        KeyEvent.VK_M, Shortcut.NONE), 200);

    this.signChooser.setEnabled(false);
    JPanel signChooserPanel = new JPanel();
    signChooserPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    signChooserPanel.add(this.signChooser);

    JPanel fromPanel = new JPanel();
    fromPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    fromPanel.add(new JLabel(tr("Not older than: ")));
    this.spinner = new SpinnerNumberModel(1, 0, 10000, 1);
    fromPanel.add(new JSpinner(this.spinner));
    this.time = new JComboBox<>(TIME_LIST);
    fromPanel.add(this.time);

    JPanel userSearchPanel = new JPanel();
    userSearchPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    this.user = new JTextField(10);
    this.user.addActionListener(new UpdateAction());
    userSearchPanel.add(new JLabel(tr("User")));
    userSearchPanel.add(this.user);

    this.imported.setSelected(true);
    this.downloaded.setSelected(true);

    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.LINE_START;
    panel.add(this.downloaded, c);
    c.gridx = 1;
    panel.add(this.imported, c);
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    panel.add(fromPanel, c);
    c.gridy = 2;
    panel.add(userSearchPanel, c);
    c.gridwidth = 1;
    c.gridy = 3;
    panel.add(this.onlySigns, c);
    c.gridx = 1;
    panel.add(signChooserPanel, c);

    createLayout(panel, true, Arrays.asList(new SideButton[]{updateButton, resetButton}));
  }

  /**
   * @return the unique instance of the class.
   */
  public static synchronized MapillaryFilterDialog getInstance() {
    if (instance == null)
      instance = new MapillaryFilterDialog();
    return instance;
  }

  @Override
  public void imagesAdded() {
    refresh();
  }

  @Override
  public void selectedImageChanged(MapillaryAbstractImage oldImage, MapillaryAbstractImage newImage) {
    // Do nothing when image selection changed
  }

  /**
   * Resets the dialog to its default state.
   */
  public void reset() {
    this.imported.setSelected(true);
    this.downloaded.setSelected(true);
    this.onlySigns.setEnabled(true);
    this.onlySigns.setSelected(false);
    this.user.setText("");
    this.time.setSelectedItem(TIME_LIST[0]);
    this.spinner.setValue(1);
    refresh();
  }

  /**
   * Applies the selected filter.
   */
  public synchronized void refresh() {
    boolean imported = this.imported.isSelected();
    boolean downloaded = this.downloaded.isSelected();
    boolean onlySigns = this.onlySigns.isSelected();

    // This predicate returns true is the image should be made invisible
    Predicate<MapillaryAbstractImage> p =
      img ->
        (img instanceof MapillaryImportedImage && !imported) ||
          (img instanceof MapillaryImage &&
            (!downloaded ||
              (onlySigns && (((MapillaryImage) img).getSigns().isEmpty() || !checkSigns((MapillaryImage) img))) ||
              (!user.getText().equals("") && !this.user.getText().equals(((MapillaryImage) img).getUser())))) ||
          checkValidTime(img);

    MapillaryLayer.getInstance().getData().getImages().parallelStream().forEach(img -> img.setVisible(!p.test(img)));

    Main.map.repaint();
  }

  private boolean checkValidTime(MapillaryAbstractImage img) {
    Long currentTime = currentTime();
    long[] timeFactor = new long[]{
      31_536_000_000L, // = 365 * 24 * 60 * 60 * 1000 = number of ms in a year
      2_592_000_000L, // = 30 * 24 * 60 * 60 * 1000 = number of ms in a month
      86_400_000 // = 24 * 60 * 60 * 1000 = number of ms in a day
    };
    for (int i = 1; i <= 3; i++) {
      if (TIME_LIST[i].equals(time.getSelectedItem())
        && img.getCapturedAt() < currentTime - ((Integer) spinner.getValue()).longValue() * timeFactor[i - 1]
        ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the image fulfills the sign conditions.
   *
   * @param img The {@link MapillaryAbstractImage} object that is going to be
   * checked.
   *
   * @return {@code true} if it fulfills the conditions; {@code false}
   * otherwise.
   */
  private boolean checkSigns(MapillaryImage img) {
    for (int i = 0; i < SIGN_TAGS.length; i++) {
      if (checkSign(img, this.SIGN_CHECKBOXES[i], SIGN_TAGS[i]))
        return true;
    }
    return false;
  }

  private static boolean checkSign(MapillaryImage img, JCheckBox signCheckBox, String signTag) {
    boolean contains = false;
    for (MapillarySign sign : img.getSigns()) {
      String[] parts = signTag.split("--");
      if (Pattern.compile(parts[0]).matcher(sign.getCategory()).find() &&
        Pattern.compile(parts[1]).matcher(sign.getType()).find()) {
        contains = true;
      }
    }
    return contains == signCheckBox.isSelected() && contains;
  }

  private static long currentTime() {
    Calendar cal = Calendar.getInstance();
    return cal.getTimeInMillis();
  }

  /**
   * Destroys the unique instance of the class.
   */
  public static synchronized void destroyInstance() {
    instance = null;
  }

  private class DownloadCheckBoxAction extends AbstractAction {

    private static final long serialVersionUID = 4672634002899519496L;

    DownloadCheckBoxAction() {
      putValue(NAME, tr("Downloaded images"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      onlySigns.setEnabled(downloaded.isSelected());
    }
  }

  private static class UpdateAction extends AbstractAction {

    private static final long serialVersionUID = -7417238601979689863L;

    UpdateAction() {
      putValue(NAME, tr("Update"));
      new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      MapillaryFilterDialog.getInstance().refresh();
    }
  }

  private static class ResetAction extends AbstractAction {
    /**
     *
     */
    private static final long serialVersionUID = 1178261778165525040L;

    ResetAction() {
      putValue(NAME, tr("Reset"));
      new ImageProvider("preferences", "reset").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      MapillaryFilterDialog.getInstance().reset();
    }
  }

  private class OnlySignsAction extends AbstractAction {

    private static final long serialVersionUID = -2937440338019185723L;

    OnlySignsAction() {
      putValue(NAME, tr("Only images with signs"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      signChooser.setEnabled(onlySigns.isSelected());
    }
  }

  /**
   * Opens a new window where you can specifically filter signs.
   *
   * @author nokutu
   */
  private static class SignChooserAction extends AbstractAction {

    private static final long serialVersionUID = 8706299665735930148L;

    SignChooserAction() {
      putValue(NAME, tr("Choose signs"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      JPanel dialog = MapillaryFilterChooseSigns.getInstance();
      JOptionPane pane = new JOptionPane(dialog, JOptionPane.PLAIN_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION);
      JDialog dlg = pane.createDialog(Main.parent, tr("Choose signs"));
      dlg.setVisible(true);
      if ((int) pane.getValue() == JOptionPane.OK_OPTION)
        MapillaryFilterDialog.getInstance().refresh();
      dlg.dispose();
    }
  }
}
