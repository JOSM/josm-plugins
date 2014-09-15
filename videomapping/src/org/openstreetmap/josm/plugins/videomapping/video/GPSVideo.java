package org.openstreetmap.josm.plugins.videomapping.video;
import java.io.File;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.plugins.videomapping.VideoPositionLayer;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;

// a specific synced video
public class GPSVideo extends Video {
	private static final String SYNC_KEY = "synced";
	public JComponent SyncComponent;
	private WayPoint syncWayPoint;
	private long syncVideoTime;
	private Date start;
	private Date end;
	public WayPoint firstWayPoint;
	public WayPoint lastWayPoint;
	private VideoPositionLayer videoPositionLayer;
	
	public GPSVideo(File filename, String id, MediaPlayerFactory mediaPlayerFactory) {
		super(filename,id,mediaPlayerFactory);
	}
	
	public GPSVideo(Video video) {
		super(video.filename, video.id, video.mediaPlayerFactory);
		this.player = video.player;
	}
	
	//calculates attributes basing upon the current position
	public void doSync(VideoPositionLayer layer) {
		this.videoPositionLayer = layer;
		if (isSynced())
			removeSyncedWayPoints();
		syncWayPoint = layer.getCurrentWayPoint();
		syncVideoTime = getCurrentTime();
		//calc now, to avoid calculations on every click
		if (syncWayPoint != null) {
    		start = new Date(syncWayPoint.getTime().getTime()-syncVideoTime);
    		end = new Date(start.getTime()+player.getLength());
		} else {
		    start = null;
		    end = null;
		}
		firstWayPoint = getFirstGPS();
		lastWayPoint = getLastGPS();
		markSyncedWayPoints();
		Main.map.mapView.repaint();
	}

	//make sure we don't leave the GPS track
	private WayPoint getFirstGPS() {
		if (start == null || start.before(videoPositionLayer.getFirstWayPoint().getTime())) {
			return videoPositionLayer.getFirstWayPoint();
		} else {
			return videoPositionLayer.getWayPointBefore(start);
		}
	}
	
	//make sure we don't leave the GPS track
	private WayPoint getLastGPS() {
		if (end == null || end.after(videoPositionLayer.getLastWayPoint().getTime())) {
			return videoPositionLayer.getLastWayPoint();
		} else {
			return videoPositionLayer.getWayPointBefore(end);
		}
	}
	
	private void removeSyncedWayPoints() {
		List <WayPoint> track = videoPositionLayer.getTrack();
		int start = track.indexOf(firstWayPoint);
		int end = track.indexOf(lastWayPoint);
        if (0 <= start && start <= end && end < track.size()) {
    		for (WayPoint n : track.subList(start, end)) {
    			n.attr.keySet().remove(SYNC_KEY);
    		}
        }
	}

	private void markSyncedWayPoints() {
		List <WayPoint> track = videoPositionLayer.getTrack();
		int start = track.indexOf(firstWayPoint);
		int end = track.indexOf(lastWayPoint);
		if (0 <= start && start <= end && end < track.size()) {
    		for (WayPoint n : track.subList(start, end)) {
    			n.attr.put(SYNC_KEY, id);
    		}
		}
	}

	public boolean isSynced() {
		return syncWayPoint != null;
	}

	//if synced jump in video to this GPS timecode 
	public void jumpTo(Date GPSTime) {
		if((GPSTime.after(firstWayPoint.getTime())&(GPSTime.before(lastWayPoint.getTime())))) {
			long diff = GPSTime.getTime()-start.getTime();
			player.setTime(diff);
			System.out.println(diff);
		}
	}
	
	public WayPoint getCurrentWayPoint() {
		if (isSynced()) {
			long videotime = player.getTime();
			Date gpstime = new Date(start.getTime()+videotime);
			return videoPositionLayer.interpolate(gpstime);
		}
		return null;
	}
}
