// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

/**
 * Internal compression details
 */
public enum InternalCompression {
    /** Unknown compression type */
    UNKNOWN,
    /** No compression */
    NONE,
    /** The GNU GZIP compression format */
    GZIP,
    /** The Google Brotli compression format */
    BROTLI,
    /** The Facebook zstandard compression format */
    ZSTD
}
