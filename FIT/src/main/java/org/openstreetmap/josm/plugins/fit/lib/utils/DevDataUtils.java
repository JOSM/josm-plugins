// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.openstreetmap.josm.plugins.fit.lib.FitBaseType;
import org.openstreetmap.josm.plugins.fit.lib.global.FitDevDataRecord;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevDoubleData;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevFloatData;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevIntData;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevLongData;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevStringData;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevUnknown;
import org.openstreetmap.josm.plugins.fit.lib.records.IFitDevData;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;

public final class DevDataUtils {
    private static final FitDevDataRecord EMPTY_IFITDEVDATA = new FitDevDataRecord();

    private DevDataUtils() {
        // Hide constructor
    }

    /**
     * Parse the dev fields
     * @param littleEndian {@code true} if the bit ordering is little endian
     * @param developerFieldList The current list of developer fiels
     * @param devFields The array of developer field descriptions
     * @param inputStream The stream to read
     * @return The dev data record
     * @throws IOException If something happened while reading the stream
     */
    public static FitDevDataRecord parseDevFields(boolean littleEndian,
            Collection<FitDeveloperField> developerFieldList, FitDeveloperFieldDescriptionMessage[] devFields,
            InputStream inputStream) throws IOException {
        if (developerFieldList.isEmpty()) {
            return EMPTY_IFITDEVDATA;
        }
        final var arrayData = new IFitDevData<?>[developerFieldList.size()];
        int index = 0;
        for (FitDeveloperField fitField : developerFieldList) {
            final var devField = devFields[fitField.developerDataIndex()];
            final String fieldName = devField.fieldName();
            final String units = devField.units();
            final short fieldSize = fitField.size();
            arrayData[index++] = getData(FitBaseType.fromBaseTypeField(devField.fitBaseTypeId()), fieldName, units,
                    fieldSize, littleEndian, inputStream);
        }
        return new FitDevDataRecord(arrayData);
    }

    public static IFitDevData<?> getData(FitBaseType type, String fieldName, String units, short fieldSize,
            boolean littleEndian, InputStream inputStream) throws IOException {
        return switch (type) {
        case float64 -> new FitDevDoubleData(fieldName, units,
                NumberUtils.decodeDouble(fieldSize, littleEndian, inputStream));
        case float32 -> new FitDevFloatData(fieldName, units,
                NumberUtils.decodeFloat(fieldSize, littleEndian, inputStream));
        case enum_, sint8, uint8, uint8z, sint16, uint16, uint16z, sint32 -> new FitDevIntData(fieldName, units,
                NumberUtils.decodeInt(fieldSize, littleEndian, inputStream));
        case uint32, uint32z, sint64, uint64, uint64z -> new FitDevLongData(fieldName, units,
                NumberUtils.decodeLong(fieldSize, littleEndian, inputStream));
        case string -> new FitDevStringData(fieldName, units, StringUtils.decodeString(inputStream));
        case byte_, UNKNOWN -> new FitDevUnknown(fieldName, units, inputStream.readNBytes(fieldSize));
        };
    }
}
