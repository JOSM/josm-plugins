// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.modules;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.opendata.OdPlugin;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * An asynchronous task for downloading module lists from the configured module download sites.
 *
 */
public class ReadRemoteModuleInformationTask extends PleaseWaitRunnable {

    private Collection<String> sites;
    private boolean canceled;
    private HttpURLConnection connection;
    private List<ModuleInformation> availableModules;

    protected enum CacheType {PLUGIN_LIST, ICON_LIST}

    protected void init(Collection<String> sites){
        this.sites = sites;
        if (sites == null) {
            this.sites = Collections.emptySet();
        }
        availableModules = new LinkedList<>();

    }
    /**
     * Creates the task
     *
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     */
    public ReadRemoteModuleInformationTask(Collection<String> sites) {
        super(tr("Download module list..."), false /* don't ignore exceptions */);
        init(sites);
    }

    /**
     * Creates the task
     *
     * @param monitor the progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     */
    public ReadRemoteModuleInformationTask(ProgressMonitor monitor, Collection<String> sites) {
        super(tr("Download module list..."), monitor == null ? NullProgressMonitor.INSTANCE: monitor, false /* don't ignore exceptions */);
        init(sites);
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {}

    /**
     * Creates the file name for the cached module list and the icon cache
     * file.
     *
     * @param site the name of the site
     * @param type icon cache or module list cache
     * @return the file name for the cache file
     */
    protected File createSiteCacheFile(File moduleDir, String site, CacheType type) {
        String name;
        try {
            site = site.replaceAll("%<(.*)>", "");
            URL url = new URL(site);
            StringBuilder sb = new StringBuilder();
            sb.append("site-");
            sb.append(url.getHost()).append("-");
            if (url.getPort() != -1) {
                sb.append(url.getPort()).append("-");
            }
            String path = url.getPath();
            for (int i =0;i<path.length()-4; i++) {
                char c = path.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(c);
                } else {
                    sb.append("_");
                }
            }
            switch (type) {
            case PLUGIN_LIST:
                sb.append(".txt");
                break;
            case ICON_LIST:
                sb.append("-icons.zip");
                break;
            }
            name = sb.toString();
        } catch(MalformedURLException e) {
            name = "site-unknown.txt";
        }
        return new File(moduleDir, name);
    }

