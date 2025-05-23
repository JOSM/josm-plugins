// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.datasets;

import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaQueryType.NODE;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaQueryType.RELATION;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaQueryType.WAY;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaRecurseType.NODE_RELATION;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaRecurseType.RELATION_WAY;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaRecurseType.WAY_NODE;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaRecurseType.WAY_RELATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi;
import org.openstreetmap.josm.tools.ImageResource;
import org.openstreetmap.josm.tools.OsmPrimitiveImageProvider;

public abstract class SimpleDataSetHandler extends AbstractDataSetHandler {

    protected static final Projection wgs84 = OdConstants.PRJ_WGS84.getProjection();

    private final List<Tag> relevantTags = new ArrayList<>();
    private final List<Tag> forbiddenTags = new ArrayList<>();

    private final boolean relevantUnion;

    protected SimpleDataSetHandler() {
        this.relevantUnion = false;
    }

    protected SimpleDataSetHandler(String relevantTag) {
        addRelevantTag(relevantTag);
        this.relevantUnion = false;
        Tag tag;
        String[] kv = relevantTag.split("=");
        if (kv.length == 2) {
            tag = new Tag(kv[0], kv[1]);
        } else {
            tag = new Tag(relevantTag);
        }
        OsmPrimitiveImageProvider.getResource(tag.getKey(), tag.getValue(), OsmPrimitiveType.NODE)
                .map(ImageResource::getImageIcon).ifPresent(this::setMenuIcon);
    }

    protected SimpleDataSetHandler(boolean relevantUnion, String... relevantTags) {
        addRelevantTag(relevantTags);
        this.relevantUnion = relevantUnion;
    }

    protected SimpleDataSetHandler(String... relevantTags) {
        this(false, relevantTags);
    }

    protected SimpleDataSetHandler(Tag relevantTag) {
        addRelevantTag(relevantTag);
        this.relevantUnion = false;
    }

    protected SimpleDataSetHandler(boolean relevantUnion, Tag... relevantTags) {
        addRelevantTag(relevantTags);
        this.relevantUnion = relevantUnion;
    }

    protected SimpleDataSetHandler(Tag... relevantTags) {
        this(false, relevantTags);
    }

    public void addRelevantTag(String... relevantTags) {
        addTags(this.relevantTags, relevantTags);
    }

    public void addRelevantTag(Tag... relevantTags) {
        addTags(this.relevantTags, relevantTags);
    }

    public void addForbiddenTag(String... forbiddenTags) {
        addTags(this.forbiddenTags, forbiddenTags);
    }

    public void addForbiddenTag(Tag... forbiddenTags) {
        addTags(this.forbiddenTags, forbiddenTags);
    }

    private static void addTags(final List<Tag> list, String... tags) {
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null) {
                    if (tag.contains("=")) {
                        String[] tab = tag.split("=");
                        list.add(new Tag(tab[0], tab[1]));
                    } else {
                        list.add(new Tag(tag));
                    }
                }
            }
        }
    }

    private static void addTags(final List<Tag> list, Tag... tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                if (tag != null) {
                    list.add(tag);
                }
            }
        }
    }

    @Override
    public boolean equals(IPrimitive p1, IPrimitive p2) {
        for (Tag tag : this.relevantTags) {
            if (!p1.get(tag.getKey()).equals(p2.get(tag.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRelevant(IPrimitive p) {
        for (Tag tag : this.relevantTags) {
            String value = p.get(tag.getKey());
            if (value == null || (tag.getValue() != null && !tag.getValue().equals(value))) {
                return false;
            }
        }
        return !isForbidden(p);
    }

    @Override
    public boolean isForbidden(IPrimitive p) {
        for (Tag tag : this.forbiddenTags) {
            String value = p.get(tag.getKey());
            if (value != null && (tag.getValue() == null || tag.getValue().equals(value))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasForbiddenTags() {
        return !this.forbiddenTags.isEmpty();
    }

    protected final String[] getOverpassApiConditions() {
        List<String> conditions = new ArrayList<>();
        for (Tag tag : this.relevantTags) {
            conditions.add(OverpassApi.hasKey(tag.getKey(), tag.getValue()));
        }
        return conditions.toArray(new String[0]);
    }

    protected String getOverpassApiQueries(String bbox, String... conditions) {
        String[] mpconditions = new String[conditions.length+1];
        mpconditions[0] = OverpassApi.hasKey("type", "multipolygon");
        System.arraycopy(conditions, 0, mpconditions, 1, conditions.length);
        return OverpassApi.query(bbox, NODE, conditions) + "\n" + // Nodes
        OverpassApi.recurse(NODE_RELATION, RELATION_WAY, WAY_NODE) + "\n" +
        OverpassApi.query(bbox, WAY, conditions) + "\n" + // Full ways and their full relations
        OverpassApi.recurse(WAY_NODE, "nodes") + "\n" +
        OverpassApi.recurse(WAY_RELATION, RELATION_WAY, WAY_NODE) + "\n" +
        OverpassApi.query(bbox, RELATION, mpconditions) + "\n" + // Full multipolygons
        OverpassApi.recurse(RELATION_WAY, WAY_NODE);
    }

    @Override
    protected String getOverpassApiRequest(String bbox) {
        StringBuilder result = new StringBuilder();
        if (this.relevantUnion) {
            for (Tag tag : this.relevantTags) {
                result.append(getOverpassApiQueries(bbox, OverpassApi.hasKey(tag.getKey(), tag.getValue())));
            }
            result = OverpassApi.union(result);
        } else {
            result = OverpassApi.union(getOverpassApiQueries(bbox, getOverpassApiConditions()));
        }
        return result + OverpassApi.print();
    }

    @Override
    protected Collection<String> getOsmXapiRequests(String bbox) {
        StringBuilder relevantTagsSB = new StringBuilder();
        for (Tag tag : this.relevantTags) {
            relevantTagsSB.append("[").append(tag.getKey()).append("=").append(tag.getValue() == null ? "*" : tag.getValue()).append("]");
        }
        StringBuilder forbiddenTagsSB = new StringBuilder();
        for (Tag tag : this.forbiddenTags) {
            forbiddenTagsSB.append("[not(").append(tag.getKey()).append("=").append(tag.getValue() == null ? "*" : tag.getValue()).append(")]");
        }
        return Collections.singleton("*[bbox="+bbox+"]"+relevantTagsSB+forbiddenTagsSB+"[@meta]");
    }
}
