// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
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
    private final JTextField gpsdHost = new JTextField(30);
    private final JTextField gpsdPort = new JTextField(30);
    private final JTextField serialDevice = new JTextField(30);

    public LiveGPSPreferences() {
        super("dialogs/livegps", tr("LiveGPS settings"), tr("Here you can change some preferences of LiveGPS plugin"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new JPanel(new GridBagLayout());

        gpsdHost.setText(Config.getPref().get(LiveGpsAcquirer.C_HOST, LiveGpsAcquirer.DEFAULT_HOST));
        gpsdHost.setToolTipText(tr("Host address of gpsd, default is {0}", LiveGpsAcquirer.DEFAULT_HOST));
        panel.add(new JLabel(tr("Host address of gpsd")), GBC.std());
        panel.add(gpsdHost, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        gpsdPort.setText(String.valueOf(Config.getPref().getInt(LiveGpsAcquirer.C_PORT, LiveGpsAcquirer.DEFAULT_PORT)));
        gpsdPort.setToolTipText(tr("Port number of gpsd, default is {0}", LiveGpsAcquirer.DEFAULT_PORT));
        panel.add(new JLabel(tr("Port number gpsd")), GBC.std());
        panel.add(gpsdPort, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        serialDevice.setText(Config.getPref().get(LiveGpsAcquirerNMEA.C_SERIAL));
        serialDevice.setToolTipText(tr("Serial device for direct NMEA input, does not exist by default"));
        panel.add(new JLabel(tr("Serial device")), GBC.std());
        panel.add(serialDevice, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GridBagConstraints.VERTICAL));
        createPreferenceTabWithScrollPane(gui, panel);
    }

    @Override
    public boolean ok() {
        Config.getPref().put(LiveGpsAcquirer.C_HOST, gpsdHost.getText());
        Config.getPref().put(LiveGpsAcquirer.C_PORT, gpsdPort.getText());
        Config.getPref().put(LiveGpsAcquirerNMEA.C_SERIAL, serialDevice.getText());
        return false;
    }
}
