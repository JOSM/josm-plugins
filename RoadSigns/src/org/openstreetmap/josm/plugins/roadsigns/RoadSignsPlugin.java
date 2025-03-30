// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.roadsigns;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.roadsigns.RoadSignInputDialog.SettingsPanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * The entry point for the {@link RoadSignsPlugin}
 */
public class RoadSignsPlugin extends Plugin {
    private static final String SELECTION_PREFERENCE = "plugin.roadsigns.preset.selection";
    private static final String SOURCES_PREFERENCE = "plugin.roadsigns.sources";
    private static final String LAST_SOURCES_PREFERENCE = "plugin.roadsigns.sources.last";
    private static final String CUSTOM = marktr("custom");
    static PresetMetaData selectedPreset;
    static List<Sign> signs;
    static List<String> iconDirs;

    private static RoadSignsPlugin plugin;
    
    private static final PresetMetaData PRESET_AT = new PresetMetaData(
            "AT", tr("Austria"), "resource://data/roadsignpresetAT.xml", "resource://images/AT/");
    private static final PresetMetaData PRESET_BE = new PresetMetaData(
            "BE", tr("Belgium"), "resource://data/roadsignpresetBE.xml", "resource://images/BE/");
    private static final PresetMetaData PRESET_CZ = new PresetMetaData(
            "CZ", tr("Czech Republic"), "resource://data/roadsignpresetCZ.xml", "resource://images/CZ/");
    private static final PresetMetaData PRESET_ES = new PresetMetaData(
            "ES", tr("Spain"), "resource://data/roadsignpresetES.xml", "resource://images/ES/");
    private static final PresetMetaData PRESET_DE = new PresetMetaData(
            "DE", tr("Germany"), "resource://data/roadsignpresetDE.xml", "resource://images/DE/");
    private static final PresetMetaData PRESET_PL = new PresetMetaData(
            "PL", tr("Poland"), "resource://data/roadsignpresetPL.xml", "resource://images/PL/");
    private static final PresetMetaData PRESET_SK = new PresetMetaData(
            "SK", tr("Slovakia"), "resource://data/roadsignpresetSK.xml", "resource://images/SK/");
    private static final Collection<PresetMetaData> DEFAULT_PRESETS = Arrays.asList(
            PRESET_AT, PRESET_BE, PRESET_CZ, PRESET_ES, PRESET_DE, PRESET_PL, PRESET_SK);

    private static void setPluginInstance(RoadSignsPlugin plugin) {
        RoadSignsPlugin.plugin = plugin;
    }

    /**
     * Create a new plugin instance
     * @param info The info to use when creating this instance
     */
    public RoadSignsPlugin(PluginInformation info) {
        super(info);
        setPluginInstance(this);
        registerAction();
    }

