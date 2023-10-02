// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;

/**
 * Used to uniquely identify developer data field sources; there can be at most 255 in a FIT file.
 *
 * @param applicationId      The 16-byte identifier for the developer
 * @param developerDataIndex The developer data index for this message
 * @param devData            Additional dev data fields (unlikely for this class)
 */
public record FitDeveloperDataIdMessage(BigInteger applicationId, short developerDataIndex,
                                        FitDevDataRecord devData) implements FitData {

    public static FitDeveloperDataIdMessage parse(boolean littleEndian, List<FitField> fieldList,
                                                  List<FitDeveloperField> developerFieldList, FitDeveloperFieldDescriptionMessage[] devFields,
                                                  InputStream inputStream)
            throws IOException {
        BigInteger applicationId = null;
        short developerDataIndex = 0xFF;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                case 1 -> applicationId = new BigInteger(inputStream.readNBytes(size));
                case 3 -> developerDataIndex = NumberUtils.decodeShort(size, littleEndian, inputStream);
                default -> inputStream.readNBytes(size);
            }
        }
        return new FitDeveloperDataIdMessage(applicationId, developerDataIndex,
                DevDataUtils.parseDevFields(littleEndian, developerFieldList, devFields, inputStream));
    }
}
