// License: Public Domain. For details, see LICENSE file.
package livegps;

import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Area;

import org.openstreetmap.josm.spi.preferences.Config;

class DrawPoint extends JPanel {
    public double threshold = Config.getPref().getDouble(LiveGPSPreferences.C_OFFSET_THRESHOLD, LiveGPSPreferences.DEFAULT_THRESHOLD);
    int x_p = 0;
    int w = getWidth();
    int h = getHeight();

    public void draw_point(double x, Graphics2D g2, Area circleArea1, Area circleArea2,
        Area circleArea3, JLabel label, double x_start, double y_start, double width) {

        // x-value of black point
        if (x < -threshold) {
            x_p = (int) (x_start + (width*0.4) + (x*(width/100)) - ((width*0.15)/2));
            if (x_p < x_start) {
                x_p = (int) x_start;
            }
        } else if (x > threshold) {
            x_p = (int) (x_start + (x*(width/100)) + (width*0.6) - ((width*0.15)/2));
            if (x_p > x_start + width) {
                x_p = (int) (x_start + width);
            }
        } else if (x >= -threshold && x <= threshold) {
            x_p = (int) (x_start + (width/2) - ((width*0.15)/2));
        }

        // fill area
        fill(x, x_p, g2, circleArea1, circleArea2, circleArea3, label, x_start, y_start, width);

        // black position point
        g2.setColor(Color.black);
        g2.fillOval(x_p, (int) (y_start+(width/2)-((width*0.15)/2)), (int) (width*0.15), (int) (width*0.15));
    }

    public void fill(double x, int x_p, Graphics2D g2, Area circleArea1, Area circleArea2, Area circleArea3,
        JLabel label, double x_start, double y_start, double width) {

        Color yellow;
        Color green;
        yellow = new Color(255, 210, 50, 255);
        green = new Color(30, 200, 50, 255);

        // Distance from point to center line, rounded to 2 decimals
        double y = Math.round(x * 100.0) / 100.0;

        if (x < 0 && -x > threshold) {
            // left yellow + text
            g2.setColor(yellow);
            g2.fill(circleArea1);

            if (width/3 < 60) { // if label larger than half circle, place label outside the circle
                label.setBounds((int) (x_start - 60), (int) (y_start + width*0.3), 60, 10);
            } else {
                label.setBounds((int) (x_start + width*0.1), (int) (y_start + width*0.3), (int) (width/3), (int) (width/15));
            }
            label.setText(y+" m");

        } else if ((-x <= threshold && x <= 0) || (x <= threshold && x >= 0)) {
            // center green
            g2.setColor(green);
            g2.fill(circleArea2);
            label.setText("");
        } else if (x > 0 && x > threshold) {
            // right yellow + text
            g2.setColor(yellow);
            g2.fill(circleArea3);

            if (width/3 < 60) {
                label.setBounds((int) (x_start + width*1.1), (int) (y_start + width*0.3), 60, 10);
            } else {
                label.setBounds((int) (x_start + width*0.7), (int) (y_start + width*0.3), (int) (width/3), (int) (width/15));
            }
            label.setText("+"+y+" m");
        }
    }
}
