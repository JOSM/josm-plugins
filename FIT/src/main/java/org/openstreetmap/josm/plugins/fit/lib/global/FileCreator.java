// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import static org.openstreetmap.josm.plugins.fit.lib.global.HeartRateCadenceDistanceSpeed.NO_UNKNOWNS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.FitBaseType;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;

public record FileCreator(short softwareVersion, short hardwareVersion, long[][] unknown, FitDevDataRecord devData)
        implements FitData {
    public static FileCreator parse(boolean littleEndian, List<FitField> fieldList, List<FitDeveloperField> developerFieldList,
                                    FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream) throws IOException {
        var hardwareVersion = FitBaseType.uint8.invalidValue();
        var softwareVersion = FitBaseType.uint8.invalidValue();
        var unknowns = NO_UNKNOWNS;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                case 0 -> softwareVersion = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 1 -> hardwareVersion = NumberUtils.decodeShort(size, littleEndian, inputStream);
                default -> {
                    unknowns = Arrays.copyOf(unknowns, unknowns.length + 1);
                    unknowns[unknowns.length - 1] = new long[]{fitField.fieldDefinitionNumber(),
                            NumberUtils.decodeLong(size, littleEndian, inputStream)};
                }
            }
        }
        return new FileCreator((short) hardwareVersion, (short) softwareVersion, unknowns,
                DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
    }
}
