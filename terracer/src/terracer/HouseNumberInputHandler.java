/**
 * Terracer: A JOSM Plugin for terraced houses.
 *
 * Copyright 2009 CloudMade Ltd.
 *
 * Released under the GPLv2, see LICENSE file for details.
 */
package terracer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.actions.JosmAction;

/**
 * The Class HouseNumberInputHandler contains all the logic
 * behind the house number input dialog.
 *
 * From a refactoring viewpoint, this class is indeed more interested in the fields
 * of the HouseNumberInputDialog. This is desired design, as the HouseNumberInputDialog
 * is already cluttered with auto-generated layout code.
 *
 * @author casualwalker
 */
 public class HouseNumberInputHandler extends JosmAction implements ActionListener, FocusListener, ItemListener {
    private TerracerAction terracerAction;
    private Way outline, street;
    private Relation associatedStreet;
    public HouseNumberInputDialog dialog;

    /**
     * Instantiates a new house number input handler.
     *
     * @param terracerAction the terracer action
     * @param outline the closed, quadrilateral way to terrace.
     * @param street the street, the buildings belong to (may be null)
     * @param associatedStreet a relation where we can add the houses (may be null)
     * @param title the title
     */
    public HouseNumberInputHandler(final TerracerAction terracerAction,
            final Way outline, final Way street, final Relation associatedStreet,
            final String title) {
        this.terracerAction = terracerAction;
        this.outline = outline;
        this.street = street;
        this.associatedStreet = associatedStreet;

        // This dialog is started modal
        this.dialog = new HouseNumberInputDialog(this, street, associatedStreet != null);

        // We're done
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
        Component children[] = root.getComponents();
         for (Component child : children) {
            JButton b;
            if (child instanceof JButton) {
                b = (JButton) child;
                if (caption.equals(b.getText())) return b;
            } else if (child instanceof Container) {
                  b = getButton((Container)child, caption);
                  if (b != null) return b;
             }
         }
        return null;
    }

    /**
     * Validate the current input fields.
     * When the validation fails, a red message is
     * displayed and the OK button is disabled.
     *
     * Should be triggered each time the input changes.
     */
    private boolean validateInput() {
        boolean isOk = true;
        StringBuffer message = new StringBuffer();

        isOk = isOk && checkNumberOrder(message);
        isOk = isOk && checkSegmentsFromHousenumber(message);
        isOk = isOk && checkSegments(message);

        // Allow non numeric characters for the low number as long as there is no high number of the segmentcount is 1
        if (dialog.hi.getText().length() > 0 | segments() > 1) {
            isOk = isOk
                    && checkNumberStringField(dialog.lo, tr("Lowest number"),
                            message);
        }
        isOk = isOk
                && checkNumberStringField(dialog.hi, tr("Highest number"),
                        message);
        isOk = isOk
                && checkNumberStringField(dialog.segments, tr("Segments"),
                        message);

        if (isOk) {
            JButton okButton = getButton(dialog, "OK");
            if (okButton != null)
                okButton.setEnabled(true);

            // For some reason the messageLabel doesn't want to show up
            dialog.messageLabel.setForeground(Color.black);
            dialog.messageLabel.setText(tr(HouseNumberInputDialog.DEFAULT_MESSAGE));
            return true;
        } else {
            JButton okButton = getButton(dialog, "OK");
            if (okButton != null)
                okButton.setEnabled(false);

            // For some reason the messageLabel doesn't want to show up, so a MessageDialog is shown instead. Someone more knowledgeable might fix this.
            dialog.messageLabel.setForeground(Color.red);
            dialog.messageLabel.setText(message.toString());
            //JOptionPane.showMessageDialog(null, message.toString(), tr("Error"), JOptionPane.ERROR_MESSAGE);

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
    private boolean checkNumberOrder(final StringBuffer message) {
        if (numberFrom() != null && numberTo() != null) {
            if (numberFrom().intValue() > numberTo().intValue()) {
                appendMessageNewLine(message);
                message.append(tr("Lowest housenumber cannot be higher than highest housenumber"));
                return false;
            }
        }
        return true;
    }

    /**
     * Obtain the number segments from the house number fields and check,
     * if they are valid.
     *
     * Also disables the segments field, if the house numbers contain
     * valid information.
     *
     * @param message the message
     *
     * @return true, if successful
     */
    private boolean checkSegmentsFromHousenumber(final StringBuffer message) {
        dialog.segments.setEditable(true);

        if (numberFrom() != null && numberTo() != null) {

            int segments = numberTo().intValue() - numberFrom().intValue();

            if (segments % stepSize() != 0) {
                appendMessageNewLine(message);
                message.append(tr("Housenumbers do not match odd/even setting"));
                return false;
            }

            int steps = segments / stepSize();
            steps++; // difference 0 means 1 building, see
            // TerracerActon.terraceBuilding
            dialog.segments.setText(String.valueOf(steps));
            dialog.segments.setEditable(false);

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
    private boolean checkSegments(final StringBuffer message) {
        if (segments() == null || segments().intValue() < 1) {
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
    private boolean checkNumberStringField(final JTextField field,
            final String label, final StringBuffer message) {
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
    private void appendMessageNewLine(final StringBuffer message) {
        if (message.length() > 0) {
            message.append("\n");
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     * Called when the user selects from a pulldown selection
     */
    public void itemStateChanged(ItemEvent e) {
        validateInput();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent e) {
        // OK or Cancel button-actions
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            if ("OK".equals(button.getActionCommand()) & button.isEnabled()) {
                if (validateInput()) {
                    saveValues();

                    terracerAction.terraceBuilding(
                        outline,
                        street,
                        associatedStreet,
                        segments(),
                        dialog.lo.getText(),
                        dialog.hi.getText(),
                        stepSize(),
                        streetName(),
                        doHandleRelation(),
                        doDeleteOutline());

                    this.dialog.dispose();
                }
            } else if ("Cancel".equals(button.getActionCommand())) {
                this.dialog.dispose();
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
        return (dialog.interpolation.getSelectedItem().equals(tr("All"))) ? 1
                : 2;
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
     * @return the  street name or null, if not set / invalid.
     */
    public String streetName() {
        if (street != null)
            return null;

        Object selected = dialog.streetComboBox.getSelectedItem();
        if (selected == null) {
            return null;
        } else {
            String name = selected.toString();
            if (name.length() == 0) {
                return null;
            } else {
                return name;
            }
        }
    }

    /**
     * Whether the user likes to create a relation or add to
     * an existing one.
     */
    public boolean doHandleRelation() {
        if (this.dialog == null) {
            JOptionPane.showMessageDialog(null, "dialog", "alert", JOptionPane.ERROR_MESSAGE);
        }
        if (this.dialog.handleRelationCheckBox == null) {
            JOptionPane.showMessageDialog(null, "checkbox", "alert", JOptionPane.ERROR_MESSAGE);
            return true;
        }  else {
            return this.dialog.handleRelationCheckBox.isSelected();
        }
    }


    /**
     * Whether the user likes to delete the outline way.
     */
    public boolean doDeleteOutline() {
        return dialog.deleteOutlineCheckBox.isSelected();
    }

    /* (non-Javadoc)
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
        // Empty, but placeholder is required
    }

    /* (non-Javadoc)
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
        if (e.getOppositeComponent() == null)
            return;

        validateInput();
    }

    /**
     * Saves settings.
     */
    public void saveValues() {
        Main.pref.put(HouseNumberInputDialog.HANDLE_RELATION, doHandleRelation());
        Main.pref.put(HouseNumberInputDialog.DELETE_OUTLINE, doDeleteOutline());
        Main.pref.put(HouseNumberInputDialog.INTERPOLATION, stepSize().toString());
    }
}
