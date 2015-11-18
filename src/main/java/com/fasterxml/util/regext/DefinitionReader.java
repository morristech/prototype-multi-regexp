package com.fasterxml.util.regext;

import java.io.*;

import com.fasterxml.util.regext.io.InputLine;
import com.fasterxml.util.regext.io.InputLineReader;
import com.fasterxml.util.regext.model.UncookedDefinitions;
import com.fasterxml.util.regext.model.UncookedDefinition;
import com.fasterxml.util.regext.util.StringAndOffset;
import com.fasterxml.util.regext.util.TokenHelper;

public class DefinitionReader
{
    private final static String KNOWN_KEYWORDS =
            "(pattern, template, extract)"
            ;

    protected InputLineReader _lineReader;

    protected UncookedDefinitions _uncooked;

    protected DefinitionReader(InputLineReader lineReader) {
        _lineReader = lineReader;
        _uncooked = new UncookedDefinitions();
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
        // !!! TBI
        return null;
    }

    public void readUncooked() throws IOException
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
        UncookedDefinition unp = new UncookedDefinition(line);
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
            unp.appendLiteralPattern(contents, offset);
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
        UncookedDefinition unp = new UncookedDefinition(line);
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
        final String contents = line.getContents();
        throw new UnsupportedOperationException();
    }
}
