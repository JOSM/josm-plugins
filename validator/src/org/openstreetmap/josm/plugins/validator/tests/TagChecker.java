package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.plugins.validator.OSMValidatorPlugin;
import org.openstreetmap.josm.plugins.validator.PreferenceEditor;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;
import org.openstreetmap.josm.plugins.validator.util.Bag;
import org.openstreetmap.josm.plugins.validator.util.Entities;
import org.openstreetmap.josm.plugins.validator.util.Util;
import org.openstreetmap.josm.tools.GBC;

/**
 * Check for mispelled or wrong properties
 *
 * @author frsantos
 */
public class TagChecker extends Test
{
    /** The default data files */
    public static final String DATA_FILE = "http://svn.openstreetmap.org/applications/editors/josm/plugins/validator/tagchecker.cfg";
    public static final String IGNORE_FILE = "http://svn.openstreetmap.org/applications/editors/josm/plugins/validator/ignoretags.cfg";
    public static final String SPELL_FILE = "http://svn.openstreetmap.org/applications/utils/planet.osm/java/speller/words.cfg";

    /** The spell check key substitutions: the key should be substituted by the value */
    protected static Map<String, String> spellCheckKeyData;
    /** The spell check preset values */
    protected static Bag<String, String> presetsValueData;
    /** The TagChecker data */
    protected static List<CheckerData> checkerData = new ArrayList<CheckerData>();
    protected static ArrayList<String> ignoreDataStartsWith = new ArrayList<String>();
    protected static ArrayList<String> ignoreDataEquals = new ArrayList<String>();
    protected static ArrayList<String> ignoreDataEndsWith = new ArrayList<String>();
    protected static ArrayList<IgnoreKeyPair> ignoreDataKeyPair = new ArrayList<IgnoreKeyPair>();
    protected static ArrayList<IgnoreTwoKeyPair> ignoreDataTwoKeyPair = new ArrayList<IgnoreTwoKeyPair>();

    /** The preferences prefix */
    protected static final String PREFIX = PreferenceEditor.PREFIX + "." + TagChecker.class.getSimpleName();

    public static final String PREF_CHECK_VALUES = PREFIX + ".checkValues";
    public static final String PREF_CHECK_KEYS = PREFIX + ".checkKeys";
    public static final String PREF_CHECK_COMPLEX = PREFIX + ".checkComplex";
    public static final String PREF_CHECK_FIXMES = PREFIX + ".checkFixmes";
    public static final String PREF_CHECK_PAINT = PREFIX + ".paint";

    public static final String PREF_SOURCES = PREFIX + ".sources";
    public static final String PREF_USE_DATA_FILE = PREFIX + ".usedatafile";
    public static final String PREF_USE_IGNORE_FILE = PREFIX + ".useignorefile";
    public static final String PREF_USE_SPELL_FILE = PREFIX + ".usespellfile";

    public static final String PREF_CHECK_KEYS_BEFORE_UPLOAD = PREF_CHECK_KEYS + "BeforeUpload";
    public static final String PREF_CHECK_VALUES_BEFORE_UPLOAD = PREF_CHECK_VALUES + "BeforeUpload";
    public static final String PREF_CHECK_COMPLEX_BEFORE_UPLOAD = PREF_CHECK_COMPLEX + "BeforeUpload";
    public static final String PREF_CHECK_FIXMES_BEFORE_UPLOAD = PREF_CHECK_FIXMES + "BeforeUpload";
    public static final String PREF_CHECK_PAINT_BEFORE_UPLOAD = PREF_CHECK_PAINT + "BeforeUpload";

    protected boolean checkKeys = false;
    protected boolean checkValues = false;
    protected boolean checkComplex = false;
    protected boolean checkFixmes = false;
    protected boolean checkPaint = false;

    protected JCheckBox prefCheckKeys;
    protected JCheckBox prefCheckValues;
    protected JCheckBox prefCheckComplex;
    protected JCheckBox prefCheckFixmes;
    protected JCheckBox prefCheckPaint;

    protected JCheckBox prefCheckKeysBeforeUpload;
    protected JCheckBox prefCheckValuesBeforeUpload;
    protected JCheckBox prefCheckComplexBeforeUpload;
    protected JCheckBox prefCheckFixmesBeforeUpload;
    protected JCheckBox prefCheckPaintBeforeUpload;

    protected JCheckBox prefUseDataFile;
    protected JCheckBox prefUseIgnoreFile;
    protected JCheckBox prefUseSpellFile;

    protected JButton addSrcButton;
    protected JButton editSrcButton;
    protected JButton deleteSrcButton;

