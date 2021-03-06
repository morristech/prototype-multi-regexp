package com.fasterxml.util.regext.model;

import java.util.*;

import java.util.LinkedHashMap;

public class UncookedDefinitions
{
    /**
     * Parsed main-level named pattern definitions.
     */
    protected HashMap<String,UncookedDefinition> _rawPatterns = new LinkedHashMap<>();

    /**
     * Parsed main-level named template definitions.
     */
    protected HashMap<String,UncookedDefinition> _rawTemplates = new LinkedHashMap<>();

    /**
     * Parsed extraction clause definitions.
     */
    protected HashMap<String,UncookedExtraction> _rawExtractions = new LinkedHashMap<>();

    public UncookedDefinitions() { }

    public UncookedDefinition addPattern(String name, UncookedDefinition def) {
        UncookedDefinition old = _rawPatterns.put(name, def);
        return old;
    }

    public UncookedDefinition addTemplate(String name, UncookedDefinition def) {
        UncookedDefinition old = _rawTemplates.put(name, def);
        return old;
    }

    public UncookedExtraction addExtraction(String name, UncookedExtraction def) {
        UncookedExtraction old = _rawExtractions.put(name, def);
        return old;
    }

    public Map<String,UncookedDefinition> getPatterns() {
        return _rawPatterns;
    }

    public Map<String,UncookedDefinition> getTemplates() {
        return _rawTemplates;
    }

    public Map<String,UncookedExtraction> getExtractions() {
        return _rawExtractions;
    }

    public UncookedDefinition findPattern(String name) {
        return _rawPatterns.get(name);
    }

    public UncookedDefinition findTemplate(String name) {
        return _rawTemplates.get(name);
    }
}
