package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Piece used to represent a reference to template parameter: name of an included
 * template, passed as a positional argument to parameterized template.
 */
public class TemplateParameterReference extends DefPiece {
    private final String _parentId;
    private final int _position;

    public TemplateParameterReference(InputLine src, int offset, String parentId, int pos) {
        super(src, offset, String.valueOf(pos));
        _parentId = parentId;
        _position = pos;
    }

    public String getParentId() { return _parentId; }
    public int getPosition() { return _position; }
}
