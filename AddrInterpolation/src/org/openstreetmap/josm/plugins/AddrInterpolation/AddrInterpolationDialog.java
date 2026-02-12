// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.AddrInterpolation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Address Interpolation dialog
 */
public class AddrInterpolationDialog extends JDialog implements ActionListener {

    private Way selectedStreet;
    private Way addrInterpolationWay;
    private Relation associatedStreetRelation;
    private ArrayList<Node> houseNumberNodes;  // Additional nodes with addr:housenumber

    private static String lastIncrement = "";
    private static int lastAccuracyIndex;
    private static String lastCity = "";
    private static String lastState = "";
    private static String lastPostCode = "";
    private static String lastCountry = "";
    private static String lastFullAddress = "";
    private static boolean lastConvertToHousenumber;

    // Edit controls
    private EscapeDialog dialog;
    private JRadioButton streetNameButton;
    private JRadioButton streetRelationButton;
    private JTextField startTextField;
    private JTextField endTextField;
    private JTextField incrementTextField;
    private JTextField cityTextField;
    private JTextField stateTextField;
    private JTextField postCodeTextField;
    private JTextField countryTextField;
    private JTextField fullTextField;
    private Checkbox cbConvertToHouseNumbers;

    private boolean relationChanged; // Whether to re-trigger data changed for relation
    // Track whether interpolation method is known so that auto detect doesn't override a previous choice.
    private boolean interpolationMethodSet;

    // NOTE: The following 2 arrays must match in number of elements and position
    // Tag values for map (Except that 'Numeric' is replaced by actual # on map)
    String[] addrInterpolationTags = {"odd", "even", "all", "alphabetic", "Numeric"};
    String[] addrInterpolationStrings = {tr("Odd"), tr("Even"), tr("All"), tr("Alphabetic"), tr("Numeric") }; // Translatable names for display
    private static final int NUMERIC_INDEX = 4;
    private JComboBox<String> addrInterpolationList;

    // NOTE: The following 2 arrays must match in number of elements and position
    String[] addrInclusionTags = {"actual", "estimate", "potential" }; // Tag values for map
    String[] addrInclusionStrings = {tr("Actual"), tr("Estimate"), tr("Potential") }; // Translatable names for display
    private JComboBox<String> addrInclusionList;

    // For tracking edit changes as group for undo
    private Collection<Command> commandGroup;
    private Relation editedRelation;

    /**
     * Create a new address interpolation dialog with a specified name
     * @param name The name to show in the title bar
     */
    public AddrInterpolationDialog(String name) {

        if (!findAndSaveSelections()) {
            return;
        }

        JPanel editControlsPane = createEditControls();

        showDialog(editControlsPane, name);
    }

