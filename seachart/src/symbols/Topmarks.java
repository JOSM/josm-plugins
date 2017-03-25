// License: GPL. For details, see LICENSE file.
package symbols;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;

import s57.S57val.BoySHP;
import s57.S57val.TopSHP;
import symbols.Symbols.Delta;
import symbols.Symbols.Form;
import symbols.Symbols.Handle;
import symbols.Symbols.Instr;
import symbols.Symbols.Symbol;

/**
 * @author Malcolm Herring
 */
public final class Topmarks {

    private Topmarks() {
        // Hide default constructor for utilities classes
    }

    // CHECKSTYLE.OFF: LineLength
    public static final Symbol RadarReflector = new Symbol();
    static {
        RadarReflector.add(new Instr(Form.BBOX, new Rectangle2D.Double(-50, -40, 100, 40)));
        RadarReflector.add(new Instr(Form.STRK, new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        RadarReflector.add(new Instr(Form.FILL, Color.black));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-30, -3); p.quadTo(0, -40, 30, -3); p.moveTo(0, -20); p.lineTo(0, -37);
        p.moveTo(-30, -3); p.lineTo(-43, -14); p.moveTo(30, -3); p.lineTo(44, -14); p.moveTo(-15, -14); p.lineTo(-25, -30); p.moveTo(15, -14); p.lineTo(25, -30);
        RadarReflector.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopBesom = new Symbol();
    static {
        TopBesom.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        TopBesom.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopBesom.add(new Instr(Form.FILL, Color.black));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, 0.0); p.lineTo(0.0, -30.0); p.lineTo(15.0, 0.0); p.moveTo(0.0, 0.0); p.lineTo(0.0, -28.0);
        TopBesom.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopIBesom = new Symbol();
    static {
        TopIBesom.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        TopIBesom.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopIBesom.add(new Instr(Form.FILL, Color.black));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -30.0); p.lineTo(0.0, 0.0); p.lineTo(15.0, -30.0);
        TopIBesom.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopBoard = new Symbol();
    static {
        TopBoard.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-29, -1); p.lineTo(-29, -59); p.lineTo(29, -59); p.lineTo(29, -1); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(-29, -1); p.lineTo(-29, -59); p.lineTo(29, -59); p.lineTo(29, -1); p.closePath();
        p.moveTo(-19, -10); p.lineTo(-19, -49); p.lineTo(19, -49); p.lineTo(19, -10); p.closePath();
        colours.add(new Instr(Form.B1, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(-29, -1); p.lineTo(-29, -30); p.lineTo(29, -30); p.lineTo(29, -59); p.lineTo(0, -59); p.lineTo(0, -1); p.closePath();
        colours.add(new Instr(Form.S2, p));
        p = new Path2D.Double(); p.moveTo(-29, -1); p.lineTo(-29, -30); p.lineTo(0, -30); p.lineTo(0, -1); p.closePath();
        colours.add(new Instr(Form.S3, p));
        p = new Path2D.Double(); p.moveTo(0, -1); p.lineTo(0, -30); p.lineTo(29, -30); p.lineTo(29, -1); p.closePath();
        colours.add(new Instr(Form.S4, p));
        TopBoard.add(new Instr(Form.COLR, colours));
        TopBoard.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopBoard.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-29, -1); p.lineTo(-29, -59); p.lineTo(29, -59); p.lineTo(29.0, -1); p.closePath();
        TopBoard.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopCan = new Symbol();
    static {
        TopCan.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-12.0, -15.0); p.lineTo(-12.0, -48.0); p.lineTo(12.0, -48.0); p.lineTo(12.0, -15.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopCan.add(new Instr(Form.COLR, colours));
        TopCan.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopCan.add(new Instr(Form.FILL, Color.black));
        TopCan.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        p = new Path2D.Double(); p.moveTo(-12.0, -15.0); p.lineTo(-12.0, -48.0); p.lineTo(12.0, -48.0); p.lineTo(12.0, -15.0); p.closePath();
        TopCan.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopCanSphere = new Symbol();
    public static final Symbol TopCircle = new Symbol();
    public static final Symbol TopCircleTriangle = new Symbol();
    public static final Symbol TopCone = new Symbol();
    static {
        TopCone.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -15.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -15.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopCone.add(new Instr(Form.COLR, colours));
        TopCone.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopCone.add(new Instr(Form.FILL, Color.black));
        TopCone.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        p = new Path2D.Double(); p.moveTo(-15.0, -15.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -15.0); p.closePath();
        TopCone.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopConeSphere = new Symbol();
    static {
        TopConeSphere.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -47.0); p.lineTo(0.0, -77.0); p.lineTo(15.0, -47.0); p.closePath();
        p.moveTo(-14.0, -28.0); p.curveTo(-14.0, -46.7, 14.0, -46.7, 14.0, -28.0); p.curveTo(14.0, -9.3, -14.0, -9.3, -14.0, -28.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopConeSphere.add(new Instr(Form.COLR, colours));
        TopConeSphere.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopConeSphere.add(new Instr(Form.FILL, Color.black));
        TopConeSphere.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        TopConeSphere.add(new Instr(Form.LINE, new Line2D.Double(0, -42, 0, -47)));
        p = new Path2D.Double(); p.moveTo(-15.0, -47.0); p.lineTo(0.0, -77.0); p.lineTo(15.0, -47.0); p.closePath();
        TopConeSphere.add(new Instr(Form.ELPS, new Ellipse2D.Double(-14, -42, 28, 28)));
        TopConeSphere.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopCross = new Symbol();
    static {
        TopCross.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-5.0, -15.0); p.lineTo(-5.0, -32.5); p.lineTo(-22.5, -32.5); p.lineTo(-22.5, -42.5); p.lineTo(-5.0, -42.5);
        p.lineTo(-5.0, -60.0); p.lineTo(5.0, -60.0); p.lineTo(5.0, -42.5); p.lineTo(22.5, -42.5); p.lineTo(22.5, -32.5); p.lineTo(5.0, -32.5); p.lineTo(5.0, -15.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopCross.add(new Instr(Form.COLR, colours));
        TopCross.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopCross.add(new Instr(Form.FILL, Color.black));
        TopCross.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        TopCross.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(-5.0, -15.0); p.lineTo(-5.0, -32.5); p.lineTo(-22.5, -32.5); p.lineTo(-22.5, -42.5); p.lineTo(-5.0, -42.5); p.lineTo(-5.0, -60.0);
        p.lineTo(5.0, -60.0); p.lineTo(5.0, -42.5); p.lineTo(22.5, -42.5); p.lineTo(22.5, -32.5); p.lineTo(5.0, -32.5); p.lineTo(5.0, -15.0); p.closePath();
        TopCross.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopCrosses = new Symbol();

    public static final Symbol TopCrossCircle = new Symbol();

    public static final Symbol TopCube = new Symbol();
    static {
        TopCube.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0, -48); p.lineTo(-15, -37); p.lineTo(-15, -20); p.lineTo(0, -9); p.lineTo(15, -20); p.lineTo(15, -37); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopCube.add(new Instr(Form.COLR, colours));
        TopCube.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopCube.add(new Instr(Form.FILL, Color.black));
        TopCube.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        p = new Path2D.Double(); p.moveTo(0, -48); p.lineTo(-14, -37); p.lineTo(0, -26); p.lineTo(14, -37); p.closePath(); p.moveTo(0, -26); p.lineTo(0, 0);
        p.moveTo(-15, -37); p.lineTo(-15, -20); p.lineTo(0, -9); p.lineTo(15, -20); p.lineTo(15, -37);
        TopCube.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopEast = new Symbol();
    static {
        TopEast.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0.0, -80.0); p.lineTo(-15.0, -47.0); p.lineTo(15.0, -47.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(0.0, -10.0); p.lineTo(-15.0, -43.0); p.lineTo(15.0, -43.0); p.closePath();
        colours.add(new Instr(Form.P2, p));
        TopEast.add(new Instr(Form.COLR, colours));
        TopEast.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopEast.add(new Instr(Form.FILL, Color.black));
        TopEast.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -10)));
        TopEast.add(new Instr(Form.LINE, new Line2D.Double(0, -43, 0, -47)));
        TopEast.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(0.0, -10.0); p.lineTo(-15.0, -43.0); p.lineTo(15.0, -43.0); p.closePath();
        p.moveTo(0.0, -80.0); p.lineTo(-15.0, -47.0); p.lineTo(15.0, -47.0); p.closePath();
        TopEast.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopFlag = new Symbol();
    static {
        TopFlag.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0, -40); p.lineTo(0, -10); p.lineTo(39, -10); p.lineTo(39, -40); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(0, -25); p.lineTo(0, -10); p.lineTo(40, -10); p.lineTo(39, -25); p.closePath();
        colours.add(new Instr(Form.H2, p));
        p = new Path2D.Double(); p.moveTo(0, -30); p.lineTo(0, -20); p.lineTo(40, -20); p.lineTo(39, -30); p.closePath();
        colours.add(new Instr(Form.H3, p));
        p = new Path2D.Double(); p.moveTo(19.5, -40); p.lineTo(19.5, -10); p.lineTo(39, -10); p.lineTo(39, -40); p.closePath();
        colours.add(new Instr(Form.V2, p));
        p = new Path2D.Double(); p.moveTo(13, -40); p.lineTo(13, -10); p.lineTo(26, -10); p.lineTo(26, -40); p.closePath();
        colours.add(new Instr(Form.V3, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(0, -40); p.lineTo(0, -10); p.lineTo(39, -10); p.lineTo(39, -40); p.closePath();
        p.moveTo(8, -35); p.lineTo(8, -15); p.lineTo(33, -15); p.lineTo(33, -35); p.closePath();
        colours.add(new Instr(Form.B1, p));
        p = new Path2D.Double(); p.moveTo(0, -25); p.lineTo(0, -10); p.lineTo(20, -10); p.lineTo(20, -40); p.lineTo(39, -40); p.lineTo(39, -25); p.closePath();
        colours.add(new Instr(Form.S2, p));
        p = new Path2D.Double(); p.moveTo(0, -25); p.lineTo(0, -10); p.lineTo(20, -10); p.lineTo(20, -25); p.closePath();
        colours.add(new Instr(Form.S3, p));
        p = new Path2D.Double(); p.moveTo(20, -25); p.lineTo(20, -10); p.lineTo(39, -10); p.lineTo(39, -25); p.closePath();
        colours.add(new Instr(Form.S4, p));
        TopFlag.add(new Instr(Form.COLR, colours));
        TopFlag.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopFlag.add(new Instr(Form.FILL, Color.black));
        TopFlag.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -45)));
        TopFlag.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopFlag.add(new Instr(Form.RECT, new Rectangle2D.Double(0, -40, 39, 30)));
    }

    public static final Symbol TopIcone = new Symbol();
    static {
        TopIcone.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -45.0); p.lineTo(0.0, -15.0); p.lineTo(15.0, -45.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopIcone.add(new Instr(Form.COLR, colours));
        TopIcone.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopIcone.add(new Instr(Form.FILL, Color.black));
        TopIcone.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        p = new Path2D.Double(); p.moveTo(-15.0, -45.0); p.lineTo(0.0, -15.0); p.lineTo(15.0, -45.0); p.closePath();
        TopIcone.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopIsol = new Symbol();
    static {
        TopIsol.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-13.0, -55.0); p.curveTo(-13.0, -72.3, 13.0, -72.3, 13.0, -55.0); p.curveTo(13.0, -37.7, -13.0, -37.7, -13.0, -55.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-13.0, -28.0); p.curveTo(-13.0, -45.3, 13.0, -45.3, 13.0, -28.0); p.curveTo(13.0, -10.7, -13.0, -10.7, -13.0, -28.0); p.closePath();
        colours.add(new Instr(Form.P2, p));
        TopIsol.add(new Instr(Form.COLR, colours));
        TopIsol.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopIsol.add(new Instr(Form.FILL, Color.black));
        TopIsol.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        TopIsol.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopIsol.add(new Instr(Form.ELPS, new Ellipse2D.Double(-13, -41, 26, 26)));
        TopIsol.add(new Instr(Form.ELPS, new Ellipse2D.Double(-13, -68, 26, 26)));
    }

    public static final Symbol TopMooring = new Symbol();
    static {
        TopMooring.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        TopMooring.add(new Instr(Form.STRK, new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopMooring.add(new Instr(Form.FILL, Color.black));
        TopMooring.add(new Instr(Form.ELPS, new Ellipse2D.Double(-1.5, -6, 3, 3)));
        TopMooring.add(new Instr(Form.ELPS, new Ellipse2D.Double(-8.5, -25, 17, 17)));
    }

    public static final Symbol TopNorth = new Symbol();
    static {
        TopNorth.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0.0, -78.0); p.lineTo(-15.0, -45.0); p.lineTo(15.0, -45.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-15.0, -10.0); p.lineTo(0.0, -43.0); p.lineTo(15.0, -10.0); p.closePath();
        colours.add(new Instr(Form.P2, p));
        TopNorth.add(new Instr(Form.COLR, colours));
        TopNorth.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopNorth.add(new Instr(Form.FILL, Color.black));
        TopNorth.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -10)));
        TopNorth.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(-15.0, -10.0); p.lineTo(0.0, -43.0); p.lineTo(15.0, -10.0); p.closePath();
        p.moveTo(0.0, -78.0); p.lineTo(-15.0, -45.0); p.lineTo(15.0, -45.0); p.closePath();
        TopNorth.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopSouth = new Symbol();
    static {
        TopSouth.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -78.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -78.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(0.0, -10.0); p.lineTo(-15.0, -43.0); p.lineTo(15.0, -43.0); p.closePath();
        colours.add(new Instr(Form.P2, p));
        TopSouth.add(new Instr(Form.COLR, colours));
        TopSouth.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSouth.add(new Instr(Form.FILL, Color.black));
        TopSouth.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -10)));
        TopSouth.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(0.0, -10.0); p.lineTo(-15.0, -43.0); p.lineTo(15.0, -43.0); p.closePath();
        p.moveTo(-15.0, -78.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -78.0); p.closePath();
        TopSouth.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopSphere = new Symbol();
    static {
        TopSphere.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-14.0, -28.0); p.curveTo(-14.0, -46.7, 14.0, -46.7, 14.0, -28.0); p.curveTo(14.0, -9.3, -14.0, -9.3, -14.0, -28.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopSphere.add(new Instr(Form.COLR, colours));
        TopSphere.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSphere.add(new Instr(Form.FILL, Color.black));
        TopSphere.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -15)));
        TopSphere.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSphere.add(new Instr(Form.ELPS, new Ellipse2D.Double(-14, -42, 28, 28)));
    }

    public static final Symbol TopSquare = new Symbol();
    static {
        TopSquare.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-13.0, -1.0); p.lineTo(-13.0, -27.0); p.lineTo(13.0, -27.0); p.lineTo(13.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-13.0, -15.0); p.lineTo(-13.0, -1.0); p.lineTo(13.0, -1.0); p.lineTo(13.0, -15.0); p.closePath();
        colours.add(new Instr(Form.H2, p));
        p = new Path2D.Double(); p.moveTo(-13.0, -19.3); p.lineTo(-13.0, -10.7); p.lineTo(13.0, -10.7); p.lineTo(13.0, -19.3); p.closePath();
        colours.add(new Instr(Form.H3, p));
        p = new Path2D.Double(); p.moveTo(0.0, -1.0); p.lineTo(0.0, -27.0); p.lineTo(13.0, -27.0); p.lineTo(13.0, -1.0); p.closePath();
        colours.add(new Instr(Form.V2, p));
        p = new Path2D.Double(); p.moveTo(-4.3, -1.0); p.lineTo(-4.3, -27.0); p.lineTo(4.3, -27.0); p.lineTo(4.3, -1.0); p.closePath();
        colours.add(new Instr(Form.V3, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(-13.0, -1.0); p.lineTo(-13.0, -27.0); p.lineTo(13.0, -27.0); p.lineTo(13.0, -1.0); p.closePath();
        p.moveTo(-8.0, -6.0); p.lineTo(-8.0, -22.0); p.lineTo(8.0, -22.0); p.lineTo(8.0, -6.0); p.closePath();
        colours.add(new Instr(Form.B1, p));
        TopSquare.add(new Instr(Form.COLR, colours));
        TopSquare.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSquare.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-10.0, -1.0); p.lineTo(-10.0, -21.0); p.lineTo(12.0, -21.0); p.lineTo(10.0, -1.0); p.closePath();
        TopSquare.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopRectangleH = new Symbol();
    static {
        TopRectangleH.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-14.0, -1.0); p.lineTo(-14.0, -23.0); p.lineTo(14.0, -23.0); p.lineTo(14.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-14.0, -1.0); p.lineTo(-14.0, -12.0); p.lineTo(14.0, -12.0); p.lineTo(14.0, -1.0); p.closePath();
        colours.add(new Instr(Form.H2, p));
        p = new Path2D.Double(); p.moveTo(-14.0, -16.0); p.lineTo(-14.0, -8.0); p.lineTo(14.0, -8.0); p.lineTo(14.0, -16.0); p.closePath();
        colours.add(new Instr(Form.H3, p));
        p = new Path2D.Double(); p.moveTo(0.0, -1.0); p.lineTo(0.0, -23.0); p.lineTo(14.0, -23.0); p.lineTo(14.0, -1.0); p.closePath();
        colours.add(new Instr(Form.V2, p));
        p = new Path2D.Double(); p.moveTo(-5.0, -1.0); p.lineTo(-5.0, -23.0); p.lineTo(5.0, -23.0); p.lineTo(5.0, -1.0); p.closePath();
        colours.add(new Instr(Form.V3, p));
        TopRectangleH.add(new Instr(Form.COLR, colours));
        TopRectangleH.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopRectangleH.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-14.0, -1.0); p.lineTo(-14.0, -23.0); p.lineTo(14.0, -23.0); p.lineTo(14.0, -1.0); p.closePath();
        TopRectangleH.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopRectangleV = new Symbol();
    static {
        TopRectangleV.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-11.0, -1.0); p.lineTo(-11.0, -29.0); p.lineTo(11.0, -29.0); p.lineTo(11.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-11.0, -1.0); p.lineTo(-11.0, -15.0); p.lineTo(11.0, -15.0); p.lineTo(11.0, -1.0); p.closePath();
        colours.add(new Instr(Form.H2, p));
        p = new Path2D.Double(); p.moveTo(-11.0, -10.0); p.lineTo(-11.0, -20.0); p.lineTo(11.0, -20.0); p.lineTo(11.0, -10.0); p.closePath();
        colours.add(new Instr(Form.H3, p));
        p = new Path2D.Double(); p.moveTo(0.0, -1.0); p.lineTo(0.0, -29.0); p.lineTo(11.0, -29.0); p.lineTo(11.0, -1.0); p.closePath();
        colours.add(new Instr(Form.V2, p));
        p = new Path2D.Double(); p.moveTo(-4.0, -1.0); p.lineTo(-4.0, -29.0); p.lineTo(4.0, -29.0); p.lineTo(4.0, -1.0); p.closePath();
        colours.add(new Instr(Form.V3, p));
        TopRectangleV.add(new Instr(Form.COLR, colours));
        TopRectangleV.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopRectangleV.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-11.0, -1.0); p.lineTo(-11.0, -29.0); p.lineTo(11.0, -29.0); p.lineTo(11.0, -1.0); p.closePath();
        TopRectangleV.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopRhombus = new Symbol();
    static {
        TopRhombus.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0.0, -29.0); p.lineTo(-15.0, -15.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -15.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-15.0, -15.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -15.0); p.closePath();
        colours.add(new Instr(Form.H2, p));
        p = new Path2D.Double(); p.moveTo(-10.0, -19.7); p.lineTo(-15.0, -15.0); p.lineTo(-10.0, -10.3); p.lineTo(10.0, -10.3); p.lineTo(15.0, -15.0); p.lineTo(10.0, -19.7); p.closePath();
        colours.add(new Instr(Form.H3, p));
        p = new Path2D.Double(); p.moveTo(0.0, -29.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -15.0); p.closePath();
        colours.add(new Instr(Form.V2, p));
        p = new Path2D.Double(); p.moveTo(0.0, -29.0); p.lineTo(-5.0, -24.3); p.lineTo(-5.0, -5.7); p.lineTo(0.0, -1.0); p.lineTo(5.0, -5.7); p.lineTo(5.0, -24.3); p.closePath();
        colours.add(new Instr(Form.V3, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(0.0, -29.0); p.lineTo(-15.0, -15.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -15.0); p.closePath();
        p.moveTo(0.0, -23.0); p.lineTo(-9.0, -15.0); p.lineTo(0.0, -7.0); p.lineTo(9.0, -15.0); p.closePath();
        colours.add(new Instr(Form.B1, p));
        TopRhombus.add(new Instr(Form.COLR, colours));
        TopRhombus.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopRhombus.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(0.0, -29.0); p.lineTo(-15.0, -15.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -15.0); p.closePath();
        TopRhombus.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopRhombusCircle = new Symbol();

    public static final Symbol TopSphereRhombus = new Symbol();
    static {
        TopSphereRhombus.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        TopSphereRhombus.add(new Instr(Form.SYMB, new Symbols.SubSymbol(TopRhombus, 1.0, 0, 0, null, null)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-14.0, -44.0); p.curveTo(-14.0, -62.7, 14.0, -62.7, 14.0, -44.0); p.curveTo(14.0, -25.3, -14.0, -25.3, -14.0, -44.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopSphereRhombus.add(new Instr(Form.COLR, colours));
        TopSphereRhombus.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSphereRhombus.add(new Instr(Form.FILL, Color.black));
        TopSphereRhombus.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopSphereRhombus.add(new Instr(Form.ELPS, new Ellipse2D.Double(-14, -58, 28, 28)));
    }

    public static final Symbol TopT = new Symbol();

    public static final Symbol TopTrapeziumU = new Symbol();
    static {
        TopTrapeziumU.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-20.0, -1.0); p.lineTo(-13.0, -27.0); p.lineTo(13.0, -27.0); p.lineTo(20.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopTrapeziumU.add(new Instr(Form.COLR, colours));
        TopTrapeziumU.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopTrapeziumU.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-20.0, -1.0); p.lineTo(-13.0, -27.0); p.lineTo(13.0, -27.0); p.lineTo(20.0, -1.0); p.closePath();
        TopTrapeziumU.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopTrapeziumD = new Symbol();
    static {
        TopTrapeziumD.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-13.0, -1.0); p.lineTo(-20.0, -27.0); p.lineTo(20.0, -27.0); p.lineTo(13.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopTrapeziumD.add(new Instr(Form.COLR, colours));
        TopTrapeziumD.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopTrapeziumD.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-13.0, -1.0); p.lineTo(-20.0, -27.0); p.lineTo(20.0, -27.0); p.lineTo(13.0, -1.0); p.closePath();
        TopTrapeziumD.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopTriangle = new Symbol();
    static {
        TopTriangle.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -1.0); p.lineTo(0.0, -29.0); p.lineTo(15.0, -1.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(-15.0, -1.0); p.lineTo(0.0, -29.0); p.lineTo(15.0, -1.0); p.closePath();
        p.moveTo(-10.0, -6.0); p.lineTo(0.0, -24.0); p.lineTo(10.0, -6.0); p.closePath();
        colours.add(new Instr(Form.B1, p));
        TopTriangle.add(new Instr(Form.COLR, colours));
        TopTriangle.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopTriangle.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-15.0, -1.0); p.lineTo(0.0, -29.0); p.lineTo(15.0, -1.0); p.closePath();
        TopTriangle.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopItriangle = new Symbol();
    static {
        TopItriangle.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -80, 40, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -29.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -29.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        p.moveTo(-15.0, -29.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -29.0); p.closePath();
        p.moveTo(-10.0, -24.0); p.lineTo(0.0, -6.0); p.lineTo(10.0, -24.0); p.closePath();
        colours.add(new Instr(Form.B1, p));
        TopItriangle.add(new Instr(Form.COLR, colours));
        TopItriangle.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopItriangle.add(new Instr(Form.FILL, Color.black));
        p = new Path2D.Double(); p.moveTo(-15.0, -29.0); p.lineTo(0.0, -1.0); p.lineTo(15.0, -29.0); p.closePath();
        TopItriangle.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopTriangleCircle = new Symbol();

    public static final Symbol TopWest = new Symbol();
    static {
        TopWest.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(-15.0, -78.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -78.0); p.closePath();
        colours.add(new Instr(Form.P1, p));
        p = new Path2D.Double(); p.moveTo(-15.0, -10.0); p.lineTo(0.0, -43.0); p.lineTo(15.0, -10.0); p.closePath();
        colours.add(new Instr(Form.P2, p));
        TopWest.add(new Instr(Form.COLR, colours));
        TopWest.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopWest.add(new Instr(Form.FILL, Color.black));
        TopWest.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -10)));
        TopWest.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(-15.0, -10.0); p.lineTo(0.0, -43.0); p.lineTo(15.0, -10.0); p.closePath();
        p.moveTo(-15.0, -78.0); p.lineTo(0.0, -45.0); p.lineTo(15.0, -78.0); p.closePath();
        TopWest.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopX = new Symbol();
    static {
        TopX.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -80, 60, 80)));
        Symbol colours = new Symbol();
        Path2D.Double p = new Path2D.Double(); p.moveTo(0.0, -27.7); p.lineTo(-12.4, -15.7); p.lineTo(-19.3, -22.6); p.lineTo(-7.3, -35.0); p.lineTo(-19.3, -47.3);
        p.lineTo(-12.4, -54.2); p.lineTo(0.0, -42.4); p.lineTo(12.4, -54.2); p.lineTo(19.3, -47.3); p.lineTo(7.3, -35.0); p.lineTo(19.3, -22.6); p.lineTo(12.4, -15.7); p.closePath();
        colours.add(new Instr(Form.P1, p));
        TopX.add(new Instr(Form.COLR, colours));
        TopX.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        TopX.add(new Instr(Form.FILL, Color.black));
        TopX.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -27)));
        TopX.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        p = new Path2D.Double(); p.moveTo(0.0, -27.7); p.lineTo(-12.4, -15.7); p.lineTo(-19.3, -22.6); p.lineTo(-7.3, -35.0); p.lineTo(-19.3, -47.3); p.lineTo(-12.4, -54.2); p.lineTo(0.0, -42.4);
        p.lineTo(12.4, -54.2); p.lineTo(19.3, -47.3); p.lineTo(7.3, -35.0); p.lineTo(19.3, -22.6); p.lineTo(12.4, -15.7); p.closePath();
        TopX.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol TopOther = new Symbol();

    public static final EnumMap<TopSHP, Symbol> Shapes = new EnumMap<>(TopSHP.class);
    static {
        Shapes.put(TopSHP.TOP_BESM, TopBesom); Shapes.put(TopSHP.TOP_IBESM, TopIBesom); Shapes.put(TopSHP.TOP_BORD, TopBoard); Shapes.put(TopSHP.TOP_CAN, TopCan);
        Shapes.put(TopSHP.TOP_CONE, TopCone); Shapes.put(TopSHP.TOP_CROS, TopCross); Shapes.put(TopSHP.TOP_EAST, TopEast); Shapes.put(TopSHP.TOP_ICONE, TopIcone);
        Shapes.put(TopSHP.TOP_ISD, TopIsol); Shapes.put(TopSHP.TOP_NORTH, TopNorth); Shapes.put(TopSHP.TOP_SOUTH, TopSouth); Shapes.put(TopSHP.TOP_SPHR, TopSphere);
        Shapes.put(TopSHP.TOP_SQUR, TopSquare); Shapes.put(TopSHP.TOP_TRI, TopTriangle); Shapes.put(TopSHP.TOP_ITRI, TopItriangle); Shapes.put(TopSHP.TOP_WEST, TopWest);
        Shapes.put(TopSHP.TOP_SALT, TopX); Shapes.put(TopSHP.TOP_RHOM, TopRhombus); Shapes.put(TopSHP.TOP_FLAG, TopFlag); Shapes.put(TopSHP.TOP_CUBE, TopCube);
        Shapes.put(TopSHP.TOP_SPRH, TopSphereRhombus); Shapes.put(TopSHP.TOP_HRECT, TopRectangleH); Shapes.put(TopSHP.TOP_VRECT, TopRectangleV);
        Shapes.put(TopSHP.TOP_TRAP, TopTrapeziumU); Shapes.put(TopSHP.TOP_ITRAP, TopTrapeziumD); Shapes.put(TopSHP.TOP_COSP, TopConeSphere);
        Shapes.put(TopSHP.TOP_CIRC, TopCircle); Shapes.put(TopSHP.TOP_CRSS, TopCrosses); Shapes.put(TopSHP.TOP_T, TopT); Shapes.put(TopSHP.TOP_TRCL, TopTriangleCircle);
        Shapes.put(TopSHP.TOP_CRCL, TopCrossCircle); Shapes.put(TopSHP.TOP_RHCL, TopRhombusCircle); Shapes.put(TopSHP.TOP_CLTR, TopCircleTriangle);
        Shapes.put(TopSHP.TOP_CYSP, TopCanSphere); Shapes.put(TopSHP.TOP_OTHR, TopOther);
    }

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    public static final EnumMap<BoySHP, Delta> BuoyDeltas = new EnumMap<>(BoySHP.class);
    static {
        BuoyDeltas.put(BoySHP.BOY_PILR, new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 31.5, -95.0)));
        BuoyDeltas.put(BoySHP.BOY_SPAR, new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 31.5, -95.0)));
        BuoyDeltas.put(BoySHP.BOY_CAN,  new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 12.7, -37.9)));
        BuoyDeltas.put(BoySHP.BOY_CONE, new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 12.7, -37.9)));
        BuoyDeltas.put(BoySHP.BOY_SPHR, new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 12.7, -37.9)));
        BuoyDeltas.put(BoySHP.BOY_BARL, new Delta(Handle.BC, new AffineTransform(0.948324, 0.317305, -0.3173047, 0.948324, 12.7, -37.9)));
        BuoyDeltas.put(BoySHP.BOY_SUPR, new Delta(Handle.BC, AffineTransform.getTranslateInstance(0.0, -42.0)));
        BuoyDeltas.put(BoySHP.BOY_ICE,  new Delta(Handle.BC, AffineTransform.getTranslateInstance(0.0, -25.0)));
    }

    public static final Delta FloatDelta  = new Delta(Handle.BC, AffineTransform.getTranslateInstance(0.0, -42.0));
    public static final Delta BeaconDelta = new Delta(Handle.BC, AffineTransform.getTranslateInstance(0.0, -70.0));
    public static final Delta LightDelta  = new Delta(Handle.BC, AffineTransform.getTranslateInstance(0.0, -20.0));
    // CHECKSTYLE.ON: SingleSpaceSeparator
}
