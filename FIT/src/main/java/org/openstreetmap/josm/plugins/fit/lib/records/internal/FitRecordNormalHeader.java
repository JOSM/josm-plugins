// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * A header for the next data record
 *
 * @param isDefinitionMessage {@code true} if it is a <i>definition message</i>, {@code false} if it is a <i>data message</i>
 * @param messageTypeSpecific This is specific to <i>definition</i> and <i>data</i> message types. For <i>definition</i>,
 *                            it indicates that the message contains extended definitions for developer data. For <i>data</i>,
 *                            it is reserved and should be 0.
 * @param reserved            Reserved (not used)
 * @param localMessageType
 */
public record FitRecordNormalHeader(boolean isDefinitionMessage, boolean messageTypeSpecific,
                                    boolean reserved, byte localMessageType) implements FitRecordHeader {
}
