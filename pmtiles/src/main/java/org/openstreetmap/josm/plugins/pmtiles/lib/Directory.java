// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A directory of tile/directory entries
 * @param entries The entries in the directory
 */
public record Directory(DirectoryEntry... entries) implements Iterable<DirectoryEntry>, Serializable {
    @Override
    public Iterator<DirectoryEntry> iterator() {
        return Arrays.stream(entries).iterator();
    }
}
