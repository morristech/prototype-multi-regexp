package com.fasterxml.util.regext.util;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.util.regext.io.InputLine;

public class TokenHelper
{
    protected final static Pattern P_KEYWORD_REST = Pattern.compile("\\s*(\\w*)\\s*(.*)");

    public static StringPair findKeyword(int offset, String contents)
    {
        Matcher m = P_KEYWORD_REST.matcher(contents);
        if (!m.matches()) {
            return null;
        }
        // Need to retain offsets for error reporting
        return new StringPair(m.start(1), m.group(1), m.start(2), m.group(2));
    }

    /**
     * Helper method to call to find expected type marker character, optionally preceded
     * by ignorable whitespace.
     *
     * @return Index of the marker character, if one found; -1 if not
     */
    public static int findTypeMarker(char marker, String contents)
    {
        for (int i = 0, end = contents.length(); i < end; ++i) {
            char c = contents.charAt(i);
            if (c == marker) {
                return i;
            }
            if (_isWS(c)) {
                break;
            }
        }
        return -1;
    }

    /**
     *<p>
     * NOTE: assumption is that there is no whitespace to skip, that is, the name
     * starts from the first character position.
     */
    public static StringPair parseName(String type, InputLine inputLine,
            int baseOffset, String contents) throws IOException
    {
        // Two options: quoted, unquoted; and two types of quotes as well
        final int len = contents.length();
        if (len == 0) {
            inputLine.reportError(baseOffset, "Missing %s name", type);
        }

        int nameStart = 0, restStart = 0; // just since compiler can't know 
        String name = null;

		char c = contents.charAt(0);
		if (c == '"' || c == '\'') {
			int ix = contents.indexOf(c, 1);
			if (ix < 0) { // should give better error message
		            return inputLine.reportError(baseOffset, "Missing closing quote ('%c') for %s name",
		                    c, type);
			}
			nameStart = 1;
			name = contents.substring(nameStart, ix);
			restStart = ix+1;
		} else if (!Character.isJavaIdentifierStart(c)) { // should give better error message
              return inputLine.reportError(baseOffset, "Invalid first character (%s) for %s name",
                      c, type);
		} else {
			nameStart = 0;
			int i = 1;
			for (; i < len; ++i) {
				c = contents.charAt(i);
				// let's allow hyphen too, in addition to acceptable Java identifier chars
				if (!Character.isJavaIdentifierPart(c) && (c != '-')) {
					break;
				}
			}
			name = contents.substring(nameStart, i);
			restStart = i;
		}
		// must be followed by whitespace, as well?
		if ((restStart >= len) || !_isWS(contents.charAt(restStart))) {
              return inputLine.reportError(baseOffset, "Missing space character after %s name '%s'",
                      type, name);
		}
		while ((++restStart < len) && _isWS(contents.charAt(restStart))) { }
		return new StringPair(baseOffset+nameStart, name, baseOffset+restStart, contents.substring(restStart));
	}

	//  could use full JDK approach, with extra whitespace; but for now just allow ASCII whitespace
	private static boolean _isWS(char ch) {
		return (ch <= ' ');
	}
}
