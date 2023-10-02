package org.openstreetmap.josm.plugins.fit.lib;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.fit.lib.global.FitDevDataRecord;
import org.openstreetmap.josm.plugins.fit.lib.global.Global;
import org.openstreetmap.josm.plugins.fit.lib.global.FitDeveloperDataIdMessage;
import org.openstreetmap.josm.plugins.fit.lib.global.FitDevice;
import org.openstreetmap.josm.plugins.fit.lib.global.HeartRateCadenceDistanceSpeed;
import org.openstreetmap.josm.plugins.fit.lib.records.FitDevIntData;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitDeveloperFieldDescriptionMessage;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitField;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitHeader;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.FitRecordNormalHeader;
import org.openstreetmap.josm.plugins.fit.lib.utils.DevDataUtils;

class FitReaderTest {
    private static final FitDeveloperFieldDescriptionMessage[] NO_DEV_FIELDS = new FitDeveloperFieldDescriptionMessage[0];
    private static final FitDevDataRecord NO_DEV_DATA = assertDoesNotThrow(() -> DevDataUtils.parseDevFields(false, Collections.emptyList(), NO_DEV_FIELDS, null));
    private static final byte[][] SAMPLE_FIGURE_14 = {
            // File header (14 byte)
            {0x0E, 0x10, (byte) 0x98, 0x08, (byte) 0xA9, 0x00, 0x00, 0x00, 0x2E, 0x46, 0x49, 0x54, /* wrong crc (fix) */ 0x00, 0x00 /* wrong crc */},
            // Record 1 (1,2)
            {0x40}, {0x00, 0x00, 0x00, 0x00, 0x05, 0x00, 0x01, 0x00, 0x01, 0x02, (byte) 0x84, 0x02, 0x02, (byte) 0x84, 0x03, 0x04, (byte) 0x8C, 0x04, 0x04, (byte) 0x86},
            // Record 2 (3,4)
            {0x00}, {0x04, 0x0F, 0x00, 0x16, 0x00, (byte) 0xD2, 0x04, 0x00, 0x00, 0x28, (byte) 0xC6, 0x0A, 0x25},
            // Record 3 (5,6)
            {0x40}, {0x00, 0x00, (byte) 0xCF, 0x00, 0x02, 0x01, 0x10, 0x0D, 0x03, 0x01, 0x02},
            // Record 4 (7,8)
            {0x00}, {0x2C, 0x01, 0x02, 0x02, 0x03, 0x01, 0x0F, 0x01, 0x02, 0x0C, 0x1F, 0x29, 0x01, 0x02, 0x01, 0x58, 0x00},
            // Record 5 (9,10)
            {0x40}, {0x00, 0x00, (byte) 0xCE, 0X00, 0x05, 0x00, 0x01, 0x02, 0x01, 0x01, 0x02, 0x02, 0x01, 0x02, 0x03, 0x40, 0x07, 0x08, 0x10, 0x07},
            // Record 6 (11,12)
            {0x00}, {0x00, 0x00, 0x01, 0x64, 0x6F, 0x75, 0x67, 0x68, 0x6E, 0x75, 0x74, 0x73, 0x5F, 0x65, 0x61, 0x72, 0x6E, 0x65, 0x64, 0x00, 0x64, 0x6F, 0x75, 0x67, 0x68, 0x6E, 0x75, 0x74, 0x73, 0x00},
            // Record 7 (13,14)
            {0x60}, {0x00, 0x00, 0x14, 0x00, 0x04, 0x03, 0x01, 0x02, 0x04, 0x01, 0x02, 0x05, 0x04, (byte) 0x86, 0x06, 0x02, (byte) 0x84, 0x01, 0x00, 0x01, 0x00},
            // Record 8 (15,16)
            {0x00}, {(byte) 0x8C, 0x58, (byte) 0xFE, 0x01, 0x00, 0x00, (byte) 0xF0, 0x0A, 0x01},
            // Record 9 (17,18)
            {0x00}, {(byte) 0x8F, 0x5A, 0x20, 0x08, 0x00, 0x00, 0x68, 0x0B, 0x01},
            // Record 10 (19, 20)
            {0x00}, {(byte) 0x90, 0x5C, 0x7E, 0x0E, 0x00, 0x00, (byte) 0xEA, 0x0B, 0x01},
            // CRC (2 byte) (fix this, see also header)
            {}
    };

