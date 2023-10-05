// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Count the bytes read from a stream
 */
public class CountingInputStream extends InputStream {
    private final InputStream inputStream;
    private long bytesRead;
    private long mark;

    /**
     * Create a new {@link CountingInputStream}
     * @param inputStream The stream to count bytes read from
     */
    public CountingInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        final var read = this.inputStream.read();
        if (read != -1) {
            bytesRead++;
        }
        return read;
    }

    /**
     * Get the number of bytes read
     * @return The bytes read
     */
    public long bytesRead() {
        return this.bytesRead;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        final int read = this.inputStream.read(bytes);
        if (read != -1) {
            this.bytesRead += read;
        }
        return read;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        final int read = this.inputStream.read(bytes, off, len);
        if (read != -1) {
            this.bytesRead += read;
        }
        return read;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        final var allBytes = this.inputStream.readAllBytes();
        this.bytesRead += allBytes.length;
        return allBytes;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        final var nBytes = this.inputStream.readNBytes(len);
        this.bytesRead += nBytes.length;
        return nBytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        final var read = this.inputStream.readNBytes(b, off, len);
        if (read != -1) {
            this.bytesRead += read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        final var read = this.inputStream.skip(n);
        this.bytesRead += read;
        return read;
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        this.inputStream.skipNBytes(n);
        this.bytesRead += n; // This might not be accurate...
    }

    @Override
    public int available() throws IOException {
        return this.inputStream.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        this.inputStream.mark(readlimit);
        this.mark = this.bytesRead;
    }

    @Override
    public void reset() throws IOException {
        this.inputStream.reset();
        this.bytesRead = this.mark;
    }

    @Override
    public boolean markSupported() {
        return this.inputStream.markSupported();
    }
}
