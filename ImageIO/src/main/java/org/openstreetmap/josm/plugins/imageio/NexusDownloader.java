// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.imageio;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Download files from JOSM nexus
 */
final class NexusDownloader {
    private record ResponseHeader(String etag, long size, long lastModified) {}
    private NexusDownloader() {
        // Hide constructor
    }

    private static final CacheAccess<String, ResponseHeader> CACHED_RESPONSES_HEAD = JCSCacheManager.getCache("imageio.maven.head",
            100, 0, null);
    private static final CacheAccess<String, String> CACHED_RESPONSES_GET = JCSCacheManager.getCache("imageio.maven.get",
            100, 0, null);

    static {
        CACHED_RESPONSES_GET.getDefaultElementAttributes()
                .setMaxLife(TimeUnit.SECONDS.toMillis(Config.getPref().getLong("mirror.maxtime", TimeUnit.DAYS.toSeconds(7))));
    }

    private static Path cacheDirectory;

    /**
     * Set the download directory for the files
     * @param cacheDirectory The directory to use
     */
    static void setDownloadDirectory(File cacheDirectory) {
        if (NexusDownloader.cacheDirectory != null) {
            throw new IllegalStateException("setDownloadDirectory can only be called once!");
        }
        NexusDownloader.cacheDirectory = cacheDirectory.getAbsoluteFile().toPath();
    }

