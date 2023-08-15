// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
    private JPanel panel;
    private LiveGpsStatus status = new LiveGpsStatus(LiveGpsStatus.GpsStatus.CONNECTING, tr("Connecting"));
    private LiveGpsStatus nmeaStatus = new LiveGpsStatus(LiveGpsStatus.GpsStatus.CONNECTING, tr("Connecting"));
    private LiveGpsData data;

    public LiveGpsDialog(final MapFrame mapFrame) {
        super(tr("Live GPS"), "livegps", tr("Show GPS data."),
        Shortcut.registerShortcut("subwindow:livegps", tr("Toggle: {0}", tr("Live GPS")),
        KeyEvent.VK_G, Shortcut.ALT_CTRL_SHIFT), 100);
        panel = new JPanel();
        panel.setLayout(new GridLayout(7, 2));
        panel.add(statusText = new JLabel(tr("Status gpsd")));
        panel.add(statusLabel = new JLabel());
        panel.add(nmeaStatusText = new JLabel(tr("Status NMEA")));
        panel.add(nmeaStatusLabel = new JLabel());
        panel.add(new JLabel(tr("Way Info")));
        panel.add(wayLabel = new JLabel());
        panel.add(latText = new JLabel(tr("Latitude")));
        panel.add(latLabel = new JLabel());
        panel.add(longText = new JLabel(tr("Longitude")));
        panel.add(longLabel = new JLabel());
        panel.add(new JLabel(tr("Speed")));
        panel.add(speedLabel = new JLabel());
        panel.add(new JLabel(tr("Course")));
        panel.add(courseLabel = new JLabel());
        setStatusVisibility(true);
        createLayout(panel, true, null);
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
                    panel.setBackground(Color.WHITE);
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
                        wayLabel.setText(wayString);
                    } else {
                        wayLabel.setText(tr("unknown"));
                    }
                } else {
                    latLabel.setText("");
                    longLabel.setText("");
                    speedLabel.setText("");
                    courseLabel.setText("");
                    panel.setBackground(Color.RED);
                }
            } });
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
                    panel.setBackground(Color.WHITE);
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
