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

package org.apache.poi.util;

import java.io.IOException;
import java.io.InputStream;

public final class IOUtils {
	private IOUtils() {
		// no instances of this class
	}


	/**
	 * Helper method, just calls <tt>readFully(in, b, 0, b.length)</tt>
	 */
	public static int readFully(InputStream in, byte[] b) throws IOException {
		return readFully(in, b, 0, b.length);
	}

	/**
	 * Same as the normal <tt>in.read(b, off, len)</tt>, but tries to ensure
	 * that the entire len number of bytes is read.
	 * <p>
	 * If the end of file is reached before any bytes are read, returns -1. If
	 * the end of the file is reached after some bytes are read, returns the
	 * number of bytes read. If the end of the file isn't reached before len
	 * bytes have been read, will return len bytes.
	 */
	public static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
		int total = 0;
		while (true) {
			int got = in.read(b, off + total, len - total);
			if (got < 0) {
				return (total == 0) ? -1 : total;
			}
			total += got;
			if (total == len) {
				return total;
			}
		}
	}

}
