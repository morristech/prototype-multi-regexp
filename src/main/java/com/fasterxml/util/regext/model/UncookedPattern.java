package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Encapsulation of pattern at point when it has not yet been fully processed
 * and may still contain references to other (uncooked) patterns.
 */
public class UncookedPattern
{
    protected String _name;

    protected InputLine _source;

    /**
     * Sequence of parts of this pattern instance.
     */
    protected List<Segment> _parts = new LinkedList<Segment>();

    public UncookedPattern(InputLine src) {
        _source = src;
    }

    public void append(String literal, String patternRef) {
        _parts.add(new Segment(literal, patternRef));
    }

    // Just for testing
    public List<Segment> getParts() {
        return _parts;
    }
    
    public static class Segment {
        public final String literal;
        public String pattern;

        public Segment(String lit, String p) {
            literal = (lit == null || lit.isEmpty()) ? null : lit;
            pattern = p;
        }

        @Override
        public String toString() {
            String l = (literal == null) ? "null" : String.format("'%s'", literal);
            String p = (pattern == null) ? "null" : String.format("'%s'", pattern);
            return String.format("[Segment, literal = %s, pattern = %s]", l, p);
        }
    }
}
