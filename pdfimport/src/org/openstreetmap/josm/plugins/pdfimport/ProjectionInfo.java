// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pdfimport;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.projection.SingleProjectionChoice;

public final class ProjectionInfo {
    private static Map<String, ProjectionChoice> allCodesPC = new HashMap<>();

    static {
        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            for (String code : pc.allCodes()) {
                allCodesPC.put(code, pc);
            }
        }
    }

    private ProjectionInfo() {
        // Hide default constructor for utilities classes
    }

    public static Projection getProjectionByCode(String code) {
        try {
            ProjectionChoice pc = new SingleProjectionChoice(code.toString(), code.toString(), code);
            Projection p = pc.getProjection();
            return p;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
