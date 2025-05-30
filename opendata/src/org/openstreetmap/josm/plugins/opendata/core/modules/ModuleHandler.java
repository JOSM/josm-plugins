// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.modules;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.preferences.sources.SourceProvider;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.plugins.opendata.OdPlugin;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.gui.OdPreferenceSetting;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ResourceProvider;

/**
 * ModuleHandler is basically a collection of static utility functions used to bootstrap
 * and manage the loaded modules.
 *
 */
public final class ModuleHandler {

    /**
     * All installed and loaded modules (resp. their main classes)
     */
    public static final Collection<Module> moduleList = new LinkedList<>();

    /**
     * Add here all ClassLoader whose resource should be searched.
     */
    private static final List<ClassLoader> sources = new LinkedList<>();

    static {
        try {
            sources.add(ClassLoader.getSystemClassLoader());
            sources.add(ModuleHandler.class.getClassLoader());
        } catch (SecurityException ex) {
            Logging.trace(ex);
            sources.add(ImageProvider.class.getClassLoader());
        }
    }

    private ModuleHandler() {
        // Hide default constructor for utilities classes
    }

    public static Collection<ClassLoader> getResourceClassLoaders() {
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * Checks whether the locally available modules should be updated and
     * asks the user if running an update is OK. An update is advised if
     * JOSM was updated to a new version since the last module updates or
     * if the modules were last updated a long time ago.
     *
     * @param parent the parent window relative to which the confirmation dialog
     * is to be displayed
     * @return true if a module update should be run; false, otherwise
     */
    public static boolean checkAndConfirmModuleUpdate(Component parent) {
        String message = null;
        String togglePreferenceKey = null;
        long tim = System.currentTimeMillis();
        long last = Config.getPref().getLong("opendata.modulemanager.lastupdate", 0);
        int maxTime = Config.getPref().getInt("opendata.modulemanager.time-based-update.interval", 60);
        long d = (tim - last) / (24 * 60 * 60 * 1000L);
        if ((last <= 0) || (maxTime <= 0)) {
            Config.getPref().put("opendata.modulemanager.lastupdate", Long.toString(tim));
        } else if (d > maxTime) {
            message =
                    "<html>"
                            + tr("Last module update more than {0} days ago.", d)
                            + "</html>";
            togglePreferenceKey = "opendata.modulemanager.time-based-update.policy";
        }
        if (message == null) return false;

        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Update modules"),
                        new ImageProvider("dialogs", "refresh"),
                        tr("Click to update the activated modules"),
                        null /* no specific help context */
                        ),
                new ButtonSpec(
                        tr("Skip update"),
                        new ImageProvider("cancel"),
                        tr("Click to skip updating the activated modules"),
                        null /* no specific help context */
                        )
        };

        UpdateModulesMessagePanel pnlMessage = new UpdateModulesMessagePanel();
        pnlMessage.setMessage(message);
        pnlMessage.initDontShowAgain(togglePreferenceKey);

        // check whether automatic update at startup was disabled
        //
        String policy = Config.getPref().get(togglePreferenceKey, "ask");
        policy = policy.trim().toLowerCase(Locale.ROOT);
        if ("never".equals(policy)) {
            if ("opendata.modulemanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Logging.info(tr("Skipping module update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return false;
        }

        if ("always".equals(policy)) {
            if ("opendata.modulemanager.time-based-update.policy".equals(togglePreferenceKey)) {
                Logging.info(tr("Running module update after elapsed update interval. Automatic update at startup is disabled."));
            }
            return true;
        }

        if (!"ask".equals(policy)) {
            Logging.warn(tr("Unexpected value ''{0}'' for preference ''{1}''. Assuming value ''ask''.", policy, togglePreferenceKey));
        }
        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                pnlMessage,
                tr("Update modules"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                null
                );

        if (pnlMessage.isRememberDecision()) {
            switch (ret) {
            case 0:
                Config.getPref().put(togglePreferenceKey, "always");
                break;
            case JOptionPane.CLOSED_OPTION:
            case 1:
                Config.getPref().put(togglePreferenceKey, "never");
                break;
            default:
                Logging.trace(Integer.toString(ret));
            }
        } else {
            Config.getPref().put(togglePreferenceKey, "ask");
        }
        return ret == 0;
    }

    /**
     * Checks whether all preconditions for loading the module <code>module</code> are met. The
     * current JOSM version must be compatible with the module and no other modules this module
     * depends on should be missing.
     *
     * @param parent parent component
     * @param modules the collection of all loaded modules
     * @param module the module for which preconditions are checked
     * @return true, if the preconditions are met; false otherwise
     */
    public static boolean checkLoadPreconditions(Component parent, Collection<ModuleInformation> modules, ModuleInformation module) {
        return true;
    }

