/*
 *      PrintDialog.java
 *      
 *      Copyright 2011 Kai Pastor
 *      
 *      This program is free software; you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation; either version 2 of the License, or
 *      (at your option) any later version.
 *      
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *      
 *      You should have received a copy of the GNU General Public License
 *      along with this program; if not, write to the Free Software
 *      Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *      MA 02110-1301, USA.
 *      
 *      
 */

package org.openstreetmap.josm.plugins.print;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.gui.jmapviewer.tilesources.AbstractOsmTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * A print dialog with preview
 */
public class PrintDialog extends JDialog implements ActionListener {

    /**
     * The printer name
     */
    protected JTextField printerField;
    
    /**
     * The media format name
     */
    protected JTextField paperField;
    
    /**
     * The media orientation
     */
    protected JTextField orientationField;

    /**
     * The preview toggle checkbox
     */
    protected JCheckBox previewCheckBox;
    
    /**
     * The resolution in dpi for printing/preview
     */
    protected SpinnerNumberModel resolutionModel;
    
    /**
     * The map scale
     */
    protected SpinnerNumberModel scaleModel;
    
    /**
     * The page preview
     */
    protected PrintPreview printPreview;
    
    /**
     * The map view for preview an printing
     */
    protected PrintableMapView mapView;
    
    /**
     * The printer job
     */
    protected PrinterJob job;
    
    /**
     * The custom printer job attributes
     */
    PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
    
