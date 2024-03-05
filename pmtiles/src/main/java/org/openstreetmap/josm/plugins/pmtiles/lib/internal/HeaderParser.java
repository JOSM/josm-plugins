// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.openstreetmap.josm.plugins.pmtiles.lib.Header;
import org.openstreetmap.josm.plugins.pmtiles.lib.InternalCompression;
import org.openstreetmap.josm.plugins.pmtiles.lib.TileType;

/**
 * The implementation for headers
 */
public final class HeaderParser {
    /** Hide the constructor */
    private HeaderParser() { /* Hide constructor */ }

    /** This is so that we can read the first 7 bytes and determine if we are reading a pmtiles file */
    private static final String MAGIC_HEADER = "PMTiles";

    /**
     * Parse the header
     * @param location The location of the PMtiles (added to the {@link Header})
     * @param inputStream The stream to read
     * @return The header
     * @throws IOException if the {@link InputStream} had issues
     */
    public static Header parse(URI location, InputStream inputStream) throws IOException {
        // First do sanity checks
        for (var i = 0; i < MAGIC_HEADER.length(); i++) {
            if (inputStream.read() != MAGIC_HEADER.charAt(i)) {
                throw new IOException("Malformed PMTiles");
            }
        }
        // We will have read 7 bytes by now, and the next one is the spec version.
        if (inputStream.read() != 3) {
            throw new IOException("Malformed PMTiles");
        }
        // OK. Header is "correct". Now we need to parse the rest of the header.
        return new Header(location, nextInt(inputStream), nextInt(inputStream), nextInt(inputStream), nextInt(inputStream),
                nextInt(inputStream), nextInt(inputStream), nextInt(inputStream), nextInt(inputStream),
                nextInt(inputStream), nextInt(inputStream), nextInt(inputStream), 0x1 == inputStream.read(),
                nextCompressionType(inputStream), nextCompressionType(inputStream), nextTileType(inputStream),
                inputStream.read(), inputStream.read(), nextDegrees(inputStream), nextDegrees(inputStream),
                nextDegrees(inputStream), nextDegrees(inputStream), inputStream.read(), nextDegrees(inputStream),
                nextDegrees(inputStream));
    }

    /**
     * Read the next degrees from a stream
     * @param inputStream The stream to read
     * @return The degrees read
     * @throws IOException See {@link InputStream#read()}
     */
    private static double nextDegrees(InputStream inputStream) throws IOException {
        return Util.nextInt(inputStream, 4) / 10_000_000d;
    }

    /**
     * Get the next int
     * @param inputStream The stream to read
     * @return The next (unsigned) int
     * @throws IOException See {@link InputStream#read()}
     */
    private static long nextInt(InputStream inputStream) throws IOException {
        return Util.nextInt(inputStream, 8);
    }

    /**
     * Get the next compression type
     * @param inputStream The stream to read
     * @return The compression type
     * @throws IOException See {@link InputStream#read()}
     */
    private static InternalCompression nextCompressionType(InputStream inputStream) throws IOException {
        final var type = inputStream.read();
        return switch (type) {
            case 0 -> InternalCompression.UNKNOWN;
            case 1 -> InternalCompression.NONE;
            case 2 -> InternalCompression.GZIP;
            case 3 -> InternalCompression.BROTLI;
            case 4 -> InternalCompression.ZSTD;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    /**
     * Get the tile type for the next tile
     * @param inputStream The stream to read
     * @return The tile type
     * @throws IOException See {@link InputStream#read()}
     */
    private static TileType nextTileType(InputStream inputStream) throws IOException {
        final var type = inputStream.read();
        return switch (type) {
            case 0 -> TileType.UNKNOWN;
            case 1 -> TileType.MVT;
            case 2 -> TileType.PNG;
            case 3 -> TileType.JPEG;
            case 4 -> TileType.WEBP;
            case 5 -> TileType.AVIF;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
