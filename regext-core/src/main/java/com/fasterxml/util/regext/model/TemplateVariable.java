package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Piece used to represent a reference to template parameter: name of an included
 * template, passed as a positional argument to parameterized template.
 */
public class TemplateVariable extends DefPiece {
    private final int _position;

    public TemplateVariable(InputLine src, int offset, int pos) {
        super(src, offset, String.valueOf(pos));
        _position = pos;
    }

    public int getPosition() { return _position; }
}
