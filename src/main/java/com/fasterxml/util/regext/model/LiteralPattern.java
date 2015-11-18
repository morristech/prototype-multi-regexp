package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

public class LiteralPattern extends DefPiece
{
    protected LiteralPattern(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }
}
