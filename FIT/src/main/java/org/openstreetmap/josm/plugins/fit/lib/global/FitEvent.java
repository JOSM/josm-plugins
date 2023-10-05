// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import static org.openstreetmap.josm.plugins.fit.lib.global.HeartRateCadenceDistanceSpeed.NO_UNKNOWNS;
import static org.openstreetmap.josm.plugins.fit.lib.global.HeartRateCadenceDistanceSpeed.decodeInstant;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.plugins.fit.lib.FitBaseType;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;

public record FitEvent(Instant timestamp, byte event, byte eventType, short eventGroup, long data, long[][] unknown,
                       FitDevDataRecord devData) implements FitData, IFitTimestamp<FitEvent> {
    @Override
    public FitEvent withTimestamp(Instant timestamp) {
        return new FitEvent(timestamp, this.event, this.eventType, this.eventGroup, this.data, this.unknown, this.devData);
    }

    public static FitEvent parse(boolean littleEndian, List<FitField> fieldList, List<FitDeveloperField> developerFieldList,
                                 FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream) throws IOException {
        var event = FitBaseType.enum_.invalidValue();
        var eventType = FitBaseType.enum_.invalidValue();
        var eventGroup = FitBaseType.enum_.invalidValue();
        var timestamp = Instant.EPOCH;
        var data = FitBaseType.uint32.invalidValue();
        var unknowns = NO_UNKNOWNS;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                case 0 -> event = NumberUtils.decodeByte(size, littleEndian, inputStream);
                case 1 -> eventType = NumberUtils.decodeByte(size, littleEndian, inputStream);
                case 3 -> data = NumberUtils.decodeLong(size, littleEndian, inputStream);
                case 4 -> eventGroup = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 253 -> timestamp = decodeInstant(size, littleEndian, inputStream);
                default -> {
                    unknowns = Arrays.copyOf(unknowns, unknowns.length + 1);
                    unknowns[unknowns.length - 1] = new long[]{fitField.fieldDefinitionNumber(),
                            NumberUtils.decodeLong(size, littleEndian, inputStream)};
                }
            }
        }
        return new FitEvent(timestamp, (byte) event, (byte) eventType, (short) eventGroup, data, unknowns,
                DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
    }
}
