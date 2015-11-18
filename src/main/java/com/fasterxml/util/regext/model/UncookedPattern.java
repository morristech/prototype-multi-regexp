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
    protected List<DefPiece> _parts = new LinkedList<DefPiece>();

    public UncookedPattern(InputLine src) {
        _source = src;
    }

    public void appendLiteralPattern(String literal, int offset) {
        _parts.add(new LiteralPattern(_source, offset, literal));
    }

    public void appendPatternRef(String name, int offset) {
        _parts.add(new PatternReference(_source, offset, name));
    }
    
    // Just for testing
    public List<DefPiece> getParts() {
        return _parts;
    }
}
