package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.DefinitionParseException;
import com.fasterxml.util.regext.io.InputLine;

/**
 * Base class for pieces of pattern, template and extractor definitions;
 * mostly used for documentation purposes, not much shared functionality
 */
public abstract class DefPiece
{
    protected final InputLine _source;
    protected final int _sourceOffset;

    protected final String _text;

    protected DefPiece(InputLine src, int offset, String text)
    {
        _source = src;
        _sourceOffset = offset;
        _text = text;
    }

    protected DefPiece(DefPiece base)
    {
        _source = base._source;
        _sourceOffset = base._sourceOffset;
        _text = base._text;
    }

    protected DefPiece(DefPiece base, String text)
    {
        _source = base._source;
        _sourceOffset = base._sourceOffset;
        _text = text;
    }
    
    public InputLine getSource() { return _source; }
    public int getSourceOffset() { return _sourceOffset; }
    public String getText() { return _text; }

    public <T> T reportError(String template, Object... args) throws DefinitionParseException {
        return _source.reportError(_sourceOffset, template, args);
    }

    @Override
    public String toString() {
        return String.format("[%s: %s]", getClass().getSimpleName(), _text);
    }
}
