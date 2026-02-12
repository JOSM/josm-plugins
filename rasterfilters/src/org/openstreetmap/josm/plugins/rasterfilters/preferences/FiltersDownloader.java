package org.openstreetmap.josm.plugins.rasterfilters.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;

/**
 * This class is responsible for downloading jars which contains
 * filters implementations, for loading meta from the
 * <a href="https://josm.openstreetmap.de/wiki/ImageFilters">filter's page</a>.
 * Also it stores the downloaded information for creating filter's GUI and etc.
 *
 * @author Nipel-Crumple
 */
public class FiltersDownloader implements ActionListener {
    private static volatile File pluginDir;

    public static Set<JsonObject> filtersMeta = new HashSet<>();
    public static Set<String> filterTitles = new TreeSet<>();
    public static List<URL> binariesLocalUrls = new ArrayList<>();
    public static ClassLoader loader;
    public static Map<String, String> urlsMap = new HashMap<>();

    private static Set<JsonObject> filtersMetaToLoad = new HashSet<>();
    static List<FilterInfo> filtersInfoList = new ArrayList<>();

    public static List<FilterInfo> downloadFiltersInfoList() {
        try {
            Document doc = Jsoup.connect(HelpUtil.getWikiBaseHelpUrl() + "/ImageFilters").get();
            for (Element element : doc.getElementsByTag("tr")) {
                Elements elems = element.getElementsByTag("td");
                if (!elems.isEmpty()) {
                    String name = elems.get(0).text();
                    String owner = elems.get(1).text();
                    String description = elems.get(2).text();
                    String link = elems.get(0).getElementsByTag("a").attr("href");

                    JsonObject meta = loadMeta(link);
                    if (meta != null) {
                        boolean needToLoad = Config.getPref().getBoolean("rasterfilters." + meta.getString("name"));
                        if (needToLoad) {
                            JsonArray binaries = meta.getJsonArray("binaries");
                            filterTitles.add(meta.getString("title"));
                            for (int i = 0; i < binaries.size(); i++) {
                                filtersMetaToLoad.add(meta);
                                loadBinaryToFile(binaries.getString(i));
                            }
                        }
                        FilterInfo newFilterInfo = new FilterInfo(name, description, meta, needToLoad);
                        newFilterInfo.setOwner(owner);

                        if (!filtersInfoList.contains(newFilterInfo)) {
                            filtersInfoList.add(newFilterInfo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Logging.error(e);
        }

        loadBinariesFromMeta(filtersMetaToLoad);

        return filtersInfoList;
    }

    public static JsonObject loadMeta(String link) {

        Pattern p = Pattern.compile("ImageFilters/\\w.*");
        Matcher m = p.matcher(link);

        if (m.find()) {
            link = link.substring(m.start());
        }

        JsonObject meta = null;

        try {
            Document doc = Jsoup.connect(HelpUtil.getWikiBaseHelpUrl() + "/" + link).get();
            try (JsonReader reader = Json.createReader(new StringReader(doc.getElementsByTag("pre").first().text()))) {
                meta = reader.readObject();
            }
        } catch (IOException e) {
            Logging.error(e);
        }

        if (meta != null) {
            filtersMeta.add(meta);
        }

        return meta;
    }

    public static void initFilters() {
        try (BufferedReader br = Files.newBufferedReader(new File(pluginDir, "urls.map").toPath(), StandardCharsets.UTF_8)) {
            String temp;
            while ((temp = br.readLine()) != null) {
                String[] mapEntry = temp.split("\\t");
                File fileUrl = new File(mapEntry[1]);
                if (fileUrl.exists()) {
                    try {
                        URL url = new URL("jar", "", fileUrl.toURI().toURL() + "!/");
                        Logging.debug("binaryUrl: " + url.toString());
                        binariesLocalUrls.add(url);
                    } catch (MalformedURLException e) {
                        Logging.debug("Initializing filters with unknown protocol. \n" + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            Logging.error(e);
        }

        loader = new URLClassLoader(
                binariesLocalUrls.toArray(new URL[binariesLocalUrls.size()]),
                FiltersDownloader.class.getClassLoader());
    }

    public static void destroyFilters() {
        filterTitles.clear();
        binariesLocalUrls.clear();
        FiltersDownloader.filtersMeta.clear();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (FilterInfo temp : filtersInfoList) {
            if (temp.isNeedToDownload()) {
                filtersMetaToLoad.add(temp.getMeta());
                filterTitles.add(temp.getMeta().getString("title"));
            } else {
                filterTitles.remove(temp.getMeta().getString("title"));
            }
        }

        loadBinariesFromMeta(filtersMetaToLoad);
        filtersMetaToLoad.clear();
    }

    public static void loadBinariesFromMeta(Set<JsonObject> metaList) {
        File file = new File(pluginDir, "urls.map");
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (JsonObject temp : metaList) {
                JsonArray binaries = temp.getJsonArray("binaries");
                for (int i = 0; i < binaries.size(); i++) {
                    String localFile = loadBinaryToFile(binaries.getString(i));
                    try {
                        writer.append(binaries.getString(i));
                        writer.append("\t");
                        writer.append(localFile);
                        writer.append("\n");
                    } catch (IOException e) {
                        Logging.error(e);
                    }
                }
            }
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    public static void setPluginDir(File dir) {
        pluginDir = dir;
    }

    public static String loadBinaryToFile(String fromUrl) {
        Pattern p = Pattern.compile("\\w.*/");
        Matcher m = p.matcher(fromUrl);

        String localFile = null;
        if (m.find() && pluginDir.exists()) {
            localFile = fromUrl.substring(m.end());
        }

        try {
            File file = new File(pluginDir.getAbsolutePath(), localFile);
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                try (InputStream in = HttpClient.create(new URL(fromUrl)).connect().getContent()) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return localFile;
            }
        } catch (IOException e) {
            Logging.error(e);
        }

        return null;
    }
}
