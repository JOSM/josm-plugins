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

package org.apache.poi.ss;

import org.apache.poi.ss.util.CellReference;

/**
 * This enum allows spreadsheets from multiple Excel versions to be handled by the common code.
 * Properties of this enum correspond to attributes of the <i>spreadsheet</i> that are easily
 * discernable to the user.  It is not intended to deal with low-level issues like file formats.
 * <p/>
 *
 * @author Josh Micich
 * @author Yegor Kozlov
 */
public enum SpreadsheetVersion {
	/**
	 * Excel97 format aka BIFF8
	 * <ul>
	 * <li>The total number of available columns is 256 (2^8)</li>
	 * <li>The total number of available rows is 64k (2^16)</li>
	 * <li>The maximum number of arguments to a function is 30</li>
	 * <li>Number of conditional format conditions on a cell is 3</li>
     * <li>Length of text cell contents is 32767</li>
	 * </ul>
	 */
	EXCEL97(0x10000, 0x0100, 30, 3, 32767);

	private final int _maxRows;
	private final int _maxColumns;

	private SpreadsheetVersion(int maxRows, int maxColumns, int maxFunctionArgs, int maxCondFormats, int maxText) {
		_maxRows = maxRows;
		_maxColumns = maxColumns;
    }

	/**
	 * @return the maximum number of usable rows in each spreadsheet
	 */
	public int getMaxRows() {
		return _maxRows;
	}

	/**
	 * @return the last (maximum) valid row index, equals to <code> getMaxRows() - 1 </code>
	 */
	public int getLastRowIndex() {
		return _maxRows - 1;
	}

	/**
	 * @return the last (maximum) valid column index, equals to <code> getMaxColumns() - 1 </code>
	 */
	public int getLastColumnIndex() {
		return _maxColumns - 1;
	}

	/**
	 *
	 * @return the last valid column index in a ALPHA-26 representation
	 *  (<code>IV</code> or <code>XFD</code>).
	 */
	public String getLastColumnName() {
		return CellReference.convertNumToColString(getLastColumnIndex());
	}
}
