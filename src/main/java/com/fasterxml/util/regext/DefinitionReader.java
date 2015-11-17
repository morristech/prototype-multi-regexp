package com.fasterxml.util.regext;

import java.io.*;
import java.util.*;

import com.fasterxml.util.regext.io.InputLine;
import com.fasterxml.util.regext.io.InputLineReader;
import com.fasterxml.util.regext.model.UncookedExtractions;
import com.fasterxml.util.regext.model.UncookedPattern;
import com.fasterxml.util.regext.util.StringPair;
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
			StringPair p = TokenHelper.findKeyword(0, line.getContents());
			if (p == null) {
				line.reportError(0, "No keyword found from line; expected one of %s", KNOWN_KEYWORDS);
			}

			final String keyword = p.left();
			final String content = p.right();
			switch (keyword) {
			case "pattern":
				readPatternDefinition(line, p.rightOffset(), content);
				break;
			case "template":
				readTemplateDefinition(line, p.rightOffset(), content);
				break;
			case "extract":
				readExtractDefinition(line, p.rightOffset(), content);
				break;
			default:
				line.reportError(p.leftOffset(), "Unrecognized keyword \"%s\" encountered; expected one of %s",
						keyword, KNOWN_KEYWORDS);
			}
		}

		// Ok; done reading all.
	}

    private void readPatternDefinition(InputLine line, int offset, String content) throws IOException
    {
        int i = TokenHelper.findTypeMarker('%', content);
        if (i < 0) {
            line.reportError(offset, "Pattern name must start with '%'");
        }
        offset += i;
        content = content.substring(i+1);
        StringPair p = TokenHelper.parseName("pattern", line, offset, content);
        String name = p.left();
        String pattern = p.right();

        // First, verify this is not dup
        UncookedPattern unp = new UncookedPattern(line);
        UncookedPattern old = _uncooked.addPattern(name, unp);
        if (old != null) {
            line.reportError(p.leftOffset(), "Duplicate pattern definition for name '%s'", name);
        }
        offset = p.rightOffset();

        // And then need to find cross-refs
        // First a quick and cheesy check for common case of no expansions
        final int end = pattern.length();
        int ix = pattern.indexOf('%');
        if (ix < 0) {
            unp.append(pattern, null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (ix > 0) {
            sb.append(pattern.substring(0, ix));
        }
        while (ix < end) {
            char c = pattern.charAt(ix++);
            if (c != '%') {
                sb.append(c);
                continue;
            }
            if (ix == (end - 1)) {
                line.reportError(offset+ix, "Orphan '%' at end of pattern definition", name);
            }
            c = pattern.charAt(ix);
            if (c == '%') {
                sb.append('%');
                ++ix;
                continue;
            }
            StringPair refPair = TokenHelper.parseName("pattern", line, offset+ix, content.substring(ix));
            // Re-calc where we continue from etc
            String refName = refPair.left();
            ix = pattern.length() - refPair.right().length();

            unp.append(sb.toString(), refName);
            sb.setLength(0);
        }

        if (sb.length() > 0) {
            unp.append(sb.toString(), null);
        }
    }

	private void readTemplateDefinition(InputLine line, int offset, String content) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	private void readExtractDefinition(InputLine line, int offset, String content) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
