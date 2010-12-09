package org.openstreetmap.josm.plugins.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;


public class ImageryAdjustAction extends MapMode implements MouseListener, MouseMotionListener{
    static ImageryOffsetDialog offsetDialog;
    static Cursor cursor = ImageProvider.getCursor("normal", "move");

    double oldDx, oldDy;
    boolean mouseDown;
    EastNorth prevEastNorth;
    private ImageryLayer layer;
    private MapMode oldMapMode;

    public ImageryAdjustAction(ImageryLayer layer) {
        super(tr("New offset"), "adjustimg",
                tr("Adjust the position of this imagery layer"), Main.map,
                cursor);
        this.layer = layer;
    }

    @Override public void enterMode() {
        super.enterMode();
        if (layer == null)
            return;
        if (!layer.isVisible()) {
            layer.setVisible(true);
        }
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        oldDx = layer.dx;
        oldDy = layer.dy;
        offsetDialog = new ImageryOffsetDialog();
        offsetDialog.setVisible(true);
    }

    @Override public void exitMode() {
        super.exitMode();
        if (offsetDialog != null) {
            layer.setOffset(oldDx, oldDy);
            offsetDialog.setVisible(false);
            offsetDialog = null;
        }
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (layer.isVisible()) {
            prevEastNorth=Main.map.mapView.getEastNorth(e.getX(),e.getY());
                Main.map.mapView.setCursor
                (Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (layer == null || prevEastNorth == null) return;
        EastNorth eastNorth =
            Main.map.mapView.getEastNorth(e.getX(),e.getY());
        double dx = layer.getDx()+eastNorth.east()-prevEastNorth.east();
        double dy = layer.getDy()+eastNorth.north()-prevEastNorth.north();
        layer.setOffset(dx, dy);
        if (offsetDialog != null) {
            offsetDialog.updateOffset();
        }
        Main.map.repaint();
        prevEastNorth = eastNorth;
    }

    @Override public void mouseReleased(MouseEvent e) {
        Main.map.mapView.repaint();
        Main.map.mapView.setCursor(Cursor.getDefaultCursor());
        prevEastNorth = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (offsetDialog != null || layer == null || Main.map == null)
            return;
        oldMapMode = Main.map.mapMode;
        super.actionPerformed(e);
    }


    class ImageryOffsetDialog extends ExtendedDialog implements PropertyChangeListener {
        public final JFormattedTextField easting = new JFormattedTextField(new DecimalFormat("0.00000E0"));
        public final JFormattedTextField northing = new JFormattedTextField(new DecimalFormat("0.00000E0"));
        JTextField tBookmarkName = new JTextField();
        private boolean ignoreListener;
        public ImageryOffsetDialog() {
            super(Main.parent,
                    tr("Adjust imagery offset"),
                    new String[] { tr("OK"),tr("Cancel") },
                    false);
            setButtonIcons(new String[] { "ok", "cancel" });
            contentInsets = new Insets(15, 15, 5, 15);
            JPanel pnl = new JPanel();
            pnl.setLayout(new GridBagLayout());
            pnl.add(new JLabel(tr("Easting") + ": "),GBC.std());
            pnl.add(easting,GBC.std().fill(GBC.HORIZONTAL).insets(0, 0, 5, 0));
            pnl.add(new JLabel(tr("Northing") + ": "),GBC.std());
            pnl.add(northing,GBC.eol());
            pnl.add(new JLabel(tr("Bookmark name: ")),GBC.eol().insets(0,5,0,0));
            pnl.add(tBookmarkName,GBC.eol().fill(GBC.HORIZONTAL));
            easting.setColumns(8);
            northing.setColumns(8);
            easting.setValue(layer.getDx());
            northing.setValue(layer.getDy());
            easting.addPropertyChangeListener("value",this);
            northing.addPropertyChangeListener("value",this);
            setContent(pnl);
            setupDialog();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ignoreListener) return;
            layer.setOffset(((Number)easting.getValue()).doubleValue(), ((Number)northing.getValue()).doubleValue());
            Main.map.repaint();
        }

        public void updateOffset() {
            ignoreListener = true;
            easting.setValue(layer.getDx());
            northing.setValue(layer.getDy());
            ignoreListener = false;
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            super.buttonAction(buttonIndex, evt);
            offsetDialog = null;
            if (buttonIndex == 1) {
                layer.setOffset(oldDx, oldDy);
            } else if (tBookmarkName.getText() != null && !"".equals(tBookmarkName.getText())) {
                OffsetBookmark b = new OffsetBookmark(
                        Main.proj,layer.getInfo().getName(),
                        tBookmarkName.getText(),
                        layer.getDx(),layer.getDy());
                OffsetBookmark.allBookmarks.add(b);
                OffsetBookmark.saveBookmarks();
            }
            ImageryPlugin.instance.refreshOffsetMenu();
            if (Main.map == null) return;
            if (oldMapMode != null) {
                Main.map.selectMapMode(oldMapMode);
                oldMapMode = null;
            } else {
                Main.map.selectSelectTool(false);
            }
        }
    }
}
