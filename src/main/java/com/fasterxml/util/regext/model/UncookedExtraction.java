package com.fasterxml.util.regext.model;

import java.util.Map;

import com.fasterxml.util.regext.io.InputLine;

/**
 * Definition of a single extraction, right after tokenization, but before
 * resolution of included pattern and template references.
 */
public class UncookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final UncookedDefinition _template;
    protected final Map<String,Object> _append;
    
    public UncookedExtraction(InputLine source, String name, UncookedDefinition t,
            Map<String,Object> append)
    {
        _source = source;
        _name = name;
        _template = t;
        _append = append;
    }

    public CookedExtraction resolve(CookedTemplate template) {
        return new CookedExtraction(_source, _name, template, _append);
    }
    
    public String getName() {
        return _name;
    }

    public UncookedDefinition getTemplate() {
        return _template;
    }
}
