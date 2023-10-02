// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * Compressed timestamp header
 *
 * @param localMessageType The local message type
 * @param timeOffset       The time offset in seconds
 */
public record FitRecordCompressedTimestampHeader(byte localMessageType, byte timeOffset) implements FitRecordHeader {
}
