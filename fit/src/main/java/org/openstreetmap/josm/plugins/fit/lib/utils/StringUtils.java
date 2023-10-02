// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utils for strings
 */
public final class StringUtils {
    private StringUtils() {
        // Hide constructor
    }

    /**
     * Decode the next string from a stream
     *
     * @param inputStream The stream to read
     * @return The decoded string
     * @throws IOException See {@link InputStream#read()}
     */
    public static String decodeString(InputStream inputStream) throws IOException {
        final var stringBuilder = new StringBuilder();
        int next;
        while ((next = inputStream.read()) != 0) {
            stringBuilder.append((char) next);
        }
        return stringBuilder.toString();
    }
}
