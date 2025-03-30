// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.imageio;

import org.openstreetmap.josm.spi.preferences.Config;

import jakarta.annotation.Nullable;

/**
 * An artifact from Maven
 * @param group The artifact group
 * @param artifact The artifact to get
 * @param version The artifact version, may be {@code null}, in which case the latest version should be used
 */
record Artifact(String group, String artifact, @Nullable String version) implements Comparable<Artifact> {

    /**
     * Get the name of the jar file that will be saved to disk
     * @return The jar name
     */
    public String getLocalName() {
        if (version() == null) {
            return group() + '.' + artifact();
        }
        return group() + '.' + artifact() + '-' + version() + ".jar";
    }

    /**
     * Get the jar URL for this artifact
     * @return The jar url
     */
    public String jar() {
        if (version == null) {
            throw new IllegalStateException("jar cannot be called when no version is set");
        }
        return getBaseUrl() + ".jar";
    }

    /**
     * Get the pom for this artifact
     * @return The pom
     */
    public String pom() {
        if (version == null) {
            throw new IllegalStateException("pom cannot be called when no version is set");
        }
        return getBaseUrl() + ".pom";
    }

    /**
     * Get the URL for additional versions of the artifact
     * @return The version URL
     */
    public String versions() {
        return baseUrl() + dotGroup() + '/' + artifact() + "/maven-metadata.xml";
    }

    private String getBaseUrl() {
        final var dotGroup = dotGroup();
        return baseUrl() + dotGroup + '/' + artifact() + '/' + version() + '/' + artifact() + '-' + version();
    }

    private String baseUrl() {
        return Config.getPref().get("imageio.nexus.url", "https://josm.openstreetmap.de/nexus/service/local/repositories/central/content/");
    }

    private String dotGroup() {
        return group().replace(".", "/");
    }

    @Override
    public int compareTo(Artifact o) {
        final int gComp = group.compareTo(o.group);
        if (gComp != 0) {
            return gComp;
        }
        final int aComp = artifact.compareTo(o.artifact);
        if (aComp != 0) {
            return aComp;
        }
        if (version == null && o.version == null) {
            return 0;
        }
        if (version == null) {
            return -1;
        } else if (o.version == null) {
            return 1;
        }
        return version.compareTo(o.version);
    }
}
