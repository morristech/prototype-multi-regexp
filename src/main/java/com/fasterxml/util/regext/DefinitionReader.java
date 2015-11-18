package com.fasterxml.util.regext;

import java.io.*;
import java.util.*;

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

    protected final InputLineReader _lineReader;

    protected final UncookedDefinitions _uncooked;

    protected final CookedDefinitions _cooked;
    
    protected DefinitionReader(InputLineReader lineReader) {
        _lineReader = lineReader;
        _uncooked = new UncookedDefinitions();
        _cooked = new CookedDefinitions();
    }

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
        // !!! TBI
        return null;
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
    
    private void readPatternDefinition(InputLine line, int offset) throws IOException
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

    private void readTemplateDefinition(InputLine line, int offset) throws IOException
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
        offset = p.restOffset;

        // And then need to find template AND pattern references, literal patterns
        final int end = contents.length();
        StringBuilder sb = new StringBuilder();
        ix = offset;
        int literalStart = offset;
        while (ix < end) {
            char c = contents.charAt(ix++);
            if ((c == '%') || (c == '@')) {
                if (ix == end) {
                    line.reportError(ix, "Orphan '%c' at end of template '%s' definition", c, name);
                }
                // doubling up used as escaping mechanism:
                char d = contents.charAt(ix);
                if (c == d) {
                    sb.append(c);
                    ++ix;
                    continue;
                }
                if (sb.length() > 0) {
                    unp.appendLiteralText(sb.toString(), literalStart);
                    sb.setLength(0);
                }

                // literal patterns allowed
                if (c == '%') {
                    if (d == '{') {
                        ++ix;
                        p = TokenHelper.parseInlinePattern(line, contents, ix);
                        unp.appendLiteralPattern(p.match, ix);
                    } else {
                        // otherwise named ref
                        p = TokenHelper.parseName("pattern", line, contents, ix);
                        unp.appendPatternRef(p.match, ix);
                    }
                } else { // template, only named refs
                    p = TokenHelper.parseName("template", line, contents, ix);
                    unp.appendTemplateRef(p.match, ix);
                }
                ix = p.restOffset;
                literalStart = offset;
                continue;
            }
            sb.append(c);
        }

        if (sb.length() > 0) {
            unp.appendLiteralText(sb.toString(), literalStart);
        }
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

        UncookedExtraction extr = new UncookedExtraction(line, name);
        _uncooked.addExtraction(name, extr);

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
            String prop = p.match;

            // !!! TODO: handle contents
        }

        // !!! TODO: finalize or something

    }

    /*
    /**********************************************************************
    /* Resolution: patterns
    /**********************************************************************
     */

    /**
     * First part of resolution: resolving and flattening of pattern declarations.
     * After this step, 
     */
    public void resolvePatterns() throws IOException
    {
        Map<String,UncookedDefinition> rawPatterns = _uncooked.getPatterns();
        for (UncookedDefinition uncooked : rawPatterns.values()) {
            String name = uncooked.getName();
            if (_cooked.findPattern(name) != null) { // due to recursion, may have done it already
                continue;
            }
            _cooked.addPattern(name, _resolvePattern(name, uncooked, null));
        }
    }

    private LiteralPattern _resolvePattern(String name, UncookedDefinition def,
            List<String> stack) throws DefinitionParseException
    {
        // Minor optimization: we might have just one part
        List<DefPiece> pieces = def.getParts();
        if (pieces.size() == 1) {
            DefPiece piece = pieces.get(0);
            if (piece instanceof LiteralPattern) {
                return (LiteralPattern) piece;
            }
            // must be reference (only literals and refs)
            if (stack == null) {
                stack = new LinkedList<>();
            }
            return _resolvePatternReference(name, (PatternReference) piece, stack);
        }
        StringBuilder sb = new StringBuilder(100);
        for (DefPiece piece : pieces) {
            LiteralPattern lit;
            if (piece instanceof LiteralPattern) {
                lit = (LiteralPattern) piece;
            } else {
                // must be reference (only literals and refs)
                if (stack == null) {
                    stack = new LinkedList<>();
                }
                lit = _resolvePatternReference(name, (PatternReference) piece, stack);
            }
            sb.append(lit.getText());
        }
        return new LiteralPattern(def.getSource(), pieces.get(0).getSourceOffset(), sb.toString());
    }

    private LiteralPattern _resolvePatternReference(String fromName, PatternReference def,
            List<String> stack) throws DefinitionParseException
    {
        final String toName = def.getText();
        // very first thing: maybe already resolved?
        LiteralPattern res = _cooked.findPattern(toName);
        if (res != null) {
            return res;
        }

        // otherwise verify that we have no loop
        stack.add(fromName);
        if (stack.contains(toName)) {
            def.reportError("Cyclic pattern reference to '%%%s' (%s)",
                    toName, _stackDesc("%", stack, toName));
        }
        UncookedDefinition raw = _uncooked.findPattern(toName);
        if (raw == null) {
            def.reportError("Referencing non-existing pattern '%%%s' (%s)",
                    toName, _stackDesc("%", stack, toName));
        }
        LiteralPattern p = _resolvePattern(toName, raw, stack);
        _cooked.addPattern(toName, p);
        // but remove from stack
        stack.remove(stack.size()-1);
        return p;
    }

    private String _stackDesc(String marker, List<String> stack, String last) {
        StringBuilder sb = new StringBuilder(100);
        for (String str : stack) {
            sb.append(marker).append(str);
            sb.append("->");
        }
        sb.append(marker).append(last);
        return sb.toString();
    }
    
    /*
    /**********************************************************************
    /* Resolution: templates
    /**********************************************************************
     */
}
