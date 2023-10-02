// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * The header for a fit file
 *
 * @param protocolVersion The protocol version number
 * @param profileVersion  The profile version number
 * @param dataSize        The size of the data in bytes (does not include header or CRC)
 * @param crc             The CRC of the data
 */
public record FitHeader(short protocolVersion, int profileVersion, long dataSize, int crc) {
}
