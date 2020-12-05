// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

import java.util.logging.LogRecord;

import org.openstreetmap.josm.plugins.jna.JosmNativeLogHandler;

/**
 * Customized log handler that redirects JOSM logs to macOS native log system.
 */
public class JosmMacNativeLogHandler extends JosmNativeLogHandler {

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            Foundation.nsLog(getFormatter().format(record), null);
        }
    }
}
