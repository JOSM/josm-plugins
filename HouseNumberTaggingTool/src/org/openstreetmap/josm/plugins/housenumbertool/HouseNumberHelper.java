// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.housenumbertool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.tools.Logging;

/**
 * A helper class for incrementing addr:housenumber
 */
public final class HouseNumberHelper {
    /**
     * prefix, number, separator, letter suffix, number suffix, e.g. "12a", "12/1", "12-b"
     */
    private static final Pattern HOUSE_NUMBER_PATTERN = Pattern.compile("([^\\pN]+)?(\\pN+)([-/ ])?([^\\pN]+)?(\\pN+)?");

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
                Matcher m = HOUSE_NUMBER_PATTERN.matcher(number);
                if (m.matches()) {
                    int n = Integer.parseInt(m.group(2)) + increment;
                    return number.substring(0, m.start(2)) + n + number.substring(m.end(2));
                }
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        return null;
    }

    /**
     * Increment the suffix of a house number, e.g. 1 to 1a, 1a to 1b or 1/1 to 1/2
     * @param number The number to increment
     * @return The incremented number
     */
    public static String incrementHouseNumberSuffix(String number) {
        if (number != null) {
            try {
                Matcher m = HOUSE_NUMBER_PATTERN.matcher(number);
                if (m.matches()) {
                    String letter = m.group(4);
                    String suffixNumber = m.group(5);
                    if (suffixNumber != null) {
                        int n = Integer.parseInt(suffixNumber) + 1;
                        return number.substring(0, m.start(5)) + n;
                    } else if (letter != null) {
                        char next = (char) (letter.charAt(letter.length() - 1) + 1);
                        return number.substring(0, m.end(4) - 1) + next;
                    } else {
                        return number + "a";
                    }
                }
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        return null;
    }

    /**
     * Check if a house number has a suffix like 1a or 1/1
     * @param number The house number
     * @return {@code true} if the number has a suffix
     */
    public static boolean hasSuffix(String number) {
        return number != null && !number.equals(removeSuffix(number));
    }

    /**
     * Remove the suffix of a house number, e.g. 1a to 1
     * @param number The house number
     * @return The number without suffix
     */
    public static String removeSuffix(String number) {
        if (number != null) {
            Matcher m = HOUSE_NUMBER_PATTERN.matcher(number);
            if (m.matches()) {
                return number.substring(0, m.end(2));
            }
        }
        return number;
    }
}
