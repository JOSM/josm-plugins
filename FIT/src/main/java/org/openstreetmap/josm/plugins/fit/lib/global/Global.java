// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.FitBaseType;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevStringData;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDefinitionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.StringUtils;

/**
 * Global message ids
 */
public interface Global {
    byte MESSAGE_NUMBER_FILE_ID = 0;
    short MESSAGE_NUMBER_APP_ID = 207;
    short MESSAGE_NUMBER_DEV_FIELD_DESCRIPTION = 206;

    /**
     * Get the constructor for the specified message number
     *
     * @param globalMessageNumber The message number for the field definition (the "meaning")
     * @param fieldList           The list of fields to populate
     * @param inputStream         The stream to read
     * @param littleEndian        The endianness of the data
     * @param developerFieldList  The list of developer fields
     * @param developerFields The global developer field definitions
     * @return The fit data or null if we couldn't parse it
     * @throws IOException see {@link InputStream#read()}
     */
    static Object parseData(int globalMessageNumber, boolean littleEndian, List<FitField> fieldList,
                            List<FitDeveloperField> developerFieldList, FitDeveloperFieldDescriptionMessage[] developerFields,
                            InputStream inputStream) throws IOException {
        return switch (globalMessageNumber) {
            case MESSAGE_NUMBER_FILE_ID ->
                    FitDevice.parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            // No clue what to call this. For all I know, it is a typo and will never appear in the wild.
            case 20 ->
                    HeartRateCadenceDistanceSpeed.parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            case 21 -> FitEvent.parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            case MESSAGE_NUMBER_DEV_FIELD_DESCRIPTION -> FitDeveloperFieldDescriptionMessage
                    .parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            case MESSAGE_NUMBER_APP_ID ->
                    FitDeveloperDataIdMessage.parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            case 49 -> FileCreator.parse(littleEndian, fieldList, developerFieldList, developerFields, inputStream);
            default -> {
                final var fieldData = new FitUnknownRecord.FieldData[fieldList.size()];
                var index = 0;
                for (FitField field : fieldList) {
                    if (field.fitBaseType() == FitBaseType.string) {
                        fieldData[index++] = new FitUnknownRecord.FieldData(field,
                                new FitDevStringData("", "",
                                        StringUtils.decodeString(new ByteArrayInputStream(inputStream.readNBytes(field.size())))));
                    } else {
                        fieldData[index++] = new FitUnknownRecord.FieldData(field, DevDataUtils.getData(field.fitBaseType(),
                                "", "", field.size(), littleEndian, inputStream));
                    }
                }
                yield new FitUnknownRecord(globalMessageNumber, fieldData,
                        DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
            }
        };
    }

    /**
     * Get the constructor for the specified message number
     *
     * @param definitionMessage The data definitions
     * @param inputStream       The stream to read
     * @return The fit data or null if we couldn't parse it
     * @throws IOException see {@link InputStream#read()}
     */
    static Object parseData(FitDefinitionMessage definitionMessage,
            FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream) throws IOException {
        return parseData(definitionMessage.globalMessageNumber(), definitionMessage.littleEndian(),
                definitionMessage.fitFields(), definitionMessage.fitDeveloperFields(), developerFields, inputStream);
    }

}
