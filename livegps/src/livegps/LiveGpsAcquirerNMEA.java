// License: Public Domain. For details, see LICENSE file.
package livegps;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.nmea.NmeaParser;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Acquires NMEA data from a (virtual) serial port
 */
public class LiveGpsAcquirerNMEA implements Runnable {
    private String serName;

    private InputStreamReader serReader;
    private NmeaParser parser;
    private boolean connected = false;
    private boolean shutdownFlag = false;

    private final List<PropertyChangeListener> propertyChangeListener = new ArrayList<>();
    private PropertyChangeEvent lastStatusEvent;
    private PropertyChangeEvent lastDataEvent;

    /**
     * Constructor, initializes the configurable settings.
     */
    public LiveGpsAcquirerNMEA() {
        serName = Config.getPref().get(LiveGPSPreferences.C_SERIAL);
    }

    /**
     * Adds a property change listener to the acquirer.
     * @param listener the new listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (!propertyChangeListener.contains(listener)) {
            propertyChangeListener.add(listener);
        }
    }

    /**
     * Remove a property change listener from the acquirer.
     * @param listener the new listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeListener.contains(listener)) {
            propertyChangeListener.remove(listener);
        }
    }

    /**
     * Fire a gps status change event. Fires events with key "gpsstatus" and a {@link LiveGpsStatus}
     * object as value.
     * The status event may be sent any time.
     * @param status the status.
     * @param statusMessage the status message.
     */
    public void fireGpsStatusChangeEvent(LiveGpsStatus.GpsStatus status, String statusMessage) {
        PropertyChangeEvent event = new PropertyChangeEvent(this, "nmeastatus",
                null, new LiveGpsStatus(status, statusMessage));

        if (!event.equals(lastStatusEvent)) {
            firePropertyChangeEvent(event);
            lastStatusEvent = event;
        }
    }

    /**
     * Fire a gps data change event to all listeners. Fires events with key "gpsdata" and a
     * {@link LiveGpsData} object as values.
     * This event is only sent, when the suppressor permits it. This
     * event will cause the UI to re-draw itself, which has some performance penalty,
     * @param oldData the old gps data.
     * @param newData the new gps data.
     */
    public void fireGpsDataChangeEvent(LiveGpsData oldData, LiveGpsData newData) {
        PropertyChangeEvent event = new PropertyChangeEvent(this, "gpsdata", oldData, newData);

        if (!event.equals(lastDataEvent)) {
            firePropertyChangeEvent(event);
            lastDataEvent = event;
        }
    }

    /**
     * Fires the given event to all listeners.
     * @param event the event to fire.
     */
    protected void firePropertyChangeEvent(PropertyChangeEvent event) {
        for (PropertyChangeListener listener : propertyChangeListener) {
            listener.propertyChange(event);
        }
    }

    @Override
    public void run() {
        LiveGpsData oldGpsData = null;

        shutdownFlag = false;
        while (!shutdownFlag) {
            while (!connected && !shutdownFlag) {
                try {
                    connect();
                } catch (IOException iox) {
                    fireGpsStatusChangeEvent(LiveGpsStatus.GpsStatus.CONNECTION_FAILED, tr("NMEA Connection Failed"));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                        Logging.info(ignore);
                    }
                }
            }

            assert connected;

            try {
                StringBuilder sb = new StringBuilder(1024);
                int loopstartChar = serReader.read();
                if (loopstartChar == -1)
                    throw new IOException();
                sb.append((char) loopstartChar);
                Instant lasttime = null;
                while (!shutdownFlag) {
                    // handle long useless data
                    if (sb.length() >= 1020) {
                        sb.delete(0, sb.length()-1);
                    }
                    int c = serReader.read();
                    if (c == '$') {
                        Logging.trace("Parsing NMEA: " + sb.toString().replaceAll("[\r\n]", ""));
                        parser.parseNMEASentence(sb.toString());
                        sb.delete(0, sb.length());
                        sb.append('$');
                    } else if (c == -1) {
                        throw new IOException();
                    } else {
                        sb.append((char) c);
                    }
                    if (!serReader.ready()) {
                        WayPoint last = null;
                        Collection<WayPoint> wpts = parser.getAndDropWaypoints();
                        for (WayPoint w : wpts) {
                            if (w.getInstant() == null)
                                continue;
                            if (w.getInstant().equals(lasttime)) {
                                Logging.info("Skip double waypoint at " + lasttime);
                            } else {
                                last = w;
                                float course = 0.0f;
                                float speed = 0.0f;
                                if (w.getString("course") != null)
                                    course = Float.valueOf(w.getString("course"));
                                if (w.getString("speed") != null)
                                    speed = Float.valueOf(w.getString("speed"))/3.6f;
                                Logging.trace("New LiveGPS entry: " + w);
                                LiveGpsData gpsData = new LiveGpsData(w.lat(), w.lon(), course, speed, 0.0f, 0.0f);
                                gpsData.setWaypoint(w);
                                fireGpsDataChangeEvent(oldGpsData, gpsData);
                                oldGpsData = gpsData;
                            }
                        }
                        if (last != null) {
                            lasttime = last.getInstant();
                        }
                        if (!serReader.ready()) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ignore) {
                                Logging.info(ignore);
                            }
                        }
                    }
                }
            } catch (IOException iox) {
                Logging.log(Logging.LEVEL_WARN, "LiveGps: lost connection to NMEA", iox);
                fireGpsStatusChangeEvent(
                        LiveGpsStatus.GpsStatus.CONNECTION_FAILED,
                        tr("NMEA Connection Failed"));
                disconnect();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    Logging.info(ignore);
                }
            } catch (IllegalDataException ex) {
                Logging.log(Logging.LEVEL_WARN, "LiveGps: Illegal NMEA", ex);
            }
        }

        Logging.info("LiveGps: Disconnected from NMEA");
        fireGpsStatusChangeEvent(LiveGpsStatus.GpsStatus.DISCONNECTED,
                tr("NMEA Not connected"));
        disconnect();
    }

    public void shutdown() {
        Logging.info("LiveGps: Shutdown NMEA");
        shutdownFlag = true;
    }

    private void connect() throws IOException {
        fireGpsStatusChangeEvent(LiveGpsStatus.GpsStatus.CONNECTING, tr("Connecting"));
        serReader = new InputStreamReader(new FileInputStream(serName), StandardCharsets.UTF_8);
        parser = new NmeaParser();
        fireGpsStatusChangeEvent(LiveGpsStatus.GpsStatus.CONNECTED, tr("Connected"));
        connected = true;
    }

    private void disconnect() {
        if (serReader != null) {
            try {
                serReader.close();
            } catch (IOException iox) {
                Logging.warn("LiveGps: Unable to close NMEA; reconnection may not be possible");
            }
            serReader = null;
        }
        parser = null;
        connected = false;
    }
}
