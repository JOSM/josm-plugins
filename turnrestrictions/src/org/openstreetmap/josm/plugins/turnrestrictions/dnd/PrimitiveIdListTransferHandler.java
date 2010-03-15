package org.openstreetmap.josm.plugins.turnrestrictions.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * PrimitiveIdListTransferHandler is a transfer handler for component which 
 * provide and/or accept a list of {@see PrimitiveId} via copy/paste or
 * drag-and-drop.
 * 
 * It creates a {@see Transferable} by retrieving the list of primitive IDs
 * from a {@see PrimitiveIdListProvider}.
 * 
 */
public class PrimitiveIdListTransferHandler extends TransferHandler {
	static private final Logger logger = Logger.getLogger(PrimitiveIdListTransferHandler.class.getName());
	private PrimitiveIdListProvider provider;
	
	/**
	 * Creates the transfer handler 
	 * 
	 * @param provider the provider of the primitive IDs. Must not be null.
	 * @throws IllegalArgumentException thrown if provider is null.
	 */
	public PrimitiveIdListTransferHandler(PrimitiveIdListProvider provider) throws IllegalArgumentException{
		CheckParameterUtil.ensureParameterNotNull(provider, "provider");
		this.provider = provider;
	}

	/**
	 * Replies true if {@code transferFlavors} includes the data flavor {@see PrimitiveIdTransferable#PRIMITIVE_ID_LIST_FLAVOR}.

	 * @param transferFlavors an array of transferFlavors
	 * @return
	 */
	protected boolean isSupportedFlavor(DataFlavor[] transferFlavors) {
		for (DataFlavor df: transferFlavors) {
			if (df.equals(PrimitiveIdTransferable.PRIMITIVE_ID_LIST_FLAVOR)) return true;
		}
		return false;
	}
	
	protected Transferable createTransferable(JComponent c) {
		return new PrimitiveIdTransferable(provider.getSelectedPrimitiveIds());			
	}

	public int getSourceActions(JComponent c) {
		return COPY;
	}

	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return isSupportedFlavor(transferFlavors);	
	}	
}
