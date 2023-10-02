// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;

public record FitDevice(short type, int manufacturer, int product, long serialNumber, long timeCreated,
                        FitDevDataRecord devData) implements FitData {

    public static FitDevice parse(boolean littleEndian, List<FitField> fieldList, List<FitDeveloperField> developerFieldList,
                                  FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream) throws IOException {
        short type = 0xFF;
        var manufacturer = 0xFFFF;
        var product = 0xFFFF;
        long serialNumber = 0x00000000;
        var timeCreated = 0xFFFFFFFFL;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                case 0 -> type = NumberUtils.decodeByte(size, littleEndian, inputStream);
                case 1 -> manufacturer = NumberUtils.decodeInt(size, littleEndian, inputStream);
                case 2 -> product = NumberUtils.decodeInt(size, littleEndian, inputStream);
                case 3 -> serialNumber = NumberUtils.decodeLong(size, littleEndian, inputStream);
                case 4 -> timeCreated = NumberUtils.decodeLong(size, littleEndian, inputStream);
                default -> inputStream.readNBytes(size);
            }
        }
        return new FitDevice(type, manufacturer, product, serialNumber, timeCreated,
                DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
    }
}
