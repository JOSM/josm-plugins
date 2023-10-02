// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.utils;

import java.io.IOException;
import java.io.InputStream;

public final class NumberUtils {
    private NumberUtils() {
        // Hide constructor
    }

    public static long decodeLong(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        long value = 0;
        int increment = littleEndian ? 8 : -8;
        int shift = littleEndian ? 0 : (byteSize - 1) * 8;
        for (var i = 0; i < byteSize; i++) {
            value += ((long) inputStream.read() << shift);
            shift += increment;
        }
        return value;
    }

    public static double decodeDouble(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return Double.longBitsToDouble(decodeLong(byteSize, littleEndian, inputStream));
    }

    public static int decodeInt(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return (int) decodeLong(byteSize, littleEndian, inputStream);
    }

    public static float decodeFloat(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return Float.intBitsToFloat(decodeInt(byteSize, littleEndian, inputStream));
    }

    public static short decodeShort(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return (short) decodeLong(byteSize, littleEndian, inputStream);
    }

    public static byte decodeByte(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return (byte) decodeLong(byteSize, littleEndian, inputStream);
    }

    public static char decodeChar(int byteSize, boolean littleEndian, InputStream inputStream) throws IOException {
        return (char) decodeLong(byteSize, littleEndian, inputStream);
    }

    public static boolean decodeBoolean(int byteSize, boolean littleEndian, InputStream inputStream)
            throws IOException {
        return decodeLong(byteSize, littleEndian, inputStream) == 1;
    }

    public static short checkShort(int number) {
        if (number > Short.MAX_VALUE || number < Short.MIN_VALUE) {
            throw new IllegalArgumentException(number + " is larger than a short");
        }
        return (short) number;
    }
}
