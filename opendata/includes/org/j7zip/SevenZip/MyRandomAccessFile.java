package org.j7zip.SevenZip;

public class MyRandomAccessFile extends org.j7zip.SevenZip.IInStream  {
    
    java.io.RandomAccessFile _file;
    
    public MyRandomAccessFile(String filename,String mode)  throws java.io.IOException {
        _file  = new java.io.RandomAccessFile(filename,mode);
    }
    
    public long Seek(long offset, int seekOrigin)  throws java.io.IOException {
        if (seekOrigin == STREAM_SEEK_SET) {
            _file.seek(offset);
        }
        else if (seekOrigin == STREAM_SEEK_CUR) {
            _file.seek(offset + _file.getFilePointer());
        }
        return _file.getFilePointer();
    }
    
    public int read() throws java.io.IOException {
        return _file.read();
    }
 
    public int read(byte [] data, int off, int size) throws java.io.IOException {
        return _file.read(data,off,size);
    }
    
    public void close() throws java.io.IOException {
        _file.close();
        _file = null;
    }   
}