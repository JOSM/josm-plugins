package org.j7zip.SevenZip;

public interface IProgress {
    public int SetTotal(long total);
    public int SetCompleted(long completeValue);
}