    /**
     * Creates a class loader for loading module code.
     *
     * @param modules the collection of modules which are going to be loaded with this class loader
     * @return the class loader
     */
    public static ClassLoader createClassLoader(Collection<ModuleInformation> modules) {
        // iterate all modules and collect all libraries of all modules:
        List<URL> allModuleLibraries = new LinkedList<>();
        File moduleDir = OdPlugin.getInstance().getModulesDirectory();
        for (ModuleInformation info : modules) {
            if (info.libraries == null) {
                continue;
            }
            allModuleLibraries.addAll(info.libraries);
            File moduleJar = new File(moduleDir, info.name + ".jar");
            I18n.addTexts(moduleJar);
            URL moduleJarUrl = ModuleInformation.fileToURL(moduleJar);
            allModuleLibraries.add(moduleJarUrl);
        }

        // create a classloader for all modules:
        return new URLClassLoader(allModuleLibraries.toArray(new URL[0]), OdPlugin.class.getClassLoader());
    }

    /**
     * Loads and instantiates the module described by <code>module</code> using
     * the class loader <code>moduleClassLoader</code>.
     *
     * @param parent parent component
     * @param module the module
     * @param moduleClassLoader the module class loader
     */
    public static void loadModule(Component parent, ModuleInformation module, ClassLoader moduleClassLoader) {
        String msg = tr("Could not load module {0}. Delete from preferences?", module.name);
        try {
            Class<? extends Module> klass = module.loadClass(moduleClassLoader);
            if (klass != null) {
                Logging.info(tr("loading module ''{0}'' (version {1})", module.name, module.localversion));
                Module mod = module.load(klass);
                if (moduleList.add(mod)) {
                    SourceProvider styleProvider = mod.getMapPaintStyleSourceProvider();
                    if (styleProvider != null) {
                        MapPaintPreference.registerSourceProvider(styleProvider);
                    }
                    SourceProvider presetProvider = mod.getPresetSourceProvider();
                    if (presetProvider != null) {
                        TaggingPresetPreference.registerSourceProvider(presetProvider);
                    }
                }
            }
            msg = null;
        } catch (ModuleException e) {
            Logging.debug(e);
            if (e.getCause() instanceof ClassNotFoundException) {
                msg = tr("<html>Could not load module {0} because the module<br>main class ''{1}'' was not found.<br>"
                        + "Delete from preferences?</html>", module.name, module.className);
            }
        }
        if (msg != null && confirmDisableModule(parent, msg, module.name)) {
            PreferencesUtils.removeFromList(Config.getPref(), OdConstants.PREF_MODULES, module.name);
        }
    }

    /**
     * Loads the module in <code>modules</code> from locally available jar files into memory.
     *
     * @param parent parent component
     * @param modules the list of modules
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     */
    public static void loadModules(Component parent, Collection<ModuleInformation> modules, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Loading modules ..."));
            monitor.subTask(tr("Checking module preconditions..."));
            List<ModuleInformation> toLoad = new LinkedList<>();
            for (ModuleInformation pi: modules) {
                if (checkLoadPreconditions(parent, modules, pi)) {
                    toLoad.add(pi);
                }
            }
            if (toLoad.isEmpty())
                return;

            ClassLoader moduleClassLoader = createClassLoader(toLoad);
            sources.add(0, moduleClassLoader);
            ResourceProvider.addAdditionalClassLoader(moduleClassLoader);
            monitor.setTicksCount(toLoad.size());
            for (ModuleInformation info : toLoad) {
                monitor.setExtraText(tr("Loading module ''{0}''...", info.name));
                loadModule(parent, info, moduleClassLoader);
                monitor.worked(1);
            }
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Loads locally available module information from local module jars and from cached
     * module lists.
     *
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the map of locally available module information
     *
     */
    private static Map<String, ModuleInformation> loadLocallyAvailableModuleInformation(ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            ReadLocalModuleInformationTask task = new ReadLocalModuleInformationTask(monitor);
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<?> future = service.submit(task);
            try {
                future.get();
            } catch (ExecutionException e) {
                Logging.error(e);
                return Collections.emptyMap();
            } catch (InterruptedException e) {
                Logging.error(e);
                Thread.currentThread().interrupt();
                return Collections.emptyMap();
            }
            HashMap<String, ModuleInformation> ret = new HashMap<>();
            for (ModuleInformation pi: task.getAvailableModules()) {
                ret.put(pi.name, pi);
            }
            return Collections.unmodifiableMap(ret);
        } finally {
            monitor.finishTask();
        }
    }

