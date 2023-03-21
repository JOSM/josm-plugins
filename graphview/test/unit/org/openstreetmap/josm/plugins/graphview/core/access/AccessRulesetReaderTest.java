// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.graphview.core.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

class AccessRulesetReaderTest {

    @Test
    void testReadAccessRulesetValidClasses() throws IOException {

        InputStream is = new FileInputStream(TestUtils.getTestDataRoot()+"accessRuleset_valid.xml");
        AccessRuleset ruleset = AccessRulesetReader.readAccessRuleset(is);
        assertNotNull(ruleset);


        assertEquals("vehicle", ruleset.getAccessHierarchyAncestors("vehicle").get(0));

        assertEquals("motor_vehicle", ruleset.getAccessHierarchyAncestors("motor_vehicle").get(0));
        assertEquals("vehicle", ruleset.getAccessHierarchyAncestors("motor_vehicle").get(1));

        assertEquals("bus", ruleset.getAccessHierarchyAncestors("bus").get(0));
        assertEquals("motor_vehicle", ruleset.getAccessHierarchyAncestors("bus").get(1));
        assertEquals("vehicle", ruleset.getAccessHierarchyAncestors("bus").get(2));

        assertEquals("bicycle", ruleset.getAccessHierarchyAncestors("bicycle").get(0));
        assertEquals("vehicle", ruleset.getAccessHierarchyAncestors("bicycle").get(1));

        assertFalse(ruleset.getAccessHierarchyAncestors("bus").contains("bicycle"));

        assertSame(ruleset.getAccessHierarchyAncestors("boat").size(), 0);

    }

    @Test
    void testReadAccessRulesetValidBasetags() throws IOException {

        InputStream is = new FileInputStream(TestUtils.getTestDataRoot()+"accessRuleset_valid.xml");
        AccessRuleset ruleset = AccessRulesetReader.readAccessRuleset(is);
        assertNotNull(ruleset);

        assertSame(2, ruleset.getBaseTags().size());

        assertTrue(ruleset.getBaseTags().contains(new Tag("highway", "residential")));
        assertTrue(ruleset.getBaseTags().contains(new Tag("highway", "cycleway")));
        assertFalse(ruleset.getBaseTags().contains(new Tag("building", "residential")));
        assertFalse(ruleset.getBaseTags().contains(new Tag("highway", "stop")));

    }

    @Test
    void testReadAccessRulesetValidImplications() throws IOException {

        InputStream is = new FileInputStream(TestUtils.getTestDataRoot()+"accessRuleset_valid.xml");
        AccessRuleset ruleset = AccessRulesetReader.readAccessRuleset(is);
        assertNotNull(ruleset);

        List<Implication> implications = ruleset.getImplications();

        assertSame(3, implications.size());

        TagGroup[] tagGroups = new TagGroup[4];
        tagGroups[0] = createTagGroup(new Tag("highway", "cycleway"));
        tagGroups[1] = createTagGroup(new Tag("highway", "steps"));
        tagGroups[2] = createTagGroup(new Tag("highway", "steps"), new Tag("escalator", "yes"));
        tagGroups[3] = createTagGroup(new Tag("disused", "yes"), new Tag("construction", "no"));

        for (Implication implication : implications) {
            for (int i = 0; i < tagGroups.length; i++) {
                tagGroups[i] = implication.apply(tagGroups[i]);
            }
        }

        assertSame(2, tagGroups[0].size());
        assertTrue(tagGroups[0].contains(new Tag("bicycle", "designated")));

        assertSame(2, tagGroups[1].size());
        assertTrue(tagGroups[1].contains(new Tag("normal_steps", "yes")));

        assertSame(2, tagGroups[2].size());
        assertFalse(tagGroups[2].contains(new Tag("normal_steps", "yes")));

        assertSame(3, tagGroups[3].size());
        assertTrue(tagGroups[3].contains(new Tag("usable", "no")));
    }

    private static TagGroup createTagGroup(Tag... tags) {
        Map<String, String> tagMap = new HashMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.key, tag.value);
        }
        return new MapBasedTagGroup(tagMap);
    }
}
