// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import static org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils.checkShort;

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

public record HeartRateCadenceDistanceSpeed(Instant timestamp, double lat, double lon, double ele, short heartRate, short cadence,
                                            int distance, int speed, long[][] unknown,
                                            FitDevDataRecord devData) implements FitData, IFitTimestamp<HeartRateCadenceDistanceSpeed> {
    static final long[][] NO_UNKNOWNS = new long[0][];

    // Using the 2023-09-09-12-0016.fit.gpx file provided by richlv:
    // 1063184416, 1063184419, 1063184488 -> 2023-09-09 + (T09:00:16Z, T09:00:19Z, T09:01:28Z)
    // If we just call ofEpochSecond, these will all be in 2003-09-10. Therefore, the epoch start
    // must be different in fit files. We can get this using the following code:
    // Instant.ofEpochSecond(Instant.parse("2023-09-09T09:00:16Z").getEpochSecond() - 1063184416).toString()
    private static final long EPOCH_DIFFERENCE = Instant.parse("1989-12-31T00:00:00Z").getEpochSecond();

    public HeartRateCadenceDistanceSpeed(int heartRate, int cadence, int distance, int speed, FitDevDataRecord devData) {
        this(Instant.EPOCH, Double.NaN, Double.NaN, Double.NaN, checkShort(heartRate), checkShort(cadence),
                distance, speed, NO_UNKNOWNS, devData);
    }

    @Override
    public HeartRateCadenceDistanceSpeed withTimestamp(Instant timestamp) {
        return new HeartRateCadenceDistanceSpeed(timestamp, this.lat, this.lon, this.ele, this.heartRate, this.cadence,
                this.distance, this.speed, this.unknown, this.devData);
    }

    public static HeartRateCadenceDistanceSpeed parse(boolean littleEndian, List<FitField> fieldList,
                                                      List<FitDeveloperField> developerFieldList,
                                                      FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream)
            throws IOException {
        long heartRate = FitBaseType.uint8.invalidValue();
        short cadence = 0xFF;
        var lat = Double.NaN;
        var lon = Double.NaN;
        var ele = Double.NaN;
        var distance = FitBaseType.uint32.invalidValue();
        var speed = FitBaseType.uint16.invalidValue();
        var timestamp = Instant.EPOCH;
        var unknowns = NO_UNKNOWNS;
        for (FitField fitField : fieldList) {
            final var size = fitField.size();
            switch (fitField.fieldDefinitionNumber()) {
                // Fields 0/1 were the only two fields with sufficient "bits" for lat/lon
                // Field 0 was "closer" to the lat, and Field 1 was "closer" to the lon.
                case 0 -> lat = decodeDegrees(NumberUtils.decodeLong(size, littleEndian, inputStream));
                case 1 -> lon = decodeDegrees(NumberUtils.decodeLong(size, littleEndian, inputStream));
                case 2 -> ele = decodeElevation(NumberUtils.decodeLong(size, littleEndian, inputStream));
                case 3 -> heartRate = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 4 -> cadence = NumberUtils.decodeShort(size, littleEndian, inputStream);
                case 5 -> distance = NumberUtils.decodeInt(size, littleEndian, inputStream);
                case 6 -> speed = NumberUtils.decodeInt(size, littleEndian, inputStream);
                // 13 seems to be either -1 (invalid entry?), or 23-27
                // 107 seems to always be -1 (invalid entry?), 0, and 1.
                case 253 -> timestamp = decodeInstant(size, littleEndian, inputStream);
                default -> {
                    unknowns = Arrays.copyOf(unknowns, unknowns.length + 1);
                    unknowns[unknowns.length - 1] = new long[]{fitField.fieldDefinitionNumber(),
                            NumberUtils.decodeLong(size, littleEndian, inputStream)};
                }
            }
        }
        return new HeartRateCadenceDistanceSpeed(timestamp, lat, lon, ele, (short) heartRate, cadence, (int) distance, (int) speed, unknowns,
                DevDataUtils.parseDevFields(littleEndian, developerFieldList, developerFields, inputStream));
    }

    static Instant decodeInstant(short size, boolean littleEndian, InputStream inputStream) throws IOException {
        final var timestamp = NumberUtils.decodeLong(size, littleEndian, inputStream);
        return Instant.ofEpochSecond(EPOCH_DIFFERENCE + timestamp);
    }

    private static double decodeDegrees(long original) {
        if (original == FitBaseType.sint32.invalidValue()) {
            return Double.NaN;
        }
        // signed ints, no need to look into zigzag decoding
        // 0 -> 0 (assumed)
        // 676960569 -> 56.7421794
        // 676960086 -> 56.7421389
        // 291082210 -> 24.3982289
        // 291082461 -> 24.3982500
        // min/max lat probably -+90
        // min/max lon probably -+180
        // Equation probably has 180 in it (max(180, 90))
        // Assume that the equation doesn't have fractions and no additions (0, 0)
        // 56.7421794 = 676960569 / 180 * x => x = 56.7421794 * 180 / 676960569 = 1.5087e-5 (inverse -> 66280.359)
        // 56.7421794 = 676960569 * 180 * x => x = 56.7421794 / (180 * 676960569) = 3.56e-8 (inverse -> 2.1474836E9, or Integer.MAX_VALUE)
        return original * 180d / Integer.MAX_VALUE;
    }

    private static double decodeElevation(long original) {
        // 2212 -> -57.6
        // 2655 -> 31.0
        // 2656 -> 31.2
        // 2657 -> 31.4
        // Each integer == 0.2 (probably m?)
        return original / 5d - 500;
    }
}
