package nanolog;

import java.text.ParseException;
import java.util.*;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.*;
import org.openstreetmap.josm.tools.Geometry;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.date.PrimaryDateParser;

/**
 * A class that establishes correlation between GPS trace and NanoLog. Mostly copied from
 * {@link org.openstreetmap.josm.gui.layer.geoimage.CorrelateGpxWithImages}, thus licensed GPL.
 *
 * @author zverik
 */
public class Correlator {

    /**
     * Matches entries to GPX so most points are on the trace.
     */
    public static long crudeMatch( List<NanoLogEntry> entries, GpxData data ) {
        List<NanoLogEntry> sortedEntries = new ArrayList<NanoLogEntry>(entries);
        PrimaryDateParser dateParser = new PrimaryDateParser();
        Collections.sort(sortedEntries);
        long firstExifDate = sortedEntries.get(0).getTime().getTime();
        long firstGPXDate = -1;
        outer:
        for( GpxTrack trk : data.tracks ) {
            for( GpxTrackSegment segment : trk.getSegments() ) {
                for( WayPoint curWp : segment.getWayPoints() ) {
                    String curDateWpStr = (String)curWp.attr.get("time");
                    if( curDateWpStr == null ) {
                        continue;
                    }

                    try {
                        firstGPXDate = dateParser.parse(curDateWpStr).getTime();
                        break outer;
                    } catch( Exception e ) {
                        Main.warn(e);
                    }
                }
            }
        }

        // No GPX timestamps found, exit
        if( firstGPXDate < 0 ) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The selected GPX track does not contain timestamps. Please select another one."),
                    tr("GPX Track has no time information"), JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        return firstExifDate - firstGPXDate;
    }

    public static void revertPos( List<NanoLogEntry> entries ) {
        for( NanoLogEntry entry : entries ) {
            entry.setPos(entry.getBasePos());
        }
    }

    /**
     * Offset is in 1/1000 of a second.
     * @param entries
     * @param data
     * @param offset
     */
    public static void correlate( List<NanoLogEntry> entries, GpxData data, long offset ) {
        List<NanoLogEntry> sortedEntries = new ArrayList<NanoLogEntry>(entries);
        int ret = 0;
        PrimaryDateParser dateParser = new PrimaryDateParser();
        Collections.sort(sortedEntries);
        for( GpxTrack track : data.tracks ) {
            for( GpxTrackSegment segment : track.getSegments() ) {
                long prevWpTime = 0;
                WayPoint prevWp = null;

                for( WayPoint curWp : segment.getWayPoints() ) {

                    String curWpTimeStr = (String)curWp.attr.get("time");
                    if( curWpTimeStr != null ) {
                        try {
                            long curWpTime = dateParser.parse(curWpTimeStr).getTime() + offset;
                            ret += matchPoints(sortedEntries, prevWp, prevWpTime, curWp, curWpTime, offset);

                            prevWp = curWp;
                            prevWpTime = curWpTime;

                        } catch( ParseException e ) {
                            Main.error("Error while parsing date \"" + curWpTimeStr + '"');
                            Main.error(e);
                            prevWp = null;
                            prevWpTime = 0;
                        }
                    } else {
                        prevWp = null;
                        prevWpTime = 0;
                    }
                }
            }
        }
    }

