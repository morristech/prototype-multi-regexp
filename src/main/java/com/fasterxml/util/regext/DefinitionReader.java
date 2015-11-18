package com.fasterxml.util.regext;

import java.io.*;

import com.fasterxml.util.regext.io.InputLine;
import com.fasterxml.util.regext.io.InputLineReader;
import com.fasterxml.util.regext.model.UncookedExtractions;
import com.fasterxml.util.regext.model.UncookedPattern;
import com.fasterxml.util.regext.util.StringAndOffset;
import com.fasterxml.util.regext.util.TokenHelper;

public class DefinitionReader
{
    private final static String KNOWN_KEYWORDS =
            "(pattern, template, extract)"
            ;

    protected InputLineReader _lineReader;

    protected UncookedExtractions _uncooked;

    protected DefinitionReader(InputLineReader lineReader) {
        _lineReader = lineReader;
        _uncooked = new UncookedExtractions();
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
				readExtractDefinition(line, p.restOffset);
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
        offset = TokenHelper.findTypeMarker('%', contents, offset);
        if (offset < 0) {
            line.reportError(offset, "Pattern name must start with '%'");
        }
        ++offset; // to skip percent marker
        // then read name, do require white space after, to be skipped
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("pattern", line, contents, offset);
        String name = p.match;

        // First, verify this is not dup
        UncookedPattern unp = new UncookedPattern(line);
        UncookedPattern old = _uncooked.addPattern(name, unp);
        if (old != null) {
            line.reportError(offset, "Duplicate pattern definition for name '%s'", name);
        }
        offset = p.restOffset;
        
        // And then need to find cross-refs
        // First a quick and cheesy check for common case of no expansions
        final int end = contents.length();
        int ix = contents.indexOf('%', offset);
        if (ix < 0) {
            unp.append(contents, null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (ix > 0) {
            sb.append(contents.substring(offset, ix));
        }
        while (ix < end) {
            char c = contents.charAt(ix++);
            if (c != '%') {
                sb.append(c);
                continue;
            }
            if (ix == (end - 1)) {
                line.reportError(offset+ix, "Orphan '%' at end of pattern definition", name);
            }
            c = contents.charAt(ix);
            if (c == '%') {
                sb.append('%');
                ++ix;
                continue;
            }
            StringAndOffset ref = TokenHelper.parseName("pattern", line, contents, ix);
            // Re-calc where we continue from etc
            String refName = ref.match;
            ix = ref.restOffset;

            if (sb.length() > 0) {
                unp.append(sb.toString(), refName);
                sb.setLength(0);
            } else {
                unp.append(null, refName);
            }
        }

        if (sb.length() > 0) {
            unp.append(sb.toString(), null);
        }
    }

    private void readTemplateDefinition(InputLine line, int offset) throws IOException
    {
        final String contents = line.getContents();
        throw new UnsupportedOperationException();
    }

    private void readExtractDefinition(InputLine line, int offset) throws IOException
    {
        final String contents = line.getContents();
        throw new UnsupportedOperationException();
    }
}
