// License: GPL. For details, see LICENSE file.
package symbols;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import symbols.Symbols.Delta;
import symbols.Symbols.Form;
import symbols.Symbols.Handle;
import symbols.Symbols.Instr;
import symbols.Symbols.Symbol;

/**
 * @author Malcolm Herring
 */
public final class Areas {
    private Areas() {
        // Hide default constructor for utilities classes
    }

    // CHECKSTYLE.OFF: LineLength
    public static final Symbol Plane = new Symbol();
    static {
        Plane.add(new Instr(Form.BBOX, new Rectangle2D.Double(-60, -60, 120, 120)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(40, 20); p.lineTo(50, 10); p.lineTo(27.0, 13.3); p.lineTo(23.7, 6.8); p.lineTo(40.0, 5.0); p.curveTo(55, 4, 55, -9, 40, -10);
        p.quadTo(31, -11, 30, -15); p.lineTo(-30, 2); p.quadTo(-35, -12, -45, -15); p.quadTo(-56, -3, -50, 15); p.lineTo(18.4, 7.3); p.lineTo(21.7, 14); p.lineTo(-20, 20); p.closePath();
        Plane.add(new Instr(Form.PGON, p));
    }

    public static final Symbol LimitDash = new Symbol();
    static {
        LimitDash.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -30, 60, 60)));
        LimitDash.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        LimitDash.add(new Instr(Form.LINE, new Line2D.Double(0, -30, 0, 30)));
    }

    public static final Symbol LimitCC = new Symbol();
    static {
        LimitCC.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -240, 60, 240)));
        LimitCC.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        LimitCC.add(new Instr(Form.LINE, new Line2D.Double(0, -200, 0, -160)));
        LimitCC.add(new Instr(Form.LINE, new Line2D.Double(-20, -180, 20, -180)));
        LimitCC.add(new Instr(Form.LINE, new Line2D.Double(0, -80, 0, -120)));
        LimitCC.add(new Instr(Form.LINE, new Line2D.Double(-20, -100, 20, -100)));
    }

    public static final Symbol Cable = new Symbol();
    static {
        Cable.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        Cable.add(new Instr(Form.STRK, new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Cable.add(new Instr(Form.FILL, new Color(0xc480ff)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(0, 0); p.curveTo(-13, -13, -13, -17, 0, -30); p.curveTo(13, -43, 13, -47, 0, -60);
        Cable.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol CableDot = new Symbol();
    static {
        CableDot.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        CableDot.add(new Instr(Form.RSHP, new Ellipse2D.Double(-10, -40, 20, 20)));
    }

    public static final Symbol CableDash = new Symbol();
    static {
        CableDash.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        CableDash.add(new Instr(Form.STRK, new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        CableDash.add(new Instr(Form.LINE, new Line2D.Double(0, -15, 0, -45)));
    }

    public static final Symbol CableFlash = new Symbol();
    static {
        CableFlash.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-30, -25); p.lineTo(-10, -40); p.lineTo(10, -26); p.lineTo(30, -35); p.lineTo(10, -20); p.lineTo(-10, -34); p.closePath();
        CableFlash.add(new Instr(Form.PGON, p));
    }

    public static final Symbol Dash = new Symbol();
    static {
        Dash.add(new Instr(Form.BBOX, new Rectangle2D.Double(-15, -30, 30, 30)));
        Dash.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)));
        Dash.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -30)));
    }

    public static final Symbol Foul = new Symbol();
    static {
        Foul.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 60)));
        Foul.add(new Instr(Form.STRK, new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Foul.add(new Instr(Form.FILL, Color.black));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, -30); p.lineTo(-20, 30);
        p.moveTo(20, -30); p.lineTo(0, 30);
        p.moveTo(-20, -15); p.lineTo(30, -15);
        p.moveTo(-30, 15); p.lineTo(20, 15);
        Foul.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol LaneArrow = new Symbol();
    static {
        LaneArrow.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -240, 40, 240)));
        LaneArrow.add(new Instr(Form.STRK, new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER)));
        LaneArrow.add(new Instr(Form.FILL, Symbols.Mtss));
        Path2D.Double p = new Path2D.Double(); p.moveTo(15, 0); p.lineTo(15, -195); p.lineTo(40, -195);
        p.lineTo(0, -240); p.lineTo(-40, -195); p.lineTo(-15, -195); p.lineTo(-15, 0); p.closePath();
        LaneArrow.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol LineAnchor = new Symbol();
    static {
        LineAnchor.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 90)));
        LineAnchor.add(new Instr(Form.SYMB, new Symbols.SubSymbol(Harbours.Anchor, 0.5, 0, 0, null, new Delta(Handle.CC, AffineTransform.getRotateInstance(Math.toRadians(-90.0))))));
    }

    public static final Symbol LineFoul = new Symbol();
    static {
        LineFoul.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 90)));
        LineFoul.add(new Instr(Form.SYMB, new Symbols.SubSymbol(Foul, 0.5, 0, 0, null, new Delta(Handle.CC, AffineTransform.getRotateInstance(Math.toRadians(-90.0))))));
    }

    public static final Symbol LinePlane = new Symbol();
    static {
        LinePlane.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 90)));
        LinePlane.add(new Instr(Form.FILL, new Color(0xc480ff)));
        LinePlane.add(new Instr(Form.SYMB, new Symbols.SubSymbol(Areas.Plane, 0.5, 0, 0, null, new Delta(Handle.CC, AffineTransform.getRotateInstance(Math.toRadians(-90.0))))));
    }

    public static final Symbol MarineFarm = new Symbol();
    static {
        MarineFarm.add(new Instr(Form.STRK, new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-23, 12); p.lineTo(-23, 23); p.lineTo(23, 23); p.lineTo(23, 12); p.moveTo(-8, 15); p.lineTo(-8, 23); p.moveTo(8, 15); p.lineTo(8, 23);
        p.moveTo(-23, -12); p.lineTo(-23, -23); p.lineTo(23, -23); p.lineTo(23, -12); p.moveTo(-8, -15); p.lineTo(-8, -23); p.moveTo(8, -15); p.lineTo(8, -23);
        p.moveTo(-21, 8); p.quadTo(-1, -14, 21, 0); p.quadTo(-1, 14, -21, -8); p.moveTo(7, 6); p.quadTo(2, 0, 7, -6);
        MarineFarm.add(new Instr(Form.PLIN, p));
        MarineFarm.add(new Instr(Form.RSHP, new Ellipse2D.Double(9, -2, 4, 4)));
    }

    public static final Symbol NoWake = new Symbol();
    static {
        NoWake.add(new Instr(Form.STRK, new BasicStroke(12, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        NoWake.add(new Instr(Form.FILL, new Color(0xa30075)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-60, 20); p.curveTo(-28, 20, -32, 0, 0, 0); p.curveTo(32, 0, 28, 20, 60, 20); p.moveTo(-60, 0); p.curveTo(-28, 0, -32, -20, 0, -20); p.curveTo(32, -20, 28, 0, 60, 0);
        NoWake.add(new Instr(Form.PLIN, p));
        NoWake.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        NoWake.add(new Instr(Form.LINE, new Line2D.Double(-60, 60, 60, -60)));
        NoWake.add(new Instr(Form.LINE, new Line2D.Double(-60, -60, 60, 60)));
    }

    public static final Symbol Pipeline = new Symbol();
    static {
        Pipeline.add(new Instr(Form.BBOX, new Rectangle2D.Double(-15, -60, 30, 60)));
        Pipeline.add(new Instr(Form.STRK, new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)));
        Pipeline.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -50)));
        Pipeline.add(new Instr(Form.RSHP, new Ellipse2D.Double(-10, -60, 20, 20)));
    }

    public static final Symbol Restricted = new Symbol();
    static {
        Restricted.add(new Instr(Form.BBOX, new Rectangle2D.Double(-15, -30, 30, 30)));
        Restricted.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)));
        Restricted.add(new Instr(Form.LINE, new Line2D.Double(0, 0, 0, -30)));
        Restricted.add(new Instr(Form.LINE, new Line2D.Double(0, -15, 17, -15)));
    }

    public static final Symbol Rock = new Symbol();
    static {
        Rock.add(new Instr(Form.FILL, new Color(0x80c0ff)));
        Rock.add(new Instr(Form.RSHP, new Ellipse2D.Double(-30, -30, 60, 60)));
        Rock.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 5}, 0)));
        Rock.add(new Instr(Form.FILL, Color.black));
        Rock.add(new Instr(Form.ELPS, new Ellipse2D.Double(-30, -30, 60, 60)));
        Rock.add(new Instr(Form.STRK, new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        Rock.add(new Instr(Form.LINE, new Line2D.Double(-20, 0, 20, 0)));
        Rock.add(new Instr(Form.LINE, new Line2D.Double(0, -20, 0, 20)));
    }

    public static final Symbol RockA = new Symbol();
    static {
        RockA.add(new Instr(Form.FILL, new Color(0x80c0ff)));
        RockA.add(new Instr(Form.RSHP, new Ellipse2D.Double(-30, -30, 60, 60)));
        RockA.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 5}, 0)));
        RockA.add(new Instr(Form.FILL, Color.black));
        RockA.add(new Instr(Form.ELPS, new Ellipse2D.Double(-30, -30, 60, 60)));
        RockA.add(new Instr(Form.STRK, new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        RockA.add(new Instr(Form.LINE, new Line2D.Double(-20, 0, 20, 0)));
        RockA.add(new Instr(Form.LINE, new Line2D.Double(0, -20, 0, 20)));
        RockA.add(new Instr(Form.RSHP, new Ellipse2D.Double(-17, -17, 8, 8)));
        RockA.add(new Instr(Form.RSHP, new Ellipse2D.Double(-17, 9, 8, 8)));
        RockA.add(new Instr(Form.RSHP, new Ellipse2D.Double(9, -17, 8, 8)));
        RockA.add(new Instr(Form.RSHP, new Ellipse2D.Double(9, 9, 8, 8)));
    }

    public static final Symbol RockC = new Symbol();
    static {
        RockC.add(new Instr(Form.FILL, new Color(0x80c0ff)));
        RockC.add(new Instr(Form.RSHP, new Ellipse2D.Double(-30, -30, 60, 60)));
        RockC.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 5}, 0)));
        RockC.add(new Instr(Form.FILL, Color.black));
        RockC.add(new Instr(Form.ELPS, new Ellipse2D.Double(-30, -30, 60, 60)));
        RockC.add(new Instr(Form.STRK, new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        RockC.add(new Instr(Form.LINE, new Line2D.Double(-20, 0, 20, 0)));
        RockC.add(new Instr(Form.LINE, new Line2D.Double(-10, 17.3, 10, -17.3)));
        RockC.add(new Instr(Form.LINE, new Line2D.Double(10, 17.3, -10, -17.3)));
    }

    public static final Symbol Seaplane = new Symbol();
    static {
        Seaplane.add(new Instr(Form.BBOX, new Rectangle2D.Double(-60, -60, 120, 120)));
        Seaplane.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        Seaplane.add(new Instr(Form.ELPS, new Ellipse2D.Double(-58, -58, 116, 116)));
        Seaplane.add(new Instr(Form.SYMB, new Symbols.SubSymbol(Areas.Plane, 1.0, 0, 0, null, null)));
    }

    public static final Symbol WindFarm = new Symbol();
    static {
        WindFarm.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        WindFarm.add(new Instr(Form.ELPS, new Ellipse2D.Double(-100, -100, 200, 200)));
        WindFarm.add(new Instr(Form.LINE, new Line2D.Double(-35, 50, 35, 50)));
        WindFarm.add(new Instr(Form.LINE, new Line2D.Double(0, 50, 0, -27.5)));
        WindFarm.add(new Instr(Form.LINE, new Line2D.Double(0, -27.5, 30, -27.5)));
        WindFarm.add(new Instr(Form.LINE, new Line2D.Double(0, -27.5, -13.8, -3.8)));
        WindFarm.add(new Instr(Form.LINE, new Line2D.Double(0, -27.5, -13.8, -53.6)));
    }

    public static final Symbol WreckD = new Symbol();
    static {
        WreckD.add(new Instr(Form.FILL, new Color(0x80c0ff)));
        WreckD.add(new Instr(Form.RSHP, new Ellipse2D.Double(-50, -40, 100, 80)));
        WreckD.add(new Instr(Form.STRK, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 5}, 0)));
        WreckD.add(new Instr(Form.FILL, Color.black));
        WreckD.add(new Instr(Form.ELPS, new Ellipse2D.Double(-50, -40, 100, 80)));
        WreckD.add(new Instr(Form.STRK, new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        WreckD.add(new Instr(Form.LINE, new Line2D.Double(-40, 0, 40, 0)));
        WreckD.add(new Instr(Form.LINE, new Line2D.Double(0, -30, 0, 30)));
        WreckD.add(new Instr(Form.LINE, new Line2D.Double(-20, -15, -20, 15)));
        WreckD.add(new Instr(Form.LINE, new Line2D.Double(20, -15, 20, 15)));
    }

    public static final Symbol WreckND = new Symbol();
    static {
        WreckND.add(new Instr(Form.STRK, new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        WreckND.add(new Instr(Form.LINE, new Line2D.Double(-40, 0, 40, 0)));
        WreckND.add(new Instr(Form.LINE, new Line2D.Double(0, -30, 0, 30)));
        WreckND.add(new Instr(Form.LINE, new Line2D.Double(-20, -15, -20, 15)));
        WreckND.add(new Instr(Form.LINE, new Line2D.Double(20, -15, 20, 15)));
    }

    public static final Symbol WreckS = new Symbol();
    static {
        WreckS.add(new Instr(Form.STRK, new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)));
        WreckS.add(new Instr(Form.ELPS, new Ellipse2D.Double(-6, -6, 12, 12)));
        WreckS.add(new Instr(Form.LINE, new Line2D.Double(-40, 0, -6, 0)));
        WreckS.add(new Instr(Form.LINE, new Line2D.Double(40, 0, 6, 0)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(-30, 0); p.lineTo(-40, -25); p.lineTo(-0.3, -12.6); p.lineTo(13.7, -37.7); p.lineTo(16.3, -36.3);
        p.lineTo(2.7, -11.6); p.lineTo(37.5, 0); p.lineTo(6, 0); p.curveTo(5.6, -8, -5.6, -8, -6, 0); p.closePath();
        WreckS.add(new Instr(Form.PGON, p));
    }

    public static final BufferedImage Sandwaves = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    static {
        Graphics2D g2 = Sandwaves.createGraphics();
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setBackground(new Color(0, true));
        g2.clearRect(0, 0, 100, 100);
        g2.setPaint(Color.black);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0.0, 34.5); p.lineTo(03.3, 30.8); p.lineTo(09.9, 19.3); p.lineTo(13.2, 16.0); p.lineTo(16.5, 16.1); p.lineTo(18.2, 19.5);
        p.lineTo(19.9, 25.0); p.lineTo(21.6, 30.3); p.lineTo(23.3, 33.4); p.lineTo(25.0, 33.3); p.lineTo(28.3, 30.1); p.lineTo(31.6, 25.0); p.lineTo(34.9, 20.1); p.lineTo(38.2, 17.2);
        p.lineTo(41.5, 17.3); p.lineTo(43.2, 20.3); p.lineTo(44.9, 25); p.lineTo(46.6, 29.6); p.lineTo(48.3, 32.2); p.lineTo(50.0, 32.1);        
        p.moveTo(50.0, 84.5); p.lineTo(53.3, 80.8); p.lineTo(56.6, 75.0); p.lineTo(59.9, 69.3); p.lineTo(63.2, 66.0); p.lineTo(66.5, 66.1); p.lineTo(68.2, 69.5); p.lineTo(69.9, 75.0);
        p.lineTo(71.6, 80.3); p.lineTo(73.3, 83.4); p.lineTo(75.0, 83.3); p.lineTo(78.3, 80.1); p.lineTo(81.6, 75.0); p.lineTo(84.9, 70.1); p.lineTo(88.2, 67.2); p.lineTo(91.5, 67.3);
        p.lineTo(93.2, 70.3); p.lineTo(94.9, 75.0); p.lineTo(96.6, 79.6); p.lineTo(98.3, 82.2); p.lineTo(100.0, 82.1);
        g2.draw(p);
    }
    
    public static final Symbol Spring = new Symbol();
    static {
        Spring.add(new Instr(Form.FILL, Color.black));
        Spring.add(new Instr(Form.STRK, new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(0, 0, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(-20, 0, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(-40, 0, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(20, 0, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(40, 0, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(0, -20, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(0, -40, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(0, -60, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(0, -80, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(-15, -90, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(15, -90, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(-35, -85, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(35, -85, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(-50, -70, 10, 10)));
        Spring.add(new Instr(Form.RSHP, new Ellipse2D.Double(50, -70, 10, 10)));
    }

    public static final BufferedImage Seagrass = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
    static {
        Graphics2D g2 = Seagrass.createGraphics();
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setBackground(new Color(0, true));
        g2.clearRect(0, 0, 240, 240);
        g2.setPaint(new Color(0x2E8E20));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(90, 100); p.quadTo(50, 93, 29, 110);
        p.moveTo(78, 98); p.quadTo(120, 75, 115, 50);
        p.moveTo(45, 100); p.quadTo(65, 80, 55, 60); p.quadTo(50, 35, 60, 10);
        p.moveTo(32, 108); p.quadTo(50, 90, 43, 70); p.quadTo(35, 40, 41, 18);
        p.moveTo(32, 108); p.quadTo(0, 70, 16, 46);
        p.moveTo(42, 103); p.quadTo(13, 75, 32, 31);
        p.moveTo(23, 95); p.quadTo(35, 90, 34, 63);
        p.moveTo(59, 99); p.quadTo(75, 75, 71, 22);
        p.moveTo(66, 98); p.quadTo(85, 75, 82, 27);
        p.moveTo(55, 97); p.quadTo(110, 50, 106, 30);
        p.moveTo(90+120, 100+120); p.quadTo(50+120, 93+120, 29+120, 110+120);
        p.moveTo(78+120, 98+120); p.quadTo(120+120, 75+120, 115+120, 50+120);
        p.moveTo(45+120, 100+120); p.quadTo(65+120, 80+120, 55+120, 60+120); p.quadTo(50+120, 35+120, 60+120, 10+120);
        p.moveTo(32+120, 108+120); p.quadTo(50+120, 90+120, 43+120, 70+120); p.quadTo(35+120, 40+120, 41+120, 18+120);
        p.moveTo(32+120, 108+120); p.quadTo(0+120, 70+120, 16+120, 46+120);
        p.moveTo(42+120, 103+120); p.quadTo(13+120, 75+120, 32+120, 31+120);
        p.moveTo(23+120, 95+120); p.quadTo(35+120, 90+120, 34+120, 63+120);
        p.moveTo(59+120, 99+120); p.quadTo(75+120, 75+120, 71+120, 22+120);
        p.moveTo(66+120, 98+120); p.quadTo(85+120, 75+120, 82+120, 27+120);
        p.moveTo(55+120, 97+120); p.quadTo(110+120, 50+120, 106+120, 30+120);
        g2.draw(p);
    }

    public static final Symbol SeagrassP = new Symbol();
    static {
    	SeagrassP.add(new Instr(Form.BBOX, new Rectangle2D.Double(-60, -60, 120, 120)));
    	SeagrassP.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        SeagrassP.add(new Instr(Form.FILL, new Color(0x2E8E20)));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(30, 40); p.quadTo(-10, 33, -31, 50);
        p.moveTo(18, 38); p.quadTo(60, 15, 55, -10);
        p.moveTo(-15, 40); p.quadTo(5, 20, -5, 0); p.quadTo(-10, -25, 0, -50);
        p.moveTo(-28, 48); p.quadTo(-10, 30, -17, 10); p.quadTo(-25, -20, -19, -42);
        p.moveTo(-28, 48); p.quadTo(-60, 10, -44, -14);
        p.moveTo(-18, 43); p.quadTo(-47, 15, -28, -29);
        p.moveTo(-37, 35); p.quadTo(-25, 30, -26, 3);
        p.moveTo(-1, 39); p.quadTo(15, 15, 11, -38);
        p.moveTo(6, 38); p.quadTo(25, 15, 22, -33);
        SeagrassP.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol Rocks = new Symbol();
    static {
        Rocks.add(new Instr(Form.BBOX, new Rectangle2D.Double(-30, -60, 60, 120)));
        Rocks.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Path2D.Double p = new Path2D.Double(); 
        p.moveTo(8, -50); p.quadTo(5, -58, 0, -60); p.quadTo(-15, -50, -8, -30); p.quadTo(-10, -20, 0, -15); p.quadTo(10, -10, 15, -25);
        p.moveTo(0, -15); p.quadTo(-18, -6, -12, 10); p.quadTo(-9, 20, 0, 20); p.quadTo(8, 16, 12, 25);
        p.moveTo(0, 20); p.quadTo(-15, 26, -10, 44); p.quadTo(-12, 55, 0, 60);
        Rocks.add(new Instr(Form.PLIN, p));
    }

    public static final Symbol Coral = new Symbol();
    static {
        Coral.add(new Instr(Form.BBOX, new Rectangle2D.Double(-20, -60, 40, 120)));
        Coral.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Path2D.Double p = new Path2D.Double(); p.moveTo(0, -60); p.lineTo(0, -50); p.moveTo(15, -48); p.lineTo(-20, -50); 
        p.lineTo(0, -30); p.lineTo(-20, -20); p.lineTo(10, 0); p.lineTo(-20, 20); p.lineTo(0, 30); p.lineTo(-20, 50); 
        p.lineTo(20, 45); p.moveTo(0, 49); p.lineTo(0, 60);
        Coral.add(new Instr(Form.PLIN, p));
    }

    public static final BufferedImage Kelp = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
    static {
        Graphics2D g2 = Kelp.createGraphics();
        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setBackground(new Color(0, true));
        g2.clearRect(0, 0, 240, 240);
        g2.setPaint(Color.black);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, 60); p.curveTo(40, 40, 44, 100, 84, 80); p.moveTo(0, 60); p.quadTo(12, 80, 28, 72); p.moveTo(24, 56); p.quadTo(36, 36, 56, 44); p.quadTo(68, 28, 80, 36);
        p.moveTo(56, 44); p.quadTo(68, 60, 80, 52); p.moveTo(52, 76); p.quadTo(72, 60, 96, 68); p.quadTo(108, 84, 120, 76); p.moveTo(96, 68); p.quadTo(108, 52, 120, 60);
        p.moveTo(120, 180); p.curveTo(160, 160, 164, 220, 204, 200); p.moveTo(120, 180); p.quadTo(132, 200, 148, 192); p.moveTo(144, 176); p.quadTo(156, 156, 176, 164); p.quadTo(188, 148, 200, 156);
        p.moveTo(176, 164); p.quadTo(188, 180, 200, 172); p.moveTo(172, 196); p.quadTo(192, 180, 216, 188); p.quadTo(228, 204, 240, 196); p.moveTo(216, 188); p.quadTo(228, 172, 240, 180);
        g2.draw(p);
    }
    
    public static final Symbol KelpP = new Symbol();
    static {
    	KelpP.add(new Instr(Form.BBOX, new Rectangle2D.Double(-60, -60, 120, 120)));
    	KelpP.add(new Instr(Form.STRK, new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
        Path2D.Double p = new Path2D.Double();
        p.moveTo(-60, 0); p.curveTo(-20, -20, -16, 40, 24, 20); p.moveTo(-60, 0); p.quadTo(-48, 20, -32, 12); p.moveTo(-36, -4); p.quadTo(-24, -24, -4, -16); p.quadTo(8, -32, 20, -24);
        p.moveTo(-4, -16); p.quadTo(8, 0, 20, -8); p.moveTo(-8, 16); p.quadTo(12, 0, 36, 8); p.quadTo(48, 24, 60, 16); p.moveTo(36, 8); p.quadTo(48, -8, 60, 0);
        KelpP.add(new Instr(Form.PLIN, p));
    }
}
