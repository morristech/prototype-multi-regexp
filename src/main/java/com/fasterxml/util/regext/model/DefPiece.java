package com.fasterxml.util.regext.model;

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

    public InputLine getSource() { return _source; }
    public int getSourceOffset() { return _sourceOffset; }
    public String getText() { return _text; }

    @Override
    public String toString() {
        return String.format("[%s: %s]", getClass().getSimpleName(), _text);
    }
}
