// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import java.io.Serializable;

/**
 * An entry for tile information
 * @param tileId The tile id using Hilbert curves starting at z=0
 * @param offset The position of the file relative to the start of the data section
 * @param length The size of the tile in bytes
 * @param runLength The number of times the tile is repeated. 0 means that it is a leaf directory where the tileid is the first entry.
 */
public record DirectoryEntry(long tileId, long offset, long length, long runLength) implements Serializable {
    /** Create a new entry with some basic validation */
    public DirectoryEntry {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
    }

    /**
     * Check if this entry is a leaf directory
     * @return {@code true} if we need to go to a leaf directory
     */
    public boolean isLeafDirectory() {
        return this.runLength == 0;
    }

    /**
     * Check if a id is inside the range
     * @param index The index to check
     * @return {@code true} if this entry contains the specified index
     */
    public boolean contains(long index) {
        return this.tileId == index || (this.tileId < index && this.tileId + this.runLength > index);
    }
}
