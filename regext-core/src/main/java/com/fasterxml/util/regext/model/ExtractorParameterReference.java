package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Piece used to represent a reference to an extractor parameter: name of property
 * to extract, passed as a positional argument to parameterized template.
 */
public class ExtractorParameterReference extends DefPiece {
    private final String _parentId;
    private final int _position;

    public ExtractorParameterReference(InputLine src, int offset, String parentId, int pos) {
        super(src, offset, String.valueOf(pos));
        _parentId = parentId;
        _position = pos;
    }

    public String getParentId() { return _parentId; }
    public int getPosition() { return _position; }
}
