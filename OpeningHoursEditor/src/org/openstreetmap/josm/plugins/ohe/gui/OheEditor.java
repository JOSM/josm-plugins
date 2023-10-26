// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ohe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.openstreetmap.josm.plugins.ohe.ClockSystem;
import org.openstreetmap.josm.plugins.ohe.OpeningTimeUtils;
import org.openstreetmap.josm.plugins.ohe.parser.OpeningTimeCompiler;
import org.openstreetmap.josm.tools.Logging;

/**
 * The editor for opening hours
 */
public class OheEditor extends JPanel implements MouseListener, MouseMotionListener {
    /** A panel for the {@link TimeRect}s */
    private final class OhePanel extends JPanel {
        @Override
        public void setSize(Dimension d) {
            super.setSize(d);
            repositionTimeRects();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (OheEditor.this.isEnabled()) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());

                // draw the time from 12PM to 00AM in a different color
                if (dialog.getHourMode() == ClockSystem.TWELVE_HOURS) {
                    g.setColor(new Color(255, 255, 218));
                    g.fillRect(0, getMinutePosition(12 * 60), getWidth(), getHeight() - getMinutePosition(12 * 60));
                }

                // horizontal Lines
                for (int i = 0; i <= 24; ++i) {
                    if (i % 3 == 0) {
                        g.setColor(Color.BLACK);
                    } else {
                        g.setColor(Color.LIGHT_GRAY);
                    }
                    g.drawLine(0, getMinutePosition(i * 60), getWidth(), getMinutePosition(i * 60));
                }

                // vertical Lines
                g.setColor(Color.BLACK);
                for (int i = 0; i <= 7; ++i) {
                    g.drawLine(getDayPosition(i), 0, getDayPosition(i), getHeight());
                }

                // if a new Rect is dragged draw it
                if (day0 >= 0) {
                    Graphics2D g2D = (Graphics2D) g;
                    int day2 = Math.min(day0, day1);
                    int day3 = Math.max(day0, day1);
                    int minute2 = Math.min(minute0, minute1);
                    int minute3 = Math.max(minute0, minute1);
                    Rectangle bounds = getPanelBoundsForTimeInterval(day2, day3 + 1, minute2, minute3);
                    TimeRect.drawTimeRect(g2D, bounds, minute2 == minute3, false);
                }
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    final OheDialogPanel dialog;

    final JPanel contentPanel;

    /** The time rectangles for this dialog */
    List<TimeRect> timeRects;

    /** The height for the cells */
    private static final int DAY_AXIS_HEIGHT = 20;
    /** The width for the cells */
    private static final int TIME_AXIS_WIDTH = 45;

