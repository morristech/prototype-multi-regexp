package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

public class LiteralText extends LiteralPiece
{
    protected LiteralText(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }
}
