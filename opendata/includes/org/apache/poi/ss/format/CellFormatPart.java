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
package org.apache.poi.ss.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Objects of this class represent a single part of a cell format expression.
 * Each cell can have up to four of these for positive, zero, negative, and text
 * values.
 * <p/>
 * Each format part can contain a color, a condition, and will always contain a
 * format specification.  For example <tt>"[Red][>=10]#"</tt> has a color
 * (<tt>[Red]</tt>), a condition (<tt>>=10</tt>) and a format specification
 * (<tt>#</tt>).
 * <p/>
 * This class also contains patterns for matching the subparts of format
 * specification.  These are used internally, but are made public in case other
 * code has use for them.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public class CellFormatPart {

    /** Pattern for the format specification part of a cell format part. */
    public static final Pattern SPECIFICATION_PAT;

    static {

        // A number specification
        // Note: careful that in something like ##, that the trailing comma is not caught up in the integer part

        // A part of a specification
        String part = "\\\\.                 # Quoted single character\n" +
                "|\"([^\\\\\"]|\\\\.)*\"         # Quoted string of characters (handles escaped quotes like \\\") \n" +
                "|_.                             # Space as wide as a given character\n" +
                "|\\*.                           # Repeating fill character\n" +
                "|@                              # Text: cell text\n" +
                "|([0?\\#](?:[0?\\#,]*))         # Number: digit + other digits and commas\n" +
                "|e[-+]                          # Number: Scientific: Exponent\n" +
                "|m{1,5}                         # Date: month or minute spec\n" +
                "|d{1,4}                         # Date: day/date spec\n" +
                "|y{2,4}                         # Date: year spec\n" +
                "|h{1,2}                         # Date: hour spec\n" +
                "|s{1,2}                         # Date: second spec\n" +
                "|am?/pm?                        # Date: am/pm spec\n" +
                "|\\[h{1,2}\\]                   # Elapsed time: hour spec\n" +
                "|\\[m{1,2}\\]                   # Elapsed time: minute spec\n" +
                "|\\[s{1,2}\\]                   # Elapsed time: second spec\n" +
                "|[^;]                           # A character\n" + "";

        int flags = Pattern.COMMENTS | Pattern.CASE_INSENSITIVE;
        SPECIFICATION_PAT = Pattern.compile(part, flags);
    }

    interface PartHandler {
        String handlePart(Matcher m, String part, CellFormatType type,
                StringBuffer desc);
    }

    /**
     * Returns a version of the original string that has any special characters
     * quoted (or escaped) as appropriate for the cell format type.  The format
     * type object is queried to see what is special.
     *
     * @param repl The original string.
     * @param type The format type representation object.
     *
     * @return A version of the string with any special characters replaced.
     *
     * @see CellFormatType#isSpecial(char)
     */
    static String quoteSpecial(String repl, CellFormatType type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repl.length(); i++) {
            char ch = repl.charAt(i);
            if (ch == '\'' && type.isSpecial('\'')) {
                sb.append('\u0000');
                continue;
            }

            boolean special = type.isSpecial(ch);
            if (special)
                sb.append("'");
            sb.append(ch);
            if (special)
                sb.append("'");
        }
        return sb.toString();
    }



    public static StringBuffer parseFormat(String fdesc, CellFormatType type,
            PartHandler partHandler) {

        // Quoting is very awkward.  In the Java classes, quoting is done
        // between ' chars, with '' meaning a single ' char. The problem is that
        // in Excel, it is legal to have two adjacent escaped strings.  For
        // example, consider the Excel format "\a\b#".  The naive (and easy)
        // translation into Java DecimalFormat is "'a''b'#".  For the number 17,
        // in Excel you would get "ab17", but in Java it would be "a'b17" -- the
        // '' is in the middle of the quoted string in Java.  So the trick we
        // use is this: When we encounter a ' char in the Excel format, we
        // output a \u0000 char into the string.  Now we know that any '' in the
        // output is the result of two adjacent escaped strings.  So after the
        // main loop, we have to do two passes: One to eliminate any ''
        // sequences, to make "'a''b'" become "'ab'", and another to replace any
        // \u0000 with '' to mean a quote char.  Oy.
        //
        // For formats that don't use "'" we don't do any of this
        Matcher m = SPECIFICATION_PAT.matcher(fdesc);
        StringBuffer fmt = new StringBuffer();
        while (m.find()) {
            String part = group(m, 0);
            if (part.length() > 0) {
                String repl = partHandler.handlePart(m, part, type, fmt);
                if (repl == null) {
                    switch (part.charAt(0)) {
                    case '\"':
                        repl = quoteSpecial(part.substring(1,
                                part.length() - 1), type);
                        break;
                    case '\\':
                        repl = quoteSpecial(part.substring(1), type);
                        break;
                    case '_':
                        repl = " ";
                        break;
                    case '*': //!! We don't do this for real, we just put in 3 of them
                        repl = expandChar(part);
                        break;
                    default:
                        repl = part;
                        break;
                    }
                }
                m.appendReplacement(fmt, Matcher.quoteReplacement(repl));
            }
        }
        m.appendTail(fmt);

        if (type.isSpecial('\'')) {
            // Now the next pass for quoted characters: Remove '' chars, making "'a''b'" into "'ab'"
            int pos = 0;
            while ((pos = fmt.indexOf("''", pos)) >= 0) {
                fmt.delete(pos, pos + 2);
            }

            // Now the final pass for quoted chars: Replace any \u0000 with ''
            pos = 0;
            while ((pos = fmt.indexOf("\u0000", pos)) >= 0) {
                fmt.replace(pos, pos + 1, "''");
            }
        }

        return fmt;
    }

    /**
     * Expands a character. This is only partly done, because we don't have the
     * correct info.  In Excel, this would be expanded to fill the rest of the
     * cell, but we don't know, in general, what the "rest of the cell" is.
     *
     * @param part The character to be repeated is the second character in this
     *             string.
     *
     * @return The character repeated three times.
     */
    static String expandChar(String part) {
        String repl;
        char ch = part.charAt(1);
        repl = "" + ch + ch + ch;
        return repl;
    }

    /**
     * Returns the string from the group, or <tt>""</tt> if the group is
     * <tt>null</tt>.
     *
     * @param m The matcher.
     * @param g The group number.
     *
     * @return The group or <tt>""</tt>.
     */
    public static String group(Matcher m, int g) {
        String str = m.group(g);
        return (str == null ? "" : str);
    }
}