    /**
     * Create a new editor component
     * @param oheDialogPanel The dialog with settings for this editor
     */
    public OheEditor(OheDialogPanel oheDialogPanel) {
        dialog = oheDialogPanel;

        // the MainPanel for showing the TimeRects
        contentPanel = new OhePanel();
        contentPanel.addMouseListener(this);
        contentPanel.addMouseMotionListener(this);
        contentPanel.setLayout(null);
        contentPanel.setPreferredSize(new Dimension(180, 384));

        initTimeRects();

        JScrollPane scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(contentPanel);

        // the upper Panel for showing Weekdays
        scrollPane.setColumnHeaderView(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(contentPanel.getWidth(), DAY_AXIS_HEIGHT);
            }

            @Override
            public void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.BLACK);
                for (int i = 0; i < 7; ++i) {
                    if (i > 0) {
                        g.drawLine(getDayPosition(i), 0, getDayPosition(i), getHeight());
                    }

                    String text = OpeningTimeCompiler.WEEKDAYS[i];
                    g.drawString(text, (int) (getDayPosition(i + 0.5) - g.getFontMetrics().stringWidth(text) * 0.5),
                            (int) (DAY_AXIS_HEIGHT * 0.5 + g.getFontMetrics().getHeight() * 0.35));
                }
            }
        });

        // the left Panel for showing the hours
        scrollPane.setRowHeaderView(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(TIME_AXIS_WIDTH, contentPanel.getHeight());
            }

            @Override
            public void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());

                // draw the time from 12PM to 00AM in a different color
                if (dialog.getHourMode() == ClockSystem.TWELVE_HOURS) {
                    g.setColor(new Color(255, 255, 218));
                    g.fillRect(0, getMinutePosition(12 * 60), getWidth(), getHeight() - getMinutePosition(12 * 60));
                }

                for (int i = 0; i <= 24; ++i) {
                    if (i % 3 == 0) {
                        g.setColor(Color.BLACK);
                        if (i % 24 != 0) {
                            String text = OpeningTimeUtils.timeString(i * 60, dialog.getHourMode(), false);
                            g.drawString(text, (TIME_AXIS_WIDTH - g.getFontMetrics().stringWidth(text)) / 2,
                                    getMinutePosition(i * 60) + (int) (g.getFontMetrics().getHeight() * 0.35));
                        }
                    } else {
                        g.setColor(Color.LIGHT_GRAY);
                    }

                    g.drawLine(getWidth() - 4, getMinutePosition(i * 60), getWidth(), getMinutePosition(i * 60));
                }

                g.setColor(Color.BLACK);
                String text = OpeningTimeUtils.timeString(0, dialog.getHourMode(), false);
                g.drawString(text, (TIME_AXIS_WIDTH - g.getFontMetrics().stringWidth(text)) / 2, getMinutePosition(0)
                        + (int) (g.getFontMetrics().getHeight() * 1.0));
                if (dialog.getHourMode() == ClockSystem.TWELVE_HOURS) {
                    text = "AM";
                    g.drawString(text, (TIME_AXIS_WIDTH - g.getFontMetrics().stringWidth(text)) / 2, getMinutePosition(0)
                            + (int) (g.getFontMetrics().getHeight() * 1.85));
                    text = "PM";
                    g.drawString(text, (TIME_AXIS_WIDTH - g.getFontMetrics().stringWidth(text)) / 2,
                            getMinutePosition(12 * 60) + (int) (g.getFontMetrics().getHeight() * 1.2));
                }
            }
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates all the TimeRects with new data.
     */
    public void initTimeRects() {
        contentPanel.removeAll();

        List<int[]> time;
        try {
            time = dialog.getTime();
        } catch (Exception exc) {
            Logging.log(Logging.LEVEL_WARN, "Disable opening hours editor:", exc);
            setEnabled(false);
            return;
        }

        setEnabled(true);
        timeRects = new ArrayList<>();
        if (time != null) {
            for (int[] timeRectValues : time) {
                int tDay0 = timeRectValues[0];
                int tDay1 = timeRectValues[1];
                int tMinute0 = timeRectValues[2];
                int tMinute1 = timeRectValues[3];
                TimeRect timeRect = new TimeRect(OheEditor.this, tDay0, tDay1, tMinute0, tMinute1);
                timeRects.add(timeRect);
                contentPanel.add(timeRect);
            }
        }

        repositionTimeRects();
        repaint();
    }

    protected void repositionTimeRects() {
        if (timeRects != null) {
            for (TimeRect timeRect : timeRects) {
                timeRect.reposition();
            }
        }
    }

    /**
     * returns the physical Borders of the TimeRect on the mainPanel
     *
     * @param dayStart The starting weekday for the opening hours
     * @param dayEnd The ending weekday for the opening hours
     * @param minutesStart The starting time for the opening hours
     * @param minutesEnd The ending time for the opening hours
     * @return The borders for the time rectangle
     */
    public Rectangle getPanelBoundsForTimeInterval(int dayStart, int dayEnd, int minutesStart, int minutesEnd) {
        int x = getDayPosition(dayStart);
        int y = getMinutePosition(minutesStart);
        int width = getDayPosition(dayEnd) - getDayPosition(dayStart);
        int height = getMinutePosition(minutesEnd) - getMinutePosition(minutesStart);

        if (minutesStart == minutesEnd)
            return new Rectangle(x, y - 2 - TimeRect.VERTICAL_NON_DRAWN_PIXELS, width, height + 5 + 2
                    * TimeRect.VERTICAL_NON_DRAWN_PIXELS);

        return new Rectangle(x, y, width, height + 1);
    }

    /**
     * Get the width for a time cell
     * @return The width in pixels
     */
    double getDayWidth() {
        return (contentPanel.getWidth() - 1) / 7.0;
    }

    /**
     * Get the position for a day
     * @param d The weekday
     * @return The starting pixel position for the day
     */
    int getDayPosition(double d) {
        return (int) (d * getDayWidth());
    }

    /**
     * Get the height for a hour
     * @return The height of an hour in pixels
     */
    double getMinuteHeight() {
        return (contentPanel.getHeight() - 1) / (24.0 * 60);
    }

    /**
     * Get the position for a minute in pixels
     * @param minute The minute to get the position for
     * @return The pixel location for the minute
     */
    int getMinutePosition(int minute) {
        return (int) (minute * getMinuteHeight());
    }

    /**
     * Removes the given {@link TimeRect} from the panel and from the arraylist
     * @param timeRectToRemove The rectangle to remove
     */
    void removeTimeRect(TimeRect timeRectToRemove) {
        timeRects.remove(timeRectToRemove);
        contentPanel.remove(timeRectToRemove);
        dialog.updateValueField(timeRects);
        repaint();
    }

    // drawing a new Rect
    /** The first day of the time slot. -1 is for new rectangles. */
    private int day0 = -1;
    /** THe first minute of the time slot */
    private int minute0;
    /** The last day of the time slot */
    private int day1;
    /** The last minute of the time slot */
    private int minute1;
    /** The x location of the mouse click */
    private int xDragStart;
    /** The y location of the mouse click */
    private int yDragStart;

    @Override
    public void mouseClicked(MouseEvent evt) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        mousePositionChanged(0, 0, true);
    }

    @Override
    public void mouseExited(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        mousePositionChanged(0, 0, false);
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        day0 = (int) Math.floor(evt.getX() / getDayWidth());
        minute0 = (int) Math.floor(evt.getY() / (getMinuteHeight() * TimeRect.MINUTE_RASTERIZE))
        * TimeRect.MINUTE_RASTERIZE;
        day1 = day0;
        minute1 = minute0;
        xDragStart = evt.getX();
        yDragStart = evt.getY();
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        // mouse must be moved 5px before creating a rect
        if (xDragStart == -1 || Math.abs(evt.getX() - xDragStart) + Math.abs(evt.getY() - yDragStart) > 5) {
            int day2 = Math.min(day0, day1);
            int day3 = Math.max(day0, day1);
            int minute2 = Math.min(minute0, minute1);
            int minute3 = Math.max(minute0, minute1);

            TimeRect timeRect = new TimeRect(OheEditor.this, day2, day3, minute2, minute3);
            timeRects.add(timeRect);
            contentPanel.add(timeRect);
            timeRect.reposition();
            dialog.updateValueField(timeRects);

            day0 = -1;
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        // mouse must be moved 5px before drawing a rect
        if (xDragStart == -1 || Math.abs(evt.getX() - xDragStart) + Math.abs(evt.getY() - yDragStart) > 5) {
            xDragStart = -1;
            day1 = (int) Math.floor(evt.getX() / getDayWidth());
            minute1 = (int) Math.floor(evt.getY() / (getMinuteHeight() * TimeRect.MINUTE_RASTERIZE))
                    * TimeRect.MINUTE_RASTERIZE;

            // ensure that the new time is in a valid range
            day1 = Math.max(day1, 0);
            day1 = Math.min(day1, 6);
            minute1 = Math.max(minute1, 0);
            minute1 = Math.min(minute1, 24 * 60);

            repaint();
        }
        mousePositionChanged(evt.getX(), evt.getY(), true);
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        if (!isEnabled()) {
            return; // allow no mouse actions when the editor is not enabled
        }
        mousePositionChanged(evt.getX(), evt.getY(), true);
    }

    /**
     * Called when the mouse position moves
     * @param x The mouse x position
     * @param y The mouse y position
     * @param mouseInside {@code true} if the mouse is inside the component for time cells
     */
    void mousePositionChanged(int x, int y, boolean mouseInside) {
        if (mouseInside) {
            int actualDay = (int) Math.floor(x / getDayWidth());
            int minutes = (int) Math.floor(y / (getMinuteHeight() * TimeRect.MINUTE_RASTERIZE))
                    * TimeRect.MINUTE_RASTERIZE;
            actualDay = Math.max(0, Math.min(6, actualDay));
            minutes = Math.max(0, Math.min(24 * 60, minutes));
            dialog.setMousePositionText(OpeningTimeCompiler.WEEKDAYS[actualDay] + " "
                    + OpeningTimeUtils.timeString(minutes, dialog.getHourMode(), true));
        } else {
            dialog.setMousePositionText("-");
        }
    }
}
