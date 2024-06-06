// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.photo_geotagging;

import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This plugin is used to write latitude and longitude information
 * to the EXIF header of jpg and tiff files.
 * It extends the core geoimage feature of JOSM by adding a new entry
 * to the right click menu of any image layer.
 * <p>
 * The real work (writing lat/lon values to file) is done by the pure Java
 * sanselan library.
 */
public class GeotaggingPlugin extends Plugin {
    public GeotaggingPlugin(PluginInformation info) {
        super(info);
        GeoImageLayer.registerMenuAddition(new GeotaggingAction());
    }

}
