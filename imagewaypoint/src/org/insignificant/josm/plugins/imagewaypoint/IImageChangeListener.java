package org.insignificant.josm.plugins.imagewaypoint;

public interface IImageChangeListener {
    void onAvailableImageEntriesChanged(ImageEntries entries);

    void onSelectedImageEntryChanged(ImageEntries entries);
}
