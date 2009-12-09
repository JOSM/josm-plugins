package livegps;

import org.openstreetmap.josm.Main;

/**
 * The LiveGpsSuppressor permits update events only once within a given timespan.
 * This is useful, when too frequent updates consume large parts of the CPU resources (esp.
 * on low-end devices, such as netbooks).
 *
 * Its own thread wakes up after the sleepTime and enables the allowUpdate flag.
 * When another thread (the LiveGpsAcquirere) "asks for permission",
 * the first call is permitted, but it also disables the updates for the following calls,
 * until the sleepTime has elapsed.
 *
 * @author casualwalker
 *
 */
public class LiveGpsSuppressor implements Runnable, ILiveGpsSuppressor {

	/**
	 * Default sleep time is 5 seconds.
	 */
	private static final int DEFAULT_SLEEP_TIME = 5;

	/**
	 * The currently used sleepTime.
	 */
	private int sleepTime = DEFAULT_SLEEP_TIME;

	/**
	 * The flag allowUpdate is enabled once during the sleepTime.
	 */
	private boolean allowUpdate = false;

	/**
	 * Controls if this thread is still in used.
	 */
	private boolean shutdownFlag = false;

	/**
	 * Run thread enables the allowUpdate flag once during its cycle.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		initSleepTime();

		shutdownFlag = false;
		// stop the thread, when explicitely shut down or when disabled by
		// config setting
		while (!shutdownFlag && isEnabled()) {
			setAllowUpdate(true);

			try {
				Thread.sleep(getSleepTime());
			} catch (InterruptedException e) {
				// TODO I never knew, how to handle this??? Probably just carry
				// on
			}
		}

	}

	/**
	 * Retrieve the sleepTime from the configuration.
	 * If no such configuration key exists, it will be initialized here.
	 */
	private void initSleepTime() {
		// fetch it from the user setting, or use the default value.
		int sleepSeconds = 0;
		sleepSeconds = Main.pref.getInteger("livegps.refreshinterval",
				DEFAULT_SLEEP_TIME);
		// creates the setting, if none present.
		Main.pref.putInteger("livegps.refreshinterval", sleepSeconds);

		// convert seconds into milliseconds internally.
		this.sleepTime = sleepSeconds * 1000;
	}

	/**
	 * Set the allowUpdate flag. May only privately accessible!
	 * @param allowUpdate the allowUpdate to set
	 */
	private synchronized void setAllowUpdate(boolean allowUpdate) {
		this.allowUpdate = allowUpdate;
	}

	/**
	 * Query, if an update is currently allowed.
	 * When it is allowed, it will disable the allowUpdate flag as a side effect.
	 * (this means, one thread got to issue an update event)
	 *
	 * @return true, if an update is currently allowed; false, if the update shall be suppressed.
	 * @see livegps.ILiveGpsSuppressor#isAllowUpdate()
	 */
	public synchronized boolean isAllowUpdate() {

		// if disabled, always permit a re-draw.
		if (!isEnabled()) {
			return true;
		} else {

			if (allowUpdate) {
				allowUpdate = false;
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * A value below 1 disables this feature.
	 * This ensures that a small value does not run this thread
	 * in a tight loop.
	 * 
	 * @return true, if suppressing is enabled
	 */
	private boolean isEnabled() {
		return this.sleepTime > 0;
	}

	/**
	 * Shut this thread down.
	 */
	public void shutdown() {
		shutdownFlag = true;
	}

	/**
	 * @return the defaultSleepTime
	 */
	private int getSleepTime() {
		return this.sleepTime;
	}

}
