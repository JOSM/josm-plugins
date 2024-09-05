// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ProjectedCoordinateFormat;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The LiveGPS dialog window showing the current data and status
 * @author cdaller
 */
public class LiveGpsDialog extends ToggleDialog implements PropertyChangeListener {
    private static final long serialVersionUID = 6183400754671501117L;
    private boolean statusGPSD;
    private boolean statusNMEA;
    private JLabel statusText;
    private JLabel statusLabel;
    private JLabel nmeaStatusText;
    private JLabel nmeaStatusLabel;
    private JLabel wayLabel;
    private JLabel latText;
    private JLabel latLabel;
    private JLabel longText;
    private JLabel longLabel;
    private JLabel courseLabel;
    private JLabel speedLabel;
    private JLabel wayText;
    private JPanel panel;
    private JPanel opanel;
    private Color backgroundColor;
    private LiveGpsStatus status = new LiveGpsStatus(LiveGpsStatus.GpsStatus.CONNECTING, tr("Connecting"));
    private LiveGpsStatus nmeaStatus = new LiveGpsStatus(LiveGpsStatus.GpsStatus.CONNECTING, tr("Connecting"));
    private LiveGpsData data;
    private CirclePanel circlePanel;

    private static volatile LiveGpsDialog dialog;

    public LiveGpsDialog(final MapFrame mapFrame) {
        super(tr("Live GPS"), "livegps", tr("Show GPS data."),
        Shortcut.registerShortcut("subwindow:livegps", tr("Toggle: {0}", tr("Live GPS")),
        KeyEvent.VK_G, Shortcut.ALT_CTRL_SHIFT), 100);

        dialog = this;

        backgroundColor = UIManager.getColor("Panel.background");

        opanel = new JPanel();
        opanel.setLayout(new BorderLayout());

        panel = new JPanel();

        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();

        // first column
        gbc.weightx = 0.3;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(statusText = new JLabel("Status gpsd "), gbc);
        panel.add(nmeaStatusText = new JLabel("Status NMEA "), gbc);
        panel.add(wayText = new JLabel("Way Info"), gbc);
        panel.add(latText = new JLabel("Latitude"), gbc);
        panel.add(longText = new JLabel("Longitude"), gbc);
        panel.add(new JLabel(tr("Speed")), gbc);
        panel.add(new JLabel(tr("Course")), gbc);

        // second column
        gbc.weightx = 0.7;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(statusLabel = new JLabel(), gbc);
        panel.add(nmeaStatusLabel = new JLabel(), gbc);
        panel.add(wayLabel = new JLabel(), gbc);
        panel.add(latLabel = new JLabel(), gbc);
        panel.add(longLabel = new JLabel(), gbc);
        panel.add(speedLabel = new JLabel(), gbc);
        panel.add(courseLabel = new JLabel(), gbc);

        opanel.add(panel, BorderLayout.NORTH);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateCirclePanelVisibility();
            }
        });

        circlePanel = new CirclePanel(0);
        opanel.add(circlePanel, BorderLayout.CENTER);

        setStatusVisibility(true);
        if (Config.getPref().getBoolean(LiveGPSPreferences.C_WAYOFFSET, false)) {
            /* I18N: way information with offset (in m) enabled */
            wayText.setText(tr("Way Info [m]"));
        } else {
            wayText.setText(tr("Way Info"));
        }
        createLayout(opanel, true, null);
    }

    public static void updateCirclePanelVisibility() {
        if (dialog != null) {
            boolean vis = Config.getPref().getBoolean(LiveGPSPreferences.C_DISTANCE_VISUALISATION, false);

            dialog.circlePanel.setVisible(vis);

            dialog.circlePanel.revalidate();
            dialog.circlePanel.repaint();
        }
    }

    /**
     * Set the visibility of the status fields
     * @param init initialize the values (don't check previous state)
     */
    private void setStatusVisibility(boolean init) {
        boolean statusGPSDNew = !Config.getPref().getBoolean(LiveGPSPreferences.C_DISABLED);
        if (init || statusGPSD != statusGPSDNew) {
            statusText.setVisible(statusGPSDNew);
            statusLabel.setVisible(statusGPSDNew);
            statusGPSD = statusGPSDNew;
        }
        boolean statusNMEANew = !Config.getPref().get(LiveGPSPreferences.C_SERIAL).isEmpty();
        if (init || statusNMEA != statusNMEANew) {
            nmeaStatusText.setVisible(statusNMEANew);
            nmeaStatusLabel.setVisible(statusNMEANew);
            statusNMEA = statusNMEANew;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!isVisible())
            return;

        if ("gpsdata".equals(evt.getPropertyName())) {
            data = (LiveGpsData) evt.getNewValue();

            SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (data.isFix()) {
                    panel.setBackground(backgroundColor);
                    ICoordinateFormat mCord = CoordinateFormatManager.getDefaultFormat();
                    if (ProjectedCoordinateFormat.INSTANCE.equals(mCord)) {
                        latText.setText(tr("Northing"));
                        longText.setText(tr("Easting"));
                    } else {
                        latText.setText(tr("Latitude"));
                        longText.setText(tr("Longitude"));
                    }

                    latLabel.setText(mCord.latToString(data.getLatLon()));
                    longLabel.setText(mCord.lonToString(data.getLatLon()));
                    double mySpeed = data.getSpeed() * 3.6f;
                    speedLabel.setText(tr("{0} km/h", Math.round(mySpeed*100)/100));
                    courseLabel.setText(tr("{0} deg", data.getCourse()));

                    String wayString = data.getWayInfo();
                    if (!wayString.isEmpty()) {
                        wayLabel.setText(tr("<html><body width={0}>{1}</html>", (int) getWidth()*0.8, wayString));

                        circlePanel.setOffset(data.getOffset());
                        circlePanel.setBackground(backgroundColor);
                        circlePanel.validate();
                        circlePanel.repaint();

                    } else {
                        wayLabel.setText(tr("unknown"));
                    }
                    if (Config.getPref().getBoolean(LiveGPSPreferences.C_WAYOFFSET, false)) {
                        /* I18N: way information with offset (in m) enabled */
                        wayText.setText(tr("Way Info [m]"));
                    } else {
                        wayText.setText(tr("Way Info"));
                    }

                } else {
                    latLabel.setText("");
                    longLabel.setText("");
                    speedLabel.setText("");
                    courseLabel.setText("");
                    panel.setBackground(Color.RED);
                }
            }

            });
        } else if ("gpsstatus".equals(evt.getPropertyName())) {
            LiveGpsStatus oldStatus = status;
            status = (LiveGpsStatus) evt.getNewValue();

            setStatusVisibility(false);

            SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                /* prevent flickering - skip the connecting message when NMEA input is working */
                if (!(oldStatus.getStatus() == LiveGpsStatus.GpsStatus.CONNECTION_FAILED
                && status.getStatus() == LiveGpsStatus.GpsStatus.CONNECTING
                && nmeaStatus.getStatus() == LiveGpsStatus.GpsStatus.CONNECTED))
                    statusLabel.setText(status.getStatusMessage());
                if (status.getStatus() != LiveGpsStatus.GpsStatus.CONNECTED
                && nmeaStatus.getStatus() != LiveGpsStatus.GpsStatus.CONNECTED) {
                    panel.setBackground(Color.RED);
                } else {
                    panel.setBackground(backgroundColor);
                }
            } });
        } else if ("nmeastatus".equals(evt.getPropertyName())) {
            nmeaStatus = (LiveGpsStatus) evt.getNewValue();

            setStatusVisibility(false);

            SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                nmeaStatusLabel.setText(nmeaStatus.getStatusMessage());
            } });
        }
    }
}