    private static void alertMissingModuleInformation(Component parent, Collection<String> modules) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("JOSM could not find information about the following module:",
                "JOSM could not find information about the following modules:",
                modules.size()));
        sb.append("<ul>");
        for (String module: modules) {
            sb.append("<li>").append(module).append("</li>");
        }
        sb.append("</ul>");
        sb.append(trn("The module is not going to be loaded.",
                "The modules are not going to be loaded.",
                modules.size()));
        sb.append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Module/Loading#MissingModuleInfos")
                );
    }

    /**
     * Builds the set of modules to load. Deprecated and unmaintained modules are filtered
     * out. This involves user interaction. This method displays alert and confirmation messages.
     *
     * @param parent parent component
     * @return the list of modules to load (as set of module names)
     */
    public static List<ModuleInformation> buildListOfModulesToLoad(Component parent) {
        Set<String> modules = new HashSet<>(Config.getPref().getList(OdConstants.PREF_MODULES, new LinkedList<>()));
        if (System.getProperty("josm."+OdConstants.PREF_MODULES) != null) {
            modules.addAll(Arrays.asList(System.getProperty("josm."+OdConstants.PREF_MODULES).split(",")));
        }
        Map<String, ModuleInformation> infos = loadLocallyAvailableModuleInformation(null);
        List<ModuleInformation> ret = new LinkedList<>();
        for (Iterator<String> it = modules.iterator(); it.hasNext();) {
            String module = it.next();
            if (infos.containsKey(module)) {
                ret.add(infos.get(module));
                it.remove();
            }
        }
        if (!modules.isEmpty()) {
            alertMissingModuleInformation(parent, modules);
        }
        return ret;
    }

    private static void alertFailedModuleUpdate(Component parent, Collection<ModuleInformation> modules) {
        StringBuilder sb = new StringBuilder(150);
        sb.append("<html>");
        sb.append(trn(
                "Updating the following module has failed:",
                "Updating the following modules has failed:",
                modules.size()
                )
                );
        sb.append("<ul>");
        for (ModuleInformation pi: modules) {
            sb.append("<li>").append(pi.name).append("</li>");
        }
        sb.append("</ul>");
        sb.append(trn(
                "Please open the Preference Dialog after JOSM has started and try to update it manually.",
                "Please open the Preference Dialog after JOSM has started and try to update them manually.",
                modules.size()
                ));
        sb.append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Module update failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Module/Loading#FailedModuleUpdated")
                );
    }

    /**
     * Updates the modules in <code>modules</code>.
     *
     * @param parent the parent window for message boxes
     * @param modules the collection of modules to update. Must not be null.
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null.
     * @return list of modules
     * @throws IllegalArgumentException thrown if modules is null
     */
    public static List<ModuleInformation> updateModules(Component parent,
            List<ModuleInformation> modules, ProgressMonitor monitor)
                    throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(modules, "modules");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            ExecutorService service = Executors.newSingleThreadExecutor();

            // try to download the module lists
            //
            ReadRemoteModuleInformationTask task1 = new ReadRemoteModuleInformationTask(
                    monitor.createSubTaskMonitor(1, false),
                    OdPreferenceSetting.getModuleSites()
                    );
            Future<?> future = service.submit(task1);
            try {
                future.get();
                modules = buildListOfModulesToLoad(parent);
            } catch (ExecutionException e) {
                Logging.warn(tr("Warning: failed to download module information list"));
                Logging.debug(e);
                // don't abort in case of error, continue with downloading modules below
            } catch (InterruptedException e) {
                Logging.warn(tr("Warning: failed to download module information list"));
                Logging.debug(e);
                Thread.currentThread().interrupt();
                return Collections.emptyList();
                // don't abort in case of error, continue with downloading modules below
            }

            // filter modules which actually have to be updated
            //
            Collection<ModuleInformation> modulesToUpdate = new ArrayList<>();
            for (ModuleInformation pi: modules) {
                if (pi.isUpdateRequired()) {
                    modulesToUpdate.add(pi);
                }
            }

            if (!modulesToUpdate.isEmpty()) {
                // try to update the locally installed modules
                //
                ModuleDownloadTask task2 = new ModuleDownloadTask(
                        monitor.createSubTaskMonitor(1, false),
                        modulesToUpdate,
                        tr("Update modules")
                        );

                future = service.submit(task2);
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Logging.debug(e);
                    alertFailedModuleUpdate(parent, modulesToUpdate);
                    return modules;
                } catch (InterruptedException e) {
                    Logging.debug(e);
                    alertFailedModuleUpdate(parent, modulesToUpdate);
                    Thread.currentThread().interrupt();
                    return modules;
                }
                // notify user if downloading a locally installed module failed
                //
                if (!task2.getFailedModules().isEmpty()) {
                    alertFailedModuleUpdate(parent, task2.getFailedModules());
                    return modules;
                }
            }
        } finally {
            monitor.finishTask();
        }
        // remember the update because it was successful
        //
        Config.getPref().put("opendata.modulemanager.lastupdate", Long.toString(System.currentTimeMillis()));
        return modules;
    }

    /**
     * Ask the user for confirmation that a module shall be disabled.
     *
     * @param parent parent component
     * @param reason the reason for disabling the module
     * @param name the module name
     * @return true, if the module shall be disabled; false, otherwise
     */
    public static boolean confirmDisableModule(Component parent, String reason, String name) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Disable module"),
                        new ImageProvider("dialogs", "delete"),
                        tr("Click to delete the module ''{0}''", name),
                        null /* no specific help context */
                        ),
                new ButtonSpec(
                        tr("Keep module"),
                        new ImageProvider("cancel"),
                        tr("Click to keep the module ''{0}''", name),
                        null /* no specific help context */
                        )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                parent,
                reason,
                tr("Disable module"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                null // FIXME: add help topic
                );
        return ret == 0;
    }

    /**
     * Installs downloaded modules. Moves files with the suffix ".jar.new" to the corresponding
     * ".jar" files.
     * <p>
     * If {@code doWarn} is true, this method emits warning messages on the console if a downloaded
     * but not yet installed module .jar can't be installed. If {@code doWarn} is false, the
     * installation of the respective module is silently skipped.
     *
     * @param doWarn if true, warning messages are displayed; false otherwise
     */
    public static void installDownloadedModules(boolean doWarn) {
        File moduleDir = OdPlugin.getInstance().getModulesDirectory();
        if (!moduleDir.exists() || !moduleDir.isDirectory() || !moduleDir.canWrite())
            return;

        final File[] files = moduleDir.listFiles((dir, name) -> name.endsWith(".jar.new"));

        if (files != null) {
            for (File updatedModule : files) {
                final String filePath = updatedModule.getPath();
                File module = new File(filePath.substring(0, filePath.length() - 4));
                String moduleName = updatedModule.getName().substring(0, updatedModule.getName().length() - 8);
                if (module.exists() && !module.delete() && doWarn) {
                    Logging.warn(tr("Warning: failed to delete outdated module ''{0}''.", module.toString()));
                    Logging.warn(tr("Warning: failed to install already downloaded module ''{0}''. Skipping installation. " +
                            "JOSM is still going to load the old module version.", moduleName));
                    continue;
                }
                if (!updatedModule.renameTo(module) && doWarn) {
                    Logging.warn(tr("Warning: failed to install module ''{0}'' from temporary download file ''{1}''. Renaming failed.",
                            module.toString(), updatedModule.toString()));
                    Logging.warn(tr("Warning: failed to install already downloaded module ''{0}''. Skipping installation. " +
                            "JOSM is still going to load the old module version.", moduleName));
                }
            }
        }
    }

    private static class UpdateModulesMessagePanel extends JPanel {
        private JMultilineLabel lblMessage;
        private JCheckBox cbDoNotShowAgain;

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.insets = new Insets(5, 5, 5, 5);
            lblMessage = new JMultilineLabel("");
            add(lblMessage, gc);
            lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN));

            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weighty = 0.0;
            cbDoNotShowAgain = new JCheckBox(
                    tr("Do not ask again and remember my decision (go to Preferences->Modules to change it later)"));
            add(cbDoNotShowAgain, gc);
            cbDoNotShowAgain.setFont(cbDoNotShowAgain.getFont().deriveFont(Font.PLAIN));
        }

        UpdateModulesMessagePanel() {
            build();
        }

        public void setMessage(String message) {
            lblMessage.setText(message);
        }

        public void initDontShowAgain(String preferencesKey) {
            String policy = Config.getPref().get(preferencesKey, "ask");
            policy = policy.trim().toLowerCase(Locale.ROOT);
            cbDoNotShowAgain.setSelected(!"ask".equals(policy));
        }

        public boolean isRememberDecision() {
            return cbDoNotShowAgain.isSelected();
        }
    }
}
