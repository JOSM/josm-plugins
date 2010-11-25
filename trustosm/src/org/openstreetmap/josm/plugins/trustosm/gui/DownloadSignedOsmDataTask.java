package org.openstreetmap.josm.plugins.trustosm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.trustosm.TrustOSMplugin;
import org.openstreetmap.josm.plugins.trustosm.data.TrustOSMItem;
import org.xml.sax.SAXException;

public class DownloadSignedOsmDataTask  extends PleaseWaitRunnable {

	private boolean cancelled;
	private Exception lastException;
	private final Collection<OsmPrimitive> missing;
	private final OsmDataLayer curLayer;
	private MultiFetchServerObjectReader objectReader;

	/**
	 * Download the given OSMPrimitives to the given layer
	 * 
	 */
	public DownloadSignedOsmDataTask(Collection<OsmPrimitive> missing, OsmDataLayer curLayer) {
		super(tr("Download signed data"));
		this.missing = missing;
		this.curLayer = curLayer;
	}


	@Override
	protected void cancel() {
		cancelled = true;
		synchronized(this) {
			if (objectReader != null) {
				objectReader.cancel();
			}
		}
	}

	@Override
	protected void finish() {
		Main.map.repaint();
		if (cancelled)
			return;
		if (lastException != null) {
			ExceptionDialogUtil.explainException(lastException);
		}
	}

	protected String buildDownloadFeedbackMessage() {
		return trn("Downloading {0} incomplete child of relation ''{1}''",
				"Downloading {0} incomplete children of relation ''{1}''",
				missing.size(),
				missing.size(),
				"Wurst"
		);
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {
		try {
			synchronized (this) {
				if (cancelled) return;
				objectReader = new MultiFetchServerObjectReader();
			}
			objectReader.append(missing);
			progressMonitor.indeterminateSubTask(
					buildDownloadFeedbackMessage()
			);
			final DataSet dataSet = objectReader.parseOsm(progressMonitor
					.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
			if (dataSet == null)
				return;
			synchronized (this) {
				if (cancelled) return;
				objectReader = null;
			}

			SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							curLayer.mergeFrom(dataSet);
							curLayer.onPostDownloadFromServer();
							AutoScaleAction.zoomTo(dataSet.allPrimitives());
							updateReferences(dataSet);
						}
					}
			);

		} catch (Exception e) {
			if (cancelled) {
				System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e.toString()));
				return;
			}
			lastException = e;
		}
	}

	public boolean updateReferences(DataSet ds) {
		for (TrustOSMItem t : TrustOSMplugin.signedItems.values()) {
			OsmPrimitive osm = ds.getPrimitiveById(t.getOsmItem().getPrimitiveId());
			if (osm != null) {
				t.setOsmItem(osm);
				return true;
			} else {
				System.out.println("No item found");
			}
		}
		return false;
	}

}

