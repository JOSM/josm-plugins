package org.j7zip.SevenZip.Archive.SevenZip;

import org.j7zip.Common.ByteBuffer;
import org.j7zip.Common.ObjectVector;
import org.j7zip.SevenZip.HRESULT;


class StreamSwitch {
    InArchive _archive;
    boolean _needRemove;

    public StreamSwitch() {
        _needRemove = false;
    }
    
    public void close() {
        Remove();
    }
    
    void Remove() {
        if (_needRemove) {
            _archive.DeleteByteStream();
            _needRemove = false;
        }
    }
    
    void Set(InArchive archive, ByteBuffer byteBuffer) {
        Set(archive, byteBuffer.data(), byteBuffer.GetCapacity());
    }
    
    void Set(InArchive archive, byte [] data, int size) {
        Remove();
        _archive = archive;
        _archive.AddByteStream(data, size);
        _needRemove = true;
    }
    
    int Set(InArchive archive, ObjectVector<ByteBuffer> dataVector)   throws java.io.IOException {
        Remove();
        int external = archive.ReadByte();
        if (external != 0) {
            int dataIndex = archive.ReadNum();
            Set(archive, dataVector.get(dataIndex));
        }
        return HRESULT.S_OK;
    }
}