    protected static int EMPTY_VALUES      = 1200;
    protected static int INVALID_KEY       = 1201;
    protected static int INVALID_VALUE     = 1202;
    protected static int FIXME             = 1203;
    protected static int INVALID_SPACE     = 1204;
    protected static int INVALID_KEY_SPACE = 1205;
    protected static int INVALID_HTML      = 1206;
    protected static int PAINT             = 1207;
    /** 1250 and up is used by tagcheck */

    /** List of sources for spellcheck data */
    protected JList Sources;


    protected static Entities entities = new Entities();
    /**
     * Constructor
     */
    public TagChecker()
    {
        super(tr("Properties checker :"),
              tr("This plugin checks for errors in property keys and values."));
    }

    public static void initialize(OSMValidatorPlugin plugin) throws Exception
    {
        initializeData();
        initializePresets();
    }

    /**
     * Reads the spellcheck file into a HashMap.
     * The data file is a list of words, beginning with +/-. If it starts with +,
     * the word is valid, but if it starts with -, the word should be replaced
     * by the nearest + word before this.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void initializeData() throws IOException
    {
        spellCheckKeyData = new HashMap<String, String>();
        String sources = Main.pref.get( PREF_SOURCES, "");
        if(Main.pref.getBoolean(PREF_USE_DATA_FILE, true))
        {
            if( sources == null || sources.length() == 0)
                sources = DATA_FILE;
            else
                sources = DATA_FILE + ";" + sources;
        }
        if(Main.pref.getBoolean(PREF_USE_IGNORE_FILE, true))
        {
            if( sources == null || sources.length() == 0)
                sources = IGNORE_FILE;
            else
                sources = IGNORE_FILE + ";" + sources;
        }
        if(Main.pref.getBoolean(PREF_USE_SPELL_FILE, true))
        {
            if( sources == null || sources.length() == 0)
                sources = SPELL_FILE;
            else
                sources = SPELL_FILE + ";" + sources;
        }

        String errorSources = "";
        if(sources.length() == 0)
            return;
        for(String source: sources.split(";"))
        {
            try
            {
                MirroredInputStream s = new MirroredInputStream(source, Util.getPluginDir(), -1);
                InputStreamReader r;
                try
                {
                    r = new InputStreamReader(s, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    r = new InputStreamReader(s);
                }
                BufferedReader reader = new BufferedReader(r);

                String okValue = null;
                Boolean tagcheckerfile = false;
                Boolean ignorefile = false;
                String line;
                while((line = reader.readLine()) != null && (tagcheckerfile || line.length() != 0))
                {
                    if(line.startsWith("#"))
                    {
                        if(line.startsWith("# JOSM TagChecker"))
                            tagcheckerfile = true;
                        if(line.startsWith("# JOSM IgnoreTags"))
                            ignorefile = true;
                        continue;
                    }
                    else if(ignorefile)
                    {
                        line = line.trim();
                        if(line.length() < 4)
                            continue;

                        String key = line.substring(0, 2);
                        line = line.substring(2);

                        if(key.equals("S:"))
                        {
                            ignoreDataStartsWith.add(line);
                        }
                        else if(key.equals("E:"))
                        {
                            ignoreDataEquals.add(line);
                        }
                        else if(key.equals("F:"))
                        {
                            ignoreDataEndsWith.add(line);
                        }
                        else if(key.equals("K:"))
                        {
                            IgnoreKeyPair tmp = new IgnoreKeyPair();
                            int mid = line.indexOf("=");
                            tmp.key = line.substring(0, mid);
                            tmp.value = line.substring(mid+1);
                            ignoreDataKeyPair.add(tmp);
                        }
                        else if(key.equals("T:"))
                        {
                            IgnoreTwoKeyPair tmp = new IgnoreTwoKeyPair();
                            int mid = line.indexOf("=");
                            int split = line.indexOf("|");
                            tmp.key1 = line.substring(0, mid);
                            tmp.value1 = line.substring(mid+1, split);
                            line = line.substring(split+1);
                            mid = line.indexOf("=");
                            tmp.key2 = line.substring(0, mid);
                            tmp.value2 = line.substring(mid+1);
                            ignoreDataTwoKeyPair.add(tmp);
                        }
                        continue;
                    }
                    else if(tagcheckerfile)
                    {
                        if(line.length() > 0)
                        {
                            CheckerData d = new CheckerData();
                            String err = d.getData(line);

                            if(err == null)
                                checkerData.add(d);
                            else
                                System.err.println(tr("Invalid tagchecker line - {0}: {1}", err, line));
                        }
                    }
                    else if(line.charAt(0) == '+')
                    {
                        okValue = line.substring(1);
                    }
                    else if(line.charAt(0) == '-' && okValue != null)
                    {
                        spellCheckKeyData.put(line.substring(1), okValue);
                    }
                    else
                    {
                        System.err.println(tr("Invalid spellcheck line: {0}", line));
                    }
                }
            }
            catch (IOException e)
            {
                errorSources += source + "\n";
            }
        }

        if( errorSources.length() > 0 )
            throw new IOException( tr("Could not access data file(s):\n{0}", errorSources) );
    }

    /**
     * Reads the presets data.
     *
     * @throws Exception
     */
    public static void initializePresets() throws Exception
    {
        if( !Main.pref.getBoolean(PREF_CHECK_VALUES, true) )
            return;

        Collection<TaggingPreset> presets = TaggingPresetPreference.taggingPresets;
        if(presets != null)
        {
            presetsValueData = new Bag<String, String>();
            for(String a : OsmPrimitive.getUninterestingKeys())
                presetsValueData.add(a);
            for(String a : OsmPrimitive.getDirectionKeys())
                presetsValueData.add(a);
            for(String a : Main.pref.getCollection(PreferenceEditor.PREFIX + ".knownkeys",
            Arrays.asList(new String[]{"is_in", "int_ref", "fixme", "population"})))
                presetsValueData.add(a);
            for(TaggingPreset p : presets)
            {
                for(TaggingPreset.Item i : p.data)
                {
                    if(i instanceof TaggingPreset.Combo)
                    {
                        TaggingPreset.Combo combo = (TaggingPreset.Combo) i;
                        for(String value : combo.values.split(","))
                            presetsValueData.add(combo.key, value);
                    }
                    else if(i instanceof TaggingPreset.Key)
                    {
                        TaggingPreset.Key k = (TaggingPreset.Key) i;
                        presetsValueData.add(k.key, k.value);
                    }
                    else if(i instanceof TaggingPreset.Text)
                    {
                        TaggingPreset.Text k = (TaggingPreset.Text) i;
                        presetsValueData.add(k.key);
                    }
                    else if(i instanceof TaggingPreset.Check)
                    {
                        TaggingPreset.Check k = (TaggingPreset.Check) i;
                        presetsValueData.add(k.key, "yes");
                        presetsValueData.add(k.key, "no");
                    }
                }
            }
        }
    }

