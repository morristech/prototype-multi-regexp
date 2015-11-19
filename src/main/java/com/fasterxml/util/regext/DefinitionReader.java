package com.fasterxml.util.regext;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.util.regext.io.InputLine;
import com.fasterxml.util.regext.io.InputLineReader;
import com.fasterxml.util.regext.model.*;
import com.fasterxml.util.regext.util.StringAndOffset;
import com.fasterxml.util.regext.util.TokenHelper;

public class DefinitionReader
{
    private final static String KNOWN_KEYWORDS =
            "(pattern, template, extract)"
            ;

    private final static String EXTRACTOR_PROPERTIES =
            "(template, append)"
            ;

    /**
     * We use Jackson-jr for simple deserialization of 'append' properties
     */
    private final static JSON _json = JSON.std;
    
    protected final InputLineReader _lineReader;

    protected final UncookedDefinitions _uncooked;

    protected final CookedDefinitions _cooked;
    
    protected DefinitionReader(InputLineReader lineReader) {
        _lineReader = lineReader;
        _uncooked = new UncookedDefinitions();
        _cooked = new CookedDefinitions();
    }

    @SuppressWarnings("resource")
    public static DefinitionReader reader(File input) throws IOException
    {
        InputStream in = new FileInputStream(input);
        String srcRef = "file '"+input.getAbsolutePath()+"'";
        return new DefinitionReader(InputLineReader.construct(srcRef, in));
    }

    public static DefinitionReader reader(String contents) throws IOException
    {
        Reader r = new StringReader(contents);
        String srcRef = "<input string>";
        return new DefinitionReader(InputLineReader.construct(srcRef, r));
    }

    public ExtractionDefinition read() throws IOException {
        readUncooked();
        resolvePatterns();
        resolveTemplates();
        resolveExtractions();

        return ExtractionDefinition.construct(_cooked);
    }

    /*
    /**********************************************************************
    /* Test support
    /**********************************************************************
     */

    void resolvePatterns() throws DefinitionParseException {
        _cooked.resolvePatterns(_uncooked);
    }

    void resolveTemplates() throws DefinitionParseException {
        _cooked.resolveTemplates(_uncooked);
    }

    void resolveExtractions() throws DefinitionParseException {
        _cooked.resolveExtractions(_uncooked);
    }
    
    /*
    /**********************************************************************
    /* High-level flow
    /**********************************************************************
     */

    /**
     * First part of processing, reading of contents of extraction definition
     * in "uncooked" form, which does basic tokenization but does not resolve
     * any of named references
     */
    public void readUncooked() throws IOException, DefinitionParseException
    {
        // 1. Read all input in mostly unprocessed form

        InputLine line;
        while ((line = _lineReader.nextLine()) != null) {
            final String contents = line.getContents();
            StringAndOffset p = TokenHelper.findKeyword(contents, 0);
            if (p == null) {
                line.reportError(0, "No keyword found from line; expected one of %s", KNOWN_KEYWORDS);
            }

            final String keyword = p.match;
            switch (keyword) {
			case "pattern":
				readPatternDefinition(line, p.restOffset);
				break;
			case "template":
				readTemplateDefinition(line, p.restOffset);
				break;
			case "extract":
				readExtractionDefinition(line, p.restOffset);
				break;
			default:
				line.reportError(0, "Unrecognized keyword \"%s\" encountered; expected one of %s",
						keyword, KNOWN_KEYWORDS);
			}
		}

		// Ok; done reading all.
    }

    /*
    /**********************************************************************
    /* Per-declaration-type parsing
    /**********************************************************************
     */
    
