/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.common.FeatFormulaErr2;
import org.apache.poi.hssf.record.common.FeatSmartTag;
import org.apache.poi.hssf.record.common.FtrHeader;
import org.apache.poi.hssf.record.common.SharedFeature;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Feat (Feature) Record
 * <P>
 * This record specifies Shared Features data. It is normally paired
 *  up with a {@link FeatHdrRecord}.
 */
public final class FeatRecord extends StandardRecord  {
	public final static short sid = 0x0868;
	
	private FtrHeader futureHeader;
	
	/**
	 * See SHAREDFEATURES_* on {@link FeatHdrRecord}
	 */
	private int isf_sharedFeatureType; 
	private byte reserved1; // Should always be zero
	private long reserved2; // Should always be zero
	/** Only matters if type is ISFFEC2 */
	private long cbFeatData;
	private int reserved3; // Should always be zero
	private CellRangeAddress[] cellRefs;

	/**
	 * Contents depends on isf_sharedFeatureType :
	 *  ISFPROTECTION -> FeatProtection 
	 *  ISFFEC2       -> FeatFormulaErr2
	 *  ISFFACTOID    -> FeatSmartTag
	 */
	private SharedFeature sharedFeature; 
	
	public FeatRecord() {
		futureHeader = new FtrHeader();
		futureHeader.setRecordType(sid);
	}

	public short getSid() {
		return sid;
	}

	public FeatRecord(RecordInputStream in) { // NO_UCD
		futureHeader = new FtrHeader(in);
		
		isf_sharedFeatureType = in.readShort();
		reserved1 = in.readByte();
		reserved2 = in.readInt();
		int cref = in.readUShort();
		cbFeatData = in.readInt();
		reserved3 = in.readShort();

		cellRefs = new CellRangeAddress[cref];
		for(int i=0; i<cellRefs.length; i++) {
			cellRefs[i] = new CellRangeAddress(in);
		}
		
		switch(isf_sharedFeatureType) {
		case FeatHdrRecord.SHAREDFEATURES_ISFFEC2:
			sharedFeature = new FeatFormulaErr2(in);
			break;
		case FeatHdrRecord.SHAREDFEATURES_ISFFACTOID:
			sharedFeature = new FeatSmartTag(in);
			break;
		default:
			System.err.println("Unknown Shared Feature " + isf_sharedFeatureType + " found!");
		}
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[SHARED FEATURE]\n");
		
		// TODO ...
		
		buffer.append("[/SHARED FEATURE]\n");
		return buffer.toString();
	}

	public void serialize(LittleEndianOutput out) {
		futureHeader.serialize(out);
		
		out.writeShort(isf_sharedFeatureType);
		out.writeByte(reserved1);
		out.writeInt((int)reserved2);
		out.writeShort(cellRefs.length);
		out.writeInt((int)cbFeatData);
		out.writeShort(reserved3);
		
		for(int i=0; i<cellRefs.length; i++) {
			cellRefs[i].serialize(out);
		}
		
		sharedFeature.serialize(out);
	}

	protected int getDataSize() {
		return 12 + 2+1+4+2+4+2+
			(cellRefs.length * CellRangeAddress.ENCODED_SIZE)
			+sharedFeature.getDataSize();
	}


    
    //HACK: do a "cheat" clone, see Record.java for more information
    public Object clone() {
        return cloneViaReserialise();
    }

    
}