    @Override
    public void visit(Node n)
    {
        checkPrimitive(n);
    }


    @Override
    public void visit(Relation n)
    {
        checkPrimitive(n);
    }


    @Override
    public void visit(Way w)
    {
        checkPrimitive(w);
    }

    /**
     * Checks the primitive properties
     * @param p The primitive to check
     */
    private void checkPrimitive(OsmPrimitive p)
    {
        // Just a collection to know if a primitive has been already marked with error
        Bag<OsmPrimitive, String> withErrors = new Bag<OsmPrimitive, String>();

        if(checkComplex)
        {
            Map<String, String> props = (p.getKeys() == null) ? Collections.<String, String>emptyMap() : p.getKeys();
            for(Entry<String, String> prop: props.entrySet() )
            {
                boolean ignore = true;
                String key1 = prop.getKey();
                String value1 = prop.getValue();

                for(IgnoreTwoKeyPair a : ignoreDataTwoKeyPair)
                {
                    if(key1.equals(a.key1) && value1.equals(a.value1))
                    {
                        ignore = false;
                        for(Entry<String, String> prop2: props.entrySet() )
                        {
                            String key2 = prop2.getKey();
                            String value2 = prop2.getValue();
                            for(IgnoreTwoKeyPair b : ignoreDataTwoKeyPair)
                            {
                                if(key2.equals(b.key2) && value2.equals(b.value2))
                                {
                                    ignore = true;
                                    break;
                                }
                            }
                            if(ignore)
                                break;
                        }
                    }
                    if(ignore)
                        break;
                }

                if(!ignore)
                {
                    errors.add( new TestError(this, Severity.ERROR, tr("Illegal tag/value combinations"),
                    tr("Illegal tag/value combinations"), tr("Illegal tag/value combinations"), 1272, p) );
                    withErrors.add(p, "TC");
                }
            }

            for(CheckerData d : checkerData)
            {
                if(d.match(p))
                {
                    errors.add( new TestError(this, d.getSeverity(), tr("Illegal tag/value combinations"),
                    d.getDescription(), d.getDescriptionOrig(), d.getCode(), p) );
                    withErrors.add(p, "TC");
                }
            }
        }
        if(checkPaint)
        {
            List<String> pe = p.getDataSet().getErrors(p);
            if(pe != null)
            {
                for(String s: pe)
                {
                    /* passing translated text also to original string, as we already
                    translated the stuff before. Makes the ignore file language dependend. */
                    errors.add( new TestError(this, Severity.WARNING, tr("Painting problem"),
                    s, s, PAINT, p) );
                    withErrors.add(p, "P");
                }
            }
        }

        Map<String, String> props = (p.getKeys() == null) ? Collections.<String, String>emptyMap() : p.getKeys();
        for(Entry<String, String> prop: props.entrySet() )
        {
            String s = marktr("Key ''{0}'' invalid.");
            String key = prop.getKey();
            String value = prop.getValue();
            if( checkValues && (value==null || value.trim().length() == 0) && !withErrors.contains(p, "EV"))
            {
                errors.add( new TestError(this, Severity.WARNING, tr("Tags with empty values"),
                tr(s, key), MessageFormat.format(s, key), EMPTY_VALUES, p) );
                withErrors.add(p, "EV");
            }
            if( checkKeys && spellCheckKeyData.containsKey(key) && !withErrors.contains(p, "IPK"))
            {
                errors.add( new TestError(this, Severity.WARNING, tr("Invalid property key"),
                tr(s, key), MessageFormat.format(s, key), INVALID_KEY, p) );
                withErrors.add(p, "IPK");
            }
            if( checkKeys && key.indexOf(" ") >= 0 && !withErrors.contains(p, "IPK"))
            {
                errors.add( new TestError(this, Severity.WARNING, tr("Invalid white space in property key"),
                tr(s, key), MessageFormat.format(s, key), INVALID_KEY_SPACE, p) );
                withErrors.add(p, "IPK");
            }
            if( checkValues && value != null && (value.startsWith(" ") || value.endsWith(" ")) && !withErrors.contains(p, "SPACE"))
            {
                errors.add( new TestError(this, Severity.OTHER, tr("Property values start or end with white space"),
                tr(s, key), MessageFormat.format(s, key), INVALID_SPACE, p) );
                withErrors.add(p, "SPACE");
            }
            if( checkValues && value != null && !value.equals(entities.unescape(value)) && !withErrors.contains(p, "HTML"))
            {
                errors.add( new TestError(this, Severity.OTHER, tr("Property values contain HTML entity"),
                tr(s, key), MessageFormat.format(s, key), INVALID_HTML, p) );
                withErrors.add(p, "HTML");
            }
            if( checkValues && value != null && value.length() > 0 && presetsValueData != null)
            {
                List<String> values = presetsValueData.get(key);
                if(values == null)
                {
                    Boolean ignore = false;
                    for(String a : ignoreDataStartsWith)
                    {
                        if(key.startsWith(a))
                            ignore = true;
                    }
                    for(String a : ignoreDataEquals)
                    {
                        if(key.equals(a))
                            ignore = true;
                    }
                    for(String a : ignoreDataEndsWith)
                    {
                        if(key.endsWith(a))
                            ignore = true;
                    }
                    if(!ignore)
                    {
                        String i = marktr("Key ''{0}'' not in presets.");
                        errors.add( new TestError(this, Severity.OTHER, tr("Presets do not contain property key"),
                        tr(i, key), MessageFormat.format(i, key), INVALID_VALUE, p) );
                        withErrors.add(p, "UPK");
                    }
                }
                else if(values.size() > 0 && !values.contains(prop.getValue()))
                {
                    boolean ignore = false;
                    for(IgnoreKeyPair a : ignoreDataKeyPair)
                    {
                        if(key.equals(a.key) && value.equals(a.value))
                            ignore = true;
                    }

                    for(IgnoreTwoKeyPair a : ignoreDataTwoKeyPair)
                    {
                        if(key.equals(a.key2) && value.equals(a.value2))
                            ignore = true;
                    }

                    if(!ignore)
                    {
                        String i = marktr("Value ''{0}'' for key ''{1}'' not in presets.");
                        errors.add( new TestError(this, Severity.OTHER, tr("Presets do not contain property value"),
                        tr(i, prop.getValue(), key), MessageFormat.format(i, prop.getValue(), key), INVALID_VALUE, p) );
                        withErrors.add(p, "UPV");
                    }
                }
            }
            if( checkFixmes && value != null && value.length() > 0 )
            {
                if( (value.contains("FIXME") || value.contains("check and delete") || key.contains("todo") || key.contains("fixme"))
                && !withErrors.contains(p, "FIXME"))
                {
                    errors.add( new TestError(this, Severity.OTHER, tr("FIXMES"), FIXME, p) );
                    withErrors.add(p, "FIXME");
                }
            }
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor)
    {
        super.startTest(monitor);
        checkKeys = Main.pref.getBoolean(PREF_CHECK_KEYS, true);
        if( isBeforeUpload )
            checkKeys = checkKeys && Main.pref.getBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, true);

        checkValues = Main.pref.getBoolean(PREF_CHECK_VALUES, true);
        if( isBeforeUpload )
            checkValues = checkValues && Main.pref.getBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, true);

