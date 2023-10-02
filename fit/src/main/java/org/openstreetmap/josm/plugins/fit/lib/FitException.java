// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib;

import java.io.IOException;

/**
 * An exception caused during parsing a fit file
 */
public class FitException extends IOException {
    /**
     * Constructs an {@code FitException} with {@code null}
     * as its error detail message.
     */
    FitException() {
        super();
    }

    /**
     * Create a new {@link FitException} with a specified message
     *
     * @param message The message to use for the exception
     */
    FitException(String message) {
        super(message);
    }

    /**
     * Create a new {@link FitException} with a specified message and cause
     *
     * @param message The message to use for the exception
     * @param cause   The cause of the exception
     */
    FitException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new {@link FitException} with specified cause
     *
     * @param cause The cause of the exception
     */
    FitException(Throwable cause) {
        super(cause);
    }
}
