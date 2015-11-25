package com.fasterxml.util.regext.util;

/**
 * Simple container of a pair of matched String and offset within input
 * for the first character of not-yet-consumed input.
 */
public class StringAndOffset
{
    public final String match;
    public final int restOffset;

    public StringAndOffset(String m, int o) {
        match = m;
        restOffset = o;
    }

    public StringAndOffset withOffset(int o) {
        if (o == restOffset) {
            return this;
        }
        return new StringAndOffset(match, o);
    }
}
