// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

/**
 * A generic data interface, mostly to make it easier for users to exhaustively use switch statements.
 */
public sealed
interface FitData
permits FileCreator, FitDeveloperDataIdMessage, FitDevice, FitEvent, FitUnknownRecord, HeartRateCadenceDistanceSpeed
{

    FitDevDataRecord devData();
}
