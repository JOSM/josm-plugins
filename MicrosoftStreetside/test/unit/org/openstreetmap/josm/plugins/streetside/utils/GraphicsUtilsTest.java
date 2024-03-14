// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;

class GraphicsUtilsTest {

    static List<Arguments> testCubeMapTileToIndex() {
        return Arrays.asList(Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 0, 0), 3, 0),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 1, 0), 3, 1),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 2, 0), 3, 2),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 3, 0), 3, 3),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 4, 0), 3, 4),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 5, 0), 3, 5),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 6, 0), 3, 6),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 7, 0), 3, 7),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 0, 1), 3, 8),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 1, 1), 3, 9),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 2, 1), 3, 10),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 3, 1), 3, 11),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 4, 1), 3, 12),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 5, 1), 3, 13),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 6, 1), 3, 14),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 7, 1), 3, 15),
                Arguments.of(new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 0, 2), 3, 16));
    }

    @ParameterizedTest
    @MethodSource
    void testCubeMapTileToIndex(CubeMapTileXY tile, int zoom, int expectedIndex) {
        assertEquals(expectedIndex, GraphicsUtils.cubeMapTileToIndex(tile, zoom));
    }
}
