package com.fasterxml.util.regext.model;

import java.util.Map;

import com.fasterxml.util.regext.io.InputLine;

public class CookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final CookedTemplate _template;
    protected final Map<String,Object> _append;
    
    protected CookedExtraction(InputLine source, String name, CookedTemplate t,
            Map<String,Object> append)
    {
        _source = source;
        _name = name;
        _template = t;
        _append = append;
    }

    public static CookedExtraction construct(UncookedExtraction src, CookedTemplate tmpl) {
        return new CookedExtraction(src.getSource(), src.getName(), tmpl, 
                src.getAppends());
    }

    public String getName() {
        return _name;
    }

    public CookedTemplate getTemplate() {
        return _template;
    }

    public Map<String,Object> getExtra() {
        return _append;
    }
}
