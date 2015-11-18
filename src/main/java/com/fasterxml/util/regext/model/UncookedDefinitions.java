package com.fasterxml.util.regext.model;

import java.util.*;

import java.util.LinkedHashMap;

public class UncookedDefinitions
{
    protected HashMap<String,UncookedDefinition> _rawPatterns = new LinkedHashMap<>();

    protected HashMap<String,UncookedDefinition> _rawTemplates = new LinkedHashMap<>();
    
    public UncookedDefinitions() { }

    public UncookedDefinition addPattern(String name, UncookedDefinition pattern) {
        UncookedDefinition old = _rawPatterns.put(name, pattern);
        return old;
    }

    public UncookedDefinition addTemplate(String name, UncookedDefinition pattern) {
        UncookedDefinition old = _rawTemplates.put(name, pattern);
        return old;
    }

    public Map<String,UncookedDefinition> getPatterns() {
        return _rawPatterns;
    }

    public Map<String,UncookedDefinition> getTemplates() {
        return _rawTemplates;
    }
}
