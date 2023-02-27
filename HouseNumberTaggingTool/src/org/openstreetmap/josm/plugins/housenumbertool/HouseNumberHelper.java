// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.housenumbertool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.tools.Logging;

/**
 * A helper class for incrementing addr:housenumber
 */
public final class HouseNumberHelper {
    private HouseNumberHelper() {
        // Hide constructor
    }

    /**
     * Increment a house number
     * @param number The number to increment
     * @param increment The amount to increment the number by
     * @return The incremented number
     */
    public static String incrementHouseNumber(String number, int increment) {
        if (number != null) {
            try {
                Matcher m = Pattern.compile("([^\\pN]+)?(\\pN+)([^\\pN]+)?").matcher(number);
                if (m.matches()) {
                    String prefix = m.group(1) != null ? m.group(1) : "";
                    int n = Integer.parseInt(m.group(2)) + increment;
                    String suffix = m.group(3) != null ? m.group(3) : "";
                    return prefix + n + suffix;
                }
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        return null;
    }
}
