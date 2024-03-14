// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.utils.TestUtil;

class StreetsideAbstractImageTest {
    private StreetsideImage image;
    private String imageUrlBase;

    @BeforeEach
    void setup() {
        this.image = TestUtil.generateImage("1013203010232123", 39.065321, -108.553035);
        this.imageUrlBase = "https://ecn.t0.tiles.virtualearth.net/tiles/hs1013203010232123{0}?"
                + "g=14336&key=Arzdiw4nlOJzRwOz__qailc8NiR31Tt51dN2D7cm57NrnceZnCpgOkmJhNpGoppU";
    }

    @Test
    void testThumbnail() {
        assertEquals(MessageFormat.format(this.imageUrlBase, "01"), this.image.getThumbnail());
    }

    private static CubeMapTileXY frontCube(int x, int y) {
        return new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, x, y);
    }

    @Test
    void testTilesZoom1() {
        // 2x2 (4 total tiles)
        final var z1 = image.getFaceTiles(CubemapUtils.CubemapFaces.FRONT, 1)
                .collect(Collectors.toMap(p -> p.a, p -> p.b));
        assertEquals(MessageFormat.format(this.imageUrlBase, "010"), z1.get(frontCube(0, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "011"), z1.get(frontCube(1, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "012"), z1.get(frontCube(0, 1)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "013"), z1.get(frontCube(1, 1)));
        assertEquals(4, z1.size());
    }

    @Test
    void testTilesZoom2() {
        // 4x4 (16 total tiles)
        final var z2 = image.getFaceTiles(CubemapUtils.CubemapFaces.FRONT, 2)
                .collect(Collectors.toMap(p -> p.a, p -> p.b));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0100"), z2.get(frontCube(0, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0101"), z2.get(frontCube(1, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0102"), z2.get(frontCube(0, 1)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0103"), z2.get(frontCube(1, 1)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0110"), z2.get(frontCube(2, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0111"), z2.get(frontCube(3, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0112"), z2.get(frontCube(2, 1)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0113"), z2.get(frontCube(3, 1)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0120"), z2.get(frontCube(0, 2)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0121"), z2.get(frontCube(1, 2)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0122"), z2.get(frontCube(0, 3)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0123"), z2.get(frontCube(1, 3)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0130"), z2.get(frontCube(2, 2)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0131"), z2.get(frontCube(3, 2)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0132"), z2.get(frontCube(2, 3)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "0133"), z2.get(frontCube(3, 3)));
        assertEquals(16, z2.size());
    }

    @Test
    void testTilesZoom3() {
        // 8x8 (64 total tiles)
        final var z3 = image.getFaceTiles(CubemapUtils.CubemapFaces.FRONT, 3)
                .collect(Collectors.toMap(p -> p.a, p -> p.b));
        // Just check the first row to keep test size smallish
        assertEquals(MessageFormat.format(this.imageUrlBase, "01000"), z3.get(frontCube(0, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01001"), z3.get(frontCube(1, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01010"), z3.get(frontCube(2, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01011"), z3.get(frontCube(3, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01100"), z3.get(frontCube(4, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01101"), z3.get(frontCube(5, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01110"), z3.get(frontCube(6, 0)));
        assertEquals(MessageFormat.format(this.imageUrlBase, "01111"), z3.get(frontCube(7, 0)));
        assertEquals(64, z3.size());
    }

    static Stream<Arguments> testQuadKeyToXY() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // 2x2
        builder.add(Arguments.of("0", 0, 0));
        builder.add(Arguments.of("1", 1, 0));
        builder.add(Arguments.of("2", 0, 1));
        builder.add(Arguments.of("3", 1, 1));
        // 4x4
        builder.add(Arguments.of("00", 0, 0));
        builder.add(Arguments.of("01", 1, 0));
        builder.add(Arguments.of("02", 0, 1));
        builder.add(Arguments.of("03", 1, 1));
        builder.add(Arguments.of("10", 2, 0));
        builder.add(Arguments.of("11", 3, 0));
        builder.add(Arguments.of("12", 2, 1));
        builder.add(Arguments.of("13", 3, 1));
        builder.add(Arguments.of("20", 0, 2));
        builder.add(Arguments.of("21", 1, 2));
        builder.add(Arguments.of("22", 0, 3));
        builder.add(Arguments.of("23", 1, 3));
        builder.add(Arguments.of("30", 2, 2));
        builder.add(Arguments.of("31", 3, 2));
        builder.add(Arguments.of("32", 2, 3));
        builder.add(Arguments.of("33", 3, 3));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testQuadKeyToXY(String quadKey, int x, int y) {
        final var xy = StreetsideAbstractImage.quadKeyToTile(quadKey);
        assertAll(() -> assertEquals(x, xy.getXIndex(), "x"), () -> assertEquals(y, xy.getYIndex(), "y"));
    }
}
