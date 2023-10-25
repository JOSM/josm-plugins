// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.terracer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextField;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Utils;

/**
 * The Class HouseNumberInputHandler contains all the logic
 * behind the house number input dialog.
 * <p>
 * From a refactoring viewpoint, this class is indeed more interested in the fields
 * of the HouseNumberInputDialog. This is desired design, as the HouseNumberInputDialog
 * is already cluttered with auto-generated layout code.
 *
 * @author casualwalker - Copyright 2009 CloudMade Ltd
 */
public class HouseNumberInputHandler extends JosmAction implements FocusListener, ItemListener {
    private final TerracerAction terracerAction;
    private final Way outline;
    private final Way street;
    private final String streetName;
    private final Node init;
    private final Relation associatedStreet;
    private final List<Node> housenumbers;
    HouseNumberInputDialog dialog;

    /**
     * Instantiates a new house number input handler.
     *
     * @param terracerAction the terracer action
     * @param outline the closed, quadrilateral way to terrace.
     * @param init The node that hints at which side to start the numbering
     * @param street the street, the buildings belong to (may be null)
     * @param streetName the name of the street, derived from either the street line or
     *            the house numbers which are guaranteed to have the same name
     *            attached (may be null)
     * @param buildingType The value to add for building key
     * @param associatedStreet a relation where we can add the houses (may be null)
     * @param housenumbers a list of house number nodes in this outline (may be empty)
     * @param title the title
     */
    public HouseNumberInputHandler(final TerracerAction terracerAction,
            final Way outline, final Node init, final Way street, final String streetName, final String buildingType,
            final Relation associatedStreet,
            final List<Node> housenumbers, final String title) {
        this.terracerAction = terracerAction;
        this.outline = outline;
        this.init = init;
        this.street = street;
        this.streetName = streetName;
        this.associatedStreet = associatedStreet;
        this.housenumbers = housenumbers;

        this.dialog = new HouseNumberInputDialog(this, street, streetName, buildingType,
                associatedStreet != null, housenumbers);
    }

    /**
     * Find a button with a certain caption.
     * Loops recursively through all objects to find all buttons.
     * Function returns on the first match.
     *
     * @param root A container object that is recursively searched for other containers or buttons
     * @param caption The caption of the button that is being searched
     *
     * @return The first button that matches the caption or null if not found
     */
    private static JButton getButton(Container root, String caption) {
        Component[] children = root.getComponents();
        for (Component child : children) {
            JButton b;
            if (child instanceof JButton) {
                b = (JButton) child;
                if (caption.equals(b.getText())) return b;
            } else if (child instanceof Container) {
                b = getButton((Container) child, caption);
                if (b != null) return b;
            }
        }
        return null;
    }

    /**
     * Validate the current input fields.
     * When the validation fails, a red message is
     * displayed and the OK button is disabled.
     * <p>
     * Should be triggered each time the input changes.
     * @return {@code true} if the inputs are ok
     */
    private boolean validateInput() {
        boolean isOk = true;
        final StringBuilder message = new StringBuilder();

        isOk &= checkNumberOrder(message);
        isOk &= checkSegmentsFromHousenumber(message);
        isOk &= checkSegments(message);

        // Allow non numeric characters for the low number as long as there is
        // no high number of the segmentcount is 1
        if (dialog.hi.getText().length() > 0 && (segments() != null || segments() < 1)) {
            isOk &= checkNumberStringField(dialog.lo, tr("Lowest number"),
                            message);
        }
        isOk &= checkNumberStringField(dialog.hi, tr("Highest number"),
                        message);
        isOk &= checkNumberStringField(dialog.segments, tr("Segments"),
                        message);

        JButton okButton = getButton(dialog, "OK");
        if (okButton != null)
            okButton.setEnabled(isOk);
        if (isOk) {

            // For some reason the messageLabel doesn't want to show up
            dialog.messageLabel.setForeground(Color.black);
            dialog.messageLabel.setText(tr(HouseNumberInputDialog.DEFAULT_MESSAGE));
            return true;
        } else {
            // For some reason the messageLabel doesn't want to show up, so a
            // MessageDialog is shown instead. Someone more knowledgeable might fix this.
            dialog.messageLabel.setForeground(Color.red);
            dialog.messageLabel.setText(message.toString());
            // JOptionPane.showMessageDialog(null, message.toString(),
            // tr("Error"), JOptionPane.ERROR_MESSAGE);

            return false;
        }
    }

    /**
     * Checks, if the lowest house number is indeed lower than the
     * highest house number.
     * This check applies only, if the house number fields are used at all.
     *
     * @param message the message
     *
     * @return true, if successful
     */
    private boolean checkNumberOrder(final StringBuilder message) {
        if (numberFrom() != null && numberTo() != null && numberFrom() > numberTo()) {
            appendMessageNewLine(message);
            message.append(tr("Lowest housenumber cannot be higher than highest housenumber"));
            return false;
        }
        return true;
    }

