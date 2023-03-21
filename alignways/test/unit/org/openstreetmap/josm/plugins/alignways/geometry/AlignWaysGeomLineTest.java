// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.alignways.geometry;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.alignways.geometry.AlignWaysGeomLine.IntersectionStatus;

/**
 * Tests of {@link AlignWaysGeomLine}
 */
class AlignWaysGeomLineTest {
    private AlignWaysGeomLine line_x1y1x2y2, line_par_x1y1x2y2;
    private AlignWaysGeomLine line_abc;
    private AlignWaysGeomLine line_mb;
    private AlignWaysGeomLine line_line;
    private AlignWaysGeomLine line_horiz, line_vert;

    @BeforeEach
    void setUp() {
        line_x1y1x2y2 = new AlignWaysGeomLine(-2.0, -1.0, 4.0, 3.0);
        line_abc = new AlignWaysGeomLine(2.0/3, -1.0, 1.0/3);
        line_mb = new AlignWaysGeomLine(2.0/3, 1.0/3);
        line_line = new AlignWaysGeomLine(line_x1y1x2y2);
        line_par_x1y1x2y2 = new AlignWaysGeomLine(-2.0, -2.0, 4.0, 2.0);
        line_horiz = new AlignWaysGeomLine(-5.0, 3.0, 6.0, 3.0);
        line_vert = new AlignWaysGeomLine(2.0, -3.0, 2.0, -10.5);
    }

    @Test
    void testLineLineEquiv() {
        assertEquals(line_x1y1x2y2, line_line);
    }

    @Test
    void testLineAbcLineEquiv() {
        assertEquals(line_x1y1x2y2, line_abc);
    }

    @Test
    void testLineMbLineEquiv() {
        assertEquals(line_x1y1x2y2, line_mb);
    }

    @Test
    void testLineAbcMbEquiv() {
        assertEquals(line_abc, line_mb);
    }

    @Test
    void testLineLineParallel() {
        line_x1y1x2y2.getIntersection(line_par_x1y1x2y2);
        assertSame(line_x1y1x2y2.getIntersectionStatus(), IntersectionStatus.LINES_PARALLEL);
    }

    @Test
    void testLineAbcOverlap() {
        line_x1y1x2y2.getIntersection(line_abc);
        assertSame(line_x1y1x2y2.getIntersectionStatus(), IntersectionStatus.LINES_OVERLAP);
    }

    @Test
    void testLineMbOverlap() {
        line_x1y1x2y2.getIntersection(line_mb);
        assertSame(line_x1y1x2y2.getIntersectionStatus(), IntersectionStatus.LINES_OVERLAP);
    }

    @Test
    void testAbcMbOverlap() {
        line_abc.getIntersection(line_mb);
        assertSame(line_abc.getIntersectionStatus(), IntersectionStatus.LINES_OVERLAP);
    }

    @Test
    void testGetXOnHoriz() {
        assertTrue(line_horiz.getXonLine(2.0).isNaN());
    }

    @Test
    void testGetYOnVert() {
        assertTrue(line_vert.getYonLine(2.0).isNaN());
    }

    @Test
    void testStatusUndefAfterConstruct() {
        AlignWaysGeomLine newLine = new AlignWaysGeomLine(1.0, 2.0);
        assertSame(newLine.getIntersectionStatus(), IntersectionStatus.UNDEFINED);
    }

}
