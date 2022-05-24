// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.search.PushbackTokenizer;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.CoreSimpleMatchFactory;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.SimpleMatchFactory;
import org.openstreetmap.josm.data.osm.search.SearchParseError;

/**
 * Extension of the {@link CoreSimpleMatchFactory}
 */
public class UtilsSimpleMatchFactory implements SimpleMatchFactory {

    private static final Collection<String> keywords =
            Collections.unmodifiableCollection(Arrays.asList("usedinways", "usedinrelations", "parents", "children"));

    @Override
    public Collection<String> getKeywords() {
        return keywords;
    }

    @Override
    public SearchCompiler.Match get(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer)
            throws SearchParseError {
        if (tokenizer == null) {
            throw new SearchParseError("<html>" + tr("Expecting {0} after {1}", "<code>:</code>", "<i>" + keyword + "</i>") + "</html>");
        }
        switch (keyword) {
        case "usedinways":
            return new UsedInWaysMatch(tokenizer);
        case "usedinrelations":
            return new UsedInRelationsMatch(tokenizer);
        case "parents":
            return new ParentsMatch(tokenizer);
        case "children":
            return new ChildrenMatch(tokenizer);
        default:
            throw new IllegalStateException("Not expecting keyword " + keyword);
        }
    }
}
