package com.fasterxml.util.regext.model;

import java.util.*;

import java.util.LinkedHashMap;

public class UncookedExtractions
{
    protected HashMap<String,UncookedPattern> _rawPatterns = new LinkedHashMap<>();

    public UncookedExtractions() { }

    public UncookedPattern addPattern(String name, UncookedPattern pattern) {
        UncookedPattern old = _rawPatterns.put(name, pattern);
        return old;
    }

    public Map<String,UncookedPattern> getPatterns() {
        return _rawPatterns;
    }
}
