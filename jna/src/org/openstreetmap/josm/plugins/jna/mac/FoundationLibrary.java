// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

import com.sun.jna.Library; // NOSONAR
import com.sun.jna.NativeLong;

/**
 * JNA Library for plain C calls, standard JNA marshalling applies to these.
 *
 * Adapted from https://github.com/iterate-ch/rococoa/
 */
interface FoundationLibrary extends Library {

    void NSLog(NativeLong pString, Object thing);

    NativeLong CFStringCreateWithBytes(NativeLong allocator, byte[] bytes, int byteCount, int encoding,
            byte isExternalRepresentation);

    void CFRelease(NativeLong cfTypeRef);
}
