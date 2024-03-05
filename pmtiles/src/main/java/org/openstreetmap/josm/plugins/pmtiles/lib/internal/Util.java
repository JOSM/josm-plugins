// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utils for reading pmtiles
 */
final class Util {
    /** The private constructor to avoid instantiation */
    private Util() {/* Hide constructor */}

    /**
     * Read the next int
     * @param inputStream The inputstream to read from
     * @param width The expected width
     * @return The next int
     * @throws IOException if there is an issue reading from the stream
     */
    static long nextInt(InputStream inputStream, int width) throws IOException {
        final var buffer = ByteBuffer.wrap(inputStream.readNBytes(width)).order(ByteOrder.LITTLE_ENDIAN);
        if (width == 8) {
            return buffer.getLong();
        }
        return buffer.getInt();
    }
}
