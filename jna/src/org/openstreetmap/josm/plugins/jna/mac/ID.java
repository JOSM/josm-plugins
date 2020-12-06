// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.jna.mac;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;

/**
 * Represents an Objective-C ID.
 * 
 * This extends NativeLong for efficiency, but you should really think of it as opaque.
 * 
 * Technically, this should be {@link Native#POINTER_SIZE} not {@link Native#LONG_SIZE},
 * but as they are both 32 on 32-bit and 64 on 64-bit we'll gloss over that. Ideally
 * it would be Pointer, but they have no protected constructors.
 *
 * Adapted from https://github.com/iterate-ch/rococoa/
 */
class ID extends NativeLong {

    private static final long serialVersionUID = 1L;

    public static ID fromLong(long value) {
        return new ID(value);
    }

    protected ID(long value) {
        super(value);
    }

    protected ID(ID anotherID) {
        this(anotherID.longValue());
    }

    @Override
    public String toString() {
        return String.format("[ID 0x%x]", longValue());
    }

    public boolean isNull() {
        return longValue() == 0;
    }

    public static ID getGlobal(String libraryName, String globalVarName) {
        return new ID(NativeLibrary.getInstance(libraryName).getGlobalVariableAddress(globalVarName).getNativeLong(0)
                .longValue());
    }
}