    /**
     * Get the plugin directory
     * @return The plugin directory
     */
    static File pluginDir() {
        File dir = plugin.getPluginDirs().getUserDataDirectory(false);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new UncheckedIOException(new IOException("Could not create directory: " + dir.getAbsolutePath()));
        }
        return dir;
    }

    private static void registerAction() {
        JButton btn = new JButton(new RoadSignAction());
        btn.setText(null);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setPreferredSize(new Dimension(18, 18));
        PropertiesDialog.pluginHook.add(btn);
        PropertiesDialog.pluginHook.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        PropertiesDialog.pluginHook.revalidate();
    }

    private static class RoadSignAction extends JosmAction {
        RoadSignAction() {
            super(tr("Roadsign tagging"), "pref/roadsigns-small", tr("Add tags by clicking on road signs"),
                    Shortcut.registerShortcut("plugin:roadsigns:dialog", tr("Roadsigns plugin: open dialog"),
                            KeyEvent.VK_Q, Shortcut.ALT_SHIFT), false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String code = Config.getPref().get(SELECTION_PREFERENCE, null);
            if (code == null) {
                ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), tr("Settings"), tr("Ok"), tr("Cancel"));
                ed.setButtonIcons("ok", "cancel");
                SettingsPanel settings = new SettingsPanel(true, null);
                ed.setContent(settings);
                ed.showDialog();
                if (ed.getValue() != 1) return;
                try {
                    settings.apply();
                } catch (IOException ex) {
                    Logging.trace(ex);
                    return;
                }
            }
            try {
                loadSignPreset();
            } catch (IOException ex) {
                Logging.trace(ex);
                return;
            }
            RoadSignInputDialog input = new RoadSignInputDialog();
            input.showDialog();
        }

    }

    /**
     * A struct class for storing metadata
     */
    public static class PresetMetaData {
        /** The country code */
        @StructEntry public String code;
        /** The display name */
        @StructEntry public String display_name;
        /** The path to the preset */
        @StructEntry public String preset_path;
        /** The path to the icons */
        @StructEntry public String icon_path;

        public PresetMetaData() {
        }

        /**
         * Create a new record with the specified data
         * @param countryCode The country code to use
         * @param displayName The display name
         * @param presetPath The path to the preset
         * @param iconsPath The path to the icons
         */
        public PresetMetaData(String countryCode, String displayName, String presetPath, String iconsPath) {
            this.code = countryCode;
            this.display_name = displayName;
            this.preset_path = presetPath;
            this.icon_path = iconsPath;
        }

        @Override
        public String toString() {
            return display_name;
        }
    }

    public static void setSelectedPreset(PresetMetaData preset) throws IOException {
        Config.getPref().put(SELECTION_PREFERENCE, preset.code);
        loadSignPreset();
    }

    public static List<PresetMetaData> getAvailablePresetsMetaData() {
        List<PresetMetaData> presetsData = Objects.requireNonNull(StructUtils.getListOfStructs(
                Config.getPref(), "plugin.roadsigns.presets", DEFAULT_PRESETS, PresetMetaData.class));

        String customFile = Config.getPref().get(SOURCES_PREFERENCE, null);
        if (customFile == null) {
            // for legacy reasons, try both string and collection preference type
            List<String> customFiles = Config.getPref().getList(SOURCES_PREFERENCE, null);
            if (customFiles != null && !customFiles.isEmpty()) {
                customFile = customFiles.iterator().next();
            }
        }

        if (customFile != null) {
            presetsData.add(readCustomFile(customFile));
        } else {
            Config.getPref().put(LAST_SOURCES_PREFERENCE, null);
        }

        return presetsData;
    }

    /**
     * Read the custom preset file
     * @param customFile The file to read
     * @return The metadata for that custom file
     */
    private static PresetMetaData readCustomFile(String customFile) {
        // first check, if custom file preference has changed. If yes,
        // change the current preset selection to custom directly
        String lastCustomFile = Config.getPref().get(LAST_SOURCES_PREFERENCE, null);
        if (!Objects.equals(customFile, lastCustomFile)) {
            Config.getPref().put(LAST_SOURCES_PREFERENCE, customFile);
            Config.getPref().put(SELECTION_PREFERENCE, CUSTOM);
        }

        String customIconDirsStr = Config.getPref().get("plugin.roadsigns.icon.sources", null);
        List<String> customIconDirs = null;
        if (customIconDirsStr != null) {
            customIconDirs = new ArrayList<>(Arrays.asList(customIconDirsStr.split(",")));
        } else {
            customIconDirs = Config.getPref().getList("plugin.roadsigns.icon.sources", null);
        }
        if (customIconDirs != null) {
            customIconDirs = new ArrayList<>(customIconDirs);
        } else {
            customIconDirs = new ArrayList<>();
        }
        // add icon directory relative to preset file
        if (!customFile.startsWith("resource:")) {
            String parentDir;
            try {
                parentDir = new URI(customFile).getPath();
            } catch (URISyntaxException ex) {
                Logging.trace(ex);
                File f = new File(customFile);
                parentDir = f.getParent();
            }
            if (parentDir != null && !parentDir.isEmpty()) {
                customIconDirs.add(parentDir);
            }
        }
        if (Config.getPref().getBoolean("plugin.roadsigns.use_default_icon_source", true)) {
            customIconDirs.add("resource://images/");
        }
        return new PresetMetaData(CUSTOM, tr(CUSTOM), customFile,
                String.join(",", customIconDirs));
    }

    protected static void loadSignPreset() throws IOException {
        List<PresetMetaData> presetsData = getAvailablePresetsMetaData();
        String code = Config.getPref().get(SELECTION_PREFERENCE, null);

        for (PresetMetaData data : presetsData) {
            if (data.code.equals(code)) {
                selectedPreset = data;
                break;
            }
        }
        if (selectedPreset == null) {
            if (!presetsData.isEmpty()) {
                selectedPreset = presetsData.get(0);
            } else {
                selectedPreset = PRESET_DE;
            }
        }
        iconDirs = Arrays.asList(selectedPreset.icon_path.split(","));
        String source = selectedPreset.preset_path;

        try (CachedFile cachedFile = new CachedFile(source);
            InputStream in = cachedFile.getInputStream()) {
            RoadSignsReader reader = new RoadSignsReader(in);
            signs = reader.parse();
        } catch (IOException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Could not read tagging preset source: ''{0}''", source),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            throw ex;
        } catch (SAXException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Error parsing tagging preset from ''{0}'':\n", source)+ex.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            throw new IOException(ex);
        }
    }
}
