package com.fasterxml.util.regext;

import java.util.List;

import com.fasterxml.util.regext.autom.PolyMatcher;
import com.fasterxml.util.regext.model.CookedDefinitions;
import com.fasterxml.util.regext.model.CookedExtraction;

public class ExtractionDefinition
{
    protected final CookedDefinitions _defs;

    protected final PolyMatcher _matcher;

    protected ExtractionDefinition(CookedDefinitions defs, PolyMatcher matcher) {
        _defs = defs;
        _matcher = matcher;
    }

    public static ExtractionDefinition construct(CookedDefinitions defs,
            PolyMatcher matcher) {
        return new ExtractionDefinition(defs, matcher);
    }

    public List<CookedExtraction> getExtractions() {
        return  _defs.getExtractions();
    }

    public PolyMatcher getMatcher() {
        return _matcher;
    }
}
