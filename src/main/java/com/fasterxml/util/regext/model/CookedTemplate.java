package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

public class CookedTemplate
    implements DefPieceAppendable
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

    @Override
    public void append(DefPiece part) {
        _parts.add(part);
    }

    @Override
    public String getName() { return _name; }

    @Override
    public Iterable<DefPiece> getParts() { return _parts; }
}
