package com.fasterxml.util.regext.model;

import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.util.regext.io.InputLine;

public class CookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final Map<String,Object> _append;

    protected final Pattern _regexp;
    protected final String _regexpSource;
    protected final String[] _extractorNames;

    protected CookedExtraction(InputLine source, String name,
            int index, Map<String,Object> append,
            Pattern regexp, String regexpSource, String[] extractorNames)
    {
        _source = source;
        _name = name;
        _append = append;
        _regexp = regexp;
        _regexpSource = regexpSource;
        _extractorNames = extractorNames;
    }

    public static CookedExtraction construct(int index, FlattenedExtraction src,
            Pattern regexp, String regexpSource, String[] extractorNames)
    {
        return new CookedExtraction(src._source, src._name,
                index, src._append,
                regexp, regexpSource, extractorNames);
    }

    public String getName() {
        return _name;
    }

    public Map<String,Object> getExtra() {
        return _append;
    }

    public Pattern getRegexp() {
        return _regexp;
    }

    public String getRegexpSource() {
        return _regexpSource;
    }

    public String getRegexpDesc() {
        return _regexp.pattern();
    }

    public String[] getExtractedNames() {
        return _extractorNames;
    }
}
