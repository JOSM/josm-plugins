package org.j7zip.SevenZip;

public interface ICompressSetInStream {
    public int SetInStream(java.io.InputStream inStream);
    public int ReleaseInStream() throws java.io.IOException ;
}

