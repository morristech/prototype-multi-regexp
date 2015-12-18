package com.fasterxml.util.regext.model;

import java.util.*;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Intermediate value class in which contents of the template have been fully
 * resolved into literal patterns, text segments, and extractors with literals.
 * These extractions are 
 */
public class FlattenedExtraction
    implements Iterable<DefPiece>
{
    protected final InputLine _source;
    protected final String _name;
    protected final Map<String,Object> _append;

    protected final List<DefPiece> _parts;
    protected final List<String> _extractorNames;
    
    public FlattenedExtraction(UncookedExtraction base,
            List<DefPiece> parts, Collection<String> extractorNames) {
        _source = base._source;
        _name = base._name;
        _append = base._append;
        _parts = parts;
        _extractorNames = new ArrayList<String>(extractorNames);
    }

    @Override
    public Iterator<DefPiece> iterator() {
        return _parts.iterator();
    }

    public String[] getExtractorNames() {
        return _extractorNames.toArray(new String[_extractorNames.size()]);
    }
}
