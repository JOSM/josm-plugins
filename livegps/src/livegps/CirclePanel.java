// License: Public Domain. For details, see LICENSE file.
package livegps;

import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.Graphics; 
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.Color;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Area;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Draw a target visualization
 */
public class CirclePanel extends JPanel {

    JLabel label = new JLabel();
    double x = 0;

    public CirclePanel(double x) {
        add(label);
        this.x = x;
    }

    public void setOffset(double offs) {
        this.x = offs;
    }

    @Override
    protected void paintComponent(Graphics g) {
        boolean isVisible = Config.getPref().getBoolean(LiveGPSPreferences.C_DISTANCE_VISUALISATION, false);

        super.paintComponent(g);
        if (isVisible == true) {
            Graphics2D g2 = (Graphics2D) g;

            int w = getWidth();
            int h = getHeight();

            double width = 0;
            if (w > h) {
                width = h*0.9;
            } else {
                width = w*0.9;
            }

            double y_start = (h/2.0) - (width/2); // center circle vertical
            double x_start = (w - width) / 2; // center circle horizontal

            // 3 rectangles
            Shape rect1 = new Rectangle2D.Double(x_start, y_start, width*0.4, width);
            Shape rect2 = new Rectangle2D.Double(x_start+width*0.4, y_start, width*0.2, width);
            Shape rect3 = new Rectangle2D.Double(x_start+width*0.6, y_start, width*0.4, width);

            // 1 circle
            Shape circle = new Ellipse2D.Double(x_start, y_start, width, width);

            // intersection
            Area rect1Area = new Area(rect1);
            Area rect2Area = new Area(rect2);
            Area rect3Area = new Area(rect3);

            Area circleArea1 = new Area(circle);
            Area circleArea2 = new Area(circle);
            Area circleArea3 = new Area(circle);

            circleArea1.intersect(rect1Area);
            circleArea2.intersect(rect2Area);
            circleArea3.intersect(rect3Area);

            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

            DrawPoint drawpoint = new DrawPoint();
            drawpoint.draw_point(x, g2, circleArea1, circleArea2, circleArea3, label, x_start, y_start, width);

            g2.draw(circleArea1);
            g2.draw(circleArea2);
            g2.draw(circleArea3);

            // red center line
            g2.setColor(Color.red);
            g2.drawLine((int) (x_start+(width/2)), (int) y_start, (int) (x_start+(width/2)), (int) (y_start+width));
        }
    }
}
