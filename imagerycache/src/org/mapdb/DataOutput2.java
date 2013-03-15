/*
 *  Copyright (c) 2012 Jan Kotek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mapdb;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
  * Provides {@link DataOutput} implementation on top of growable {@code byte[]}
 * <p/>
 *  {@link ByteArrayOutputStream} is not used as it requires {@code byte[]} copying
 *
 * @author Jan Kotek
 */
public final class DataOutput2 implements DataOutput {

    byte[] buf;
    int pos;

    DataOutput2(){
        pos = 0;
        buf = new byte[16]; //TODO take hint from serializer for initial size
    }

    byte[] copyBytes(){
        return Arrays.copyOf(buf, pos);
    }

    /**
     * make sure there will be enought space in buffer to write N bytes
     */
    private void ensureAvail(final int n) {
        if (pos + n >= buf.length) {
            int newSize = Math.max(pos + n, buf.length * 2);
            buf = Arrays.copyOf(buf, newSize);
        }
    }


    @Override
    public void write(final int b) throws IOException {
        ensureAvail(1);
        buf[pos++] = (byte) b;
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        ensureAvail(len);
        System.arraycopy(b, off, buf, pos, len);
        pos += len;
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        ensureAvail(1);
        buf[pos++] = (byte) (v ? 1 : 0);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        ensureAvail(1);
        buf[pos++] = (byte) (v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        ensureAvail(2);
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
    }

    @Override
    public void writeChar(final int v) throws IOException {
        writeInt(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        ensureAvail(4);
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
    }

    @Override
    public void writeLong(final long v) throws IOException {
        ensureAvail(8);
        buf[pos++] = (byte) (0xff & (v >> 56));
        buf[pos++] = (byte) (0xff & (v >> 48));
        buf[pos++] = (byte) (0xff & (v >> 40));
        buf[pos++] = (byte) (0xff & (v >> 32));
        buf[pos++] = (byte) (0xff & (v >> 24));
        buf[pos++] = (byte) (0xff & (v >> 16));
        buf[pos++] = (byte) (0xff & (v >> 8));
        buf[pos++] = (byte) (0xff & (v));
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        ensureAvail(4);
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        ensureAvail(8);
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        writeUTF(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        writeUTF(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        SerializerBase.serializeString(this, s);
    }
}
