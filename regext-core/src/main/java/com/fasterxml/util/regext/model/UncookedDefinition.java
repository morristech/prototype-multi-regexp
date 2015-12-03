package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.DefinitionParseException;
import com.fasterxml.util.regext.io.InputLine;

/**
 * Encapsulation of a definition (pattern, template, extractor) that
 * has only been tokenized, but where named references have not been
 * resolved, nor any escaping/quoting performed.
 */
public class UncookedDefinition
    implements DefPieceContainer
{
    protected final String _name;

    protected final InputLine _source;

    /**
     * Sequence of pieces of this definition instance.
     */
    protected List<DefPiece> _parts = new LinkedList<DefPiece>();

    public UncookedDefinition(InputLine src, String name) {
        _source = src;
        _name = name;
    }

    @Override
    public void appendLiteralPattern(String literal, int offset) {
        _parts.add(new LiteralPattern(_source, offset, literal));
    }

    @Override
    public void appendLiteralText(String literal, int offset) {
        _parts.add(new LiteralText(_source, offset, literal));
    }

    @Override
    public void appendPatternRef(String name, int offset) {
        _parts.add(new PatternReference(_source, offset, name));
    }

    @Override
    public void appendTemplateRef(String name, int offset) {
        _parts.add(new TemplateReference(_source, offset, name));
    }

    @Override
    public ExtractorExpression appendExtractor(String name, int offset) {
        ExtractorExpression extr = new ExtractorExpression(_source, offset, name);
        _parts.add(extr);
        return extr;
    }

    @Override
    public String getName() {
        return _name;
    }

    public InputLine getSource() {
        return _source;
    }

    public List<DefPiece> getParts() {
        return _parts;
    }

    public void reportError(String template, Object... args) throws DefinitionParseException {
        InputLine source;
        int offset;
        if (_parts.isEmpty()) { // unlikely?
            source = _source;
            offset = 0;
        } else {
            DefPiece p = _parts.get(0);
            source = p.getSource();
            offset = p.getSourceOffset();
        }
        source.reportError(offset, template, args);
    }
}