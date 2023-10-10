// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preferences of LiveGPS
 */
public class LiveGPSPreferences extends DefaultTabPreferenceSetting {
    /* option to use serial port direct access */
    public static final String C_SERIAL = "livegps.serial.port";
    /* default gpsd host address */
    public static final String DEFAULT_HOST = "localhost";
    /* option to use specify gpsd host address */
    public static final String C_HOST = "livegps.gpsd.host";
    /* default gpsd port number */
    public static final int DEFAULT_PORT = 2947;
    /* option to use specify gpsd port number */
    public static final String C_PORT = "livegps.gpsd.port";
    /* option to use specify gpsd disabling */
    public static final String C_DISABLED = "livegps.gpsd.disabled";

    public static final String C_LIVEGPS_COLOR_POSITION = "color.livegps.position";
    public static final String C_LIVEGPS_COLOR_POSITION_ESTIMATE = "color.livegps.position_estimate";

    /* options below are hidden/expert options */

    /* option to use even duplicate positions (default false) */
    public static final String C_ALLPOSITIONS = "livegps.positions.all";
    /* option to show offset to next way (default false) */
    public static final String C_WAYOFFSET = "livegps.way.offset";

    public static final String C_CURSOR_H = "livegps.cursor_height"; /* in pixels */
    public static final String C_CURSOR_W = "livegps.cursor_width"; /* in pixels */
    public static final String C_CURSOR_T = "livegps.cursor_thickness"; /* in pixels */

    public static final int DEFAULT_REFRESH_INTERVAL = 250;
    public static final String C_REFRESH_INTERVAL = "livegps.refresh_interval_msec";  /* in msec */
    public static final int DEFAULT_CENTER_INTERVAL = 5000;
    public static final String C_CENTER_INTERVAL = "livegps.center_interval_msec";  /* in msec */
    public static final int DEFAULT_CENTER_FACTOR = 80;
    public static final String C_CENTER_FACTOR = "livegps.center_factor" /* in percent */;

    private final JTextField gpsdHost = new JTextField(30);
    private final JTextField gpsdPort = new JTextField(30);
    private final JTextField serialDevice = new JTextField(30);
    private final JCheckBox disableGPSD = new JCheckBox(tr("Disable GPSD"));
    private final JCheckBox showOffset = new JCheckBox(tr("Show Distance to nearest way"));

    public LiveGPSPreferences() {
        super("dialogs/livegps", tr("LiveGPS settings"), tr("Here you can change some preferences of LiveGPS plugin"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new JPanel(new GridBagLayout());

        gpsdHost.setText(Config.getPref().get(C_HOST, DEFAULT_HOST));
        gpsdHost.setToolTipText(tr("Host address of gpsd, default is {0}", DEFAULT_HOST));
        panel.add(new JLabel(tr("Host address of gpsd")), GBC.std());
        panel.add(gpsdHost, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        gpsdPort.setText(String.valueOf(Config.getPref().getInt(C_PORT, DEFAULT_PORT)));
        gpsdPort.setToolTipText(tr("Port number of gpsd, default is {0}", DEFAULT_PORT));
        panel.add(new JLabel(tr("Port number gpsd")), GBC.std());
        panel.add(gpsdPort, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        disableGPSD.setSelected(Config.getPref().getBoolean(C_DISABLED, false));
        panel.add(disableGPSD, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 0, 0, 5));

        serialDevice.setText(Config.getPref().get(C_SERIAL));
        serialDevice.setToolTipText(tr(".Serial device for direct NMEA input, does not exist by default.</html>"));
        panel.add(new JLabel(tr("Serial device")), GBC.std());
        panel.add(serialDevice, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));
        /* I18n : {0} to {3} is like /dev/ttyACM<b>x</b>, {4} and {5} are COM1 and COM9 */
        panel.add(new JLabel(tr("<html>For Linux {0}, {1}, {2} or {3} (<b>x</b> means any number beginning with 0).<br>"
            +"For Windows {4} to {5} (COM ports bigger than 9 wont work).</html>", "/dev/ttyS<b>x</b>",
            "/dev/ttyACM<b>x</b>", "/dev/ttyUSB<b>x</b>", "/dev/rfcomm<b>x</b>", "COM1", "COM9")),
            GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(10, 0, 0, 5));

        showOffset.setSelected(Config.getPref().getBoolean(C_WAYOFFSET, false));
        panel.add(showOffset, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 0, 0, 5));

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GridBagConstraints.VERTICAL));
        createPreferenceTabWithScrollPane(gui, panel);
    }

    @Override
    public boolean ok() {
        Config.getPref().put(C_HOST, gpsdHost.getText());
        Config.getPref().put(C_PORT, gpsdPort.getText());
        Config.getPref().put(C_SERIAL, serialDevice.getText());
        Config.getPref().putBoolean(C_DISABLED, disableGPSD.isSelected());
        Config.getPref().putBoolean(C_WAYOFFSET, showOffset.isSelected());
        return false;
    }
}
