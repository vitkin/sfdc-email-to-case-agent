/*
 * #%L
 * sfdc-email-to-case-agent
 * %%
 * Copyright (C) 2006 salesforce.com, inc.
 * %%
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.sforce.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * TextUtil
 *
 * Utility class for processing text efficiently
 *
 */
public class TextUtil {

    private TextUtil () {}

    /**
     * Returns a comma-delimited string representation of the specified array, with []s.
     */
    public static String arrayToString(Object[] array) {
        return arrayToString(array, ", ", -1);
    }

    /**
     * Returns a delimited string representation of the specified array, with []s.
     * @param delim what delimiter to use in between array elements.
     */
    public static String arrayToString(Object[] array, String delim) {
        return arrayToString(array, delim, -1);
    }

    /**
     * @param delim what delimiter to use in between array elements.
     * @param maxValues how many array elements to include.
     *      When less than 0, all values are included.
     *      If <code>maxValues</code> is greater than the number of elements in <code>array</code>,
     *      then all elements are included.
     *      If any elements are not included, <code>...</code> will be inserted after the last element.
     */
    public static String arrayToString(Object[] array, String delim, int maxValues) {
        if (delim == null) throw new IllegalArgumentException();
        if (array == null) return null;
        if (array.length == 0) return "";

        int max = maxValues < 0 ? array.length : Math.min(array.length, maxValues);
        StringBuffer temp = new StringBuffer(2 + (max * 16));

        for (int i = 0; i < max; i++) {
            if (i > 0) temp.append(delim);
            temp.append(array[i]);
        }
        if (max < array.length) temp.append("...");

        return temp.toString();
    }

    /**
     * Search source for instances of patterns from patterns[].  If found, replace
     * with the corresponding replacement in replacements[] (i.e., pattern[n] is replaced by
     * replacement[n])
     *
     * @param patterns an array of regexes of what to find and what will be replaced. Note, that since they are
     *      regexes, you must be sure to escape special regex tokens.
     */
    public static final String replace(String source, String[] patterns, String[] replacements) {
        if (source == null) return null;
        StringBuffer buf = new StringBuffer();
        for(int i=0; i < patterns.length; ++i) {
            if (i != 0) buf.append("|");
            buf.append("(").append(patterns[i]).append(")");
        }
        String regexString = buf.toString();
        Pattern regex = Pattern.compile(regexString);

        Matcher m = regex.matcher(source);
        if (m.groupCount() != replacements.length)
            throw new IllegalArgumentException("Mismatch between pattern and replacements array");

        StringBuffer result = null;
        int idx = 0;
        while(m.find()) {
            if (result == null) result = new StringBuffer(source.length()+32);
            result.append(source.substring(idx,m.start()));
            for(int i=0; i < replacements.length; ++i) {
                // the 0th group is the entire expression
                if (m.group(i+1) != null) {
                    result.append(replacements[i]);
                    idx = m.end();
                    break;
                }
            }
        }
        if (result == null) return source;
        result.append(source.substring(idx));
        return result.toString();
    }

    /**
     * @return the replacement of all occurrences of src[i] with target[i] in s. Src and target are not regex's
     * so this uses simple searching with indexOf()
     */
    public static final String replaceSimple(String s, String[] src, String[] target) {

        if (s == null) return null;
        StringBuffer sb = new StringBuffer(s.length());
        int pos = 0;
        int limit = s.length();
        int lastMatch = 0;
        while (pos < limit) {
            boolean matched = false;
            for(int i=0; i < src.length; i++) {
                if (s.startsWith(src[i],pos)) {
                    // we found a matching pattern - append the acculumation plus the replacement
                    sb.append(s.substring(lastMatch,pos)).append(target[i]);
                    pos += src[i].length();
                    lastMatch = pos;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // we didn't match any patterns, so move forward 1 character
                pos++;
            }
        }
        // see if we found any matches
        if (lastMatch == 0) {
            // we didn't match anything, so return the source string
            return s;
        }

        // apppend the trailing portion
        sb.append(s.substring(lastMatch));

        return sb.toString();
    }

    /** Translate characters from the String inStr found in the String oldChars to the corresponding
     *  character found in newChars.  If a character occurs in oldChars at a position beyond the
     *  end of newChars, the character is removed.  This function is an analog of the SQL function
     *  TRANSLATE().  By setting newChars to empty String (""), it functions as a
     *  strip-oldChars-from-inStr function.
     */
    public static final String translate(String s, String oldChars, String newChars) {
        if (s == null || s.length() == 0) return s; // nothing to do
        StringBuffer outBuf = new StringBuffer(s.length()); //result can't be bigger
        int idx = -1;
        int limit = s.length();
        for(int i=0; i < limit; ++i) {
            char c = s.charAt(i);
            if ( (idx = oldChars.indexOf(c)) != -1) { // found the character in oldChars!
                if (idx < newChars.length()) { // it has a mapping in the newChars set
                    outBuf.append(newChars.charAt(idx));
                }
            } else {
                outBuf.append(c); // character wasn't in oldChars
            }
        }
        return outBuf.toString();
    }

    /**
     * @param s
     * @return The input string stripped of all whitespace characters.
     */
    public static final String removeWhitespace(String s) {

        if (s == null) return "";

        StringBuffer buf = new StringBuffer(s.length());
        int pos = 0;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (!Character.isWhitespace(c)) {
                buf.append(c);
            }
            ++pos;
        }
        return buf.toString();
    }

    /**
     * Splits <code>s</code> into a List of Strings separated by <code>split</code>,
     * which is not a regex. This is more efficient than String.split or TextUtil.split
     * because it doesn't use a regex.
     */
    public static List splitSimple(String split, String s) {
        return splitSimple(split, s, 4);
    }

    /**
     * Splits <code>s</code> into a List of Strings separated by <code>split</code>,
     * which is not a regex. This is more efficient than String.split or TextUtil.split
     * because it doesn't use a regex.
     *
     * @param expectedSize the expected size of the resulting list
     */
    public static List splitSimple(String split, String s, int expectedSize) {
        if (split.length() == 0) throw new IllegalArgumentException();

        List<String> result = new ArrayList<String>(expectedSize);
        int start = 0;
        int indexof;
        while ((indexof = s.indexOf(split, start)) != -1) {
            result.add(s.substring(start, indexof));
            start = indexof + split.length();
            if (start >= s.length()) break;
        }
        if (start == s.length()) {
            result.add("");
        } else if (start < s.length()) {
            result.add(s.substring(start));
        }
        return result;
    }

    public static boolean isNullEmptyOrWhitespace(CharSequence str){
        if (str == null)
            return true;

        int end = str.length();
        char c;
        for (int i = 0; i < end; i++){
            if (!((c = str.charAt(i)) <= ' ' || Character.isWhitespace(c))){
                return false;
            }
        }
        return true;
    }

}
