// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.Logging;

/**
 * Customized log handler that redirects JOSM logs to native log system.
 */
public abstract class JosmNativeLogHandler extends Handler {

    protected JosmNativeLogHandler() {
        setFormatter(new SimpleFormatter());
    }

    // CHECKSTYLE.OFF: SingleSpaceSeparator

    private static final BooleanProperty PROP_ERROR = new BooleanProperty("native.log.redirect.error", Boolean.TRUE);
    private static final BooleanProperty PROP_WARN  = new BooleanProperty("native.log.redirect.warn",  Boolean.TRUE);
    private static final BooleanProperty PROP_INFO  = new BooleanProperty("native.log.redirect.info",  Boolean.FALSE);
    private static final BooleanProperty PROP_DEBUG = new BooleanProperty("native.log.redirect.debug", Boolean.FALSE);
    private static final BooleanProperty PROP_TRACE = new BooleanProperty("native.log.redirect.trace", Boolean.FALSE);

    @Override
    public final boolean isLoggable(LogRecord record) {
        return record != null && 
                  (isLoggable(record, Logging.LEVEL_ERROR, PROP_ERROR)
                || isLoggable(record, Logging.LEVEL_WARN,  PROP_WARN)
                || isLoggable(record, Logging.LEVEL_INFO,  PROP_INFO)
                || isLoggable(record, Logging.LEVEL_DEBUG, PROP_DEBUG)
                || isLoggable(record, Logging.LEVEL_TRACE, PROP_TRACE));
    }

    private static boolean isLoggable(LogRecord record, Level level, BooleanProperty prop) {
        return level.equals(record.getLevel()) && prop.isSet();
    }

    // CHECKSTYLE.ON: SingleSpaceSeparator

    @Override
    public final void flush() {
        // Do nothing
    }

    @Override
    public final void close() {
        // Do nothing
    }
}
