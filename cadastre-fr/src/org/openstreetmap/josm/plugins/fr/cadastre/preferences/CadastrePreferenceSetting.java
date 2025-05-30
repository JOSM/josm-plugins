// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fr.cadastre.preferences;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.plugins.fr.cadastre.CadastrePlugin;
import org.openstreetmap.josm.plugins.fr.cadastre.wms.CacheControl;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Preference settings for the French Cadastre plugin
 *
 * @author Pieren &lt;pieren3@gmail.com&gt;
 */
public class CadastrePreferenceSetting extends DefaultTabPreferenceSetting {
    private static final int TRANS_MIN = 1;
    private static final int TRANS_MAX = 10;
    private static final String HIGH = marktr("high");
    private static final String MEDIUM = marktr("medium");
    private static final String LOW = marktr("low");
    private static final String CADASTREWMS_RESOLUTION = "cadastrewms.resolution";
    private static final String CADASTREWMS_IMAGE_INTERPOLATION = "cadastrewms.imageInterpolation";
    private static final String CADASTREWMS_SCALE = "cadastrewms.scale";
    private static final String CADASTREWMS_CROSSPIECES = "cadastrewms.crosspieces";
    private static final String PREFERENCES = "preferences";
    private final JSlider sliderTrans = new JSlider(SwingConstants.HORIZONTAL, TRANS_MIN, TRANS_MAX, TRANS_MAX);

    private final JTextField sourcing = new JTextField(20);

    private final JCheckBox alterColors = new JCheckBox(tr("Replace original background by JOSM background color."));

    private final JCheckBox reversGrey = new JCheckBox(tr("Reverse grey colors (for black backgrounds)."));

    private final JCheckBox transparency = new JCheckBox(tr("Set background transparent."));

    private final JCheckBox drawBoundaries = new JCheckBox(tr("Draw boundaries of downloaded data"));

    private final JComboBox<String> imageInterpolationMethod = new JComboBox<>();

    private final JCheckBox disableImageCropping = new JCheckBox(tr("Disable image cropping during georeferencing."));

    private final JCheckBox enableTableauAssemblage = new JCheckBox(tr("Use \"Tableau d''assemblage\""));

    private final JCheckBox simplify2BitsColors = new JCheckBox(tr("Replace grey shades by white color only"));

    private final JCheckBox autoFirstLayer = new JCheckBox(tr("Select first WMS layer in list."));

    private final JCheckBox dontUseRelation = new JCheckBox(tr("Don''t use relation for addresses (but \"addr:street\" on elements)."));

    private final JCheckBox mergeDataLayers = new JCheckBox(tr("Merge downloaded cadastre data layers together."));

    private final JRadioButton grabMultiplier1 = new JRadioButton("", true);

    private final JRadioButton grabMultiplier2 = new JRadioButton("", true);

    private final JRadioButton grabMultiplier3 = new JRadioButton("", true);

    private final JRadioButton grabMultiplier4 = new JRadioButton("", true);

    private final JRadioButton crosspiece1 = new JRadioButton(tr("off"));

    private final JRadioButton crosspiece2 = new JRadioButton(tr("25 m"));

    private final JRadioButton crosspiece3 = new JRadioButton(tr("50 m"));

    private final JRadioButton crosspiece4 = new JRadioButton(tr("100 m"));

    private final JRadioButton grabRes1 = new JRadioButton(tr(HIGH));
    private final JRadioButton grabRes2 = new JRadioButton(tr(MEDIUM));
    private final JRadioButton grabRes3 = new JRadioButton(tr(LOW));

    private final JCheckBox layerLS3 = new JCheckBox(tr("water"));
    private final JCheckBox layerLS2 = new JCheckBox(tr("building"));
    private final JCheckBox layerLS1 = new JCheckBox(tr("symbol"));
    private final JCheckBox layerParcel = new JCheckBox(tr("parcel"));
    private final JCheckBox layerLabel = new JCheckBox(tr("parcel number"));
    private final JCheckBox layerNumero = new JCheckBox(tr("address"));
    private final JCheckBox layerLieudit = new JCheckBox(tr("locality"));
    private final JCheckBox layerSection = new JCheckBox(tr("section"));
    private final JCheckBox layerCommune = new JCheckBox(tr("commune"));

