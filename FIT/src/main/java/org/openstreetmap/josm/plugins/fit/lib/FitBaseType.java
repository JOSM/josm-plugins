// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib;

/**
 * The base fit types in order
 */
public enum FitBaseType {
    // enum types
    enum_(false, 0x00, 1, 0xFF),
    // signed int (8 bit)
    sint8(false, 0x01, 1, 0x7F),
    // unsigned int (8 bit)
    uint8(false, 0x02, 1, 0xFF),
    // signed int (16 bit)
    sint16(true, 0x83, 2, 0x7FFF),
    // unsigned int (16 bit)
    uint16(true, 0x84, 2, 0xFFFF),
    // signed int (32 bit)
    sint32(true, 0x85, 4, 0x7FFFFFFF),
    // unsigned int (32 bit)
    uint32(true, 0x86, 4, 0xFFFFFFFFL),
    // string (delimited by nul byte)
    string(false, 0x07, 1, 0x00),
    // float
    float32(true, 0x88, 4, 0xFFFFFFFFL),
    // double
    float64(true, 0x89, 8, 0xF_FFF_FFF_FFF_FFF_FFFL),
    // unsigned int (8 bit)
    uint8z(false, 0x0A, 1, 0x00),
    // unsigned int (16 bit)
    uint16z(true, 0x8B, 2, 0x0000),
    // unsigned int (32 bit)
    uint32z(true, 0x8C, 4, 0x00000000),
    // byte array -- I believe the size is the number of bytes in the array
    byte_(false, 0x0D, 1, 0xFF),
    // signed int (64 bit)
    sint64(true, 0x8E, 8, 0x7_FFF_FFF_FFF_FFF_FFFL),
    // unsigned int (64 bit)
    uint64(true, 0x8F, 8, 0xF_FFF_FFF_FFF_FFF_FFFL),
    // unsigned int (64 bit)
    uint64z(true, 0x90, 8, 0x0_000_000_000_000_000),
    // Unknown type
    UNKNOWN(false, 0xFF, 0, 0);

    private static final FitBaseType[] values = values();
    private final boolean endianAbility;
    private final short baseTypeField;
    private final byte size;
    private final long invalidValue;

    FitBaseType(boolean endianAbility, int baseTypeField, int size, long invalidValue) {
        this.size = (byte) size;
        this.invalidValue = invalidValue;
        this.endianAbility = endianAbility;
        this.baseTypeField = (short) baseTypeField;
    }

    /**
     * A cached copy of {@link #values()} for memory allocation reasons. Do not modify!
     *
     * @return The cached {@link #values()}.
     */
    public static FitBaseType fromBaseTypeField(int baseTypeField) {
        for (FitBaseType type : values) {
            if (type.baseTypeField == baseTypeField) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * The expected size of the type. Some, notable {@link #string} and {@link #byte_} may not follow this. Specifically,
     * {@link #string} is terminated by a null byte.
     *
     * @return The expected size of the type
     */
    byte size() {
        return this.size;
    }

    /**
     * The invalid value for the type
     *
     * @return The invalid value
     */
    public long invalidValue() {
        return invalidValue;
    }
}
