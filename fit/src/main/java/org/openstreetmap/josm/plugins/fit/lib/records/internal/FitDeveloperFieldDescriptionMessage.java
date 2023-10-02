// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.global.FitDevDataRecord;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.StringUtils;

public record FitDeveloperFieldDescriptionMessage(short developerDataIndex, short fieldDefinitionNumber,
                                                  short fitBaseTypeId, String fieldName, String units,
                                                  short nativeFieldNumber,
                                                  FitDevDataRecord devData) {


    public static FitDeveloperFieldDescriptionMessage parse(boolean littleEndian, List<FitField> fieldList,
                                                            List<FitDeveloperField> developerFieldList,
                                                            FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream)
            throws IOException {
        short developerDataIndex = 0xFF;
        short fieldDefinitionNumber = 0xFF;
        short fitBaseTypeId = 0xFF;
        var fieldName = "";
        var units = "";
        short nativeFieldNumber = Short.MAX_VALUE;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                case 0 -> developerDataIndex = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 1 -> fieldDefinitionNumber = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 2 -> fitBaseTypeId = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 3 -> fieldName = StringUtils.decodeString(inputStream);
                case 8 -> units = StringUtils.decodeString(inputStream);
                default -> inputStream.readNBytes(size);
            }
        }

        return new FitDeveloperFieldDescriptionMessage(developerDataIndex, fieldDefinitionNumber, fitBaseTypeId,
                fieldName, units, nativeFieldNumber, DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
    }
}
