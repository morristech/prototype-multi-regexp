package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Similar to other {@link DefPiece}s, expect that it is structured and besides
 * name (of variable/property to extract value for) also contains nested sequence
 * of pieces (possibly including other extractors, but more commonly patterns,
 * pattern references; theoretically also template references).
 */
public class ExtractorExpression
    extends DefPiece
    implements DefPieceContainer, DefPieceAppendable
{
    private List<DefPiece> _parts;
    
    public ExtractorExpression(InputLine src, int offset, String lit) {
        super(src, offset, lit);
        _parts = new ArrayList<>();
    }

    public ExtractorExpression empty() {
        return new ExtractorExpression(_source, _sourceOffset, _text);
    }

    @Override
    public String getName() {
        return getText();
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

    // Also DefPieceAppendable during resolution
    @Override
    public void append(DefPiece part) {
        _parts.add(part);
    }

    @Override
    public Iterable<DefPiece> getParts() {
        return _parts;
    }
}
