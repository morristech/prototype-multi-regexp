package com.fasterxml.util.regext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import com.fasterxml.util.regext.autom.PolyMatcher;
import com.fasterxml.util.regext.model.CookedDefinitions;
import com.fasterxml.util.regext.model.CookedExtraction;

/**
 * Processor built from a definition that is used to actually extract
 * information out of input lines.
 *<p>
 * Instances are fully thread-safe and may be used concurrently.
 */
public class RegExtractor
{
    /**
     * Multi-expression matcher that is capable of figuring out which extraction
     * rules, if any, matched. This is needed to know which actual extraction-based
     * regular expression to use for actual data extraction.
     */
    protected final PolyMatcher _matcher;

    protected final CookedExtraction[] _extractions;

    protected RegExtractor(PolyMatcher matcher, CookedExtraction[] extr) {
        _matcher = matcher;
        _extractions = extr;
    }

    public static RegExtractor construct(CookedDefinitions defs,
            PolyMatcher matcher)
    {
        List<CookedExtraction> extrL = defs.getExtractions();
        CookedExtraction[] extr = extrL.toArray(new CookedExtraction[extrL.size()]);
        return new RegExtractor(matcher, extr);
    }

    public List<CookedExtraction> getExtractions() {
        return Arrays.asList(_extractions);
    }

    public PolyMatcher getMatcher() {
        return _matcher;
    }

    /**
     * Match method that expects the first full match to work as expected,
     * evaluate extraction and return the result. If the first match
     * by multi-matcher fails for some reason (internal problem with
     * translations), a {@link ExtractionException} will be thrown.
     */
    public ExtractionResult match(String input) throws ExtractionException {
        return match(input, false);
    }

    /**
     * Match method that tries potential matches in order, returning first
     * that fully works. Should only be used if there is fear that sometimes
     * matchers are not properly translated; but if so, it is preferable to
     * get a lower-precedence match, or possible none at all.
     */
    public ExtractionResult matchSafe(String input) throws ExtractionException {
        return match(input, true);
    }
    
    public ExtractionResult match(String input, boolean allowFallbacks) throws ExtractionException
    {
        int[] matchIndexes = _matcher.match(input);
        if (matchIndexes.length == 0) {
            return null;
        }
        // First one ought to suffice, try that first
        int matchIndex = matchIndexes[0];
        CookedExtraction extr = _extractions[matchIndex];
        Matcher m = extr.getRegexp().matcher(input);
        if (m.matches()) {
            return _match(input, extr, m);
        }
        // More than one? Should we throw an exception or play safe?
        if (!allowFallbacks) {
            throw new ExtractionException(input,
                    String.format("Internal error: high-level match for extraction #%d (%s) failed to match generated regexp: %s",
                            matchIndex, extr.getName(), extr.getRegexpDesc()));
        }
        for (int i = 1, end = matchIndexes.length; i < end; ++i) {
            extr = _extractions[matchIndex];
            m = extr.getRegexp().matcher(input);
            if (m.matches()) {
                return _match(input, extr, m);
            }
        }
        // nothing matches, despite initially seeming they would?
        return null;
    }

    protected ExtractionResult _match(String input, CookedExtraction extr, Matcher m)
    {
        String[] names = extr.getExtractedNames();
        final int count = names.length;
        String[] values = new String[count];
        for (int i = 0; i < count; ++i) {
            values[i] = m.group(i+1);
        }
        return new ExtractionResult(extr.getName(), input, extr, names, values);
    }
}
