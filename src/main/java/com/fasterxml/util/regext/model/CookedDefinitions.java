package com.fasterxml.util.regext.model;

import java.util.*;

public class CookedDefinitions
{
    protected HashMap<String,LiteralPattern> _patterns = new LinkedHashMap<>();

    public CookedDefinitions() { }

    public void addPattern(String name, LiteralPattern p) {
        _patterns.put(name, p);
    }

    public LiteralPattern findPattern(String name) {
        return _patterns.get(name);
    }

    public Map<String,LiteralPattern> getPatterns() {
        return _patterns;
    }
}
