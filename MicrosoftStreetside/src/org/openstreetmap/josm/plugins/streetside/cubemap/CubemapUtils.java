// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import java.util.logging.Logger;

import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.tools.Logging;

/**
 * Utils for cubemaps
 */
public final class CubemapUtils {

    public static final int NUM_SIDES = 6;
    private static final Logger LOGGER = Logger.getLogger(CubemapUtils.class.getCanonicalName());
    // numerical base for decimal conversion (quaternary in the case of Streetside)
    private static final int NUM_BASE = 4;

    private CubemapUtils() {
        // Private constructor to avoid instantiation
    }

    /**
     * Get the maximum columns for the image
     * @param image The image to get the max columns for
     * @return The maximum number of columns
     */
    public static int getMaxCols(StreetsideAbstractImage image) {
        if (Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
            return image.xCols(image.zoomMax());
        }
        return image.xCols(image.zoomMin());
    }

    /**
     * Get the maximum rows for the image
     * @param image The image to get the max rows for
     * @return The maximum number of rows
     */
    public static int getMaxRows(StreetsideAbstractImage image) {
        return getMaxCols(image);
    }

    /**
     * Convert a decimal number to a quaternary (base 4) number
     * @param inputNum The number to convert
     * @return The quaternary as a string
     */
    public static String convertDecimal2Quaternary(long inputNum) {
        return Long.toString(inputNum, CubemapUtils.NUM_BASE);
    }

    /**
     * Convert a quaternary number to a standard decimal number
     * @param inputNum The quaternary input number to convert
     * @return The standard decimal number
     */
    public static String convertQuaternary2Decimal(String inputNum) {
        try {
            return Long.toString(Long.valueOf(inputNum, CubemapUtils.NUM_BASE));
        } catch (NumberFormatException numberFormatException) {
            Logging.trace(numberFormatException);
            LOGGER.log(Logging.LEVEL_ERROR, "Error converting quadkey {0} to decimal.", inputNum);
            return "000000000";
        }
    }

    /**
     * The faces for a cubemap
     */
    public enum CubemapFaces {
        FRONT("01"), RIGHT("02"), BACK("03"), LEFT("10"), UP("11"), DOWN("12");

        private final String value;

        CubemapFaces(String value) {
            this.value = value;
        }

        /**
         * The base value for the side
         * @return The base value for the side (top is 1-1 in Streetside API docs, here it is 11)
         */
        public String getValue() {
            return value;
        }

        /**
         * The face id
         * @return The face id
         */
        public String faceId() {
            return this.value.substring(0, 1);
        }

        /**
         * The starting tile id
         * @return The starting tile id
         */
        public String startingTileId() {
            return this.value.substring(1, 2);
        }
    }
}