    private static int matchPoints( List<NanoLogEntry> entries, WayPoint prevWp, long prevWpTime,
            WayPoint curWp, long curWpTime, long offset ) {
        // Time between the track point and the previous one, 5 sec if first point, i.e. photos take
        // 5 sec before the first track point can be assumed to be take at the starting position
        long interval = prevWpTime > 0 ? Math.abs(curWpTime - prevWpTime) : 5 * 1000;
        int ret = 0;

        // i is the index of the timewise last photo that has the same or earlier EXIF time
        int i = getLastIndexOfListBefore(entries, curWpTime);

        // no photos match
        if( i < 0 )
            return 0;

        Integer direction = null;
        if( prevWp != null ) {
            direction = Long.valueOf(Math.round(180.0 / Math.PI * prevWp.getCoor().heading(curWp.getCoor()))).intValue();
        }

        // First trackpoint, then interval is set to five seconds, i.e. photos up to five seconds
        // before the first point will be geotagged with the starting point
        if( prevWpTime == 0 || curWpTime <= prevWpTime ) {
            while( true ) {
                if( i < 0 ) {
                    break;
                }
                final NanoLogEntry curImg = entries.get(i);
                long time = curImg.getTime().getTime();
                if( time > curWpTime || time < curWpTime - interval ) {
                    break;
                }
                if( curImg.getPos() == null ) {
                    curImg.setPos(curWp.getCoor());
                    curImg.setDirection(direction);
                    ret++;
                }
                i--;
            }
            return ret;
        }

        // This code gives a simple linear interpolation of the coordinates between current and
        // previous track point assuming a constant speed in between
        while( true ) {
            if( i < 0 ) {
                break;
            }
            NanoLogEntry curImg = entries.get(i);
            long imgTime = curImg.getTime().getTime();
            if( imgTime < prevWpTime ) {
                break;
            }

            if( curImg.getPos() == null && prevWp != null ) {
                // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless variable
                double timeDiff = (double)(imgTime - prevWpTime) / interval;
                curImg.setPos(prevWp.getCoor().interpolate(curWp.getCoor(), timeDiff));
                curImg.setDirection(direction);

                ret++;
            }
            i--;
        }
        return ret;
    }

    private static int getLastIndexOfListBefore(List<NanoLogEntry> entries, long searchedTime) {
        int lstSize= entries.size();

        // No photos or the first photo taken is later than the search period
        if(lstSize == 0 || searchedTime < entries.get(0).getTime().getTime())
            return -1;

        // The search period is later than the last photo
        if (searchedTime > entries.get(lstSize - 1).getTime().getTime())
            return lstSize-1;

        // The searched index is somewhere in the middle, do a binary search from the beginning
        int curIndex= 0;
        int startIndex= 0;
        int endIndex= lstSize-1;
        while (endIndex - startIndex > 1) {
            curIndex= (endIndex + startIndex) / 2;
            if (searchedTime > entries.get(curIndex).getTime().getTime()) {
                startIndex= curIndex;
            } else {
                endIndex= curIndex;
            }
        }
        if (searchedTime < entries.get(endIndex).getTime().getTime())
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize-1)) && (entries.get(endIndex).getTime().getTime()
                == entries.get(endIndex + 1).getTime().getTime())) {
            endIndex++;
        }
        return endIndex;
    }

    /**
     * Returns date of a potential point on GPX track (which can be between points).
     */
    public static long getGpxDate( GpxData data, LatLon pos ) {
        EastNorth en = Main.getProjection().latlon2eastNorth(pos);
        PrimaryDateParser dateParser = new PrimaryDateParser();
        for( GpxTrack track : data.tracks ) {
            for( GpxTrackSegment segment : track.getSegments() ) {
                long prevWpTime = 0;
                WayPoint prevWp = null;
                for( WayPoint curWp : segment.getWayPoints() ) {
                    String curWpTimeStr = (String)curWp.attr.get("time");
                    if( curWpTimeStr != null ) {
                        try {
                            long curWpTime = dateParser.parse(curWpTimeStr).getTime();
                            if( prevWp != null ) {
                                EastNorth c1 = Main.getProjection().latlon2eastNorth(prevWp.getCoor());
                                EastNorth c2 = Main.getProjection().latlon2eastNorth(curWp.getCoor());
                                if( !c1.equals(c2) ) {
                                    EastNorth middle = Geometry.getSegmentAltituteIntersection(c1, c2, en);
                                    if( middle != null && en.distance(middle) < 1 ) {
                                        // found our point, no further search is neccessary
                                        double prop = c1.east() == c2.east()
                                                ? (middle.north() - c1.north()) / (c2.north() - c1.north())
                                                : (middle.east() - c1.east()) / (c2.east() - c1.east());
                                        if( prop >= 0 && prop <= 1 ) {
                                            return Math.round(prevWpTime + prop * (curWpTime - prevWpTime));
                                        }
                                    }
                                }
                            }

                            prevWp = curWp;
                            prevWpTime = curWpTime;
                        } catch( ParseException e ) {
                            Main.error("Error while parsing date \"" + curWpTimeStr + '"');
                            Main.error(e);
                            prevWp = null;
                            prevWpTime = 0;
                        }
                    } else {
                        prevWp = null;
                        prevWpTime = 0;
                    }
                }
            }
        }
        return 0;
    }
}
