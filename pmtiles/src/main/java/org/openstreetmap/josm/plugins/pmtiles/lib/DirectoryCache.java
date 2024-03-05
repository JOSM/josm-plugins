// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * A cache for directories
 */
public final class DirectoryCache implements Iterable<Directory> {
    /** The cached directories; [0] is the root, [1] is the last read directory */
    private final Directory[] directories = new Directory[2];
    /**
     * Create a new cache
     * @param root The root directory. This is <i>never</i> evicted.
     */
    public DirectoryCache(Directory root) {
        this.directories[0] = root;
    }

    /**
     * Add a directory to the cache. It is highly likely to evict another directory.
     * @param directory The directory to cache.
     */
    public void addDirectory(Directory directory) {
        this.directories[1] = directory;
    }

    @Override
    public Iterator<Directory> iterator() {
        return Arrays.stream(this.directories).filter(Objects::nonNull).iterator();
    }
}
