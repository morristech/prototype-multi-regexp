package com.fasterxml.util.regext;

import java.util.List;

import com.fasterxml.util.regext.model.CookedDefinitions;
import com.fasterxml.util.regext.model.CookedExtraction;

public class ExtractionDefinition
{
    protected final CookedDefinitions _defs;

    protected ExtractionDefinition(CookedDefinitions defs) {
        _defs = defs;
    }

    public static ExtractionDefinition construct(CookedDefinitions defs) {
        return new ExtractionDefinition(defs);
    }

    public List<CookedExtraction> getExtractions() {
        return  _defs.getExtractions();
    }
}
