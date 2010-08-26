package wmsplugin;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.io.CacheFiles;

import wmsplugin.GeorefImage.State;

abstract public class Grabber implements Runnable {
	protected final MapView mv;
	protected final WMSLayer layer;
	protected final CacheFiles cache;

	protected ProjectionBounds b;
	protected Projection proj;
	protected double pixelPerDegree;
	protected WMSRequest request;
	protected volatile boolean canceled;

	Grabber(MapView mv, WMSLayer layer, CacheFiles cache) {
		this.mv = mv;
		this.layer = layer;
		this.cache = cache;
	}

	private void updateState(WMSRequest request) {
		b = new ProjectionBounds(
				layer.getEastNorth(request.getXIndex(), request.getYIndex()),
				layer.getEastNorth(request.getXIndex() + 1, request.getYIndex() + 1));
		if (b.min != null && b.max != null && WMSPlugin.PROP_OVERLAP.get()) {
			double eastSize =  b.max.east() - b.min.east();
			double northSize =  b.max.north() - b.min.north();

			double eastCoef = WMSPlugin.PROP_OVERLAP_EAST.get() / 100.0;
			double northCoef = WMSPlugin.PROP_OVERLAP_NORTH.get() / 100.0;

			this.b = new ProjectionBounds( new EastNorth(b.min.east(),
					b.min.north()),
					new EastNorth(b.max.east() + eastCoef * eastSize,
							b.max.north() + northCoef * northSize));
		}

		this.proj = Main.proj;
		this.pixelPerDegree = request.getPixelPerDegree();
		this.request = request;
	}

	abstract void fetch(WMSRequest request) throws Exception; // the image fetch code

	int width(){
		return layer.getImageWidth(request.getXIndex());
	}
	int height(){
		return layer.getImageHeight(request.getYIndex());
	}

	@Override
	public void run() {
		while (true) {
			if (canceled) {
				return;
			}
			WMSRequest request = layer.getRequest();
			if (request == null) {
				return;
			}
			updateState(request);
			if(!loadFromCache(request)){
				attempt(request);
			}
			if (request.getState() != null) {
				layer.finishRequest(request);
				mv.repaint();
			}
		}
	}

	protected void attempt(WMSRequest request){ // try to fetch the image
		int maxTries = 5; // n tries for every image
		for (int i = 1; i <= maxTries; i++) {
			if (canceled) {
				return;
			}
			try {
				if (!layer.requestIsValid(request)) {
					return;
				}
				fetch(request);
				break; // break out of the retry loop
			} catch (Exception e) {
				try { // sleep some time and then ask the server again
					Thread.sleep(random(1000, 2000));
				} catch (InterruptedException e1) {}

				if(i == maxTries) {
					e.printStackTrace();
					request.finish(State.FAILED, null);
				}
			}
		}
	}

	public static int random(int min, int max) {
		return (int)(Math.random() * ((max+1)-min) ) + min;
	}

	abstract public boolean loadFromCache(WMSRequest request);

	public void cancel() {
		canceled = true;
	}
}
