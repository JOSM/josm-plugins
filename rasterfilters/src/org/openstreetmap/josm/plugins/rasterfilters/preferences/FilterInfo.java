package org.openstreetmap.josm.plugins.rasterfilters.preferences;

import java.util.Objects;

import javax.json.JsonObject;

class FilterInfo {
    private String name;
    private String description;
    private JsonObject meta;
    private boolean needToDownload;
    private String owner;

    FilterInfo(String name, String description, JsonObject meta, boolean needToDownload) {
        this.setName(name);
        this.setDescription(description);
        this.meta = meta;
        this.setNeedToDownload(needToDownload);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    public boolean isNeedToDownload() {
        return needToDownload;
    }

    public void setNeedToDownload(boolean needToDownload) {
        this.needToDownload = needToDownload;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "name: " + getName() + "\nDescription: " + getDescription() + "\nMeta: " + getMeta();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, meta, description);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof FilterInfo) && name.equals(((FilterInfo) o).getName())
                && meta.equals(((FilterInfo) o).getMeta()) && description.equals(((FilterInfo) o).getDescription());
    }
}