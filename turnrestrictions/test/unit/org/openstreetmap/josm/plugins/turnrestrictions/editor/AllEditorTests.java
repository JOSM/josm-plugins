// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import org.junit.platform.suite.api.SelectClasses;

import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    JosmSelectionListModelTest.class,
    TurnRestrictionEditorModelUnitTest.class,
    TurnRestrictionLegEditorUnitTest.class,
    TurnRestrictionTypeRendererTest.class,
    TurnRestrictionTypeTest.class,
    ExceptValueModelTest.class
})
public class AllEditorTests {}
