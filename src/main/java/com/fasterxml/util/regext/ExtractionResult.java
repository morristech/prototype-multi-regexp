package com.fasterxml.util.regext;

import java.util.*;

import com.fasterxml.util.regext.model.CookedExtraction;

/**
 * Result gotten by matching an input line against extraction rules.
 * If multiple matches would occur, highest matching one (one defined
 * first in the source definition) is used.
 */
public class ExtractionResult
{
    protected final String _id;

    protected final String _input;

    protected final CookedExtraction _matchedExtraction;

    protected final String[] _matchNames;
    protected final String[] _matchValues;

    public ExtractionResult(String id, String input, CookedExtraction extr,
            String[] names, String[] values)
    {
        _id = id;
        _input = input;
        _matchedExtraction = extr;
        _matchNames = names;
        _matchValues = values;
    }

    public String getId() { return _id; }
    public String getInput() { return _input; }
    public CookedExtraction getMatchedExtraction() { return _matchedExtraction; }

    public Map<String,Object> getExtra() { return _matchedExtraction.getExtra(); }

    public Map<String,Object> asMap(String idAs) {
        Map<String,Object> extra = getExtra();
        int size = _matchValues.length;
        if (extra != null) {
            size += extra.size();
        }
        if (idAs != null) {
            ++size;
        }
        LinkedHashMap<String,Object> result = new LinkedHashMap<>(size);

        // Start with id; add actual matches, append appendables
        if (idAs != null) {
            result.put(idAs, _id);
        }
        for (int i = 0, end = _matchValues.length; i < end; ++i) {
            result.put(_matchNames[i], _matchValues[i]);
        }
        if (extra != null) {
            result.putAll(extra);
        }
        return result;
    }
}
