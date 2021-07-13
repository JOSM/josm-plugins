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
public final class Charts {
    private Charts() {
        // Hide default constructor for utilities classes
    }

    // CHECKSTYLE.OFF: LineLength
    public static final Symbol Grid = new Symbol();
    static {
    }

    public static final Symbol Rose = new Symbol();
    static {
    }

}
