// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.directdownload;

import java.util.ArrayList;
import java.util.List;

class UserTrack {
    public String id;
    public String filename;
    public String description;
    public final List<String> tags = new ArrayList<>();
    public String datetime;
}
