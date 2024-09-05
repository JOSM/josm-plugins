// License: Public Domain. For details, see LICENSE file.
package livegps;

/**
 * Representation of the LiveGPS connection status
 * @author cdaller
 */
public class LiveGpsStatus {
    /**
     * Possible status of LiveGPS data input
     */
    public enum GpsStatus {
        CONNECTING, CONNECTED, DISCONNECTED, CONNECTION_FAILED
    }

    private String statusMessage;
    private GpsStatus status;

    /**
     * Create a status representation
     * @param status current status code
     * @param statusMessage current status description message
     */
    public LiveGpsStatus(GpsStatus status, String statusMessage) {
        super();
        this.status = status;
        this.statusMessage = statusMessage;
    }

    /**
     * Retrieve the current status
     * @return current status
     */
    public GpsStatus getStatus() {
        return this.status;
    }

    /**
     * Set the current status
     * @param status the status to set
     */
    public void setStatus(GpsStatus status) {
        this.status = status;
    }

    /**
     * Access the status message
     * @return status message
     */
    public String getStatusMessage() {
        return this.statusMessage;
    }

    /**
     * Set the current status message
     * @param statusMessage the status message to set
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