    /** 
     * Create a new print dialog
     * 
     * @param parent the parent component
     */
    public PrintDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), tr("Print the Map"), ModalityType.DOCUMENT_MODAL);
        mapView = new PrintableMapView();
        job = PrinterJob.getPrinterJob();
        job.setJobName("JOSM Map");
        job.setPrintable(mapView);
        build();
        loadPrintSettings();
        updateFields();
        pack();
        //setMinimumSize(getPreferredSize());
        setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
    }
    
    /**
     * Show or hide the dialog
     * 
     * Set the dialog size to reasonable values.
     * 
     * @param visible a flag indication the visibility of the dialog
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Make the dialog at most as large as the parent JOSM window
            // Have to take window decorations into account or the windows will
            // be too large
            Insets i = this.getParent().getInsets();
            Dimension p = this.getParent().getSize();
            p = new Dimension(Math.min(p.width-i.left-i.right, 1000),
                    Math.min(p.height-i.top-i.bottom, 700));
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            p
                    )
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
            Main.pref.put("print.preview.enabled",previewCheckBox.isSelected());
        }
        super.setVisible(visible);
    }

    /**
     * Construct the dialog from components
     */
    public void build() {
        setLayout(new GridBagLayout());
        final GBC std = GBC.std().insets(0,5,5,0);
        std.fill = GBC.HORIZONTAL;
        final GBC twoColumns   = GBC.std().insets(0,5,5,0).span(2);
        twoColumns.fill = GBC.HORIZONTAL;
        final GBC threeColumns = GBC.std().insets(0,5,5,0).span(3);
        threeColumns.fill = GBC.HORIZONTAL;
        
        JLabel caption;
        
        int row = 0;
        caption = new JLabel(tr("Printer")+":");
        add(caption, twoColumns.grid(2, row));
        printerField = new JTextField();
        printerField.setEditable(false);
        add(printerField, std.grid(GBC.RELATIVE, row));

        row++;
        caption = new JLabel(tr("Media")+":");
        add(caption, twoColumns.grid(2, row));
        paperField = new JTextField();
        paperField.setEditable(false);
        add(paperField, std.grid(GBC.RELATIVE, row));

        row++;
        caption = new JLabel(tr("Orientation")+":");
        add(caption, twoColumns.grid(2, row));
        orientationField = new JTextField();
        orientationField.setEditable(false);
        add(orientationField, std.grid(GBC.RELATIVE, row));

        row++;
        JButton printerButton = new JButton(tr("Printer settings")+"...");
        printerButton.setActionCommand("printer-dialog");
        printerButton.addActionListener(this);
        add(printerButton, threeColumns.grid(2, row));

        row++;
        add(GBC.glue(5,10), GBC.std(1,row).fill(GBC.VERTICAL));
        
        row++;
        caption = new JLabel(tr("Scale")+":");
        add(caption, std.grid(2, row));
        caption = new JLabel(" 1 :");
        add(caption, std.grid(GBC.RELATIVE, row));
        int mapScale = Main.pref.getInteger("print.map-scale", PrintPlugin.DEF_MAP_SCALE);
        mapView.setFixedMapScale(mapScale);
        scaleModel = new SpinnerNumberModel(mapScale, 500, 5000000, 500);
        final JSpinner scaleField = new JSpinner(scaleModel);
        scaleField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            scaleField.commitEdit();
                            Main.pref.put("print.map-scale",scaleModel.getNumber().toString());
                            mapView.setFixedMapScale(scaleModel.getNumber().intValue());
                            printPreview.repaint();
                        }
                        catch (ParseException pe) {
                            ; // NOP
                        }
                    }
                });
            }
        });
        add(scaleField, std.grid(GBC.RELATIVE, row));

        row++;
        caption = new JLabel(tr("Resolution")+":");
        add(caption, std.grid(2, row));
        caption = new JLabel("ppi");
        add(caption, std.grid(GBC.RELATIVE, row));
        resolutionModel = new SpinnerNumberModel(
          Main.pref.getInteger("print.resolution.dpi", PrintPlugin.DEF_RESOLUTION_DPI),
          30, 1200, 10 );
        final JSpinner resolutionField = new JSpinner(resolutionModel);
        resolutionField.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            resolutionField.commitEdit();
                            Main.pref.put("print.resolution.dpi",resolutionModel.getNumber().toString());
                            printPreview.repaint();
                        }
                        catch (ParseException pe) {
                            ; // NOP
                        }
                    }
                });
            }
        });
        add(resolutionField, std.grid(GBC.RELATIVE, row));
        
        row++;
        caption = new JLabel(tr("Map information")+":");
        add(caption, threeColumns.grid(2, row));

        row++;
        final JTextArea attributionText = new JTextArea(Main.pref.get("print.attribution", AbstractOsmTileSource.DEFAULT_OSM_ATTRIBUTION));
        attributionText.setRows(10);
        attributionText.setLineWrap(true);
        attributionText.setWrapStyleWord(true);
        attributionText.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Main.pref.put("print.attribution", attributionText.getText());
                        printPreview.repaint();
                    }
                });
            }
            public void removeUpdate(DocumentEvent evt) {
                this.insertUpdate(evt);
            }
            public void changedUpdate(DocumentEvent evt) {
                ; // NOP
            }
        });
        JScrollPane attributionPane = new JScrollPane(attributionText);
        add(attributionPane, GBC.std().insets(0,5,5,0).span(3).fill(GBC.BOTH).weight(0.0,1.0).grid(2, row));

        row++;
        add(GBC.glue(5,10), GBC.std(1,row).fill(GBC.VERTICAL));
        
        row++;
        previewCheckBox = new JCheckBox(tr("Map Preview"));
        previewCheckBox.setSelected(Main.pref.getBoolean("print.preview.enabled",false));
        previewCheckBox.setActionCommand("toggle-preview");
        previewCheckBox.addActionListener(this);
        add(previewCheckBox, threeColumns.grid(2, row));

        row++;
        JButton zoomInButton = new JButton(tr("Zoom In"));
        zoomInButton.setActionCommand("zoom-in");
        zoomInButton.addActionListener(this);
        add(zoomInButton, threeColumns.grid(2, row));

        row++;
        JButton zoomOutButton = new JButton(tr("Zoom Out"));
        zoomOutButton.setActionCommand("zoom-out");
        zoomOutButton.addActionListener(this);
        add(zoomOutButton, threeColumns.grid(2, row));
        
        row++;
        JButton zoomToPageButton = new JButton(tr("Zoom To Page"));
        zoomToPageButton.setActionCommand("zoom-to-page");
        zoomToPageButton.addActionListener(this);
        add(zoomToPageButton, threeColumns.grid(2, row));
        
        row++;
        JButton zoomToActualSize = new JButton(tr("Zoom To Actual Size"));
        zoomToActualSize.setActionCommand("zoom-to-actual-size");
        zoomToActualSize.addActionListener(this);
        add(zoomToActualSize, threeColumns.grid(2, row));
        
        printPreview = new PrintPreview();
        if (previewCheckBox.isSelected()) {
            printPreview.setPrintable(mapView);
        }
        JScrollPane previewPane = new JScrollPane(printPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewPane.setPreferredSize(Main.main != null ? Main.map.mapView.getSize() : new Dimension(210,297));
        add(previewPane, GBC.std(0,0).span(1, GBC.RELATIVE).fill().weight(5.0,5.0));

        row++;
        JPanel actionPanel = new JPanel();
        JButton printButton = new JButton(tr("Print"));
        printButton.setActionCommand("print");
        printButton.addActionListener(this);
        actionPanel.add(printButton);
        JButton cancelButton = new JButton(tr("Cancel"));
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        actionPanel.add(cancelButton);
        add(actionPanel, GBC.std(0,row).insets(5,5,5,5).span(GBC.REMAINDER).fill(GBC.HORIZONTAL));
    }
    
    /**
     * Update the dialog fields from the underlying model
     */
    protected void updateFields() {
        PrintService service = job.getPrintService();
        if (service == null) {
            printerField.setText("-");
            paperField.setText("-");
            orientationField.setText("-");
        }
        else {
            printerField.setText(service.getName());
            if (! attrs.containsKey(Media.class)) {
                attrs.add((Attribute) service.getDefaultAttributeValue(Media.class));
            } 
            if (attrs.containsKey(Media.class)) {
                paperField.setText(attrs.get(Media.class).toString());
            }

            if (! attrs.containsKey(OrientationRequested.class)) {
                attrs.add((Attribute) service.getDefaultAttributeValue(OrientationRequested.class));
            }
            if (attrs.containsKey(OrientationRequested.class)) {
                orientationField.setText(attrs.get(OrientationRequested.class).toString());
            }

            if (! attrs.containsKey(MediaPrintableArea.class)) {
                PageFormat pf = job.defaultPage();
                attrs.add(new MediaPrintableArea(
                  (float)pf.getImageableX()/72,(float)pf.getImageableY()/72,
                  (float)pf.getImageableWidth()/72,(float)pf.getImageableHeight()/72,
                  MediaPrintableArea.INCH) );
            }
            
            PageFormat pf = job.getPageFormat(attrs);
            printPreview.setPageFormat(pf);
        }
    }
    
    /**
     * Handle user input
     * 
     * @param e an ActionEvent with one of the known commands
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("printer-dialog")) {
            if (job.printDialog(attrs)) {
                updateFields();
                savePrintSettings();
            }
        }
        else if (cmd.equals("toggle-preview")) {
            Main.pref.put("print.preview.enabled", previewCheckBox.isSelected());
            if (previewCheckBox.isSelected() == true) {
                printPreview.setPrintable(mapView);
            }
            else {
                printPreview.setPrintable(null);
            }
        }
        else if (cmd.equals("zoom-in")) {
            printPreview.zoomIn();
        }
        else if (cmd.equals("zoom-out")) {
            printPreview.zoomOut();
        }
        else if (cmd.equals("zoom-to-page")) {
            printPreview.zoomToPage();
        }
        else if (cmd.equals("zoom-to-actual-size")) {
            printPreview.setZoom(1.0);
        }
        else if (cmd.equals("print")) {
            try {
                job.print(attrs);
            }
            catch (PrinterAbortException ex) {
                String msg = ex.getLocalizedMessage();
                if (msg.length() == 0) {
                    msg = tr("Printing has been cancelled.");
                }
                JOptionPane.showMessageDialog(Main.parent, msg,
                  tr("Printing stopped"),
                  JOptionPane.WARNING_MESSAGE);
            }
            catch (PrinterException ex) {
                String msg = ex.getLocalizedMessage();
                if (msg.length() == 0) {
                    msg = tr("Printing has failed.");
                }
                JOptionPane.showMessageDialog(Main.parent, msg,
                  tr("Printing stopped"),
                  JOptionPane.ERROR_MESSAGE);
            }
            dispose();
        }
        else if (cmd.equals("cancel")) {
            dispose();
        }
    }
    
    protected void savePrintSettings() {
        // Save only one printer service attribute: printer name 
        PrintService service = job.getPrintService();
        if (service != null) {
            Collection<Collection<String>> serviceAttributes = new ArrayList<>();
            for (Attribute a : service.getAttributes().toArray()) {
                if ("printer-name".equals(a.getName()) && a instanceof TextSyntax) {
                    serviceAttributes.add(marshallPrintSetting(a, TextSyntax.class, ((TextSyntax)a).getValue()));
                }
            }
            Main.pref.putArray("print.settings.service-attributes", serviceAttributes);
        }
        
        // Save all request attributes
        Collection<String> ignoredAttributes = Arrays.asList("media-printable-area");
        Collection<Collection<String>> requestAttributes = new ArrayList<>();
        for (Attribute a : attrs.toArray()) {
            Collection<String> setting = null;
            if (a instanceof TextSyntax) {
                setting = marshallPrintSetting(a, TextSyntax.class, ((TextSyntax)a).getValue());
            } else if (a instanceof EnumSyntax) {
                setting = marshallPrintSetting(a, EnumSyntax.class, Integer.toString(((EnumSyntax)a).getValue()));
            } else if (a instanceof IntegerSyntax) {
                setting = marshallPrintSetting(a, IntegerSyntax.class, Integer.toString(((IntegerSyntax)a).getValue()));
            } else if (!ignoredAttributes.contains(a.getName())) {
                // TODO: Add support for DateTimeSyntax, SetOfIntegerSyntax, ResolutionSyntax if needed
                Main.warn("Print request attribute not supported: "+a.getName() +": "+a.getCategory()+" - "+a.toString());
            }
            if (setting != null) {
                requestAttributes.add(setting);
            }
        }
        Main.pref.putArray("print.settings.request-attributes", requestAttributes);
    }
    
    protected Collection<String> marshallPrintSetting(Attribute a, Class<?> syntaxClass, String value) {
        return new ArrayList<>(Arrays.asList(a.getCategory().getName(), a.getClass().getName(), syntaxClass.getName(), value));
    }
    
    @SuppressWarnings("unchecked")
    protected Attribute unmarshallPrintSetting(Collection<String> setting) throws 
        IllegalArgumentException, ClassNotFoundException, SecurityException, NoSuchMethodException, 
        InstantiationException, IllegalAccessException, InvocationTargetException {
        
        if (setting == null || setting.size() != 4) {
            throw new IllegalArgumentException("Invalid setting: "+setting);
        }
        Iterator<String> it = setting.iterator();
        Class<? extends Attribute> category = (Class<? extends Attribute>) Class.forName(it.next());
        Class<? extends Attribute> realClass = (Class<? extends Attribute>) Class.forName(it.next());
        Class<?> syntax = Class.forName(it.next());
        String value = it.next();
        if (syntax.equals(TextSyntax.class)) {
            return realClass.getConstructor(String.class, Locale.class).newInstance(value, null);
        } else if (syntax.equals(EnumSyntax.class)) {
            int intValue = Integer.parseInt(value);
            for (Field f : realClass.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) && category.isAssignableFrom(f.getType())) {
                    EnumSyntax es = (EnumSyntax) f.get(null);
                    if (es.getValue() == intValue) {
                        return (Attribute) es;
                    }
                }
            }
            throw new IllegalArgumentException("Invalid enum: "+realClass+" - "+value);
        } else if (syntax.equals(IntegerSyntax.class)) {
            return realClass.getConstructor(int.class).newInstance(Integer.parseInt(value));
        } else {
            Main.warn("Attribute syntax not supported: "+syntax);
        }
        return null;
    }

    protected void loadPrintSettings() {
        for (Collection<String> setting : Main.pref.getArray("print.settings.service-attributes")) {
            try {
                PrintServiceAttribute a = (PrintServiceAttribute) unmarshallPrintSetting(setting);
                if ("printer-name".equals(a.getName())) {
                    job.setPrintService(PrintServiceLookup.lookupPrintServices(null, new HashPrintServiceAttributeSet(a))[0]);
                }
            } catch (Exception e) {
                Main.warn(e.getClass().getSimpleName()+": "+e.getMessage());
            }
        }
        for (Collection<String> setting : Main.pref.getArray("print.settings.request-attributes")) {
            try {
                attrs.add(unmarshallPrintSetting(setting));
            } catch (Exception e) {
                Main.warn(e.getClass().getSimpleName()+": "+e.getMessage());
            }
        }
    }
}