    /**
     * Download a maven artifact
     * @param artifact The artifact to download
     * @return A set of jars to add to the classpath
     */
    static Set<String> download(Artifact artifact) {
        try {
            if (artifact.version() == null) {
                artifact = getLatest(artifact);
            }
            return new TreeSet<>(getDependencies(artifact))
                    .stream().map(NexusDownloader::realDownload)
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static String realDownload(Artifact artifact) {
        final Path output;
        try {
            if (artifact.version() == null) {
                artifact = getLatest(artifact);
            }
            final var baseName = artifact.group() + '.' + artifact.artifact();
            output = NexusDownloader.cacheDirectory.resolve(artifact.getLocalName());

            // Check if the file is already downloaded
            final var jarUrl = URI.create(artifact.jar()).toURL();
            final var head = CACHED_RESPONSES_HEAD.get(artifact.jar(), () -> {
                final var client = HttpClient.create(jarUrl, "HEAD");
                try {
                    final var response = client.connect();
                    return new ResponseHeader(response.getHeaderField("ETag"), response.getContentLength(), response.getLastModified());
                } catch (IOException e) {
                    // We cannot put null in the cache
                    throw new UncheckedIOException(e);
                } finally {
                    client.disconnect();
                }
            });
            final var alreadyDownloaded = Files.exists(output) && head.size() == Files.size(output)
                    && Files.getLastModifiedTime(output).toMillis() == head.lastModified()
                    && checkETag(output, head.etag());

            if (!alreadyDownloaded) {
                final var client = HttpClient.create(jarUrl);
                final var response = client.connect();
                try (var writer = Files.newOutputStream(output)) {
                    var is = response.getContent();
                    is.transferTo(writer);
                    Files.setLastModifiedTime(output, FileTime.fromMillis(response.getLastModified()));
                }
                client.disconnect();
            }
            // Now we can delete "old" versions of the artifact
            try (var files = Files.walk(NexusDownloader.cacheDirectory)) {
                    files.filter(path -> path.startsWith(baseName) && !path.equals(output))
                        .filter(Files::isWritable)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } catch (IOException exception) {
            throw new JosmRuntimeException(exception);
        }
        return output.toString();
    }

    /**
     * If the etag matches a well-known hash pattern, check that the file content matches
     * @param path The path to hash
     * @param etag The ETag to check the path against
     * @return {@code true} if the ETag is not a well-known hash pattern <i>or</i> the file matches the expected hash.
     */
    private static boolean checkETag(Path path, String etag) {
        final MessageDigest md;
        final String expected;
        if (etag.startsWith("\"") && etag.endsWith("\"")) {
            etag = etag.substring(1, etag.length() - 1);
        }
        if (etag.startsWith("{SHA1{")) {
            try {
                md = MessageDigest.getInstance("SHA-1");
                expected = etag.substring("{SHA1{".length(), etag.length() - 2);
            } catch (NoSuchAlgorithmException e) {
                // Realistically, we shouldn't hit this. But if we do, maybe SHA1 has been removed, and hopefully
                // etags use something else.
                throw new JosmRuntimeException(e);
            }
        } else {
            return true;
        }
        try (var is = Files.newInputStream(path);
             var dis = new DigestInputStream(is, md)) {
            final var tByte = new byte[4096];
            while (dis.readNBytes(tByte, 0, tByte.length) > 0) {
                // We need to read the bytes...
            }
        } catch (IOException e) {
            Logging.error(e);
            return false; // Assume that the file is corrupted
        }
        return expected.equals(Utils.toHexString(md.digest()));
    }

    /**
     * Get the dependencies from a pom
     * @param artifact The artifact to use
     * @return The jars to add to the classpath
     * @throws MalformedURLException if the pom string is not a valid URL
     * @throws IOException if there is a read error
     */
    private static Set<Artifact> getDependencies(Artifact artifact) throws IOException {
        final var url = URI.create(artifact.pom()).toURL();
        final var text = CACHED_RESPONSES_GET.get(artifact.pom(), () -> {
            final var client = HttpClient.create(url);
            try {
                return client.connect().fetchContent();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                client.disconnect();
            }
        });
        try (var is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            var handler = new MavenDependenciesHandler();
            XmlUtils.newSafeSAXParser().parse(is, handler);
            final var dependencies = new TreeSet<>(handler.dependencies);
            dependencies.add(artifact);
            for (var dependency : handler.dependencies) {
                if (dependency.version() == null) {
                    dependency = getLatest(dependency);
                }
                dependencies.addAll(getDependencies(dependency));
            }
            return dependencies;
        } catch (ParserConfigurationException | SAXException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static Artifact getLatest(Artifact artifact) throws IOException {
        final var url = URI.create(artifact.versions()).toURL();
        final var text = CACHED_RESPONSES_GET.get(artifact.versions(), () -> {
            final var client = HttpClient.create(url);
            try {
                return client.connect().fetchContent();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                client.disconnect();
            }
        });
        try (var is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            final var handler = new MavenMetadataHandler();
            XmlUtils.newSafeSAXParser().parse(is, handler);
            final var version = Utils.firstNotEmptyString(handler.release, handler.latest,
                    handler.versions.stream()
                            .filter(v -> !v.contains("rc") && !v.contains("b"))
                            .findFirst().orElse(artifact.version()));
            return new Artifact(artifact.group(), artifact.artifact(), version);
        } catch (ParserConfigurationException | SAXException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static class MavenMetadataHandler extends DefaultHandler {
        private final StringBuilder characters = new StringBuilder();
        private final List<String> versions = new ArrayList<>();
        private String latest;
        private String release;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            this.characters.setLength(0);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("latest".equals(qName)) {
                this.latest = this.characters.toString();
            } else if ("release".equals(qName)) {
                this.release = this.characters.toString();
            } else if ("version".equals(qName)) {
                this.versions.add(this.characters.toString());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            this.characters.append(ch, start, length);
        }
    }

    private static class MavenDependenciesHandler extends DefaultHandler {
        private final List<Artifact> dependencies = new ArrayList<>();
        private final StringBuilder characters = new StringBuilder();
        private Artifact parent;
        private String groupId;
        private String artifactId;
        private String version;
        private String type;
        private String scope;
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            this.characters.setLength(0);
            if ("dependency".equals(qName) || "parent".equals(qName)) {
                this.groupId = null;
                this.artifactId = null;
                this.version = null;
                this.type = null;
                this.scope = null;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if ("parent".equals(qName) && groupId != null && artifactId != null) {
                this.parent = new Artifact(groupId, artifactId, version);
            } else if ("dependency".equals(qName) && groupId != null && artifactId != null) {
                if (!"test-jar".equals(this.type) && !"test".equals(this.scope) && !"provided".equals(this.scope)) {
                    dependencies.add(new Artifact(groupId, artifactId, version));
                }
            } else if ("groupId".equals(qName)) {
                this.groupId = this.characters.toString();
                if ("${project.groupId}".equals(this.groupId)) {
                    this.groupId = this.parent.group();
                }
            } else if ("artifactId".equals(qName)) {
                this.artifactId = this.characters.toString();
            } else if ("version".equals(qName)) {
                this.version = this.characters.toString();
            } else if ("type".equals(qName)) {
                this.type = this.characters.toString();
            } else if ("scope".equals(qName)) {
                this.scope = this.characters.toString();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            this.characters.append(ch, start, length);
        }
    }
}