    /**
     * Show the dialog
     * @param editControlsPane The controls to show
     * @param name The name of the dialog
     */
    private void showDialog(JPanel editControlsPane, String name) {
        dialog = new EscapeDialog(MainApplication.getMainFrame(), name, true);

        dialog.add(editControlsPane);
        dialog.setSize(new Dimension(300, 500));
        dialog.setLocation(new Point(100, 300));

        // Listen for windowOpened event to set focus
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (addrInterpolationWay != null) {
                    startTextField.requestFocus();
                } else {
                    cityTextField.requestFocus();
                }
            }
        });

        dialog.setVisible(true);
        updateFields(this);
    }

    private static void updateFields(AddrInterpolationDialog dialog) {
        lastIncrement = dialog.incrementTextField.getText();
        lastCity = dialog.cityTextField.getText();
        lastState = dialog.stateTextField.getText();
        lastPostCode = dialog.postCodeTextField.getText();
        lastCountry = dialog.countryTextField.getText();
        lastFullAddress = dialog.fullTextField.getText();
        lastConvertToHousenumber = dialog.cbConvertToHouseNumbers.getState();
    }

    /**
     * Create edit control items and return JPanel on which they reside
     * @return the edit controls
     */
    private JPanel createEditControls() {

        JPanel editControlsPane = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        editControlsPane.setLayout(gridbag);

        editControlsPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String streetName = selectedStreet.get("name");
        String streetRelation = findRelation();
        if ("".equals(streetRelation)) {
            streetRelation = " (Create new)";
        }
        streetNameButton = new JRadioButton(tr("Name: {0}", streetName));
        streetRelationButton = new JRadioButton(tr("Relation: {0}", streetRelation));
        if (associatedStreetRelation == null) {
            streetNameButton.setSelected(true);
        } else {
            streetRelationButton.setSelected(true);
        }

        // Create edit controls for street / relation radio buttons
        ButtonGroup group = new ButtonGroup();
        group.add(streetNameButton);
        group.add(streetRelationButton);
        JPanel radioButtonPanel = new JPanel(new BorderLayout());
        radioButtonPanel.setBorder(BorderFactory.createTitledBorder(tr("Associate with street using:")));
        radioButtonPanel.add(streetNameButton, BorderLayout.NORTH);
        radioButtonPanel.add(streetRelationButton, BorderLayout.SOUTH);

        // Add to edit panel
        c.gridx = 0;
        c.gridwidth = 2; // # of columns to span
        c.fill = GridBagConstraints.HORIZONTAL;      // Full width
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        editControlsPane.add(radioButtonPanel, c);

        JLabel numberingLabel = new JLabel(tr("Numbering Scheme:"));
        addrInterpolationList = new JComboBox<>(addrInterpolationStrings);

        JLabel incrementLabel = new JLabel(tr("Increment:"));
        incrementTextField = new JTextField(lastIncrement, 100);
        incrementTextField.setEnabled(false);

        JLabel startLabel = new JLabel(tr("Starting #:"));
        JLabel endLabel = new JLabel(tr("Ending #:"));

        startTextField = new JTextField(10);
        endTextField = new JTextField(10);

        JLabel inclusionLabel = new JLabel(tr("Accuracy:"));
        addrInclusionList = new JComboBox<>(addrInclusionStrings);
        addrInclusionList.setSelectedIndex(lastAccuracyIndex);

        // Preload any values already set in map
        getExistingMapKeys();


        JLabel[] textLabels = {startLabel, endLabel, numberingLabel, incrementLabel, inclusionLabel};
        Component[] editFields = {startTextField, endTextField, addrInterpolationList, incrementTextField, addrInclusionList};
        addEditControlRows(textLabels, editFields, editControlsPane);

        cbConvertToHouseNumbers = new Checkbox(tr("Convert way to individual house numbers."), null, lastConvertToHousenumber);
        // cbConvertToHouseNumbers.setSelected(lastConvertToHousenumber);

        // Address interpolation fields not valid if Way not selected
        if (addrInterpolationWay == null) {
            addrInterpolationList.setEnabled(false);
            startTextField.setEnabled(false);
            endTextField.setEnabled(false);
            cbConvertToHouseNumbers.setEnabled(false);
        }

        JPanel optionPanel = createOptionalFields();
        c.gridx = 0;
        c.gridwidth = 2; // # of columns to span
        c.fill = GridBagConstraints.BOTH;      // Full width
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row

        editControlsPane.add(optionPanel, c);

        KeyAdapter enterProcessor = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && validateAndSave()) {
                    dialog.dispose();
                }
            }
        };

        // Make Enter == OK click on fields using this adapter
        endTextField.addKeyListener(enterProcessor);
        cityTextField.addKeyListener(enterProcessor);
        addrInterpolationList.addKeyListener(enterProcessor);
        incrementTextField.addKeyListener(enterProcessor);

        // Watch when Interpolation Method combo box is selected so that
        // it can auto-detect method based on entered numbers.
        addrInterpolationList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent fe) {
                if (!interpolationMethodSet && autoDetectInterpolationMethod()) {
                    interpolationMethodSet = true;  // Don't auto detect over a previous choice
                }
            }
        });

        // Watch when Interpolation Method combo box is changed so that
        // Numeric increment box can be enabled or disabled.
        addrInterpolationList.addActionListener(e -> {
            int selectedIndex = addrInterpolationList.getSelectedIndex();
            incrementTextField.setEnabled(selectedIndex == NUMERIC_INDEX); // Enable or disable numeric field
        });

        editControlsPane.add(cbConvertToHouseNumbers, c);

        if (!houseNumberNodes.isEmpty()) {
            JLabel houseNumberNodeNote = new JLabel(tr("Will associate {0} additional house number nodes",
                    houseNumberNodes.size()));
            editControlsPane.add(houseNumberNodeNote, c);
        }

        editControlsPane.add(new UrlLabel("https://wiki.openstreetmap.org/wiki/JOSM/Plugins/AddrInterpolation",
                tr("More information about this feature"), 2), c);

        c.gridx = 0;
        c.gridwidth = 1; //next-to-last
        c.fill = GridBagConstraints.NONE;      //reset to default
        c.weightx = 0.0;
        c.insets = new Insets(15, 0, 0, 0);
        c.anchor = GridBagConstraints.LINE_END;
        JButton okButton = new JButton(tr("OK"), ImageProvider.get("ok"));
        editControlsPane.add(okButton, c);

        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;     //end row
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LINE_START;

        JButton cancelButton = new JButton(tr("Cancel"), ImageProvider.get("cancel"));
        editControlsPane.add(cancelButton, c);

        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);

        return editControlsPane;
    }

    // Call after both starting and ending housenumbers have been entered - usually when
    // combo box gets focus.
    // Return true if a method was detected
    private boolean autoDetectInterpolationMethod() {
        String startValueString = readTextField(startTextField);
        String endValueString = readTextField(endTextField);
        if ((startValueString == null) || (endValueString == null)) {
            // Not all values entered yet
            return false;
        }

        // String[] addrInterpolationTags = { "odd", "even", "all", "alphabetic", ### };  // Tag values for map

        if (isLong(startValueString) && isLong(endValueString)) {
            // Have 2 numeric values
            long startValue = Long.parseLong(startValueString);
            long endValue = Long.parseLong(endValueString);

            if (isEven(startValue)) {
                if (isEven(endValue)) {
                    selectInterpolationMethod("even");
                } else {
                    selectInterpolationMethod("all");
                }
            } else {
                if (!isEven(endValue)) {
                    selectInterpolationMethod("odd");
                } else {
                    selectInterpolationMethod("all");
                }
            }
        } else {
            // Test for possible alpha
            char startingChar = startValueString.charAt(startValueString.length()-1);
            char endingChar = endValueString.charAt(endValueString.length()-1);

            if (!isNumeric("" + startingChar) && !isNumeric("" + endingChar)) {
                // Both end with alpha
                selectInterpolationMethod("alphabetic");
                return true;
            }

            if (isNumeric("" + startingChar) && !isNumeric("" + endingChar)) {
                endingChar = Character.toUpperCase(endingChar);
                if ((endingChar >= 'A') && (endingChar <= 'Z')) {
                    // First is a number, last is Latin alpha
                    selectInterpolationMethod("alphabetic");
                    return true;
                }
            }
            
            // Did not detect alpha
            return false;
        }
        return true;
    }

    /**
     * Set Interpolation Method combo box to method specified by 'currentMethod' (an OSM key value)
     * @param currentMethod The current interpolation method (number or key values)
     */
    private void selectInterpolationMethod(String currentMethod) {
        int currentIndex = 0;
        if (isLong(currentMethod)) {
            // Valid number: Numeric increment method
            currentIndex = addrInterpolationTags.length-1;
            incrementTextField.setText(currentMethod);
            incrementTextField.setEnabled(true);
        } else {
            // Must scan OSM key values because combo box is already loaded with translated strings
            for (int i = 0; i < addrInterpolationTags.length; i++) {
                if (addrInterpolationTags[i].equals(currentMethod)) {
                    currentIndex = i;
                    break;
                }
            }
        }
        addrInterpolationList.setSelectedIndex(currentIndex);

    }

    /**
     * Set Inclusion Method combo box to method specified by 'currentMethod' (an OSM key value)
     * @param currentMethod The key to use
     */
    private void selectInclusion(String currentMethod) {
        int currentIndex = 0;
        // Must scan OSM key values because combo box is already loaded with translated strings
        for (int i = 0; i < addrInclusionTags.length; i++) {
            if (addrInclusionTags[i].equals(currentMethod)) {
                currentIndex = i;
                break;
            }
        }
        addrInclusionList.setSelectedIndex(currentIndex);
    }

    /**
     * Create optional control fields in a group box
     * @return a panel with optional fields
     */
    private JPanel createOptionalFields() {

        JPanel editControlsPane = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();

        editControlsPane.setLayout(gridbag);

        editControlsPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel[] optionalTextLabels = {new JLabel(tr("City:")),
                new JLabel(tr("State:")),
                new JLabel(tr("Post Code:")),
                new JLabel(tr("Country:")),
                new JLabel(tr("Full Address:"))};
        cityTextField = new JTextField(lastCity, 100);
        stateTextField = new JTextField(lastState, 100);
        postCodeTextField = new JTextField(lastPostCode, 20);
        countryTextField = new JTextField(lastCountry, 2);
        fullTextField = new JTextField(lastFullAddress, 300);

        // Special processing for addr:country code, max length and uppercase
        countryTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                JTextField jtextfield = (JTextField) e.getSource();
                String text = jtextfield.getText();
                int length = text.length();
                if (length == jtextfield.getColumns()) {
                    e.consume();
                } else if (length > jtextfield.getColumns()) {
                    // show error message ??
                    e.consume();
                } else {
                    // Accept key; convert to upper case
                    if (!e.isActionKey()) {
                        e.setKeyChar(Character.toUpperCase(e.getKeyChar()));
                    }
                }
            }
        });

        Component[] optionalEditFields = {cityTextField, stateTextField, postCodeTextField, countryTextField, fullTextField};
        addEditControlRows(optionalTextLabels, optionalEditFields, editControlsPane);

        JPanel optionPanel = new JPanel(new BorderLayout());
        Border groupBox = BorderFactory.createEtchedBorder();
        TitledBorder titleBorder = BorderFactory.createTitledBorder(groupBox, tr("Optional Information:"),
                TitledBorder.LEFT, TitledBorder.TOP);

        optionPanel.setBorder(titleBorder);
        optionPanel.add(editControlsPane, BorderLayout.CENTER);

        return optionPanel;
    }

    /**
     * Populate dialog for any possible existing settings if editing an existing Address interpolation way
     */
    private void getExistingMapKeys() {

        // Check all nodes for optional addressing data
        //    Address interpolation nodes will overwrite these value if they contain optional data
        for (Node node : houseNumberNodes) {
            checkNodeForAddressTags(node);
        }

        if (addrInterpolationWay != null) {
            String currentMethod = addrInterpolationWay.get("addr:interpolation");
            if (currentMethod != null) {

                selectInterpolationMethod(currentMethod);
                interpolationMethodSet = true;  // Don't auto detect over a previous choice
            }

            String currentInclusion = addrInterpolationWay.get("addr:inclusion");
            if (currentInclusion != null) {
                selectInclusion(currentInclusion);
            }

            Node firstNode = addrInterpolationWay.getNode(0);
            Node lastNode = addrInterpolationWay.getNode(addrInterpolationWay.getNodesCount()-1);

            // Get any existing start / end # values
            String value = firstNode.get("addr:housenumber");
            if (value != null) {
                startTextField.setText(value);
            }

            value = lastNode.get("addr:housenumber");
            if (value != null) {
                endTextField.setText(value);
            }
            checkNodeForAddressTags(firstNode);
            checkNodeForAddressTags(lastNode);
        }
    }

    /**
     * Check for any existing address data.
     * If found, overwrite any previous data
     * @param checkNode The node to look for possible existing values
     */
    private static void checkNodeForAddressTags(Tagged checkNode) {
        // Interrogate possible existing optional values
        String value = checkNode.get("addr:city");
        if (value != null) {
            lastCity = value;
        }
        value = checkNode.get("addr:state");
        if (value != null) {
            lastState = value;
        }
        value = checkNode.get("addr:postcode");
        if (value != null) {
            lastPostCode = value;
        }
        value = checkNode.get("addr:country");
        if (value != null) {
            lastCountry = value;
        }
        value = checkNode.get("addr:full");
        if (value != null) {
            lastFullAddress = value;
        }
    }

    /**
     * Look for a possible 'associatedStreet' type of relation for selected street
     * @return relation description, or an empty string
     */
    private String findRelation() {
        final DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
        if (currentDataSet != null) {
            for (Relation relation : currentDataSet.getRelations()) {
                if ("associatedStreet".equals(relation.get("type"))) {
                    for (RelationMember relationMember : relation.getMembers()) {
                        if (relationMember.isWay()) {
                            final StringBuilder relationDescription = new StringBuilder();
                            Way way = (Way) relationMember.getMember();
                            // System.out.println("Name: " + way.get("name") );
                            if (way == selectedStreet) {
                                associatedStreetRelation = relation;
                                relationDescription.append(way.getId()).append('(');

                                if (relation.getKeys().containsKey("name")) {
                                    relationDescription.append(relation.get("name"));
                                } else {
                                    // Relation is unnamed - use street name
                                    relationDescription.append(selectedStreet.get("name"));
                                }
                                relationDescription.append(')');
                                return relationDescription.toString();
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /** We can proceed only if there is both a named way (the 'street') and
     * one un-named way (the address interpolation way ) selected.
     *    The plugin menu item is enabled after a single way is selected to display a more meaningful
     *    message (a new user may not realize that they need to select both the street and
     *    address interpolation way first).
     * Also, selected street and working address interpolation ways are saved.
     * @return {@code true} if
     */
    private boolean findAndSaveSelections() {

        boolean isValid = false;

        int namedWayCount = 0;
        int unNamedWayCount = 0;
        DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
        if (currentDataSet != null) {
            for (Way way : currentDataSet.getSelectedWays()) {
                if (way.getKeys().containsKey("name")) {
                    namedWayCount++;
                    this.selectedStreet = way;
                } else {
                    unNamedWayCount++;
                    this.addrInterpolationWay = way;
                }
            }

            // Get additional nodes with addr:housenumber tags:
            //   Either selected or in the middle of the Address Interpolation way
            //     Do not include end points of Address Interpolation way in this set yet.
            houseNumberNodes = new ArrayList<>();
            // Check selected nodes
            for (Node node : currentDataSet.getSelectedNodes()) {
                if (node.getKeys().containsKey("addr:housenumber")) {
                    houseNumberNodes.add(node);
                }
            }

            // Check nodes in middle of address interpolation way
            if (addrInterpolationWay != null && addrInterpolationWay.getNodesCount() > 2) {
                for (int i = 1; i < (addrInterpolationWay.getNodesCount()-2); i++) {
                    Node testNode = addrInterpolationWay.getNode(i);
                    if (testNode.getKeys().containsKey("addr:housenumber")) {
                        houseNumberNodes.add(testNode);
                    }
                }
            }
        }

        if (namedWayCount != 1) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Please select a street to associate with address interpolation way"),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            // Avoid 2 error dialogs if both conditions don't match
            if (unNamedWayCount != 1) {
                // Allow for street + house number nodes only to be selected (no address interpolation way).
                if (!houseNumberNodes.isEmpty()) {
                    isValid = true;
                } else {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("Please select address interpolation way for this street"),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                isValid = true;
            }
        }

        return isValid;
    }

    /**
     * Add rows of edit controls - with labels in the left column, and controls in the right
     * column on the gridbag of the specified container.
     * @param labels labels on left column
     * @param editFields edit fields on the right column
     * @param container container
     */
    private static void addEditControlRows(JLabel[] labels,
                                    Component[] editFields,
                                    Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++) {
            c.gridx = 0;
            c.gridwidth = 1; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            container.add(labels[i], c);

            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            container.add(editFields[i], c);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("ok".equals(e.getActionCommand())) {
            if (validateAndSave()) {
                dialog.dispose();
            }
        } else if ("cancel".equals(e.getActionCommand())) {
            dialog.dispose();
        }
    }

    /**
     * For Alpha interpolation, return base string
     * For example: "22A" -> "22"
     * For example: "A" -> ""
     *  Input string must not be empty
     * @param strValue The value to get the base string of
     * @return the base string, or an empty string
     */
    private static String baseAlpha(String strValue) {
        if (strValue.length() > 0) {
            return strValue.substring(0, strValue.length() - 1);
        } else {
            return "";
        }
    }

    /**
     * Get the last char of a string
     * @param strValue The string to get the last char of
     * @return The last char, or {@code 0}.
     */
    private static char lastChar(String strValue) {
        if (strValue.length() > 0) {
            return strValue.charAt(strValue.length() - 1);
        } else {
            return 0;
        }
    }

    /**
     * Test for valid positive long int
     * @param input The input string
     * @return {@code true} if the input is a positive long
     */
    private static boolean isLong(String input) {
        try {
            long val = Long.parseLong(input);
            return (val > 0);
        } catch (NumberFormatException e) {
            Logging.trace(e);
            return false;
        }
    }

    /**
     * Check if a number is even
     * @param input The input number
     * @return {@code true} if the number is even
     */
    private static boolean isEven(long input) {
        return ((input % 2) == 0);
    }

    private static final Pattern PATTERN_NUMERIC = Pattern.compile("^[0-9]+$");

    /**
     * Check if a string is numeric
     * @param s The string to check
     * @return {@code true} if it is a numeric string
     */
    private static boolean isNumeric(String s) {
        return PATTERN_NUMERIC.matcher(s).matches();
    }


    private void interpolateAlphaSection(int startNodeIndex, int endNodeIndex, String endValueString,
                                         char startingChar, char endingChar) {


        String baseAlpha = baseAlpha(endValueString);
        int nSegments = endNodeIndex - startNodeIndex;

        double[] segmentLengths = new double[nSegments];
        // Total length of address interpolation way section
        double totalLength = calculateSegmentLengths(startNodeIndex, endNodeIndex, segmentLengths);


        int nHouses = endingChar - startingChar-1;  // # of house number nodes to create
        if (nHouses > 0) {

            double houseSpacing = totalLength / (nHouses+1);

            Node lastHouseNode = addrInterpolationWay.getNode(startNodeIndex);
            int currentSegment = 0; // Segment being used to place new house # node
            char currentChar = startingChar;
            while (nHouses > 0) {
                double distanceNeeded = houseSpacing;

                // Move along segments until we can place the new house number
                while (distanceNeeded > segmentLengths[currentSegment]) {
                    distanceNeeded -= segmentLengths[currentSegment];
                    currentSegment++;
                    lastHouseNode = addrInterpolationWay.getNode(startNodeIndex + currentSegment);
                }

                // House number is to be positioned in current segment.
                double proportion = distanceNeeded / segmentLengths[currentSegment];
                Node toNode = addrInterpolationWay.getNode(startNodeIndex + 1 + currentSegment);
                LatLon newHouseNumberPosition = lastHouseNode.getCoor().interpolate(toNode.getCoor(), proportion);

                Node newHouseNumberNode = new Node(newHouseNumberPosition);
                currentChar++;
                if ((currentChar > 'Z') && (currentChar < 'a')) {
                    // Wraparound past uppercase Z: go directly to lower case a
                    currentChar = 'a';

                }
                String newHouseNumber = baseAlpha + currentChar;
                newHouseNumberNode.put("addr:housenumber", newHouseNumber);

                commandGroup.add(new AddCommand(MainApplication.getLayerManager().getEditDataSet(), newHouseNumberNode));
                houseNumberNodes.add(newHouseNumberNode);   // Street, etc information to be added later

                lastHouseNode = newHouseNumberNode;


                segmentLengths[currentSegment] -= distanceNeeded; // Track amount used
                nHouses--;
            }
        }
    }

    private void createAlphaInterpolation(String startValueString, String endValueString) {
        char startingChar = lastChar(startValueString);
        char endingChar = lastChar(endValueString);

        if (isLong(startValueString)) {
            // Special case of numeric first value, followed by 'A'
            startingChar = 'A'-1;
        }

        // Search for possible anchors from the 2nd node to 2nd from last, interpolating between each anchor
        int startIndex = 0; // Index into first interpolation zone of address interpolation way
        for (int i = 1; i < addrInterpolationWay.getNodesCount()-1; i++) {
            Node testNode = addrInterpolationWay.getNode(i);
            String endNodeNumber = testNode.get("addr:housenumber");
            // This is a potential anchor node
            if (endNodeNumber != null && !"".equals(endNodeNumber)) {
                char anchorChar = lastChar(endNodeNumber);
                if ((anchorChar > startingChar) && (anchorChar < endingChar)) {
                    // Lies within the expected range
                    interpolateAlphaSection(startIndex, i, endNodeNumber, startingChar, anchorChar);

                    // For next interpolation section
                    startingChar = anchorChar;
                    startValueString = endNodeNumber;
                    startIndex = i;
                }
            }
        }

        // End nodes do not actually contain housenumber value yet (command has not executed), so use user-entered value
        interpolateAlphaSection(startIndex, addrInterpolationWay.getNodesCount()-1, endValueString, startingChar, endingChar);
    }

    private double calculateSegmentLengths(int startNodeIndex, int endNodeIndex, double[] segmentLengths) {
        Node fromNode = addrInterpolationWay.getNode(startNodeIndex);
        double totalLength = 0.0;
        int nSegments = segmentLengths.length;
        for (int segment = 0; segment < nSegments; segment++) {
            Node toNode = addrInterpolationWay.getNode(startNodeIndex + 1 + segment);
            segmentLengths[segment] = fromNode.greatCircleDistance(toNode);
            totalLength += segmentLengths[segment];

            fromNode = toNode;
        }
        return totalLength;
    }

    private void interpolateNumericSection(int startNodeIndex, int endNodeIndex,
                                           long startingAddr, long endingAddr,
                                           long increment) {

        int nSegments = endNodeIndex - startNodeIndex;

        double[] segmentLengths = new double[nSegments];

        // Total length of address interpolation way section
        double totalLength = calculateSegmentLengths(startNodeIndex, endNodeIndex, segmentLengths);

        int nHouses = (int) ((endingAddr - startingAddr) / increment) -1;
        if (nHouses > 0) {

            double houseSpacing = totalLength / (nHouses+1);

            Node lastHouseNode = addrInterpolationWay.getNode(startNodeIndex);
            int currentSegment = 0; // Segment being used to place new house # node
            long currentHouseNumber = startingAddr;
            while (nHouses > 0) {
                double distanceNeeded = houseSpacing;

                // Move along segments until we can place the new house number
                while (distanceNeeded > segmentLengths[currentSegment]) {
                    distanceNeeded -= segmentLengths[currentSegment];
                    currentSegment++;
                    lastHouseNode = addrInterpolationWay.getNode(startNodeIndex + currentSegment);
                }

                // House number is to be positioned in current segment.
                double proportion = distanceNeeded / segmentLengths[currentSegment];
                Node toNode = addrInterpolationWay.getNode(startNodeIndex + 1 + currentSegment);
                LatLon newHouseNumberPosition = lastHouseNode.getCoor().interpolate(toNode.getCoor(), proportion);


                Node newHouseNumberNode = new Node(newHouseNumberPosition);
                currentHouseNumber += increment;
                String newHouseNumber = Long.toString(currentHouseNumber);
                newHouseNumberNode.put("addr:housenumber", newHouseNumber);

                commandGroup.add(new AddCommand(MainApplication.getLayerManager().getEditDataSet(), newHouseNumberNode));
                houseNumberNodes.add(newHouseNumberNode);   // Street, etc information to be added later

                lastHouseNode = newHouseNumberNode;

                segmentLengths[currentSegment] -= distanceNeeded; // Track amount used
                nHouses--;
            }
        }
    }

    private void createNumericInterpolation(String startValueString, String endValueString, long increment) {

        long startingAddr = Long.parseLong(startValueString);
        long endingAddr = Long.parseLong(endValueString);

        // Search for possible anchors from the 2nd node to 2nd from last, interpolating between each anchor
        int startIndex = 0; // Index into first interpolation zone of address interpolation way
        for (int i = 1; i < addrInterpolationWay.getNodesCount()-1; i++) {
            Node testNode = addrInterpolationWay.getNode(i);
            String strEndNodeNumber = testNode.get("addr:housenumber");
            // This is a potential anchor node
            if (strEndNodeNumber != null && isLong(strEndNodeNumber)) {
                long anchorAddrNumber = Long.parseLong(strEndNodeNumber);
                if ((anchorAddrNumber > startingAddr) && (anchorAddrNumber < endingAddr)) {
                    // Lies within the expected range
                    interpolateNumericSection(startIndex, i, startingAddr, anchorAddrNumber, increment);

                    // For next interpolation section
                    startingAddr = anchorAddrNumber;
                    startValueString = strEndNodeNumber;
                    startIndex = i;
                }
            }
        }

        // End nodes do not actually contain housenumber value yet (command has not executed), so use user-entered value
        interpolateNumericSection(startIndex, addrInterpolationWay.getNodesCount()-1, startingAddr, endingAddr, increment);
    }

    // Called if user has checked "Convert to House Numbers" checkbox.
    private void convertWayToHousenumbers(String selectedMethod, String startValueString, String endValueString,
                                          String incrementString) {
        // - Use nodes labeled with 'same type' as interim anchors in the middle of the way to identify unequal spacing.
        // - Ignore nodes of different type; for example '25b' is ignored in sequence 5..15

        // Calculate required number of house numbers to create
        if ("alphabetic".equals(selectedMethod)) {

            createAlphaInterpolation(startValueString, endValueString);

        } else {
            long increment = 1;
            if ("odd".equals(selectedMethod) || "even".equals(selectedMethod)) {
                increment = 2;
            } else if ("Numeric".equals(selectedMethod)) {
                increment = Long.parseLong(incrementString);
            }
            createNumericInterpolation(startValueString, endValueString, increment);
        }

        removeAddressInterpolationWay();

    }

    private void removeAddressInterpolationWay() {
        final Command deleteCommand = DeleteCommand.delete(Collections.singleton(addrInterpolationWay), true);
        if (deleteCommand != null) {
            commandGroup.add(deleteCommand);
        }

        addrInterpolationWay = null;
    }

    private boolean validateAndSave() {

        String startValueString = readTextField(startTextField);
        String endValueString = readTextField(endTextField);
        String incrementString = readTextField(incrementTextField);
        String city = readTextField(cityTextField);
        String state = readTextField(stateTextField);
        String postCode = readTextField(postCodeTextField);
        String country = readTextField(countryTextField);
        String fullAddress = readTextField(fullTextField);

        String selectedMethod = getInterpolationMethod();
        if (addrInterpolationWay != null) {
            Long startAddr = 0L, endAddr = 0L;
            if (!"alphabetic".equals(selectedMethod)) {
                Long[] addrArray = {startAddr, endAddr};
                if (!validAddressNumbers(startValueString, endValueString, addrArray)) {
                    return false;
                }
                startAddr = addrArray[0];
                endAddr = addrArray[1];
            }

            String errorMessage = "";
            switch (selectedMethod) {
                case "odd":
                    if (isEven(startAddr) || isEven(endAddr)) {
                        errorMessage = tr("Expected odd numbers for addresses");
                    }
                    break;
                case "even":
                    if (!isEven(startAddr) || !isEven(endAddr)) {
                        errorMessage = tr("Expected even numbers for addresses");
                    }
                    break;
                case "alphabetic":
                    errorMessage = validateAlphaAddress(startValueString, endValueString);
                    break;
                case "Numeric":
                    if (!validNumericIncrementString(incrementString, startAddr, endAddr)) {
                        errorMessage = tr("Expected valid number for increment");
                    }
                    break;
                case "all":
                default:
                    // Fall through
            }
            if (!"".equals(errorMessage)) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), errorMessage, tr("Error"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        if (country != null && country.length() != 2) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Country code must be 2 letters"), tr("Error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Entries are valid ... save in map

        commandGroup = new LinkedList<>();

        String streetName = selectedStreet.get("name");
        DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();

        if (addrInterpolationWay != null) {

            Node firstNode = addrInterpolationWay.getNode(0);
            Node lastNode = addrInterpolationWay.getNode(addrInterpolationWay.getNodesCount()-1);

            // De-select address interpolation way; leave street selected
            currentDataSet.clearSelection(addrInterpolationWay);
            currentDataSet.clearSelection(lastNode);  // Workaround for JOSM Bug #3838

            String interpolationTagValue = selectedMethod;
            if ("Numeric".equals(selectedMethod)) {
                // The interpolation method is the number for 'Numeric' case
                interpolationTagValue = incrementString;
            }

            if (cbConvertToHouseNumbers.getState()) {
                // Convert way to house numbers is checked.
                //  Create individual nodes and delete interpolation way
                convertWayToHousenumbers(selectedMethod, startValueString, endValueString, incrementString);
            } else {
                // Address interpolation way will remain
                commandGroup.add(new ChangePropertyCommand(addrInterpolationWay, "addr:interpolation", interpolationTagValue));
                commandGroup.add(new ChangePropertyCommand(addrInterpolationWay, "addr:inclusion", getInclusionMethod()));
            }

            commandGroup.add(new ChangePropertyCommand(firstNode, "addr:housenumber", startValueString));
            commandGroup.add(new ChangePropertyCommand(lastNode, "addr:housenumber", endValueString));
            // Add address interpolation house number nodes to main house number node list for common processing
            houseNumberNodes.add(firstNode);
            houseNumberNodes.add(lastNode);

        }

        if (streetRelationButton.isSelected()) {

            // Relation button was selected
            if (associatedStreetRelation == null) {
                createRelation(currentDataSet, streetName);
                // relationChanged = true;   (not changed since it was created)
            }
            // Make any additional changes only to the copy
            editedRelation = new Relation(associatedStreetRelation);

            if (addrInterpolationWay != null) {
                addToRelation(associatedStreetRelation, addrInterpolationWay, "house");
            }
        }

        // For all nodes, add to relation and
        //   Add optional text fields to all nodes if specified
        for (Node node : houseNumberNodes) {

            if (streetRelationButton.isSelected()) {
                addToRelation(associatedStreetRelation, node, "house");
            }
            Map<String, String> tags = new HashMap<>();
            if (city != null || streetNameButton.isSelected()) {
                // Include street unconditionally if adding nodes only or city name specified
                tags.put("addr:street", streetName);
            }
            // Set or remove remaining optional fields
            tags.put("addr:city", city);
            tags.put("addr:state", state);
            tags.put("addr:postcode", postCode);
            tags.put("addr:country", country);
            tags.put("addr:full", fullAddress);
            commandGroup.add(new ChangePropertyCommand(currentDataSet, Collections.singleton(node), tags));
        }

        if (relationChanged) {
            commandGroup.add(new ChangeCommand(currentDataSet, associatedStreetRelation, editedRelation));
        }

        if (!commandGroup.isEmpty()) {
            UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Address Interpolation"), commandGroup));
            MainApplication.getLayerManager().getEditLayer().invalidate();
        }

        return true;
    }

    private static boolean validNumericIncrementString(String incrementString, long startingAddr, long endingAddr) {

        if (!isLong(incrementString)) {
            return false;
        }
        long testIncrement = Long.parseLong(incrementString);
        if ((testIncrement <= 0) || (testIncrement > endingAddr)) {
            return false;
        }

        return ((endingAddr - startingAddr) % testIncrement) == 0;
    }

    // Create Associated Street relation, add street, and add to list of commands to perform
    private void createRelation(DataSet currentDataSet, String streetName) {
        associatedStreetRelation = new Relation();
        associatedStreetRelation.put("name", streetName);
        associatedStreetRelation.put("type", "associatedStreet");
        RelationMember newStreetMember = new RelationMember("street", selectedStreet);
        associatedStreetRelation.addMember(newStreetMember);
        commandGroup.add(new AddCommand(currentDataSet, associatedStreetRelation));
    }

    // Read from dialog text box, removing leading and trailing spaces
    // Return the string, or null for a zero length string
    private static String readTextField(JTextField field) {
        String value = field.getText();
        if (value != null) {
            value = value.trim();
            if ("".equals(value)) {
                value = null;
            }
        }
        return value;
    }

    // Test if relation contains specified member
    //   If not already present, it is added
    private void addToRelation(Relation relation, OsmPrimitive testMember, String role) {
        boolean isFound = false;
        for (RelationMember relationMember : relation.getMembers()) {

            if (testMember == relationMember.getMember()) {
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RelationMember newMember = new RelationMember(role, testMember);
            editedRelation.addMember(newMember);

            relationChanged = true;
        }
    }

    // Check alphabetic style address
    //   Last character of both must be alpha
    //   Last character of ending must be greater than starting
    //   Return empty error message if OK
    private static String validateAlphaAddress(String startValueString,
                                        String endValueString) {
        String errorMessage = "";

        if ("".equals(startValueString) || "".equals(endValueString)) {
            errorMessage = tr("Please enter valid number for starting and ending address");
        } else {
            char startingChar = lastChar(startValueString);
            char endingChar = lastChar(endValueString);


            boolean isOk = false;
            if (isNumeric("" + startingChar) && !isNumeric("" + endingChar)) {
                endingChar = Character.toUpperCase(endingChar);
                if ((endingChar >= 'A') && (endingChar <= 'Z')) {
                    // First is a number, last is Latin alpha
                    isOk = true;
                }
            } else if (!isNumeric("" + startingChar) && !isNumeric("" + endingChar)) {
                // Both are alpha
                isOk = true;
            }
            if (!isOk) {
                errorMessage = tr("Alphabetic address must end with a letter");
            }

            // if a number is included, validate that it is the same number
            if (endValueString.length() > 1) {

                // Get number portion of first item: may or may not have letter suffix
                String numStart = baseAlpha(startValueString);
                if (isNumeric(startValueString)) {
                    numStart = startValueString;
                }

                String numEnd = baseAlpha(endValueString);
                if (!numStart.equals(numEnd)) {
                    errorMessage = tr("Starting and ending numbers must be the same for alphabetic addresses");
                }
            }

            // ?? Character collation in all languages ??
            if (startingChar >= endingChar) {
                errorMessage = tr("Starting address letter must be less than ending address letter");
            }
        }

        return errorMessage;
    }

    // Convert string addresses to numeric, with error check
    private static boolean validAddressNumbers(String startValueString,
                                        String endValueString, Long[] addrArray) {
        String errorMessage = "";

        if (!isLong(startValueString)) {
            errorMessage = tr("Please enter valid number for starting address");
        }
        if (!isLong(endValueString)) {
            errorMessage = tr("Please enter valid number for ending address");
        }
        if ("".equals(errorMessage)) {
            addrArray[0] = Long.parseLong(startValueString);
            addrArray[1] = Long.parseLong(endValueString);

            if (addrArray[1] <= addrArray[0]) {
                errorMessage = tr("Starting address number must be less than ending address number");
            }
        }

        if ("".equals(errorMessage)) {
            return true;

        } else {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), errorMessage, tr("Error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String getInterpolationMethod() {
        int selectedIndex = addrInterpolationList.getSelectedIndex();
        return addrInterpolationTags[selectedIndex];
    }

    private String getInclusionMethod() {
        int selectedIndex = addrInclusionList.getSelectedIndex();
        lastAccuracyIndex = selectedIndex;
        return addrInclusionTags[selectedIndex];
    }
}