    private void readPatternDefinition(InputLine line, int offset) throws DefinitionParseException
    {
        final String contents = line.getContents();
        int ix = TokenHelper.findTypeMarker('%', contents, offset);
        if (ix < 0) {
            line.reportError(offset, "Pattern name must be prefixed with '%'");
        }
        offset = ix+1;// to skip percent marker
        // then read name, do require white space after, to be skipped
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("pattern", line, contents, offset);
        String name = p.match;

        // First, verify this is not dup
        UncookedDefinition unp = new UncookedDefinition(line, name);
        UncookedDefinition old = _uncooked.addPattern(name, unp);
        if (old != null) {
            line.reportError(offset, "Duplicate pattern definition for name '%s'", name);
        }
        offset = p.restOffset;
        
        // And then need to find cross-refs
        // First a quick and cheesy check for common case of no expansions
        final int end = contents.length();
        ix = contents.indexOf('%', offset);
        if (ix < 0) {
            unp.appendLiteralPattern(contents.substring(offset), offset);
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (ix > 0) {
            sb.append(contents.substring(offset, ix));
        }
        int literalStart = offset;
        while (ix < end) {
            char c = contents.charAt(ix++);
            if (c != '%') {
                sb.append(c);
                continue;
            }
            if (ix == end) {
                line.reportError(ix, "Orphan '%%' at end of pattern '%s' definition", name);
            }
            c = contents.charAt(ix);
            if (c == '%') {
                sb.append(c);
                ++ix;
                continue;
            }
            StringAndOffset ref = TokenHelper.parseName("pattern", line, contents, ix);
            // Re-calc where we continue from etc
            String refName = ref.match;

            if (sb.length() > 0) {
                unp.appendLiteralPattern(sb.toString(), literalStart);
                sb.setLength(0);
            }
            unp.appendPatternRef(refName, ix);
            ix = ref.restOffset;
            literalStart = offset;
        }

        if (sb.length() > 0) {
            unp.appendLiteralPattern(sb.toString(), literalStart);
        }
    }

    private void readTemplateDefinition(InputLine line, int offset) throws DefinitionParseException
    {
        final String contents = line.getContents();
        int ix = TokenHelper.findTypeMarker('@', contents, offset);
        if (ix < 0) {
            line.reportError(offset, "Template name must be prefixed with '@'");
        }
        offset = ix+1;
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("template", line, contents, offset);
        String name = p.match;

        // First, verify this is not dup
        UncookedDefinition unp = new UncookedDefinition(line, name);
        UncookedDefinition old = _uncooked.addTemplate(name, unp);
        if (old != null) {
            line.reportError(offset, "Duplicate template definition for name '%s'", name);
        }
        _readTemplateContents(line, p.restOffset, unp, -1,
                "template '"+name+"' definition");
    }

    /**
     * Shared parsing method implementation that handles parsing of contents of either
     * template, or extractor.
     */
    private int _readTemplateContents(InputLine line, int ix, DefPieceContainer container,
            int parenCount, String desc)
        throws DefinitionParseException
    {
        // And then need to find template AND pattern references, literal patterns
        final String contents = line.getContents();
        final int end = contents.length();
        StringBuilder sb = new StringBuilder();
        int literalStart = ix;
        while (ix < end) {
            char c = contents.charAt(ix++);
            if ((c == '%') || (c == '@') || (c == '$')) {
                if (ix == end) {
                    line.reportError(ix, "Orphan '%c' at end of %s", c, desc);
                }
                // doubling up used as escaping mechanism:
                char d = contents.charAt(ix);
                if (c == d) {
                    sb.append(c);
                    ++ix;
                    continue;
                }
                if (sb.length() > 0) {
                    container.appendLiteralText(sb.toString(), literalStart);
                    sb.setLength(0);
                }

                // literal patterns allowed
                StringAndOffset p;
                if (c == '%') {
                    if (d == '{') {
                        ++ix;
                        p = TokenHelper.parseInlinePattern(line, contents, ix);
                        container.appendLiteralPattern(p.match, ix);
                    } else {
                        // otherwise named ref
                        p = TokenHelper.parseName("pattern", line, contents, ix);
                        container.appendPatternRef(p.match, ix);
                    }
                    ix = p.restOffset;
                } else if (c == '@') { // template, only named refs
                    p = TokenHelper.parseName("template", line, contents, ix);
                    container.appendTemplateRef(p.match, ix);
                    ix = p.restOffset;
                } else { // must be '$', extractor definition
                    p = TokenHelper.parseName("extractor", line, contents, ix);
                    ExtractorExpression extr = container.appendExtractor(p.match, ix);
                    ix = p.restOffset;
                    // That was simple, but now need to decode contents, recursively
                    ix = _readInlineExtractor(line, ix, extr);
                }
                literalStart = ix;
                continue;
            }
            // When parsing contents of an extractor
            if (parenCount > 0) {
                if (c == '(') {
                    ++parenCount;
                } else if (c == ')') {
                    if (--parenCount == 0) {
                        break;
                    }
                }
                // but append normally, unless we bailed out
            }
            sb.append(c);
        }

        if (sb.length() > 0) {
            container.appendLiteralText(sb.toString(), literalStart);
        }

        if (parenCount > 0) {
            line.reportError(ix, "Missing closing parenthesis at end of %s", desc);
        }
        return ix;
    }

    /**
     * Method that will read contents of a given inline extractor definition, up to
     * closing parenthesis
     */
    private int _readInlineExtractor(InputLine line, int ix, ExtractorExpression extr)
        throws DefinitionParseException
    {
        final String contents = line.getContents();
        final int end = contents.length();

        if ((ix >= end) || contents.charAt(ix) != '(') {
            line.reportError(ix, "Invalid declaration for extractor '%s': missing opening parenthesis",
                    extr.getName());
        }
        ++ix;

        return _readTemplateContents(line, ix, extr, 1,
                "extractor '"+extr.getName()+"' expression");
    }

    private void readExtractionDefinition(InputLine line, int offset) throws IOException
    {
        String contents = line.getContents();
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("extraction", line, contents, offset);
        String name = p.match;

        // And the rest should consist of just a single open curly brace, and optional white space
        int ix = TokenHelper.matchRemaining(contents, p.restOffset, '{');
        if (ix != contents.length()) {
            line.reportError(p.restOffset, "Unexpected content for extraction '%s': expected only opening '{'",
                    name);
        }

        UncookedDefinition template = null;
        Map<String,Object> append = null;
        
        // For contents within, should have name/content sections
        while (true) {
            line = _lineReader.nextLine();
            if (line == null) {
                _lineReader.reportError("Unexpected end-of-input in extraction '%s' definition", name);
            }
            contents = line.getContents();
            // Either name/value pair, or closing brace
            ix = TokenHelper.matchRemaining(contents, 0, '}');
            if (ix >= 0) {
                if (ix >= contents.length()) {
                    break;
                }
                line.reportError(p.restOffset, "Unexpected content after closing '}' for extraction '%s'",
                        name);
            }

            ix = TokenHelper.skipSpace(contents, 0);
            p = TokenHelper.parseNameAndSkipSpace("extraction", line, contents, ix);
            ix = p.restOffset;
            String prop = p.match;

            switch (prop) {
            case "template":
                {
                    if (template != null) {
                        line.reportError(ix, "More than one 'template' specified for '"+name+"'");
                    }
                    template = new UncookedDefinition(line, "");
                    ix = _readTemplateContents(line, ix, template,
                            0, "extraction template for '"+name+"'");
                }
                break;
            case "append":
                // Contents are JSON, but for convenience we wrap it as Object if not
                // already Object (since we must get key/value pairs)
                {
                    append = _readAppend(line, ix, contents.substring(ix), append);
                }
                break;
            default:
                line.reportError(ix, "Unrecognized extraction property \"%s\" encountered; expected one of %s",
                        prop, EXTRACTOR_PROPERTIES);
            }
        }

        if (template == null) {
            line.reportError(ix, "Missing 'template' for extraction '%s'", name);
        }
        
        UncookedExtraction extr = new UncookedExtraction(line, name, template, append);
        _uncooked.addExtraction(name, extr);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private Map<String,Object> _readAppend(InputLine line, int offset,
            String rawJson, Map<String,Object> old)
        throws DefinitionParseException
    {
        rawJson = rawJson.trim();
        if (rawJson.isEmpty()) {
            return old;
        }
        
        // First things first: ensure it's a JSON Object
        if (!rawJson.startsWith("{")) {
            // but must start with a field name
            if (rawJson.startsWith("\"")) {
                rawJson = "{" + rawJson + "}";
            }
        }
        Object raw;
        try {
            raw = _json.anyFrom(rawJson);
        } catch (Exception e) {
            DefinitionParseException exc = DefinitionParseException.construct
                    ("Invalid JSON content to 'append': "+e.getMessage(), line, offset);
            exc.initCause(e);
            throw exc;
        }
        if (!(raw instanceof Map<?,?>)) {
            line.reportError(offset,
                    "Invalid 'append' value: must be JSON Object, or sequence of key/value pairs; was parsed as %s",
                    raw.getClass().getName());
        }
        @SuppressWarnings("unchecked")
        Map<String,Object> newStuff = (Map<String,Object>) raw;

        if (old == null) {
            return newStuff;
        }
        old.putAll(newStuff);
        return old;
    }
}
