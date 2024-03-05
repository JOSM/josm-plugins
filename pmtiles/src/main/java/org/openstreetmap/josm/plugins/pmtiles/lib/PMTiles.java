// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.openstreetmap.josm.plugins.pmtiles.lib.internal.DirectoryParser;
import org.openstreetmap.josm.plugins.pmtiles.lib.internal.HeaderParser;
import org.openstreetmap.josm.tools.Utils;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * The entry point for PMTiles
 */
public final class PMTiles {
    /** An empty byte array for reuse */
    private static final byte[] EMPTY_BYTE = new byte[0];

    /** The constructor for this class. Hidden, so we don't have instances of this class. */
    private PMTiles() {/* hide the constructor */}

    /**
     * Read the tiles from a specified location
     * @param location The location to read them from
     * @return The PMTiles header
     * @throws MalformedURLException if the URI could not be converted to a URL
     * @throws IOException If there was an error reading the data
     */
    public static Header readHeader(URI location) throws IOException {
        try (var inputStream = getInputStream(location, 0, 127)) {
            return HeaderParser.parse(location, inputStream);
        }
    }

    /**
     * Read metadata from a file
     * @param header The header with offset information
     * @return The metadata
     * @throws IOException If there was an error reading the data
     */
    public static JsonObject readMetadata(Header header) throws IOException {
        try (var inputStream = decompressInputStream(header.internalCompression(),
                getInputStream(header.location(), header.metadataOffset(), header.metadataLength()));
             var reader = Json.createReader(inputStream)) {
            return reader.readObject();
        }
    }

    /**
     * Read the root directory
     * @param header The header data
     * @return The root directory
     * @throws IOException If there was an error reading the data
     */
    public static Directory readRootDirectory(Header header) throws IOException {
        try (var inputStream = decompressInputStream(header.internalCompression(),
                getInputStream(header.location(), header.rootOffset(), header.rootLength()))) {
            return DirectoryParser.parse(inputStream);
        }
    }

    /**
     * Read the root directory
     * @param header The header data
     * @param offset The offset inside the leaf directory area
     * @param length The length of the leaf directory
     * @return The root directory
     * @throws IOException If there was an error reading the data
     */
    public static Directory readLeafDirectory(Header header, long offset, long length) throws IOException {
        try (var inputStream = decompressInputStream(header.internalCompression(),
                getInputStream(header.location(), header.leafOffset() + offset, length))) {
            return DirectoryParser.parse(inputStream);
        }
    }

