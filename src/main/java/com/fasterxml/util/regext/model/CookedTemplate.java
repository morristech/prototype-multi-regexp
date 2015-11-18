package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

public class CookedTemplate
{
    protected final InputLine _source;
    protected final int _sourceOffset;

    protected final String _name;
    protected final List<DefPiece> _parts;

    protected CookedTemplate(InputLine src, int srcOffset,
            String name, List<DefPiece> parts)
    {
        _source = src;
        _sourceOffset = srcOffset;
        _name = name;
        _parts = parts;
    }

    public static CookedTemplate construct(UncookedDefinition uncooked) {
        // could get offset of the first piece, which points to name. But for now let's not bother
        return new CookedTemplate(uncooked.getSource(), 0, uncooked.getName(),
                new ArrayList<DefPiece>(Math.min(4, uncooked.getParts().size())));
    }

    public void addResolved(DefPiece part) {
        _parts.add(part);
    }
    
    public String getName() { return _name; }

    public List<DefPiece> getParts() { return _parts; }
}
