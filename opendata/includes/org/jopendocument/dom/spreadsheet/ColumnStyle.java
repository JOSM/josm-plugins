/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 jOpenDocument, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU
 * General Public License Version 3 only ("GPL").  
 * You may not use this file except in compliance with the License. 
 * You can obtain a copy of the License at http://www.gnu.org/licenses/gpl-3.0.html
 * See the License for the specific language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 * 
 */

package org.jopendocument.dom.spreadsheet;

import org.jdom2.Element;
import org.jopendocument.dom.ODPackage;
import org.jopendocument.dom.StyleDesc;
import org.jopendocument.dom.StyleStyle;
import org.jopendocument.dom.XMLVersion;

public class ColumnStyle extends StyleStyle {

    static public final String STYLE_FAMILY = "table-column";
    // from section 18.728 in v1.2-part1
    public static final StyleDesc<ColumnStyle> DESC = new StyleDesc<ColumnStyle>(ColumnStyle.class, XMLVersion.OD, STYLE_FAMILY, "co", "table") {
        @Override
        public ColumnStyle create(ODPackage pkg, Element e) {
            return new ColumnStyle(pkg, e);
        }
    };

    public ColumnStyle(final ODPackage pkg, Element tableColElem) {
        super(pkg, tableColElem);
    }
}
