// SPDX-License-Identifier: WTFPL
package org.openstreetmap.josm.plugins.rasterfilters.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.rmi.server.UID;
import java.util.Hashtable;

import com.bric.colorpicker.ColorPickerMode;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.rasterfilters.model.FiltersManager;
import org.openstreetmap.josm.tools.Logging;

import com.bric.colorpicker.ColorPicker;

/**
 * FilterPanel is usual JPanel with its
 * own GUI elements which is added according to
 * meta-information of filter.
 *
 * @author Nipel-Crumple
 */
public class FilterPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "title";
    private static final String DEFAULT = "default";
    private static final String ARIAL = "Arial";
    private UID filterId;
    private int neededHeight;

    /**
     * Create a new {@link FilterPanel}
     */
    public FilterPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBackground(Color.white);
    }

    /**
     * Methods adds GUI element on filter's panel according to meta-information and
     * automatically resizes the given filter's panel.
     *
     * @param json filter's meta-information
     * @return added GUI element
     */
    public JComponent addGuiElement(JsonObject json) {
        String type = json.getString("type");

        switch (type) {
            case "linear_slider":
                setNeededHeight(getNeededHeight() + 70);
                return createSlider(json);

            case "checkbox":
                setNeededHeight(getNeededHeight() + 30);

                JCheckBox checkBox = createCheckBox(json.getString(TITLE));
                checkBox.setSelected(json.getBoolean(DEFAULT));
                checkBox.setName(json.getString("name"));

                return checkBox;

            case "select":
                setNeededHeight(getNeededHeight() + 50);
                return createSelect(json);

            case "colorpicker":
                setNeededHeight(getNeededHeight() + 220);
                return createColorPicker(json);
            default:
                return null;
        }
    }

    private JComponent createSelect(JsonObject json) {

        Font font = new Font(ARIAL, Font.PLAIN, 14);

        JPanel selectPanel = new JPanel();

        selectPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        selectPanel.setBackground(Color.white);
        selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.X_AXIS));
        selectPanel.setMaximumSize(new Dimension(300, 40));

        JLabel selectTitle = new JLabel(json.getString(TITLE));

        selectTitle.setFont(font);
        selectTitle.setBackground(Color.white);

        JsonArray valuesArray = json.getJsonArray("values");

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();

        model.setSelectedItem(json.getString(DEFAULT));

        for (int i = 0; i < valuesArray.size(); i++) {
            model.addElement(valuesArray.getString(i));
        }

        JComboBox<String> selectBox = new JComboBox<>(model);
        selectBox.setMinimumSize(new Dimension(140, 30));

        selectPanel.add(selectTitle);
        selectPanel.add(Box.createHorizontalGlue());
        selectPanel.add(selectBox);
        selectBox.setName(json.getString("name"));

        this.add(selectPanel);

        return selectBox;
    }

    public JComponent createColorPicker(JsonObject json) {

        ColorPicker picker = new ColorPicker(false, false);
        picker.setPreferredSize(new Dimension(200, 180));
        picker.setMode(ColorPickerMode.HUE);
        picker.setName(json.getString("name"));

        addControlTitle(json.getString(TITLE));

        this.add(picker);

        return picker;
    }

    public JCheckBox createCheckBox(String text) {

        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setMaximumSize(new Dimension(300, 30));
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.X_AXIS));
        checkBoxPanel.setBackground(Color.white);

        JCheckBox checkBox = new JCheckBox(text);
        Font font = new Font(ARIAL, Font.PLAIN, 12);

        checkBox.setFont(font);
        checkBox.setBackground(Color.white);
        checkBox.setName(text);

        checkBoxPanel.add(checkBox);

        this.add(checkBoxPanel);

        return checkBox;
    }

    private static JCheckBox createDisableBox(ItemListener listener) {
        JCheckBox disable = new JCheckBox("Disable");
        Font font = new Font(ARIAL, Font.PLAIN, 12);

        disable.addItemListener(listener);
        disable.setFont(font);

        return disable;
    }

    private static JButton createRemoveButton(ActionListener listener) {
        JButton removeButton = new JButton("Remove");
        Font font = new Font(ARIAL, Font.PLAIN, 12);

        removeButton.setFont(font);
        removeButton.setName("remove");

        removeButton.addActionListener(listener);

        return removeButton;
    }

    public JPanel createBottomPanel(FiltersManager listener) {

        this.add(Box.createRigidArea(new Dimension(0, 10)));
        JPanel bottom = new JPanel();

        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        bottom.setMaximumSize(new Dimension(300, 40));
        bottom.setBorder(BorderFactory
                .createMatteBorder(2, 0, 0, 0, Color.gray));

        bottom.add(createDisableBox(listener));
        bottom.add(Box.createHorizontalGlue());
        bottom.add(createRemoveButton(listener));

        this.add(bottom);

        return bottom;
    }

    private void addControlTitle(String labelText) {
        Font labelFont = new Font(ARIAL, Font.PLAIN, 14);

        JPanel sliderLabelPanel = new JPanel();
        sliderLabelPanel.setMaximumSize(new Dimension(400, 30));
        sliderLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        sliderLabelPanel.setBackground(Color.white);

        JLabel sliderLabel = new JLabel(labelText, SwingConstants.LEFT);
        sliderLabel.setFont(labelFont);
        sliderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sliderLabel.setVisible(true);

        sliderLabelPanel.add(sliderLabel);

        this.add(sliderLabelPanel);
    }

    public JSlider createSlider(JsonObject json) {

        Border sliderBorder = new EmptyBorder(5, 5, 5, 5);

        addControlTitle(json.getString(TITLE));

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        JsonArray array = json.getJsonArray("scale");

        String valueType = json.getString("value_type");

        JSlider slider = null;
        if ("integer".equals(valueType)) {
            int minValue = array.getInt(0);
            int maxValue = array.getInt(1);
            int initValue = json.getInt(DEFAULT);

            Logging.debug("Slider is integer\n");
            Logging.debug("minValue: " + minValue
                    + "maxValue: " + maxValue);
            try {
                slider = new JSlider(SwingConstants.HORIZONTAL, minValue, maxValue,
                        initValue);
                slider.setName(json.getString("name"));
                slider.setToolTipText(String.valueOf(slider.getValue()));
                slider.setMinorTickSpacing(maxValue / 4);
            } catch (IllegalArgumentException e) {
                Logging.trace(e);
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("JSlider initialization error. Make sure your meta-inf is correct."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }

        } else if ("float".equals(valueType)) {

            Logging.debug("Slider is float\n");
            // every value is supplied by 10 to be integer for slider
            double minValueDouble = array.getJsonNumber(0).doubleValue();
            double maxValueDouble = array.getJsonNumber(1).doubleValue();
            Logging.debug("DminValue: " + minValueDouble
                    + "DmaxValue: " + maxValueDouble);

            int minValue = (int) (minValueDouble * 100);
            int maxValue = (int) (maxValueDouble * 100);


            double initValue = json.getJsonNumber(DEFAULT).doubleValue() * 100;
            double delta = (maxValue - minValue) / 100d;

            for (int i = 0; i <= maxValue; i++) {

                if ((i % 20) == 0) {

                    labelTable.put(Integer.valueOf(i),
                            new JLabel(String.valueOf(i * delta / 100)));

                }
            }

            try {
                slider = new JSlider(SwingConstants.HORIZONTAL, minValue, maxValue, (int) initValue);
                slider.setMinorTickSpacing(maxValue / 4);
                slider.setName(json.getString("name"));
                slider.setToolTipText(String.valueOf((double) slider.getValue() / 100));
            } catch (IllegalArgumentException e) {
                Logging.trace(e);
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("JSlider initialization error. Make sure your meta-inf is correct."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        slider.setBackground(this.getBackground());
        slider.setBorder(sliderBorder);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        this.add(slider);

        return slider;
    }

    public void setFilterId(UID filterId) {
        this.filterId = filterId;
    }

    public UID getFilterId() {
        return filterId;
    }

    public int getNeededHeight() {
        return neededHeight;
    }

    public void setNeededHeight(int neededHeight) {
        this.neededHeight = neededHeight;
    }
}
