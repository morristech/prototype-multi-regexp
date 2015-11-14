package com.fasterxml.util.regext.io;

import java.io.*;

/**
 * Simple line-oriented reader abstraction that adds following features on top of
 * standard {@link BufferedReader}:
 *<ol>
 * <li>Skips all lines starting with '#' (possibly prefixed with whitespace)
 *  </li>
 * <li>Skips all empty lines (only whitespace)
 *  </li>
 * <li>Keeps track of line numbers (similar to {@link java.io.LineNumberReader}),
 *    for error reporting purposes
 *  </li>
 * <li>Combines multi-line segments (physical lines that end with backslash character)
 *    into single logical lines, represented as {@link InputLine}s.
 *  </li>
 *</ol>
 */
public class InputLineReader
{
    protected final Object _sourceRef;

    protected final BufferedReader _reader;

    /**
     * Row is 1-based, but advanced after reading physical line; hence this
     * always refers to the row that was just read (and 0 before any reads).
     */
    protected int _row = 0;

    protected InputLineReader(Object srcRef, BufferedReader r) {
        _sourceRef = srcRef;
        _reader = r;
    }

    public static InputLineReader construct(Object srcRef, InputStream in) throws IOException {
        return construct(srcRef, new InputStreamReader(in, "UTF-8"));
    }

    public static InputLineReader construct(Object srcRef, Reader r) throws IOException {
        BufferedReader br = (r instanceof BufferedReader)
                ? ((BufferedReader) r)
                        : new BufferedReader(r);
        return new InputLineReader(srcRef, br);
    }

    public void close() {
        try {
            _reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public InputLine nextLine() throws IOException
    {
        String line = _nextContentLine();
        if (line == null) {
            return null;
        }
        
        int start = _row;
        if (!line.endsWith("\\")) {
            return InputLine.create(_sourceRef, start, line);
        }
        line = line.substring(0, line.length() - 1);
        InputLine combo = InputLine.create(_sourceRef, start, line);

        while (true) {
            line = _nextContentLine();
            // Illegal to end with continuation
            if (line == null)  {
                _reportError("Unexpected end-of-input when expecting line continuation'");
            }
            if (!line.endsWith("\\")) {
                return combo.appendSegment(line);
            }
            line = line.substring(0, line.length() - 1);
            combo = combo.appendSegment(line);
        }
    }

    protected String _nextContentLine() throws IOException
    {
        while (true) {
            String line = _reader.readLine();
            if (line == null) {
                return line;
            }
            ++_row;
            if (!_isEmptyOrComment(line)) {
                return line;
            }
        }
    }

    protected boolean _isEmptyOrComment(String line) {
        for (int i = 0, end = line.length(); i < end; ++i) {
            int ch = line.charAt(i);
            if (ch <= 0x0020) { // skip whitespace
                continue;
            }
            return (ch == '#');
        }
        // must be whitespace if we got this far
        return true;
    }

    protected void _reportError(String msg) throws IOException {
        throw new IOException(String.format("(%s, row %d): %s", _sourceRef, _row, msg));
    }
}