    /**
     * Downloads the list from a remote location
     *
     * @param site the site URL
     * @param monitor a progress monitor
     * @return the downloaded list
     */
    protected String downloadModuleList(String site, ProgressMonitor monitor) {
        StringBuilder sb = new StringBuilder();
        try {
            // replace %<x> with empty string */
            String printsite = site.replaceAll("%<(.*)>", "");

            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading module list from ''{0}''", printsite));

            URL url = new URL(printsite);
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("User-Agent",Version.getInstance().getAgentString());
                connection.setRequestProperty("Host", url.getHost());
                connection.setRequestProperty("Accept-Charset", "utf-8");
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch(MalformedURLException e) {
            if (canceled) return null;
            e.printStackTrace();
            return null;
        } catch(IOException e) {
            if (canceled) return null;
            e.printStackTrace();
            return null;
        } finally {
            synchronized(this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            monitor.finishTask();
        }
    }

    /**
     * Downloads the icon archive from a remote location
     *
     * @param site the site URL
     * @param monitor a progress monitor
     */
    protected void downloadModuleIcons(String site, File destFile, ProgressMonitor monitor) {
        try {
            site = site.replaceAll("%<(.*)>", "");

            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading module list from ''{0}''", site));

            URL url = new URL(site);
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("User-Agent",Version.getInstance().getAgentString());
                connection.setRequestProperty("Host", url.getHost());
            }
            try (
                InputStream in = connection.getInputStream();
                OutputStream out = new FileOutputStream(destFile)
            ) {
                byte[] buffer = new byte[8192];
                for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                    out.write(buffer, 0, read);
                }
            }
        } catch(MalformedURLException e) {
            if (canceled) return;
            e.printStackTrace();
            return;
        } catch(IOException e) {
            if (canceled) return;
            e.printStackTrace();
            return;
        } finally {
            synchronized(this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            monitor.finishTask();
        }
        for (ModuleInformation pi : availableModules) {
            if (pi.icon == null && pi.iconPath != null) {
                pi.icon = new ImageProvider(pi.name+".jar/"+pi.iconPath)
                                .setArchive(destFile)
                                .setMaxWidth(24)
                                .setMaxHeight(24)
                                .setOptional(true).get();
            }
        }
    }

    /**
     * Writes the list of modules to a cache file
     *
     * @param site the site from where the list was downloaded
     * @param list the downloaded list
     */
    protected void cacheModuleList(String site, String list) {
        PrintWriter writer = null;
        try {
            File moduleDir = OdPlugin.getInstance().getModulesDirectory();
            if (!moduleDir.exists()) {
                if (! moduleDir.mkdirs()) {
                    System.err.println(tr("Warning: failed to create module directory ''{0}''. Cannot cache module list from module site ''{1}''.", moduleDir.toString(), site));
                }
            }
            File cacheFile = createSiteCacheFile(moduleDir, site, CacheType.PLUGIN_LIST);
            getProgressMonitor().subTask(tr("Writing module list to local cache ''{0}''", cacheFile.toString()));
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), "utf-8"));
            writer.write(list);
        } catch (IOException e) {
            // just failed to write the cache file. No big deal, but log the exception anyway
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    /**
     * Parses the module list
     *
     * @param site the site from where the list was downloaded
     * @param doc the document with the module list
     */
    protected void parseModuleListDocument(String site, String doc) {
        try {
            getProgressMonitor().subTask(tr("Parsing module list from site ''{0}''", site));
            InputStream in = new ByteArrayInputStream(doc.getBytes("UTF-8"));
            availableModules.addAll(new ModuleListParser().parse(in));
        } catch(UnsupportedEncodingException e) {
            System.err.println(tr("Failed to parse module list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            e.printStackTrace();
        } catch(ModuleListParseException e) {
            System.err.println(tr("Failed to parse module list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            e.printStackTrace();
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        if (sites == null) return;
        getProgressMonitor().setTicksCount(sites.size() * 3);
        File moduleDir = OdPlugin.getInstance().getModulesDirectory();

        // collect old cache files and remove if no longer in use
        List<File> siteCacheFiles = new LinkedList<>();
        for (String location : ModuleInformation.getModuleLocations()) {
            File [] f = new File(location).listFiles(
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.matches("^([0-9]+-)?site.*\\.txt$") ||
                            name.matches("^([0-9]+-)?site.*-icons\\.zip$");
                        }
                    }
            );
            if(f != null && f.length > 0) {
                siteCacheFiles.addAll(Arrays.asList(f));
            }
        }

        for (String site: sites) {
            String printsite = site.replaceAll("%<(.*)>", "");
            getProgressMonitor().subTask(tr("Processing module list from site ''{0}''", printsite));
            String list = downloadModuleList(site, getProgressMonitor().createSubTaskMonitor(0, false));
            if (canceled) return;
            siteCacheFiles.remove(createSiteCacheFile(moduleDir, site, CacheType.PLUGIN_LIST));
            siteCacheFiles.remove(createSiteCacheFile(moduleDir, site, CacheType.ICON_LIST));
            if(list != null)
            {
                getProgressMonitor().worked(1);
                cacheModuleList(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                parseModuleListDocument(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                if (canceled) return;
            }
            downloadModuleIcons(site.replace(".txt", "")+"-icons.zip", createSiteCacheFile(moduleDir, site, CacheType.ICON_LIST), getProgressMonitor().createSubTaskMonitor(0, false));
        }
        for (File file: siteCacheFiles) /* remove old stuff or whole update process is broken */
        {
            file.delete();
        }
    }

    /**
     * Replies true if the task was canceled
     * @return
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the list of modules described in the downloaded module lists
     *
     * @return  the list of modules
     */
    public List<ModuleInformation> getAvailableModules() {
        return availableModules;
    }
}
