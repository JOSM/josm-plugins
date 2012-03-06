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

package org.apache.poi.hssf.model;

import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.FormulaRenderer;

/**
 * HSSF wrapper for the {@link FormulaParser} and {@link FormulaRenderer}
 *
 * @author Josh Micich
 */
public final class HSSFFormulaParser {

	private HSSFFormulaParser() {
		// no instances of this class
	}

	/**
	 * Static method to convert an array of {@link Ptg}s in RPN order
	 * to a human readable string format in infix mode.
	 * @param book  used for defined names and 3D references
	 * @param ptgs  must not be <code>null</code>
	 * @return a human readable String
	 */
	public static String toFormulaString(HSSFWorkbook book, Ptg[] ptgs) {
		return FormulaRenderer.toFormulaString(HSSFEvaluationWorkbook.create(book), ptgs);
	}
}
