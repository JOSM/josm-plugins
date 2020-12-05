// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

import java.nio.charset.StandardCharsets;

import com.sun.jna.Native; // NOSONAR

/**
 * The core of Rococoa - statics to handle selectors and messaging at a function call level.
 *
 * Marshalling of Java types to C types is handled by JNA. Marshalling of Java
 * type to Objective-C types is handled by RococoaTypeMapper.
 *
 * Not to be confused with the Mac Foundation or Core Foundation frameworks, most
 * users shouldn't need to access this class directly.
 *
 * Adapted from https://github.com/iterate-ch/rococoa/
 */
abstract class Foundation {

    private static final FoundationLibrary foundationLibrary;

    static {
        foundationLibrary = Native.load("Foundation", FoundationLibrary.class);
    }

    private Foundation() {
        // Hide public constructor
    }

    /**
     * Logs an error message to the Apple System Log facility.
     * @param format format string.
     *   The format specification allowed by these functions is that which is understood by NSString’s formatting capabilities
     *   (which is not necessarily the set of format escapes and flags understood by printf).
     *   The supported format specifiers are described in
     *   <a href="https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFStrings/formatSpecifiers.html">
     *   String Format Specifiers</a>.
     *   A final hard return is added to the error message if one is not present in the format.
     * @param args arguments to be inserted into the string
     */
    public static void nsLog(String format, Object args) {
        ID formatAsCFString = cfString(format);
        try {
            foundationLibrary.NSLog(formatAsCFString, args);
        } finally {
            cfRelease(formatAsCFString);
        }
    }

    /**
     * Return a CFString as an ID, toll-free bridged to NSString.
     *
     * Note that the returned string must be freed with {@link #cfRelease(ID)}.
     */
    static ID cfString(String s) {
        // Use a byte[] rather than letting jna do the String -> char* marshalling itself.
        // Turns out about 10% quicker for long strings.
        byte[] utf16Bytes = s.getBytes(StandardCharsets.UTF_16LE);
        return foundationLibrary.CFStringCreateWithBytes(null, utf16Bytes, utf16Bytes.length,
                StringEncoding.kCFStringEncodingUTF16LE.value, (byte) 0);
    }

    /**
     * Release the NSObject with id
     */
    static void cfRelease(ID id) {
        foundationLibrary.CFRelease(id);
    }
}