    /**
     * Read tile data
     * @param header The header data
     * @param index The hilbert index (from {@link #convertToHilbert(int, int, int)} in most cases)
     * @param cachedDirectories The directories to look through.
     * @return The data
     * @throws IOException if the file could not be read
     */
    public static byte[] readData(Header header, long index, DirectoryCache cachedDirectories) throws IOException {
        final var entry = getDataLocation(header, index, cachedDirectories);
        if (entry == null) {
            return EMPTY_BYTE;
        }
        try (var inputStream = decompressInputStream(header.tileCompression(),
                getInputStream(header.location(), header.tileOffset() + entry.offset(), entry.length()))) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * Get the data location in PM tiles
     * @param header The header to read
     * @param index The index to find
     * @param cachedDirectories The directories to use to avoid recurring calls; this array <i>should</i> be of length 2.
     *                          The first directory should be the root directory. The second should be the most recently used
     *                          leaf directory, if available.
     * @return The entry with the data. If {@code null} there is no entry for the data.
     * @throws IOException if we could not read data
     */
    public static DirectoryEntry getDataLocation(Header header, long index, DirectoryCache cachedDirectories) throws IOException {
        DirectoryEntry leaf = null;
        for (var directory : cachedDirectories) {
            final var entry = getDataEntry(index, directory);
            if (entry != null && entry.isLeafDirectory()) {
                leaf = entry;
            } else if (entry != null) {
                return entry;
            }
        }
        // We now need to find the appropriate leaf directory, since it wasn't in the cached directories
        while (leaf != null) {
            final var leafDirectory = readLeafDirectory(header, leaf.offset(), leaf.length());
            cachedDirectories.addDirectory(leafDirectory);
            leaf = getDataEntry(index, leafDirectory);
            if (leaf != null && leaf.contains(index) && !leaf.isLeafDirectory()) {
                return leaf;
            }
        }
        return null;
    }

    /**
     * Perform a search for the specified entry
     * @param index The index to find
     * @param directory The directory to look through
     * @return The appropriate entry (may be a leaf directory or {@code null})
     */
    private static DirectoryEntry getDataEntry(long index, Directory directory) {
        // This finds either the entry or the leaf directory
        DirectoryEntry entry = null;
        for (var current : directory) {
            if (current.contains(index)) {
                return current;
            } else if (current.tileId() < index && current.isLeafDirectory() && (entry == null || current.tileId() > entry.tileId())) {
                entry = current;
            }
        }
        return entry;
    }

    /**
     * Convert a traditional tile to a Hilbert tile
     * @param tile The tile to convert
     * @return The Hilbert tile
     */
    public static long convertToHilbert(TileXYZ tile) {
        return convertToHilbert(tile.z(), tile.x(), tile.y());
    }

    /**
     * Convert a traditional tile to a Hilbert tile
     * @param z The z index (zoom level)
     * @param x The x index
     * @param y The y index
     * @return The Hilbert tile
     */
    public static long convertToHilbert(int z, int x, int y) {
        // The maximum x/y coordinates are defined by the z level. Keep in mind that we are 0 indexed.
        // 1, 4, 16, 64, ...
        final var maxSquare = Math.pow(4, z);
        if (x >= maxSquare || y >= maxSquare) {
            throw new IllegalArgumentException("x or y out of bounds: " + z + " (x = " + x + ", y = " + y);
        }
        // We need to sum up the previous z levels
        long start = 0L;
        var currentZoom = z;
        // TODO profile this and the integral form (4^x)/(log(4)). Might not be as accurate though due to fp issues.
        // Maybe also profile Math.pow(4, currentZoom)
        while (currentZoom > 0) {
            currentZoom--;
            start += (1L << currentZoom) * (1L << currentZoom);
        }
        // Now we need to calculate the coordinates inside the specified zoom level
        long d = 0;
        int n = 1 << z;
        final var xy = new int[]{x, y};
        for (int s = n / 2; s > 0; s /= 2) {
            int rx = (xy[0] & s) > 0 ? 1 : 0;
            int ry = (xy[1] & s) > 0 ? 1 : 0;
            d += ((long) s) * s * ((3 * rx) ^ ry);
            rotate(n, xy, rx, ry);
        }
        return start + d;
    }

    /**
     * Convert a hilbert number to a tile
     * @param hilbert The continuous hilbert number
     * @return The traditional tile (Z/X/Y)
     */
    public static TileXYZ convertToXYZ(long hilbert) {
        var z = 0;
        var start = 0;
        while (true) {
            final var zTiles = Math.toIntExact(Math.round(Math.pow(4, z)));
            if (start + zTiles > hilbert) {
                break;
            }
            start += zTiles;
            z++;
        }
        long t = hilbert - start;
        final var xy = new int[]{0, 0};
        final int n = 1 << z;
        for (var s = 1; s < n; s *= 2) {
            var rx = (int) (1 & (t / 2));
            var ry = (int) (1 & (t ^ rx));
            rotate(s, xy, rx, ry);
            xy[0] += s * rx;
            xy[1] += s * ry;
            t /= 4;
        }
        return new TileXYZ(z, xy[0], xy[1]);
    }

    /**
     * Rotate the curve
     * @param n The nxn cells
     * @param xy The coordinates to modify
     * @param rx The x rotation
     * @param ry The y rotation
     */
    private static void rotate(int n, int[] xy, int rx, int ry) {
        if (ry == 0) {
            if (rx == 1) {
                xy[0] = n - 1 - xy[0];
                xy[1] = n - 1 - xy[1];
            }
            final var temp = xy[0];
            xy[0] = xy[1];
            xy[1] = temp;
        }
    }

    /**
     * Decompress a stream
     * @param compression The compression the stream uses
     * @param inputStream The stream to decompress
     * @return The decompressed stream
     * @throws IOException if one of the decompressors had an issue
     */
    private static InputStream decompressInputStream(InternalCompression compression, InputStream inputStream) throws IOException {
        return switch (compression) {
            case GZIP -> new GzipCompressorInputStream(inputStream);
            case ZSTD -> new InflaterInputStream(inputStream);
            case BROTLI -> new BrotliCompressorInputStream(inputStream);
            case NONE -> inputStream;
            case UNKNOWN -> throw new UnsupportedOperationException("Unknown compression type");
        };
    }

    /**
     * Get the stream for a given range and location
     * @param location The location of the data
     * @param start The start byte
     * @param length The end byte (exclusive)
     * @return The stream to read for the data
     * @throws IOException If there is something that prevents reading the stream from the given location.
     */
    private static InputStream getInputStream(URI location, long start, long length) throws IOException {
        if (Utils.isLocalUrl(location.toString())) {
            final var file = Path.of(location);
            try (var is = Files.newInputStream(file)) {
                if (start != is.skip(start)) {
                    throw new IOException("Something is wrong with the file");
                }
                if (length < Integer.MAX_VALUE) {
                    return new ByteArrayInputStream(is.readNBytes((int) length));
                } else {
                    throw new IOException("The PMTiles plugin currently does not support large streams from the file system");
                }
            }
        }
        var request = HttpRequest.newBuilder(location).header("Range", "bytes=" + start + "-" + (start + length - 1))
                .header("User-Agent", "JOSM PMTiles v1").GET().build();
        try {
            final var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() > 300) {
                throw new IOException("Bad response code for " + response.request().uri() + ": " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IOException(interruptedException);
        }
    }
}