    private static byte[] convert2dByteArray(byte[][] array) {
        var size = 0;
        for (byte[] bytes : array) {
            size += bytes.length;
        }
        final byte[] returnArray = new byte[size];
        var index = 0;
        for (byte[] bytes : array) {
            System.arraycopy(bytes, 0, returnArray, index, bytes.length);
            index += bytes.length;
        }
        return returnArray;
    }

    @Test
    void testFitHeader() throws IOException {
        final var header = FitReader.readFitHeader(new ByteArrayInputStream(
                new byte[] {0x0e, 0x10, (byte) 0x98, 0x08, (byte) 0xda, (byte) 0xd4, 0x00, 0x00, 0x2e, 0x46, 0x49, 0x54, (byte) 0x9a, (byte) 0xfc}));
        assertEquals(new FitHeader((short) 16, 2200, 54490L, 64666), header);
    }

    @Test
    void testReadRecordDefinitionHeader() throws IOException {
        final var recordHeader = FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[1]));
        final var normalHeader = assertInstanceOf(FitRecordNormalHeader.class, recordHeader);
        assertAll(() -> assertTrue(normalHeader.isDefinitionMessage()),
                () -> assertFalse(normalHeader.messageTypeSpecific()),
                () -> assertFalse(normalHeader.reserved()),
                () -> assertEquals(0, normalHeader.localMessageType()));
    }

    @Test
    void testReadRecordDataHeader() throws IOException {
        final var recordHeader = FitReader.readNextRecordHeader(new ByteArrayInputStream(new byte[] {0x00}));
        final var normalHeader = assertInstanceOf(FitRecordNormalHeader.class, recordHeader);
        assertAll(() -> assertFalse(normalHeader.isDefinitionMessage()),
                () -> assertFalse(normalHeader.messageTypeSpecific()),
                () -> assertFalse(normalHeader.reserved()),
                () -> assertEquals(0, normalHeader.localMessageType()));
    }

    @Test
    void testRecordDefinitionMessage() throws IOException {
        final var recordHeader = assertInstanceOf(FitRecordNormalHeader.class,
                FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[1])));
        final var definitionMessage = FitReader.readNextDefinition(recordHeader, new ByteArrayInputStream(SAMPLE_FIGURE_14[2]));
        assertAll(() -> assertEquals(5, definitionMessage.fitFields().size()),
                () -> assertEquals(0, definitionMessage.fitDeveloperFields().size()),
                () -> assertEquals(0, definitionMessage.globalMessageNumber()),
                () -> assertTrue(definitionMessage.littleEndian()),
                () -> assertEquals(0, definitionMessage.reserved()));
        assertAll(() -> assertEquals(new FitField(0, 1, 0), definitionMessage.fitFields().getFirst()),
                () -> assertEquals(new FitField(1, 2, 132), definitionMessage.fitFields().get(1)),
                () -> assertEquals(new FitField(2, 2, 132), definitionMessage.fitFields().get(2)),
                () -> assertEquals(new FitField(3, 4, 140), definitionMessage.fitFields().get(3)),
                () -> assertEquals(new FitField(4, 4, 134), definitionMessage.fitFields().getLast()));
    }

    @Test
    void testDeviceFit() throws IOException {
        final var definitionMessage = FitReader.readNextDefinition(
                assertInstanceOf(FitRecordNormalHeader.class, FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[1]))),
                new ByteArrayInputStream(SAMPLE_FIGURE_14[2]));
        final var device = assertInstanceOf(FitDevice.class, Global.parseData(definitionMessage, NO_DEV_FIELDS, new ByteArrayInputStream(SAMPLE_FIGURE_14[4])));
        assertAll(() -> assertEquals(4, device.type()),
                () -> assertEquals(15, device.manufacturer()),
                () -> assertEquals(22, device.product()),
                () -> assertEquals(1234, device.serialNumber()),
                () -> assertEquals(621463080, device.timeCreated()));
    }

    @Test
    void testDeveloperDataIdMessage() throws IOException {
        final var definitionHeader = assertInstanceOf(FitRecordNormalHeader.class,
                FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[5])));
        final var definitionMessage = FitReader.readNextDefinition(definitionHeader, new ByteArrayInputStream(SAMPLE_FIGURE_14[6]));
        assertInstanceOf(FitRecordNormalHeader.class,
                FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[7])));
        assertInstanceOf(FitDeveloperDataIdMessage.class, Global.parseData(definitionMessage, NO_DEV_FIELDS, new ByteArrayInputStream(SAMPLE_FIGURE_14[8])));
    }

    @Test
    void testRecordDefinitionMessageDevFields() throws IOException {
        final var recordHeader = assertInstanceOf(FitRecordNormalHeader.class,
                FitReader.readNextRecordHeader(new ByteArrayInputStream(SAMPLE_FIGURE_14[9])));
        // definition message for the dev data
        final var definitionMessage = FitReader.readNextDefinition(recordHeader, new ByteArrayInputStream(SAMPLE_FIGURE_14[10]));
        assertAll(() -> assertEquals(5, definitionMessage.fitFields().size()),
                () -> assertEquals(0, definitionMessage.fitDeveloperFields().size()),
                () -> assertTrue(definitionMessage.littleEndian()),
                () -> assertEquals(0, definitionMessage.reserved()),
                () -> assertEquals(206, definitionMessage.globalMessageNumber()));
        final var devDefinitionMessage = assertInstanceOf(FitDeveloperFieldDescriptionMessage.class,
                Global.parseData(definitionMessage, NO_DEV_FIELDS, new ByteArrayInputStream(SAMPLE_FIGURE_14[12])));

        assertAll(() -> assertEquals(0, devDefinitionMessage.developerDataIndex()),
                () -> assertEquals(0, devDefinitionMessage.fieldDefinitionNumber()),
                () -> assertEquals(1, devDefinitionMessage.fitBaseTypeId()),
                () -> assertEquals("doughnuts_earned", devDefinitionMessage.fieldName()),
                () -> assertEquals("doughnuts", devDefinitionMessage.units()));
    }

    @Test
    void testReadSampleFigure14() throws FitException {
        final var fitData = FitReader.read(new ByteArrayInputStream(convert2dByteArray(SAMPLE_FIGURE_14)));
        final var doughnuts = new FitDevDataRecord(new FitDevIntData("doughnuts_earned", "doughnuts", 1));
        assertEquals(5, fitData.length);
        assertAll(() -> assertEquals(new FitDevice((short) 4, 15, 22, 1234, 621463080,
                                NO_DEV_DATA), fitData[0]),
                () -> assertEquals(new FitDeveloperDataIdMessage(
                        new BigInteger("58491264835600100376640728332759073112"), (short) 0, NO_DEV_DATA), fitData[1]),
                () -> assertEquals(new HeartRateCadenceDistanceSpeed(140, 88, 510, 2800, doughnuts), fitData[2]),
                () -> assertEquals(new HeartRateCadenceDistanceSpeed(143, 90, 2080, 2920, doughnuts), fitData[3]),
                () -> assertEquals(new HeartRateCadenceDistanceSpeed(144, 92, 3710, 3050, doughnuts), fitData[4]));
    }
}