package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Similar to other {@link DefPiece}s, expect that it is structured and besides
 * name (of variable/property to extract value for) also contains nested sequence
 * of pieces (possibly including other extractors, but more commonly patterns,
 * pattern references; theoretically also template references).
 */
public class ExtractorPiece extends DefPiece {
    private List<DefPiece> _parts;
    
    public ExtractorPiece(InputLine src, int offset, String lit) {
        super(src, offset, lit);
        _parts = new ArrayList<>();
    }

    public void addPart(DefPiece p) {
        _parts.add(p);
    }

    // Just for semantics, same as getText()
    public String getName() {
        return getText();
    }
}
