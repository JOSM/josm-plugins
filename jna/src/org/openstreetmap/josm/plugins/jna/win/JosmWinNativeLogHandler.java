// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.win;

import static com.sun.jna.platform.win32.WinError.ERROR_ACCESS_DENIED;
import static com.sun.jna.platform.win32.WinNT.EVENTLOG_ERROR_TYPE;
import static com.sun.jna.platform.win32.WinNT.EVENTLOG_INFORMATION_TYPE;
import static com.sun.jna.platform.win32.WinNT.EVENTLOG_WARNING_TYPE;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.openstreetmap.josm.plugins.jna.JosmNativeLogHandler;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Logging;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * Customized log handler that redirects JOSM logs to Windows
 * <a href="https://docs.microsoft.com/en-us/windows/win32/eventlog/event-logging-functions">native log system</a>.
 */
public class JosmWinNativeLogHandler extends JosmNativeLogHandler implements Destroyable {

    private static final String JOSM = "JOSM";

    private final HANDLE hLog;

    /**
     * Constructs a new {@code JosmWinNativeLogHandler}.
     */
    public JosmWinNativeLogHandler() {
        hLog = registerEventSource();
    }

    private static HANDLE registerEventSource() {
        HANDLE handle = Advapi32.INSTANCE.RegisterEventSource(null, JOSM);
        if (handle == null) {
            if (Kernel32.INSTANCE.GetLastError() == ERROR_ACCESS_DENIED) {
                throw new IllegalStateException("ERROR_ACCESS_DENIED: RegisterEventSource " + JOSM);
            } else {
                throw new Win32Exception(Native.getLastError());
            }
        }
        return handle;
    }

    private static void deregisterEventSource(HANDLE handle) {
        if (!Advapi32.INSTANCE.DeregisterEventSource(handle)) {
            throw new Win32Exception(Native.getLastError());
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record) && !Advapi32.INSTANCE.ReportEvent(hLog, logType(record.getLevel()), 0, 0, null, 1, 0,
                    new String[] {getFormatter().format(record)}, null)) {
            System.err.println("Unable to report Windows log event for " + record); // NOSONAR
        }
    }

    private static int logType(Level logLevel) {
        if (Logging.LEVEL_ERROR.equals(logLevel)) {
            return EVENTLOG_ERROR_TYPE;
        } else if (Logging.LEVEL_WARN.equals(logLevel)) {
            return EVENTLOG_WARNING_TYPE;
        } else {
            return EVENTLOG_INFORMATION_TYPE;
        }
    }

    @Override
    public void destroy() {
        deregisterEventSource(hLog);
    }
}