        checkComplex = Main.pref.getBoolean(PREF_CHECK_COMPLEX, true);
        if( isBeforeUpload )
            checkComplex = checkValues && Main.pref.getBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, true);

        checkFixmes = Main.pref.getBoolean(PREF_CHECK_FIXMES, true);
        if( isBeforeUpload )
            checkFixmes = checkFixmes && Main.pref.getBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, true);

        checkPaint = Main.pref.getBoolean(PREF_CHECK_PAINT, true);
        if( isBeforeUpload )
            checkPaint = checkPaint && Main.pref.getBoolean(PREF_CHECK_PAINT_BEFORE_UPLOAD, true);
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection)
    {
        if( checkKeys || checkValues || checkComplex || checkPaint || checkFixmes)
            super.visit(selection);
    }

    @Override
    public void addGui(JPanel testPanel)
    {
        GBC a = GBC.eol();
        a.anchor = GBC.EAST;

        testPanel.add( new JLabel(name), GBC.eol().insets(3,0,0,0) );

        prefCheckKeys = new JCheckBox(tr("Check property keys."), Main.pref.getBoolean(PREF_CHECK_KEYS, true));
        prefCheckKeys.setToolTipText(tr("Validate that property keys are valid checking against list of words."));
        testPanel.add(prefCheckKeys, GBC.std().insets(20,0,0,0));

        prefCheckKeysBeforeUpload = new JCheckBox();
        prefCheckKeysBeforeUpload.setSelected(Main.pref.getBoolean(PREF_CHECK_KEYS_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckKeysBeforeUpload, a);

        prefCheckComplex = new JCheckBox(tr("Use complex property checker."), Main.pref.getBoolean(PREF_CHECK_COMPLEX, true));
        prefCheckComplex.setToolTipText(tr("Validate property values and tags using complex rules."));
        testPanel.add(prefCheckComplex, GBC.std().insets(20,0,0,0));

        prefCheckComplexBeforeUpload = new JCheckBox();
        prefCheckComplexBeforeUpload.setSelected(Main.pref.getBoolean(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckComplexBeforeUpload, a);

        Sources = new JList(new DefaultListModel());

        String sources = Main.pref.get( PREF_SOURCES );
        if(sources != null && sources.length() > 0)
        {
            for(String source : sources.split(";"))
                ((DefaultListModel)Sources.getModel()).addElement(source);
        }

        addSrcButton = new JButton(tr("Add"));
        addSrcButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String source = JOptionPane.showInputDialog(
                        Main.parent,
                        tr("TagChecker source"),
                        tr("TagChecker source"),
                        JOptionPane.QUESTION_MESSAGE
                        );
                if (source != null)
                    ((DefaultListModel)Sources.getModel()).addElement(source);
                Sources.clearSelection();
            }
        });

        editSrcButton = new JButton(tr("Edit"));
        editSrcButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int row = Sources.getSelectedIndex();
                if(row == -1 && Sources.getModel().getSize() == 1)
                {
                    Sources.setSelectedIndex(0);
                    row = 0;
                }
                if (row == -1)
                {
                    if(Sources.getModel().getSize() == 0)
                    {
                        String source = JOptionPane.showInputDialog(Main.parent, tr("TagChecker source"), tr("TagChecker source"), JOptionPane.QUESTION_MESSAGE);
                        if (source != null)
                            ((DefaultListModel)Sources.getModel()).addElement(source);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Please select the row to edit."),
                                tr("Information"),
                                JOptionPane.INFORMATION_MESSAGE
                                );
                    }
                }
                else {
                    String source = (String)JOptionPane.showInputDialog(Main.parent,
                            tr("TagChecker source"),
                            tr("TagChecker source"),
                            JOptionPane.QUESTION_MESSAGE, null, null,
                            Sources.getSelectedValue());
                    if (source != null)
                        ((DefaultListModel)Sources.getModel()).setElementAt(source, row);
                }
                Sources.clearSelection();
            }
        });

        deleteSrcButton = new JButton(tr("Delete"));
        deleteSrcButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (Sources.getSelectedIndex() == -1)
                    JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."), tr("Information"), JOptionPane.QUESTION_MESSAGE);
                else {
                    ((DefaultListModel)Sources.getModel()).remove(Sources.getSelectedIndex());
                }
            }
        });
        Sources.setMinimumSize(new Dimension(300,50));
        Sources.setVisibleRowCount(3);

        Sources.setToolTipText(tr("The sources (URL or filename) of spell check (see http://wiki.openstreetmap.org/index.php/User:JLS/speller) or tag checking data files."));
        addSrcButton.setToolTipText(tr("Add a new source to the list."));
        editSrcButton.setToolTipText(tr("Edit the selected source."));
        deleteSrcButton.setToolTipText(tr("Delete the selected source from the list."));

        testPanel.add(new JLabel(tr("Data sources")), GBC.eol().insets(23,0,0,0));
        testPanel.add(new JScrollPane(Sources), GBC.eol().insets(23,0,0,0).fill(GBC.HORIZONTAL));
        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        testPanel.add(buttonPanel, GBC.eol().fill(GBC.HORIZONTAL));
        buttonPanel.add(addSrcButton, GBC.std().insets(0,5,0,0));
        buttonPanel.add(editSrcButton, GBC.std().insets(5,5,5,0));
        buttonPanel.add(deleteSrcButton, GBC.std().insets(0,5,0,0));

        ActionListener disableCheckActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                handlePrefEnable();
            }
        };
        prefCheckKeys.addActionListener(disableCheckActionListener);
        prefCheckKeysBeforeUpload.addActionListener(disableCheckActionListener);
        prefCheckComplex.addActionListener(disableCheckActionListener);
        prefCheckComplexBeforeUpload.addActionListener(disableCheckActionListener);

        handlePrefEnable();

        prefCheckValues = new JCheckBox(tr("Check property values."), Main.pref.getBoolean(PREF_CHECK_VALUES, true));
        prefCheckValues.setToolTipText(tr("Validate that property values are valid checking against presets."));
        testPanel.add(prefCheckValues, GBC.std().insets(20,0,0,0));

        prefCheckValuesBeforeUpload = new JCheckBox();
        prefCheckValuesBeforeUpload.setSelected(Main.pref.getBoolean(PREF_CHECK_VALUES_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckValuesBeforeUpload, a);

        prefCheckFixmes = new JCheckBox(tr("Check for FIXMES."), Main.pref.getBoolean(PREF_CHECK_FIXMES, true));
        prefCheckFixmes.setToolTipText(tr("Looks for nodes or ways with FIXME in any property value."));
        testPanel.add(prefCheckFixmes, GBC.std().insets(20,0,0,0));

        prefCheckFixmesBeforeUpload = new JCheckBox();
        prefCheckFixmesBeforeUpload.setSelected(Main.pref.getBoolean(PREF_CHECK_FIXMES_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckFixmesBeforeUpload, a);

        prefCheckPaint = new JCheckBox(tr("Check for paint notes."), Main.pref.getBoolean(PREF_CHECK_PAINT, true));
        prefCheckPaint.setToolTipText(tr("Check if map painting found data errors."));
        testPanel.add(prefCheckPaint, GBC.std().insets(20,0,0,0));

        prefCheckPaintBeforeUpload = new JCheckBox();
        prefCheckPaintBeforeUpload.setSelected(Main.pref.getBoolean(PREF_CHECK_PAINT_BEFORE_UPLOAD, true));
        testPanel.add(prefCheckPaintBeforeUpload, a);

        prefUseDataFile = new JCheckBox(tr("Use default data file."), Main.pref.getBoolean(PREF_USE_DATA_FILE, true));
        prefUseDataFile.setToolTipText(tr("Use the default data file (recommended)."));
        testPanel.add(prefUseDataFile, GBC.eol().insets(20,0,0,0));

        prefUseIgnoreFile = new JCheckBox(tr("Use default tag ignore file."), Main.pref.getBoolean(PREF_USE_IGNORE_FILE, true));
        prefUseIgnoreFile.setToolTipText(tr("Use the default tag ignore file (recommended)."));
        testPanel.add(prefUseIgnoreFile, GBC.eol().insets(20,0,0,0));

        prefUseSpellFile = new JCheckBox(tr("Use default spellcheck file."), Main.pref.getBoolean(PREF_USE_SPELL_FILE, true));
        prefUseSpellFile.setToolTipText(tr("Use the default spellcheck file (recommended)."));
        testPanel.add(prefUseSpellFile, GBC.eol().insets(20,0,0,0));
    }

    public void handlePrefEnable()
    {
        boolean selected = prefCheckKeys.isSelected() || prefCheckKeysBeforeUpload.isSelected()
        || prefCheckComplex.isSelected() || prefCheckComplexBeforeUpload.isSelected();
        Sources.setEnabled( selected );
        addSrcButton.setEnabled(selected);
        editSrcButton.setEnabled(selected);
        deleteSrcButton.setEnabled(selected);
    }

    @Override
    public boolean ok()
    {
        enabled = prefCheckKeys.isSelected() || prefCheckValues.isSelected() || prefCheckComplex.isSelected() || prefCheckFixmes.isSelected();
        testBeforeUpload = prefCheckKeysBeforeUpload.isSelected() || prefCheckValuesBeforeUpload.isSelected()
        || prefCheckFixmesBeforeUpload.isSelected() || prefCheckComplexBeforeUpload.isSelected();

        Main.pref.put(PREF_CHECK_VALUES, prefCheckValues.isSelected());
        Main.pref.put(PREF_CHECK_COMPLEX, prefCheckComplex.isSelected());
        Main.pref.put(PREF_CHECK_KEYS, prefCheckKeys.isSelected());
        Main.pref.put(PREF_CHECK_FIXMES, prefCheckFixmes.isSelected());
        Main.pref.put(PREF_CHECK_PAINT, prefCheckPaint.isSelected());
        Main.pref.put(PREF_CHECK_VALUES_BEFORE_UPLOAD, prefCheckValuesBeforeUpload.isSelected());
        Main.pref.put(PREF_CHECK_COMPLEX_BEFORE_UPLOAD, prefCheckComplexBeforeUpload.isSelected());
        Main.pref.put(PREF_CHECK_KEYS_BEFORE_UPLOAD, prefCheckKeysBeforeUpload.isSelected());
        Main.pref.put(PREF_CHECK_FIXMES_BEFORE_UPLOAD, prefCheckFixmesBeforeUpload.isSelected());
        Main.pref.put(PREF_CHECK_PAINT_BEFORE_UPLOAD, prefCheckPaintBeforeUpload.isSelected());
        Main.pref.put(PREF_USE_DATA_FILE, prefUseDataFile.isSelected());
        Main.pref.put(PREF_USE_IGNORE_FILE, prefUseIgnoreFile.isSelected());
        Main.pref.put(PREF_USE_SPELL_FILE, prefUseSpellFile.isSelected());
        String sources = "";
        if( Sources.getModel().getSize() > 0 )
        {
            String sb = "";
            for (int i = 0; i < Sources.getModel().getSize(); ++i)
                sb += ";"+Sources.getModel().getElementAt(i);
            sources = sb.substring(1);
        }
        if(sources.length() == 0)
            sources = null;
        return Main.pref.put(PREF_SOURCES, sources);
    }

    @Override
    public Command fixError(TestError testError)
    {
        List<Command> commands = new ArrayList<Command>(50);

        int i = -1;
        List<? extends OsmPrimitive> primitives = testError.getPrimitives();
        for(OsmPrimitive p : primitives )
        {
            i++;
            Map<String, String> tags = p.getKeys();
            if( tags == null || tags.size() == 0 )
                continue;

            for(Entry<String, String> prop: tags.entrySet() )
            {
                String key = prop.getKey();
                String value = prop.getValue();
                if( value == null || value.trim().length() == 0 )
                    commands.add( new ChangePropertyCommand(Collections.singleton(primitives.get(i)), key, null) );
                else if(value.startsWith(" ") || value.endsWith(" "))
                    commands.add( new ChangePropertyCommand(Collections.singleton(primitives.get(i)), key, value.trim()) );
                else if(key.startsWith(" ") || key.endsWith(" "))
                    commands.add( new ChangePropertyKeyCommand(Collections.singleton(primitives.get(i)), key, key.trim()) );
                else
                {
                    String evalue = entities.unescape(value);
                    if(!evalue.equals(value))
                        commands.add( new ChangePropertyCommand(Collections.singleton(primitives.get(i)), key, evalue) );
                    else
                    {
                        String replacementKey = spellCheckKeyData.get(key);
                        if( replacementKey != null )
                        {
                            commands.add( new ChangePropertyKeyCommand(Collections.singleton(primitives.get(i)),
                            key, replacementKey) );
                        }
                    }
                }
            }
        }

        if( commands.size() == 0 )
            return null;
        else if( commands.size() == 1 )
            return commands.get(0);
        else
            return new SequenceCommand(tr("Fix properties"), commands);
    }

    @Override
    public boolean isFixable(TestError testError)
    {
        if( testError.getTester() instanceof TagChecker)
        {
            int code = testError.getCode();
            return code == INVALID_KEY || code == EMPTY_VALUES || code == INVALID_SPACE || code == INVALID_KEY_SPACE || code == INVALID_HTML;
        }

        return false;
    }

    private static class IgnoreTwoKeyPair {
        public String key1;
        public String value1;
        public String key2;
        public String value2;
    }

    private static class IgnoreKeyPair {
        public String key;
        public String value;
    }

    private static class CheckerData {
        private String description;
        private List<CheckerElement> data = new ArrayList<CheckerElement>();
        private Integer type = 0;
        private Integer code;
        protected Severity severity;
        protected static int NODE = 1;
        protected static int WAY = 2;
        protected static int RELATION = 3;
        protected static int ALL = 4;
        protected static int TAG_CHECK_ERROR  = 1250;
        protected static int TAG_CHECK_WARN   = 1260;
        protected static int TAG_CHECK_INFO   = 1270;

        private class CheckerElement {
            public Object tag;
            public Object value;
            public Boolean noMatch;
            public Boolean tagAll = false;
            public Boolean valueAll = false;
            public Boolean valueBool = false;
            private Pattern getPattern(String str) throws IllegalStateException, PatternSyntaxException
            {
                if(str.endsWith("/i"))
                    return Pattern.compile(str.substring(1,str.length()-2), Pattern.CASE_INSENSITIVE);
                else if(str.endsWith("/"))
                    return Pattern.compile(str.substring(1,str.length()-1));
                throw new IllegalStateException();
            }
            public CheckerElement(String exp) throws IllegalStateException, PatternSyntaxException
            {
                Matcher m = Pattern.compile("(.+)([!=]=)(.+)").matcher(exp);
                m.matches();

                String n = m.group(1).trim();
                if(n.equals("*"))
                    tagAll = true;
                else
                    tag = n.startsWith("/") ? getPattern(n) : n;
                noMatch = m.group(2).equals("!=");
                n = m.group(3).trim();
                if(n.equals("*"))
                    valueAll = true;
                else if(n.equals("BOOLEAN_TRUE"))
                {
                    valueBool = true;
                    value = OsmUtils.trueval;
                }
                else if(n.equals("BOOLEAN_FALSE"))
                {
                    valueBool = true;
                    value = OsmUtils.falseval;
                }
                else
                    value = n.startsWith("/") ? getPattern(n) : n;
            }
            public Boolean match(OsmPrimitive osm)
            {
                for(Entry<String, String> prop: osm.getKeys().entrySet())
                {
                    String key = prop.getKey();
                    String val = valueBool ? OsmUtils.getNamedOsmBoolean(prop.getValue()) : prop.getValue();
                    if((tagAll || (tag instanceof Pattern ? ((Pattern)tag).matcher(key).matches() : key.equals(tag)))
                    && (valueAll || (value instanceof Pattern ? ((Pattern)value).matcher(val).matches() : val.equals(value))))
                        return !noMatch;
                }
                return noMatch;
            }
        };

        public String getData(String str)
        {
            Matcher m = Pattern.compile(" *# *([^#]+) *$").matcher(str);
            str = m.replaceFirst("").trim();
            try
            {
                description = m.group(1);
                if(description != null && description.length() == 0)
                    description = null;
            }
            catch (IllegalStateException e)
            {
                description = null;
            }
            String[] n = str.split(" *: *", 3);
            if(n[0].equals("way"))
                type = WAY;
            else if(n[0].equals("node"))
                type = NODE;
            else if(n[0].equals("relation"))
                type = RELATION;
            else if(n[0].equals("*"))
                type = ALL;
            if(type == 0 || n.length != 3)
                return tr("Could not find element type");
            if(n[1].equals("W"))
            {
                severity = Severity.WARNING;
                code = TAG_CHECK_WARN;
            }
            else if(n[1].equals("E"))
            {
                severity = Severity.ERROR;
                code = TAG_CHECK_ERROR;
            }
            else if(n[1].equals("I"))
            {
                severity = Severity.OTHER;
                code = TAG_CHECK_INFO;
            }
            else
                return tr("Could not find warning level");
            for(String exp: n[2].split(" *&& *"))
            {
                try
                {
                    data.add(new CheckerElement(exp));
                }
                catch(IllegalStateException e)
                {
                    return tr("Illegal expression ''{0}''", exp);
                }
                catch(PatternSyntaxException e)
                {
                    return tr("Illegal regular expression ''{0}''", exp);
                }
            }
            return null;
        }
        public Boolean match(OsmPrimitive osm)
        {
            if(osm.getKeys() == null || (type == NODE && !(osm instanceof Node))
            || (type == RELATION && !(osm instanceof Relation)) || (type == WAY && !(osm instanceof Way)))
                return false;
            for(CheckerElement ce : data)
            {
                if(!ce.match(osm))
                    return false;
            }
            return true;
        }
        public String getDescription()
        {
            return tr(description);
        }
        public String getDescriptionOrig()
        {
            return description;
        }
        public Severity getSeverity()
        {
            return severity;
        }
        public int getCode()
        {
            return code + type;
        }
    }
}
