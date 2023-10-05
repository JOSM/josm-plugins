// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pmtiles.lib;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.plugins.pmtiles.PMTestUtils.ODBL_RASTER_STAMEN;
import static org.openstreetmap.josm.plugins.pmtiles.PMTestUtils.ODBL_VECTOR_FIRENZE;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link PMTiles}
 */
class PMTilesTest {
    @Test
    void testHeader() {
        final var header = assertDoesNotThrow(() -> PMTiles.readHeader(ODBL_VECTOR_FIRENZE));
        assertSame(ODBL_VECTOR_FIRENZE, header.location());
        assertEquals(InternalCompression.GZIP, header.internalCompression());
        assertTrue(header.clustered());
        assertEquals(TileType.MVT, header.tileType());
        assertEquals(108, header.tileEntries());
        assertEquals(108, header.addressedTiles());
    }

    @Test
    void testMetadata() {
        final var header = assertDoesNotThrow(() -> PMTiles.readHeader(ODBL_VECTOR_FIRENZE));
        final var metadata = assertDoesNotThrow(() -> PMTiles.readMetadata(header));
        assertNotNull(metadata);
        assertEquals(4, metadata.size());
        assertEquals("baselayer", metadata.getString("type"));
        assertEquals("protomaps 2023-01-18T07:49:39Z", metadata.getString("name"));
    }

    @Test
    void testRootDirectory() {
        final var header = assertDoesNotThrow(() -> PMTiles.readHeader(ODBL_VECTOR_FIRENZE));
        final var root = assertDoesNotThrow(() -> PMTiles.readRootDirectory(header));
        assertNotNull(root);
        assertEquals(header.tileEntries(), root.entries().length);
        assertEquals(new DirectoryEntry(0, 0, 588, 1), root.entries()[0]);
        assertEquals(new DirectoryEntry(317221111, 994639, 17276, 1), root.entries()[47]);
        // Any issue with delta encoding will probably show up here
        assertEquals(new DirectoryEntry(317301844, 3927477, 31394, 1), root.entries()[107]);
    }

    @Test
    void testHilbertConversion() {
        assertEquals(0, PMTiles.convertToHilbert(0, 0, 0));
        assertEquals(1, PMTiles.convertToHilbert(1, 0, 0));
        assertEquals(2, PMTiles.convertToHilbert(1, 0, 1));
        assertEquals(3, PMTiles.convertToHilbert(1, 1, 1));
        assertEquals(4, PMTiles.convertToHilbert(1, 1, 0));
        assertEquals(5, PMTiles.convertToHilbert(2, 0, 0));
        // This is from https://protomaps.com/blog/pmtiles-v3-hilbert-tile-ids .
        assertEquals(36052, PMTiles.convertToHilbert(new TileXYZ(8, 40, 87)));
    }

    @Test
    void testTileConversion() {
        assertEquals(new TileXYZ(0, 0, 0), PMTiles.convertToXYZ(0));
        assertEquals(new TileXYZ(1, 0, 0), PMTiles.convertToXYZ(1));
        assertEquals(new TileXYZ(1, 0, 1), PMTiles.convertToXYZ(2));
        assertEquals(new TileXYZ(1, 1, 1), PMTiles.convertToXYZ(3));
        assertEquals(new TileXYZ(1, 1, 0), PMTiles.convertToXYZ(4));
        assertEquals(new TileXYZ(2, 0, 0), PMTiles.convertToXYZ(5));
        assertEquals(new TileXYZ(8, 40, 87), PMTiles.convertToXYZ(36052));
    }

    @Test
    void testTileReading223() throws IOException {
        final var header = assertDoesNotThrow(() -> PMTiles.readHeader(ODBL_RASTER_STAMEN));
        final var root = assertDoesNotThrow(() -> PMTiles.readRootDirectory(header));
        final var data = PMTiles.readData(header, PMTiles.convertToHilbert(2, 2, 3), new DirectoryCache(root));
        assertEquals(new DirectoryEntry(14, 169957, 4503, 1), root.entries()[14]);
        assertEquals(4503, data.length);
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
        assertEquals((byte) 0x4e, data[2]);
        assertEquals((byte) 0x47, data[3]);
        assertEquals((byte) 0x0d, data[4]);
        assertEquals((byte) 0x44, data[4498]);
        assertEquals((byte) 0xae, data[4499]);
        assertEquals((byte) 0x42, data[4500]);
        assertEquals((byte) 0x60, data[4501]);
        assertEquals((byte) 0x82, data[4502]);
    }

    @Test
    void testTileReading352() throws IOException {
        final var header = assertDoesNotThrow(() -> PMTiles.readHeader(ODBL_RASTER_STAMEN));
        final var root = assertDoesNotThrow(() -> PMTiles.readRootDirectory(header));
        final var data = PMTiles.readData(header, PMTiles.convertToHilbert(3, 5, 2), new DirectoryCache(root));
        assertEquals(new DirectoryEntry(76, 662416, 9344, 1), root.entries()[75]);
        assertEquals(9344, data.length);
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
        assertEquals((byte) 0x4e, data[2]);
        assertEquals((byte) 0x47, data[3]);
        assertEquals((byte) 0x0d, data[4]);
        assertEquals((byte) 0x44, data[9339]);
        assertEquals((byte) 0xae, data[9340]);
        assertEquals((byte) 0x42, data[9341]);
        assertEquals((byte) 0x60, data[9342]);
        assertEquals((byte) 0x82, data[9343]);
    }
}
