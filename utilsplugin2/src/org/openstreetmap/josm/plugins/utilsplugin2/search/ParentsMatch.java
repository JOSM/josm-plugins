// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.search.PushbackTokenizer;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Matches objects with a number of parent primitives in the given range.
 */
public class ParentsMatch extends RangeMatch {
    public ParentsMatch(PushbackTokenizer.Range range) {
        super(range);
    }

    public ParentsMatch(PushbackTokenizer tokenizer) throws SearchCompiler.ParseError {
        this(tokenizer.readRange(tr("Range of parent primitives count")));
    }

    @Override
    protected Long getNumber(OsmPrimitive osm) {
        return Long.valueOf(osm.getReferrers().size());
    }

    @Override
    protected String getString() {
        return "parents";
    }
}
