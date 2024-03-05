// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib.internal;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.plugins.pmtiles.lib.Directory;
import org.openstreetmap.josm.plugins.pmtiles.lib.DirectoryEntry;

/**
 * Parse directories from PMTiles
 */
public final class DirectoryParser {
    /** Hide the constructor */
    private DirectoryParser() { /* Hide the constructor */ }

    /**
     * Parse a directory
     * @param inputStream The stream to read
     * @return The parsed directory
     * @throws IOException See {@link InputStream#read()}
     */
    public static Directory parse(InputStream inputStream) throws IOException {
        int lastByte = inputStream.read();
        var currentInt = lastByte & 0x7F;
        var shift = 7;
        while ((lastByte & 0x80) != 0) {
            lastByte = inputStream.read();
            currentInt |= (lastByte & 0x7F) << shift;
            shift += 7;
        }
        final var entrySize = currentInt;
        final var tileIds = new long[entrySize];
        final var runLengths = new long[entrySize];
        final var lengths = new long[entrySize];
        final var offsets = new long[entrySize];
        var index = 0;
        while (lastByte != -1 && index < entrySize * 4) {
            lastByte = inputStream.read();
            long currentLong = lastByte & 0x7F;
            shift = 7;
            while ((lastByte & 0x80) != 0 && lastByte != -1) {
                lastByte = inputStream.read();
                currentLong |= (long) (lastByte & 0x7F) << shift;
                shift += 7;
            }
            if (index < tileIds.length) {
                tileIds[index] = currentLong;
            } else if (index < entrySize * 2) {
                runLengths[index - entrySize] = currentLong;
            } else if (index < entrySize * 3) {
                lengths[index - 2 * entrySize] = currentLong;
            } else {
                offsets[index - 3 * entrySize] = currentLong;
            }
            index++;
        }
        final var entries = new DirectoryEntry[entrySize];
        for (var i = 0; i < entries.length; i++) {
            if (i == 0) {
                entries[i] = new DirectoryEntry(tileIds[i], offsets[i] - 1, lengths[i], runLengths[i]);
            } else {
                final var lastEntry = entries[i - 1];
                final long offset;
                if (offsets[i] == 0) {
                    offset = lastEntry.offset() + lastEntry.length();
                } else {
                    offset = offsets[i] - 1;
                }
                entries[i] = new DirectoryEntry(tileIds[i] + lastEntry.tileId(), offset, lengths[i], runLengths[i]);
            }
        }
        return new Directory(entries);
    }
}
