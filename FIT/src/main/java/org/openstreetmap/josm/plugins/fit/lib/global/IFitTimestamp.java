// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import java.time.Instant;

public sealed

interface IFitTimestamp<T>
permits FitEvent, HeartRateCadenceDistanceSpeed
{

    Instant timestamp();

    T withTimestamp(Instant timestamp);
}
