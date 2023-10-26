// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ohe.gui;

/**
 * An exception for Opening Hourse
 */
public class OheException extends Exception {
    /**
     * See {@link super#Exception()}
     */
    public OheException() {
        super();
    }

    /**
     * See {@link super#Exception(String)}
     * @param message the message
     */
    public OheException(String message) {
        super(message);
    }

    /**
     * See {@link super#Exception(String, Throwable)}
     * @param message the message
     * @param cause the cause
     */
    public OheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * See {@link super#Exception(Throwable)}
     * @param cause the cause
     */
    public OheException(Throwable cause) {
        super(cause);
    }
}