    public static final int DEFAULT_SQUARE_SIZE = 100;
    private final JTextField grabMultiplier4Size = new JTextField(5);

    private final JCheckBox enableCache = new JCheckBox(tr("Enable automatic caching."));

    public static final int DEFAULT_CACHE_SIZE = 0; // disabled by default
    JLabel jLabelCacheSize = new JLabel(tr("Max. cache size (in MB)"));
    private final JTextField cacheSize = new JTextField(20);

    public static final String DEFAULT_RASTER_DIVIDER = "7";
    private final JTextField rasterDivider = new JTextField(10);

    static final int DEFAULT_CROSSPIECES = 0;

    public static final String DEFAULT_GRAB_MULTIPLIER = Scale.SQUARE_100M.value;

    /**
     * Constructs a new {@code CadastrePreferenceSetting}.
     */
    public CadastrePreferenceSetting() {
        super("cadastrewms.png", I18n.tr("French cadastre WMS"),
            tr("A special handler of the French cadastre wms at www.cadastre.gouv.fr" + "<BR><BR>"
                + "Please read the Terms and Conditions of Use here (in French): <br>"
                + "<a href=\"http://www.cadastre.gouv.fr/scpc/html/CU_01_ConditionsGenerales_fr.html\"> "
                + "http://www.cadastre.gouv.fr/scpc/html/CU_01_ConditionsGenerales_fr.html</a> <BR>"
                + "before any upload of data created by this plugin.")
        );
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        JPanel cadastrewmsMast = gui.createPreferenceTab(this);

        JPanel cadastrewms = new JPanel(new GridBagLayout());
        cadastrewms.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        addGuiGenericOptions(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        addGuiVectorImages(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        addGuiRasterImages(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        addGuiAutoCache(cadastrewms);
        // option to fix the cache size(in MB)
        addGuiCacheSize(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        // option to select the first WMS layer
        addGuiFirstWMSLayer(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        addGuiUseRelationsInAddresses(cadastrewms);
        // separator
        addGuiSeparator(cadastrewms);
        addGuiMergeDownloadedDataLayers(cadastrewms);
        addGuiEndOfDialogScrollBar(cadastrewms, cadastrewmsMast);
    }

    private static void addGuiSeparator(JPanel cadastrewms) {
        cadastrewms.add(new JSeparator(SwingConstants.HORIZONTAL), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
    }

    private void addGuiGenericOptions(JPanel cadastrewms) {
        // option to automatically set the source tag when uploading
        sourcing.setText(CadastrePlugin.source);
        sourcing.setToolTipText(tr("<html>Value of key \"source\" when autosourcing is enabled</html>"));
        JLabel jLabelSource = new JLabel(tr("Source"));
        cadastrewms.add(jLabelSource, GBC.eop().insets(0, 0, 0, 0));
        cadastrewms.add(sourcing, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 0, 0, 5));

        // option to alter the original colors of the wms images
        alterColors.setSelected(Config.getPref().getBoolean("cadastrewms.alterColors", false));
        alterColors.setToolTipText(tr("Replace the original white background by the background color defined in JOSM preferences."));
        cadastrewms.add(alterColors, GBC.eop().insets(0, 0, 0, 0));

        // option to reverse the grey colors (to see texts background)
        reversGrey.setSelected(Config.getPref().getBoolean("cadastrewms.invertGrey", false));
        reversGrey.setToolTipText(
                tr("Invert the original black and white colors (and all intermediate greys). Useful for texts on dark backgrounds."));
        cadastrewms.add(reversGrey, GBC.eop().insets(0, 0, 0, 0));

        // option to enable transparency
        transparency.addActionListener(e -> sliderTrans.setEnabled(transparency.isSelected()));
        transparency.setSelected(Config.getPref().getBoolean("cadastrewms.backgroundTransparent", false));
        transparency.setToolTipText(tr("Allows multiple layers stacking"));
        cadastrewms.add(transparency, GBC.eop().insets(0, 0, 0, 0));

        // slider for transparency level
        sliderTrans.setSnapToTicks(true);
        sliderTrans.setToolTipText(tr("Set WMS layers transparency. Right is opaque, left is transparent."));
        sliderTrans.setMajorTickSpacing(10);
        sliderTrans.setMinorTickSpacing(1);
        sliderTrans.setValue((int) (Float.parseFloat(Config.getPref().get("cadastrewms.brightness", "1.0f"))*10));
        sliderTrans.setPaintTicks(true);
        sliderTrans.setPaintLabels(false);
        sliderTrans.setEnabled(transparency.isSelected());
        cadastrewms.add(sliderTrans, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(20, 0, 250, 0));

        // option to draw boundaries of downloaded data
        drawBoundaries.setSelected(Config.getPref().getBoolean("cadastrewms.drawBoundaries", false));
        drawBoundaries.setToolTipText(tr("Draw a rectangle around downloaded data from WMS server."));
        cadastrewms.add(drawBoundaries, GBC.eop().insets(0, 0, 0, 5));

        // option to select the single grabbed image resolution
        JLabel jLabelRes = new JLabel(tr("Image resolution:"));
        cadastrewms.add(jLabelRes, GBC.std().insets(0, 5, 10, 0));
        ButtonGroup bgResolution = new ButtonGroup();
        grabRes1.setToolTipText(tr("High resolution (1000x800)"));
        grabRes2.setToolTipText(tr("Medium resolution (800x600)"));
        grabRes3.setToolTipText(tr("Low resolution (600x400)"));
        bgResolution.add(grabRes1);
        bgResolution.add(grabRes2);
        bgResolution.add(grabRes3);
        String currentResolution = Config.getPref().get(CADASTREWMS_RESOLUTION, HIGH);
        switch (currentResolution) {
            case "high" -> grabRes1.setSelected(true);
            case "medium" -> grabRes2.setSelected(true);
            case "low" -> grabRes3.setSelected(true);
            default -> { /* Do nothing */ }
        }
        cadastrewms.add(grabRes1, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(grabRes2, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(grabRes3, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 0, 5));

        // option to select image zooming interpolation method
        JLabel jLabelImageZoomInterpolation = new JLabel(tr("Image filter interpolation:"));
        cadastrewms.add(jLabelImageZoomInterpolation, GBC.std().insets(0, 0, 10, 0));
        imageInterpolationMethod.addItem(tr("Nearest-Neighbor (fastest) [ Default ]"));
        imageInterpolationMethod.addItem(tr("Bilinear (fast)"));
        imageInterpolationMethod.addItem(tr("Bicubic (slow)"));
        String savedImageInterpolationMethod = Config.getPref().get(CADASTREWMS_IMAGE_INTERPOLATION, "standard");
        if ("bilinear".equals(savedImageInterpolationMethod)) {
            imageInterpolationMethod.setSelectedIndex(1);
        } else if ("bicubic".equals(savedImageInterpolationMethod)) {
            imageInterpolationMethod.setSelectedIndex(2);
        } else
            imageInterpolationMethod.setSelectedIndex(0);
        cadastrewms.add(imageInterpolationMethod, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 200, 5));
    }

    private void addGuiVectorImages(JPanel cadastrewms) {
        // the vectorized images multiplier
        JLabel jLabelScale = new JLabel(tr("Vector images grab multiplier:"));
        cadastrewms.add(jLabelScale, GBC.std().insets(0, 5, 10, 0));
        ButtonGroup bgGrabMultiplier = new ButtonGroup();
        ActionListener multiplierActionListener = actionEvent -> {
            AbstractButton button = (AbstractButton) actionEvent.getSource();
            grabMultiplier4Size.setEnabled(button == grabMultiplier4);
        };
        grabMultiplier1.setIcon(ImageProvider.get(PREFERENCES, "unsel_box_1"));
        grabMultiplier1.setSelectedIcon(ImageProvider.get(PREFERENCES, "sel_box_1"));
        grabMultiplier1.addActionListener(multiplierActionListener);
        grabMultiplier1.setToolTipText(tr("Grab one image full screen"));
        grabMultiplier2.setIcon(ImageProvider.get(PREFERENCES, "unsel_box_2"));
        grabMultiplier2.setSelectedIcon(ImageProvider.get(PREFERENCES, "sel_box_2"));
        grabMultiplier2.addActionListener(multiplierActionListener);
        grabMultiplier2.setToolTipText(tr("Grab smaller images (higher quality but use more memory)"));
        grabMultiplier3.setIcon(ImageProvider.get(PREFERENCES, "unsel_box_3"));
        grabMultiplier3.setSelectedIcon(ImageProvider.get(PREFERENCES, "sel_box_3"));
        grabMultiplier3.addActionListener(multiplierActionListener);
        grabMultiplier3.setToolTipText(tr("Grab smaller images (higher quality but use more memory)"));
        grabMultiplier4.setIcon(ImageProvider.get(PREFERENCES, "unsel_box_4"));
        grabMultiplier4.setSelectedIcon(ImageProvider.get(PREFERENCES, "sel_box_4"));
        grabMultiplier4.addActionListener(multiplierActionListener);
        grabMultiplier4.setToolTipText(tr("Fixed size square (default is 100m)"));
        bgGrabMultiplier.add(grabMultiplier1);
        bgGrabMultiplier.add(grabMultiplier2);
        bgGrabMultiplier.add(grabMultiplier3);
        bgGrabMultiplier.add(grabMultiplier4);
        String currentScale = Config.getPref().get(CADASTREWMS_SCALE, DEFAULT_GRAB_MULTIPLIER);
        if (currentScale.equals(Scale.X1.value))
            grabMultiplier1.setSelected(true);
        if (currentScale.equals(Scale.X2.value))
            grabMultiplier2.setSelected(true);
        if (currentScale.equals(Scale.X3.value))
            grabMultiplier3.setSelected(true);
        if (currentScale.equals(Scale.SQUARE_100M.value))
            grabMultiplier4.setSelected(true);
        cadastrewms.add(grabMultiplier1, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(grabMultiplier2, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(grabMultiplier3, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(grabMultiplier4, GBC.std().insets(5, 0, 5, 0));
        int squareSize = getNumber("cadastrewms.squareSize", DEFAULT_SQUARE_SIZE);
        grabMultiplier4Size.setText(String.valueOf(squareSize));
        grabMultiplier4Size.setToolTipText(tr("Fixed size (from 25 to 1000 meters)"));
        grabMultiplier4Size.setEnabled(currentScale.equals(Scale.SQUARE_100M.value));
        cadastrewms.add(grabMultiplier4Size, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 0, 5));

        // WMS layers selection
        JLabel jLabelLayers = new JLabel(tr("Layers:"));
        cadastrewms.add(jLabelLayers, GBC.std().insets(0, 5, 10, 0));
        layerLS3.setSelected(Config.getPref().getBoolean("cadastrewms.layerWater", true));
        layerLS3.setToolTipText(tr("Sea, rivers, swimming pools."));
        cadastrewms.add(layerLS3, GBC.std().insets(5, 0, 5, 0));
        layerLS2.setSelected(Config.getPref().getBoolean("cadastrewms.layerBuilding", true));
        layerLS2.setToolTipText(tr("Buildings, covers, underground constructions."));
        cadastrewms.add(layerLS2, GBC.std().insets(5, 0, 5, 0));
        layerLS1.setSelected(Config.getPref().getBoolean("cadastrewms.layerSymbol", true));
        layerLS1.setToolTipText(tr("Symbols like cristian cross."));
        cadastrewms.add(layerLS1, GBC.std().insets(5, 0, 5, 0));
        layerParcel.setSelected(Config.getPref().getBoolean("cadastrewms.layerParcel", true));
        layerParcel.setToolTipText(tr("Parcels."));
        cadastrewms.add(layerParcel, GBC.eop().insets(5, 0, 5, 0));
        layerLabel.setSelected(Config.getPref().getBoolean("cadastrewms.layerLabel", true));
        layerLabel.setToolTipText(tr("Parcels numbers, street names."));
        cadastrewms.add(layerLabel, GBC.std().insets(70, 0, 5, 0));
        layerNumero.setSelected(Config.getPref().getBoolean("cadastrewms.layerNumero", true));
        layerNumero.setToolTipText(tr("Address, houses numbers."));
        cadastrewms.add(layerNumero, GBC.std().insets(5, 0, 5, 0));
        layerLieudit.setSelected(Config.getPref().getBoolean("cadastrewms.layerLieudit", true));
        layerLieudit.setToolTipText(tr("Locality, hamlet, place."));
        cadastrewms.add(layerLieudit, GBC.std().insets(5, 0, 5, 0));
        layerSection.setSelected(Config.getPref().getBoolean("cadastrewms.layerSection", true));
        layerSection.setToolTipText(tr("Cadastral sections and subsections."));
        cadastrewms.add(layerSection, GBC.std().insets(5, 0, 5, 0));
        layerCommune.setSelected(Config.getPref().getBoolean("cadastrewms.layerCommune", true));
        layerCommune.setToolTipText(tr("Municipality administrative borders."));
        cadastrewms.add(layerCommune, GBC.eop().insets(5, 0, 5, 0));
    }

    private void addGuiRasterImages(JPanel cadastrewms) {
        // for raster images (not vectorized), image grab divider (from 1 to 12)
        String savedRasterDivider = Config.getPref().get("cadastrewms.rasterDivider", DEFAULT_RASTER_DIVIDER);
        JLabel jLabelRasterDivider = new JLabel(tr("Raster images grab multiplier:"));
        rasterDivider.setText(savedRasterDivider);
        rasterDivider.setToolTipText("Raster image grab division, from 1 to 12; 12 is very high definition");
        cadastrewms.add(jLabelRasterDivider, GBC.std().insets(0, 5, 10, 0));
        cadastrewms.add(rasterDivider, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 200, 5));
        // option to disable image cropping during raster image georeferencing
        disableImageCropping.setSelected(Config.getPref().getBoolean("cadastrewms.noImageCropping", false));
        disableImageCropping.setToolTipText(tr("Disable image cropping during georeferencing."));
        cadastrewms.add(disableImageCropping, GBC.std().insets(0, 0, 10, 0));
        // option to add the "Tableau d'assemblage" in list of sheets to grab
        enableTableauAssemblage.setSelected(Config.getPref().getBoolean("cadastrewms.useTA", false));
        enableTableauAssemblage.setToolTipText(tr("Add the \"Tableau(x) d''assemblage\" in the list of cadastre sheets to grab."));
        cadastrewms.add(enableTableauAssemblage, GBC.eop().insets(0, 0, 0, 0));
        // option to use 2 bits colors only
        simplify2BitsColors.setSelected(Config.getPref().getBoolean("cadastrewms.raster2bitsColors", false));
        simplify2BitsColors.setToolTipText(tr("Replace greyscale by white color (smaller files and memory usage)."));
        cadastrewms.add(simplify2BitsColors, GBC.eop().insets(0, 0, 0, 0));
        // the crosspiece display
        JLabel jLabelCrosspieces = new JLabel(tr("Display crosspieces:"));
        cadastrewms.add(jLabelCrosspieces, GBC.std().insets(0, 0, 10, 0));
        ButtonGroup bgCrosspieces = new ButtonGroup();
        int crosspieces = getNumber(CADASTREWMS_CROSSPIECES, DEFAULT_CROSSPIECES);
        if (crosspieces == 0) crosspiece1.setSelected(true);
        if (crosspieces == 1) crosspiece2.setSelected(true);
        if (crosspieces == 2) crosspiece3.setSelected(true);
        if (crosspieces == 3) crosspiece4.setSelected(true);
        bgCrosspieces.add(crosspiece1);
        bgCrosspieces.add(crosspiece2);
        bgCrosspieces.add(crosspiece3);
        bgCrosspieces.add(crosspiece4);
        cadastrewms.add(crosspiece1, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(crosspiece2, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(crosspiece3, GBC.std().insets(5, 0, 5, 0));
        cadastrewms.add(crosspiece4, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 0, 5));
    }

    private void addGuiAutoCache(JPanel cadastrewms) {
        // option to enable automatic caching
        enableCache.addActionListener(e -> {
            jLabelCacheSize.setEnabled(enableCache.isSelected());
            cacheSize.setEnabled(enableCache.isSelected());
        });
        enableCache.setSelected(Config.getPref().getBoolean("cadastrewms.enableCaching", true));
        enableCache.setToolTipText(tr("Allows an automatic caching"));
        cadastrewms.add(enableCache, GBC.eop().insets(0, 0, 0, 0));
    }

    private void addGuiCacheSize(JPanel cadastrewms) {
        int size = getNumber("cadastrewms.cacheSize", DEFAULT_CACHE_SIZE);
        cacheSize.setText(String.valueOf(size));
        cacheSize.setToolTipText(tr("Oldest files are automatically deleted when this size is exceeded"));
        cadastrewms.add(jLabelCacheSize, GBC.std().insets(20, 0, 0, 0));
        cadastrewms.add(cacheSize, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 200, 5));
    }

    private void addGuiFirstWMSLayer(JPanel cadastrewms) {
        autoFirstLayer.setSelected(Config.getPref().getBoolean("cadastrewms.autoFirstLayer", false));
        autoFirstLayer.setToolTipText(tr("Automatically selects the first WMS layer if multiple layers exist when grabbing."));
        cadastrewms.add(autoFirstLayer, GBC.eop().insets(0, 0, 0, 0));
    }

    private void addGuiUseRelationsInAddresses(JPanel cadastrewms) {
        // option to use or not relations in addresses
        dontUseRelation.setSelected(Config.getPref().getBoolean("cadastrewms.addr.dontUseRelation", false));
        dontUseRelation.setToolTipText(tr("Enable this to use the tag \"add:street\" on nodes."));
        cadastrewms.add(dontUseRelation, GBC.eop().insets(0, 0, 0, 0));
    }

    private void addGuiMergeDownloadedDataLayers(JPanel cadastrewms) {
        // option to merge downloaded data layers
        mergeDataLayers.setSelected(Config.getPref().getBoolean("cadastrewms.merge.data.layers", false));
        mergeDataLayers.setToolTipText(tr("Merge automatically all cadastre data layers in a single final layer."));
        cadastrewms.add(mergeDataLayers, GBC.eop().insets(0, 0, 0, 0));
    }

    private static void addGuiEndOfDialogScrollBar(JPanel cadastrewms, JPanel cadastrewmsMast) {
        // end of dialog, scroll bar
        cadastrewms.add(Box.createVerticalGlue(), GBC.eol().fill(GridBagConstraints.VERTICAL));
        JScrollPane scrollpane = new JScrollPane(cadastrewms);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cadastrewmsMast.add(scrollpane, GBC.eol().fill(GridBagConstraints.BOTH));
    }

    @Override
    public boolean ok() {
        Config.getPref().put("cadastrewms.source", sourcing.getText());
        CadastrePlugin.source = sourcing.getText();
        Config.getPref().putBoolean("cadastrewms.alterColors", alterColors.isSelected());
        Config.getPref().putBoolean("cadastrewms.invertGrey", reversGrey.isSelected());
        Config.getPref().putBoolean("cadastrewms.backgroundTransparent", transparency.isSelected());
        Config.getPref().put("cadastrewms.brightness", Float.toString((float) sliderTrans.getValue()/10));
        Config.getPref().putBoolean("cadastrewms.drawBoundaries", drawBoundaries.isSelected());
        if (grabRes1.isSelected())
            Config.getPref().put(CADASTREWMS_RESOLUTION, HIGH);
        else if (grabRes2.isSelected())
            Config.getPref().put(CADASTREWMS_RESOLUTION, MEDIUM);
        else if (grabRes3.isSelected())
            Config.getPref().put(CADASTREWMS_RESOLUTION, LOW);
        if (imageInterpolationMethod.getSelectedIndex() == 2)
            Config.getPref().put(CADASTREWMS_IMAGE_INTERPOLATION, "bicubic");
        else if (imageInterpolationMethod.getSelectedIndex() == 1)
            Config.getPref().put(CADASTREWMS_IMAGE_INTERPOLATION, "bilinear");
        else
            Config.getPref().put(CADASTREWMS_IMAGE_INTERPOLATION, "standard");
        if (grabMultiplier1.isSelected())
            Config.getPref().put(CADASTREWMS_SCALE, Scale.X1.toString());
        else if (grabMultiplier2.isSelected())
            Config.getPref().put(CADASTREWMS_SCALE, Scale.X2.toString());
        else if (grabMultiplier3.isSelected())
            Config.getPref().put(CADASTREWMS_SCALE, Scale.X3.toString());
        else {
            Config.getPref().put(CADASTREWMS_SCALE, Scale.SQUARE_100M.toString());
            try {
                int squareSize = Integer.parseInt(grabMultiplier4Size.getText());
                if (squareSize >= 25 && squareSize <= 1000)
                    Config.getPref().put("cadastrewms.squareSize", grabMultiplier4Size.getText());
            } catch (NumberFormatException e) {
                Logging.debug(e);
            }
        }
        Config.getPref().putBoolean("cadastrewms.layerWater", layerLS3.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerBuilding", layerLS2.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerSymbol", layerLS1.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerParcel", layerParcel.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerLabel", layerLabel.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerNumero", layerNumero.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerLieudit", layerLieudit.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerSection", layerSection.isSelected());
        Config.getPref().putBoolean("cadastrewms.layerCommune", layerCommune.isSelected());
        try {
            int i = Integer.parseInt(rasterDivider.getText());
            if (i > 0 && i < 13)
                Config.getPref().put("cadastrewms.rasterDivider", String.valueOf(i));
        } catch (NumberFormatException e) {
            Logging.debug(e);
        }
        Config.getPref().putBoolean("cadastrewms.noImageCropping", disableImageCropping.isSelected());
        Config.getPref().putBoolean("cadastrewms.useTA", enableTableauAssemblage.isSelected());
        Config.getPref().putBoolean("cadastrewms.raster2bitsColors", simplify2BitsColors.isSelected());
        if (crosspiece1.isSelected()) Config.getPref().put(CADASTREWMS_CROSSPIECES, "0");
        else if (crosspiece2.isSelected()) Config.getPref().put(CADASTREWMS_CROSSPIECES, "1");
        else if (crosspiece3.isSelected()) Config.getPref().put(CADASTREWMS_CROSSPIECES, "2");
        else if (crosspiece4.isSelected()) Config.getPref().put(CADASTREWMS_CROSSPIECES, "3");
        Config.getPref().putBoolean("cadastrewms.enableCaching", enableCache.isSelected());

        // spread data into objects instead of restarting the application
        try {
            CacheControl.cacheSize = Integer.parseInt(cacheSize.getText());
            Config.getPref().put("cadastrewms.cacheSize", String.valueOf(CacheControl.cacheSize));
        } catch (NumberFormatException e) {
            Logging.debug(e);
        }
        Config.getPref().putBoolean("cadastrewms.autoFirstLayer", autoFirstLayer.isSelected());
        CacheControl.cacheEnabled = enableCache.isSelected();
        Config.getPref().putBoolean("cadastrewms.addr.dontUseRelation", dontUseRelation.isSelected());
        Config.getPref().putBoolean("cadastrewms.merge.data.layers", mergeDataLayers.isSelected());
        CadastrePlugin.refreshConfiguration();
        CadastrePlugin.refreshMenu();

        return false;
    }

    private static int getNumber(String prefParameter, int defValue) {
        try {
            return Integer.parseInt(Config.getPref().get(prefParameter, String.valueOf(defValue)));
        } catch (NumberFormatException e) {
            return defValue;
        }
    }
}
