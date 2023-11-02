// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.plugins.fit.lib.global.FitData;
import org.openstreetmap.josm.plugins.fit.lib.global.Global;
import org.openstreetmap.josm.plugins.fit.lib.global.IFitTimestamp;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDefinitionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitHeader;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitRecordCompressedTimestampHeader;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitRecordHeader;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitRecordNormalHeader;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.IField;
import org.openstreetmap.josm.plugins.fit.lib.utils.CountingInputStream;
import org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils;

/**
 * Read a fit file
 */
public final class FitReader {
    private record LastTimestamp(long lastTimestamp, int offsetAddition, byte lastOffset){}
    private record ParseFitRecordHeader(LastTimestamp lastTimestamp, FitDeveloperFieldDescriptionMessage[] devFields){}
    private static final FitData[] EMPTY_FIT_DATA_ARRAY = new FitData[0];

    private FitReader() {
        // Hide constructor
    }

    /**
     * Read a fit file from an {@link InputStream}
     *
     * @param inputStream The stream to read
     * @param options options for reading the file
     * @return The parsed FIT data
     * @throws FitException if there was an error reading the fit file <i>or</i> there was an {@link InputStream} read
     *                      exception.
     */
    public static FitData[] read(InputStream inputStream, FitReaderOptions... options) throws FitException {
        final var bufferedInputStream = new CountingInputStream(
                inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream));
        final var optionsSet = options.length == 0 ? EnumSet.noneOf(FitReaderOptions.class)
                : EnumSet.of(options[0], options);
        final var fitData = new ArrayList<FitData>();
        try {
            parseData(bufferedInputStream, optionsSet, fitData);
        } catch (FitException fitException) {
            handleException(optionsSet, fitException);
        } catch (IllegalArgumentException | IOException ioException) {
            handleException(optionsSet, new FitException(ioException));
        }
        return fitData.toArray(EMPTY_FIT_DATA_ARRAY);
    }

    private static void parseData(CountingInputStream bufferedInputStream,
                                  Set<FitReaderOptions> optionsSet, List<FitData> fitData) throws IOException {
        final var header = readFitHeader(bufferedInputStream);
        final var headerSize = bufferedInputStream.bytesRead();
        bufferedInputStream.mark(1);
        var localMessageHeaders = new FitDefinitionMessage[1];
        var developerData = new FitDeveloperFieldDescriptionMessage[0];
        var lastTimeStamp = new LastTimestamp(Long.MIN_VALUE, 0, (byte) 0);
        while (bufferedInputStream.read() != -1
                && bufferedInputStream.bytesRead() < header.dataSize() + headerSize) {
            bufferedInputStream.reset();
            final var nextRecordHeader = readNextRecordHeader(bufferedInputStream);
            if (nextRecordHeader instanceof FitRecordNormalHeader normalHeader) {
                if (normalHeader.isDefinitionMessage()) {
                    localMessageHeaders = parseFitRecordDefinitionHeader(bufferedInputStream, localMessageHeaders, normalHeader);
                } else {
                    final var parsed = parseFitRecordHeader(optionsSet, bufferedInputStream,
                            localMessageHeaders, developerData, fitData, normalHeader);
                    if (parsed.lastTimestamp() != null) {
                        lastTimeStamp = parsed.lastTimestamp();
                    }
                    developerData = parsed.devFields();
                }
            } else if (nextRecordHeader instanceof FitRecordCompressedTimestampHeader compressedHeader) {
                if (lastTimeStamp.lastTimestamp() == Long.MIN_VALUE) {
                    handleException(optionsSet,
                            new FitException("No full timestamp found before compressed timestamp header"));
                    break;
                }
                if (compressedHeader.timeOffset() < lastTimeStamp.lastOffset()) {
                    lastTimeStamp = new LastTimestamp(lastTimeStamp.lastTimestamp(), lastTimeStamp.offsetAddition() + 1,
                            lastTimeStamp.lastOffset());
                }
                parseFitRecordCompressedHeader(optionsSet, bufferedInputStream, localMessageHeaders, developerData,
                        fitData, compressedHeader, lastTimeStamp);
                lastTimeStamp = new LastTimestamp(lastTimeStamp.lastTimestamp(), lastTimeStamp.offsetAddition(),
                        compressedHeader.timeOffset());
            } else {
                handleException(optionsSet,
                        new UnsupportedOperationException("Unknown record type: " + nextRecordHeader.getClass()));
            }
            bufferedInputStream.mark(1);
        }
    }

    private static FitDefinitionMessage[] parseFitRecordDefinitionHeader(InputStream inputStream,
                                                                         FitDefinitionMessage[] localMessageHeaders,
                                                                         FitRecordNormalHeader normalHeader) throws IOException {
        final var fitDefinitionMessage = readNextDefinition(normalHeader, inputStream);
        if (localMessageHeaders.length <= normalHeader.localMessageType()) {
            localMessageHeaders = Arrays.copyOf(localMessageHeaders,
                    normalHeader.localMessageType() + 1);
        }
        localMessageHeaders[normalHeader.localMessageType()] = fitDefinitionMessage;
        return localMessageHeaders;
    }

    private static ParseFitRecordHeader parseFitRecordHeader(Set<FitReaderOptions> optionsSet, InputStream bufferedInputStream,
                                               FitDefinitionMessage[] localMessageHeaders, FitDeveloperFieldDescriptionMessage[] developerData,
                                               List<FitData> fitData, FitRecordNormalHeader normalHeader) throws IOException {
        final Object obj = readNextRecord(localMessageHeaders[normalHeader.localMessageType()],
                developerData, bufferedInputStream);
        LastTimestamp lastTimeStamp = null;
        if (obj instanceof IFitTimestamp<?> timestamp) {
            lastTimeStamp = new LastTimestamp(timestamp.timestamp().getEpochSecond() & 0xFF_FFF_FE0L, 0, (byte) 0);
        }
        if (obj instanceof FitData fd) {
            fitData.add(fd);
        } else if (obj instanceof FitDeveloperFieldDescriptionMessage developerFieldDescriptionMessage) {
            if (developerData.length <= developerFieldDescriptionMessage.developerDataIndex()) {
                developerData = Arrays.copyOf(developerData,
                        developerFieldDescriptionMessage.developerDataIndex() + 1);
            }
            developerData[developerFieldDescriptionMessage
                    .developerDataIndex()] = developerFieldDescriptionMessage;
        } else {
            handleException(optionsSet, new IllegalStateException("Unknown record type"));
        }
        return new ParseFitRecordHeader(lastTimeStamp, developerData);
    }

    private static void parseFitRecordCompressedHeader(Set<FitReaderOptions> optionsSet, InputStream bufferedInputStream,
                                                       FitDefinitionMessage[] localMessageHeaders,
                                                       FitDeveloperFieldDescriptionMessage[] developerData, List<FitData> fitData,
                                                       FitRecordCompressedTimestampHeader compressedHeader, LastTimestamp lastTimeStamp)
            throws IOException {
        final Object obj = readNextRecord(localMessageHeaders[compressedHeader.localMessageType()],
                developerData, bufferedInputStream);
        if (obj instanceof FitData fd) {
            // Note: this could be wrong
            long actualTimestamp = lastTimeStamp.lastTimestamp() + compressedHeader.timeOffset() + (lastTimeStamp.offsetAddition() * 0x20L);
            if (fd instanceof IFitTimestamp<?> timestamp) {
                fitData.add((FitData) timestamp.withTimestamp(Instant.ofEpochSecond(actualTimestamp)));
            } else {
                fitData.add(fd);
            }
        } else {
            handleException(optionsSet, new IllegalStateException(
                    "Cannot handle non-data types with compressed timestamp headers"));
        }
    }

    private static <T extends Throwable> void handleException(Set<FitReaderOptions> options, T throwable) throws T {
        if (!options.contains(FitReaderOptions.TRY_TO_FINISH)) {
            throw throwable;
        }
        Logger.getLogger(FitReader.class.getCanonicalName()).log(Level.WARNING, throwable.getMessage(), throwable);
    }

    /**
     * Read the FIT file header; this should only ever be called once.
     *
     * @param inputStream The stream to read
     * @return The header
     * @throws IOException If something happened while reading the fit file (see {@link InputStream#read()} for some causes)
     */
    static FitHeader readFitHeader(InputStream inputStream) throws IOException {
        final int size = inputStream.read();
        if (size < 12) {
            throw new FitException("FIT header is not the expected size.");
        }
        final int protocolVersion = inputStream.read();
        final int profileVersion = inputStream.read() + (inputStream.read() << 8);
        final long dataSize = inputStream.read() + (inputStream.read() << 8) + (inputStream.read() << 16)
                + ((long) inputStream.read() << 24);
        final var fit = new byte[4];
        int read = inputStream.read(fit);
        if (read != fit.length || fit[0] != '.' || fit[1] != 'F' || fit[2] != 'I' || fit[3] != 'T') {
            throw new FitException("FIT header has the wrong data type: " + new String(fit, StandardCharsets.US_ASCII));
        }
        final int crc;
        if (size >= 14) {
            crc = inputStream.read() + (inputStream.read() << 8);
        } else {
            crc = 0;
        }
        return new FitHeader((short) protocolVersion, profileVersion, dataSize, crc);
    }

    /**
     * Read the header for the next record
     *
     * @param inputStream The stream to parse
     * @return The header for the next record
     * @throws IOException See {@link InputStream#read()}
     */
    static FitRecordHeader readNextRecordHeader(InputStream inputStream) throws IOException {
        final int header = inputStream.read();
        if ((header & 0x80) == 0) {
            return new FitRecordNormalHeader((header & 0x40) != 0, (header & 0x20) != 0, (header & 0x10) != 0,
                    (byte) (header & 0xF));
        }
        return new FitRecordCompressedTimestampHeader((byte) ((header & 0x60) >> 6), (byte) (header & 0x1F));
    }

    /**
     * Read the next definition message
     *
     * @param normalHeader The header for the message
     * @param inputStream  The stream to read
     * @return The definition message
     * @throws IOException see {@link InputStream#read()}
     */
    static FitDefinitionMessage readNextDefinition(FitRecordNormalHeader normalHeader, InputStream inputStream)
            throws IOException {
        // first byte is reserved; skip it.
        final int reserved = inputStream.read();
        final boolean littleEndian = inputStream.read() == 0; // 0 = little endian, 1 == big endian
        final var globalMessageNumber = NumberUtils.decodeInt(2, littleEndian, inputStream);
        final int numberOfFields = inputStream.read();
        final var fitFields = new ArrayList<FitField>(Math.max(0, numberOfFields));
        for (var i = 0; i < numberOfFields; i++) {
            fitFields.add(readNextField(inputStream));
        }
        // Read developer fields
        final List<FitDeveloperField> fitDeveloperFields;
        if (normalHeader.messageTypeSpecific()) {
            final int numberOfDeveloperFields = inputStream.read();
            fitDeveloperFields = new ArrayList<>(numberOfDeveloperFields);
            for (var i = 0; i < numberOfDeveloperFields; i++) {
                fitDeveloperFields.add(readNextDeveloperField(inputStream));
            }
        } else {
            fitDeveloperFields = Collections.emptyList();
        }
        return new FitDefinitionMessage((short) reserved, littleEndian, globalMessageNumber,
                Collections.unmodifiableList(fitFields), Collections.unmodifiableList(fitDeveloperFields));
    }

    /**
     * Read the next field definition
     *
     * @param inputStream The stream to read
     * @return The next field
     * @throws IOException see {@link InputStream#read()} for most reasons; may also occur if the fit file is corrupted.
     */
    static FitField readNextField(InputStream inputStream) throws IOException {
        return readNextField(FitField.class, inputStream);
    }

    /**
     * Read the next developer field definition
     *
     * @param inputStream The stream to read
     * @return The next developer field
     * @throws IOException see {@link InputStream#read()} for most reasons; may also occur if the fit file is corrupted.
     */
    static FitDeveloperField readNextDeveloperField(InputStream inputStream) throws IOException {
        return readNextField(FitDeveloperField.class, inputStream);
    }

    private static <T extends IField> T readNextField(Class<T> clazz, InputStream inputStream) throws IOException {
        final int fieldNumber = inputStream.read();
        final int size = inputStream.read();
        final int baseType = inputStream.read();
        if (fieldNumber == -1 || size == -1 || baseType == -1) {
            throw new FitException("Reached end of FIT file unexpectedly");
        }
        if (clazz == FitDeveloperField.class) {
            return clazz.cast(new FitDeveloperField((short) fieldNumber, (short) size, (short) baseType));
        } else if (clazz == FitField.class) {
            return clazz.cast(new FitField(fieldNumber, size, baseType));
        } else {
            throw new IllegalArgumentException("Unknown type: " + clazz);
        }
    }

    static Object readNextRecord(FitDefinitionMessage definitionMessage,
            FitDeveloperFieldDescriptionMessage[] developerFields, InputStream inputStream) throws IOException {
        return Global.parseData(definitionMessage, developerFields, inputStream);
    }
}
