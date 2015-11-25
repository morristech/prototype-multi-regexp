package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Intermediate base class that represents pieces that are leaf components
 * and do not contain any further references that need resolution.
 * This does not necessarily mean that no further processing is needed;
 * typically values contained will still go through various escaping or
 * translation processes.
 */
public class LiteralPiece extends DefPiece
{
    public LiteralPiece(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }
}
