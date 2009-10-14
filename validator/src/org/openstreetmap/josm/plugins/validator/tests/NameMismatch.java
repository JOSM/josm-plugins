package org.openstreetmap.josm.plugins.validator.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.validator.Severity;
import org.openstreetmap.josm.plugins.validator.Test;
import org.openstreetmap.josm.plugins.validator.TestError;

/**
 * Check for missing name:* translations.
 * <p>
 * This test finds multilingual objects whose 'name' attribute is not
 * equal to any 'name:*' attribute and not a composition of some
 * 'name:*' attributes separated by ' - '.
 * <p>
 * For example, a node with name=Europe, name:de=Europa should have
 * name:en=Europe to avoid triggering this test.  An object with
 * name='Suomi - Finland' should have at least name:fi=Suomi and
 * name:sv=Finland to avoid a warning (name:et=Soome would not
 * matter).  Also, complain if an object has some name:* attribute but
 * no name.
 *
 * @author Skela
 */
public class NameMismatch extends Test {
    protected static final int NAME_MISSING = 1501;
    protected static final int NAME_TRANSLATION_MISSING = 1502;

    public NameMismatch() {
        super(tr("Missing name:* translation."),
            tr("This test finds multilingual objects whose 'name' attribute is not equal to some 'name:*' attribute and not a composition of 'name:*' attributes, e.g., Italia - Italien - Italy."));
    }

    /**
     * Report a missing translation.
     *
     * @param p The primitive whose translation is missing
     */
    private void missingTranslation(OsmPrimitive p) {
        errors.add(new TestError(this, Severity.OTHER,
            tr("A name:* translation is missing."),
            NAME_TRANSLATION_MISSING, p));
    }

    /**
     * Check a primitive for a name mismatch.
     *
     * @param p The primitive to be tested
     */
    public void check(OsmPrimitive p) {
        HashSet<String> names = new HashSet<String>();

        for (Entry<String, String> entry : p.entrySet()) {
            if (entry.getKey().startsWith("name:")) {
                String name_s = entry.getValue();
                if (name_s != null) {
                    names.add(name_s);
                }
            }
        }

        if (names.isEmpty()) return;

        String name = p.get("name");

        if (name == null) {
            errors.add(new TestError(this, Severity.OTHER,
                tr("A name is missing, even though name:* exists."),
                                     NAME_MISSING, p));
	    return;
	}

        if (names.contains(name)) return;
        /* If name is not equal to one of the name:*, it should be a
        composition of some (not necessarily all) name:* labels.
        Check if this is the case. */

        String split_names[] = name.split(" - ");
        if (split_names.length == 1) {
            /* The name is not composed of multiple parts. Complain. */
            missingTranslation(p);
            return;
        }

        /* Check that each part corresponds to a translated name:*. */
        for (String n : split_names) {
            if (!names.contains(n)) {
                missingTranslation(p);
                return;
            }
        }
    }

    /**
     * Checks a name mismatch in all primitives.
     *
     * @param selection The primitives to be tested
     */
    @Override public void visit(Collection<OsmPrimitive> selection) {
        for (OsmPrimitive p : selection)
            if (!p.isDeleted() && !p.incomplete)
                check(p);
    }
}
