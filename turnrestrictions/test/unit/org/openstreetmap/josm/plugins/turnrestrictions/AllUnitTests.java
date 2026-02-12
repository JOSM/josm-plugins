// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.openstreetmap.josm.plugins.turnrestrictions.editor.AllEditorTests;

@Suite
@SelectClasses({
    AllEditorTests.class,
    TurnRestrictionBuilderTest.class
})
public class AllUnitTests {}
