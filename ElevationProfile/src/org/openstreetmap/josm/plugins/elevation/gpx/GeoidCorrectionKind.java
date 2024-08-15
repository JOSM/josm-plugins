// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.elevation.gpx;

/**
 * Enumeration for available elevation correction modes.
 * @author Oliver Wieland &lt;oliver.wieland@online.de&gt;
 */
public enum GeoidCorrectionKind {
    /** Elevation values remain unchanged */
    None,
    /** Automatic correction by geoid lookup table */
    Auto,
    /** Fixed value */
    Fixed
}
