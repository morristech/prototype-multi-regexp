package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Encapsulation of a definition (pattern, template, extractor) that
 * has only been tokenized, but where named references have not been
 * resolved, nor any escaping/quoting performed.
 */
public class UncookedDefinition
{
    protected String _name;

    protected InputLine _source;

    /**
     * Sequence of pieces of this definition instance.
     */
    protected List<DefPiece> _parts = new LinkedList<DefPiece>();

    public UncookedDefinition(InputLine src) {
        _source = src;
    }

    public void appendLiteralPattern(String literal, int offset) {
        _parts.add(new LiteralPattern(_source, offset, literal));
    }

    public void appendLiteralText(String literal, int offset) {
        _parts.add(new LiteralText(_source, offset, literal));
    }

    public void appendPatternRef(String name, int offset) {
        _parts.add(new PatternReference(_source, offset, name));
    }

    public void appendTemplateRef(String name, int offset) {
        _parts.add(new TemplateReference(_source, offset, name));
    }
    
    // Just for testing
    public List<DefPiece> getParts() {
        return _parts;
    }
}
