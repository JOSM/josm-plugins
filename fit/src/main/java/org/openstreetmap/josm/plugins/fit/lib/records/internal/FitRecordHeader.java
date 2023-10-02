// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * The headers for data in a FIT file
 */
sealed
public interface FitRecordHeader
permits FitRecordCompressedTimestampHeader, FitRecordNormalHeader
{

    /**
     * Get the local message type
     *
     * @return The local message type
     */
    byte localMessageType();
}
