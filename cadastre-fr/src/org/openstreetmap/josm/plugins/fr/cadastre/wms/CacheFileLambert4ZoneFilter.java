// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre.wms;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

public final class CacheFileLambert4ZoneFilter extends FileFilter {
    private static final String LAMBERT_ZONE_MESSAGE = marktr("Lambert Zone {0} cache file (.{0})");

    /**
     * Derived from ExtensionFileFilter writen by imi
     */
    private final String extension;
    private final String description;

    /**
     * Filters for each one of the 4 Lambert zones.
     */
    public static final CacheFileLambert4ZoneFilter[] filters = {
        new CacheFileLambert4ZoneFilter("1", tr(LAMBERT_ZONE_MESSAGE, 1)),
        new CacheFileLambert4ZoneFilter("2", tr(LAMBERT_ZONE_MESSAGE, 2)),
        new CacheFileLambert4ZoneFilter("3", tr(LAMBERT_ZONE_MESSAGE, 3)),
        new CacheFileLambert4ZoneFilter("4", tr(LAMBERT_ZONE_MESSAGE, 4))
        };

    /**
     * Construct an extension file filter by giving the extension to check after.
     * @param extension file extension
     * @param description file description
     */
    private CacheFileLambert4ZoneFilter(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    boolean acceptName(String filename) {
        String name = filename.toLowerCase(Locale.FRANCE);
        for (String ext : extension.split(",")) {
            if (name.endsWith("." + ext))
                return true;
        }
        return false;
    }

    @Override
    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        return acceptName(pathname.getName());
    }

    @Override
    public String getDescription() {
        return description;
    }
}
