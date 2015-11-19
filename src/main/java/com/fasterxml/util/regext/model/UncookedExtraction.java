package com.fasterxml.util.regext.model;

import java.util.Map;

import com.fasterxml.util.regext.io.InputLine;

// placeholder
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

    public String getName() {
        return _name;
    }

    public UncookedDefinition getTemplate() {
        return _template;
    }

    public Map<String,Object> getExtra() {
        return _append;
    }
}