    /**
     * Obtain the number segments from the house number fields and check,
     * if they are valid.
     * <p>
     * Also disables the segments field, if the house numbers contain
     * valid information.
     *
     * @param message the message
     *
     * @return true, if successful
     */
    private boolean checkSegmentsFromHousenumber(final StringBuilder message) {
        if (!dialog.numbers.isVisible()) {
            dialog.segments.setEditable(true);

            if (numberFrom() != null && numberTo() != null) {
                int segments = numberTo() - numberFrom();

                if (segments % stepSize() != 0) {
                    appendMessageNewLine(message);
                    message
                            .append(tr("Housenumbers do not match odd/even setting"));
                    return false;
                }

                int steps = segments / stepSize();
                steps++; // difference 0 means 1 building, see
                // TerracerActon.terraceBuilding
                dialog.segments.setText(String.valueOf(steps));
                dialog.segments.setEditable(false);
            }
        }
        return true;
    }

    /**
     * Check the number of segments.
     * It must be a number and greater than 1.
     *
     * @param message the message
     *
     * @return true, if successful
     */
    private boolean checkSegments(final StringBuilder message) {
        if (segments() == null || segments() < 1) {
            appendMessageNewLine(message);
            message.append(tr("Segment must be a number greater 1"));
            return false;

        }
        return true;
    }

    /**
     * Check, if a string field contains a positive integer.
     *
     * @param field the field
     * @param label the label
     * @param message the message
     *
     * @return true, if successful
     */
    private static boolean checkNumberStringField(final JTextField field,
            final String label, final StringBuilder message) {
        final String content = field.getText();
        if (content != null && content.length() != 0) {
            try {
                int i = Integer.parseInt(content);
                if (i < 0) {
                    appendMessageNewLine(message);
                    message.append(tr("{0} must be greater than 0", label));
                    return false;
                }
            } catch (NumberFormatException e) {
                appendMessageNewLine(message);
                message.append(tr("{0} is not a number", label));
                return false;
            }

        }
        return true;
    }

    /**
     * Append a new line to the message, if the message is not empty.
     *
     * @param message the message
     */
    private static void appendMessageNewLine(final StringBuilder message) {
        if (message.length() > 0) {
            message.append("\n");
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        validateInput();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        // OK or Cancel button-actions
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            if (tr("OK").equals(button.getActionCommand()) && button.isEnabled()) {
                if (validateInput()) {
                    saveValues();

                    terracerAction.terraceBuilding(
                        outline,
                        init,
                        street,
                        associatedStreet,
                        segments(),
                        dialog.lo.getText(),
                        dialog.hi.getText(),
                        stepSize(),
                        housenumbers,
                        streetName(),
                        doHandleRelation(),
                        doKeepOutline(), buildingType());

                    this.dialog.setVisible(false);
                }
            } else if (tr("Cancel").equals(button.getActionCommand())) {
                this.dialog.setVisible(false);
            }
        } else {
            // Anything else is a change in the input (we don't get here though)
            validateInput();
        }
    }

    /**
     * Calculate the step size between two house numbers,
     * based on the interpolation setting.
     *
     * @return the stepSize (1 for all, 2 for odd /even)
     */
    public Integer stepSize() {
        return tr("All").equals(dialog.interpolationType.getSelectedItem()) ? 1 : 2;
    }

    /**
     * Gets the number of segments, if set.
     *
     * @return the number of segments or null, if not set / invalid.
     */
    public Integer segments() {
        try {
            return Integer.parseInt(dialog.segments.getText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Gets the lowest house number.
     *
     * @return the number of lowest house number or null, if not set / invalid.
     */
    public Integer numberFrom() {
        try {
            return Integer.parseInt(dialog.lo.getText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Gets the highest house number.
     *
     * @return the number of highest house number or null, if not set / invalid.
     */
    public Integer numberTo() {
        try {
            return Integer.parseInt(dialog.hi.getText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Gets the street name.
     *
     * @return the street name or null, if not set / invalid.
     */
    public String streetName() {
        if (streetName != null)
            return streetName;

        return getItemText(dialog.streetComboBox);
    }

    /**
     * Gets the building type.
     *
     * @return the building type or null, if not set / invalid.
     */
    public String buildingType() {
        return getItemText(dialog.buildingComboBox);
    }

    private static String getItemText(AutoCompComboBox<?> box) {
        Object selected = box.getSelectedItem();
        if (selected == null) {
            return null;
        } else {
            String name;
            if (selected instanceof AutoCompletionItem) {
               name = ((AutoCompletionItem) selected).getValue();
            } else {
               name = selected.toString();
            }

            return Utils.isEmpty(name) ? null : name;
        }
    }

    /**
     * Whether the user likes to create a relation or add to
     * an existing one.
     * @return {@code true} if the user wants to create a relation
     */
    public boolean doHandleRelation() {
        return this.dialog.handleRelationCheckBox.isSelected();
    }

    /**
     * Whether the user likes to keep the outline way.
     * @return {@code true} if the user wants to keep the selected outline
     */
    public boolean doKeepOutline() {
        return dialog.keepOutlineCheckBox.isSelected();
    }

    @Override
    public void focusGained(FocusEvent e) {
        // Empty, but placeholder is required
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (e.getOppositeComponent() == null)
            return;

        validateInput();
    }

    /**
     * Saves settings.
     */
    public void saveValues() {
        Config.getPref().putBoolean(HouseNumberInputDialog.HANDLE_RELATION, doHandleRelation());
        Config.getPref().putBoolean(HouseNumberInputDialog.KEEP_OUTLINE, doKeepOutline());
        Config.getPref().put(HouseNumberInputDialog.INTERPOLATION, stepSize().toString());
    }
}
