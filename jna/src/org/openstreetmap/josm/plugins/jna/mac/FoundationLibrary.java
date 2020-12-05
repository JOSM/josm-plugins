// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

import com.sun.jna.Library; // NOSONAR

/**
 * JNA Library for plain C calls, standard JNA marshalling applies to these.
 *
 * Adapted from https://github.com/iterate-ch/rococoa/
 */
interface FoundationLibrary extends Library {

    void NSLog(ID pString, Object thing);

    ID CFStringCreateWithBytes(ID allocator, byte[] bytes, int byteCount, int encoding, byte isExternalRepresentation);

    void CFRelease(ID cfTypeRef);
}
