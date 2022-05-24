// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.utilsplugin2.search;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.PushbackTokenizer;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;

/**
 * Matches objects with properties in a certain range.
 * TODO: remove this copied class and make it public in JOSM core
 */
abstract class RangeMatch extends SearchCompiler.Match {

    private final long min;
    private final long max;

    RangeMatch(long min, long max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    RangeMatch(PushbackTokenizer.Range range) {
        this(range.getStart(), range.getEnd());
    }

    protected abstract Long getNumber(OsmPrimitive osm);

    protected abstract String getString();

    @Override
    public boolean match(OsmPrimitive osm) {
        Long num = getNumber(osm);
        if (num == null)
            return false;
        else
            return (num >= min) && (num <= max);
    }

    @Override
    public String toString() {
        return getString() + '=' + min + '-' + max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(max, min);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RangeMatch other = (RangeMatch) obj;
        return max == other.max
                && min == other.min;
    }
}

