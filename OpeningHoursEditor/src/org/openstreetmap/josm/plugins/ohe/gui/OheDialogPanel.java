// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ohe.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.plugins.ohe.ClockSystem;
import org.openstreetmap.josm.plugins.ohe.OpeningTimeUtils;
import org.openstreetmap.josm.plugins.ohe.parser.OpeningTimeCompiler;
import org.openstreetmap.josm.plugins.ohe.parser.ParseException;
import org.openstreetmap.josm.plugins.ohe.parser.SyntaxException;
import org.openstreetmap.josm.plugins.ohe.parser.TokenMgrError;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/** The dialog panel for editing opening hourse */
public class OheDialogPanel extends JPanel {

    /** The key we are modifying */
    private final JTextField keyField;

    /** The Component for showing the Time as a Text */
    private final JTextField valueField;

    /** The position of the mouse pointer*/
    private final JLabel actualPositionLabel;

    /** The important Panel for showing/editing the Time graphical */
    private final OheEditor editorPanel;

    /** The original key for the initial timespan */
    private final String oldKey;

    /** The ClockSystem that the user wants us to use */
    private final ClockSystem clockSystem;

    /**
     * The Panel for editing the time-values.
     *
     * @param key The key to edit
     * @param valuesToEdit
     *            can be a String or a Map&lt;String, Integer&gt; which contains
     *            multiple values and their number of occurences
     * @param clockSystem The clocksystem to use
     */
    public OheDialogPanel(String key, Object valuesToEdit, ClockSystem clockSystem) {
        this.clockSystem = clockSystem;

        oldKey = key;
        keyField = new JTextField(key);

        String value = "";
        if (valuesToEdit instanceof String)
            value = (String) valuesToEdit;
        else if (valuesToEdit instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> valuesMap = (Map<String, Integer>) valuesToEdit;
            if (valuesMap.size() == 1)
                value = valuesMap.keySet().iterator().next();
            else if (valuesMap.size() > 1) {
                // TODO let the user choose which value he wants to edit (e.g. with a combobox)
                int mostOccurences = 0;
                for (Map.Entry<String, Integer> entry : valuesMap.entrySet()) {
                    if (entry.getValue() > mostOccurences) {
                        value = entry.getKey();
                        mostOccurences = entry.getValue();
                    }
                }
            }
        }
        valueField = new JTextField(value);

        JButton twentyfourSevenButton = new JButton(tr("apply {0}", "24/7"));

        actualPositionLabel = new JLabel("-");
        JPanel toolsPanel = new JPanel(new GridBagLayout());
        toolsPanel.add(twentyfourSevenButton, GBC.std());
        toolsPanel.add(Box.createGlue(), GBC.std().fill(GridBagConstraints.HORIZONTAL));
        toolsPanel.add(actualPositionLabel, GBC.eop());
        
        editorPanel = new OheEditor(this);

        // on every action in the textfield the timeRects are reloaded
        valueField.addActionListener(evt -> editorPanel.initTimeRects());

        twentyfourSevenButton.addActionListener(arg0 -> {
            valueField.setText("24/7");
            editorPanel.initTimeRects();
        });

        // adding all Components in a Gridbaglayout
        setLayout(new GridBagLayout());
        add(new JLabel(tr("Key")), GBC.std());
        add(Box.createHorizontalStrut(10), GBC.std());
        add(keyField, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        add(new JLabel(tr("Value")), GBC.std());
        add(Box.createHorizontalStrut(10), GBC.std());
        add(valueField, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        add(toolsPanel, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        add(editorPanel, GBC.eol().fill());

        setPreferredSize(new Dimension(480, 520));
    }

    public String[] getChangedKeyValuePair() {
        return new String[] {oldKey, keyField.getText(), valueField.getText()};
    }

    /**
     * Returns the compiled Time from the valueField.
     * @return the compiled Time from the valueField
     * @throws OheException if the time value could not be parsed
     */
    public List<int[]> getTime() throws OheException {
        String value = valueField.getText();
        List<int[]> time = null;
        if (value.length() > 0) {
            OpeningTimeCompiler compiler = new OpeningTimeCompiler(value);
            try {
                time = OpeningTimeUtils.convert(compiler.startCompile());
            } catch (ParseException | SyntaxException | TokenMgrError t) {
                Logging.warn(t);
                
                int[] tColumns = null;
                String info = t.getMessage();

                if (t instanceof ParseException) {
                    ParseException parserExc = (ParseException) t;
                    tColumns = new int[] {parserExc.currentToken.beginColumn - 1, parserExc.currentToken.endColumn + 1};
                } else if (t instanceof SyntaxException) {
                    SyntaxException syntaxError = (SyntaxException) t;
                    tColumns = new int[] {syntaxError.getStartColumn(), syntaxError.getEndColumn()};
                } else if (t instanceof TokenMgrError) {
                    try {
                        // With JavaCC 6 Message is: "Lexical error at line 1, column 20.  Encountered: "P" (80), after : ""
                        int idx = info.indexOf("column ");
                        if (idx > -1) {
                            int col = Integer.parseInt(info.substring(idx+"column ".length(), info.indexOf('.', idx)));
                            tColumns = new int[] {col - 1, col + 1};
                        }
                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
                        Logging.warn(e);
                    }
                }

                // shows a Information Dialog, where the Error occurred
                if (tColumns != null || info != null) {
                    String message = "<html>";
                    if (tColumns != null) {
                        int first = Math.max(0, tColumns[0]);
                        int last = Math.min(value.length(), tColumns[1]);
                        String begin = value.substring(0, first);
                        String middle = value.substring(first, last);
                        String end = value.substring(last);
                        valueField.setCaretPosition(first);
                        // TODO focus on the valueField
                        message += tr("There is something wrong in the value near:") + "<br>" + begin
                                + "<span style='background-color:red;'>" + middle + "</span>" + end;
                    }
                    if (info != null) {
                        if (tColumns != null) {
                            message += "<br>";
                        }
                        message += tr("Info: {0}", tr(info));
                    }
                    message += "<br>" + tr("Correct the value manually and than press Enter.") + "</html>";
                    JOptionPane.showMessageDialog(this, message, tr("Error in timeformat"), JOptionPane.INFORMATION_MESSAGE);
                }

                throw new OheException("Error in the TimeValue", t);
            }
        }

        return time;
    }

    /**
     * Updates the valueField with the given {@link TimeRect}s.
     * @param timeRects The time rectangles to set the value from
     */
    void updateValueField(List<TimeRect> timeRects) {
        if (valueField != null && timeRects != null)
            valueField.setText(OpeningTimeUtils.makeStringFromRects(timeRects));
    }

    /**
     * Set the position text for the mouse
     * @param positionText The text to use
     */
    void setMousePositionText(String positionText) {
        actualPositionLabel.setText(positionText);
    }

    /**
     * Returns the clock system (12 or 24 hours).
     * @return the clock system
     */
    public ClockSystem getHourMode() {
        return clockSystem;
    }
}
