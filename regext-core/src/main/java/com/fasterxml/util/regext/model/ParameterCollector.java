package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.DefinitionParseException;
import com.fasterxml.util.regext.io.InputLine;

/**
 * Simple container for keeping track of template and extractor variables
 * that declarations may have; these are simple index-references (instead of
 * named ones)
 */
public class ParameterCollector
{
    protected final StringBuilder _types = new StringBuilder(10);

    protected int _count;

    public ParameterCollector() { }

    public void add(InputLine src, int srcOffset,
            int pos, char type) throws DefinitionParseException
    {
        --pos;
        if (pos >= _count) { // new one, past the end
            if (pos >= _types.length()) {
                _types.setLength(pos+1);
            }
            _count = pos+1;
        } else { // might already have; verify
            char old = _types.charAt(pos);
            if ((old != type) && (old != '\0')) {
                throw DefinitionParseException.construct(String.format(
                        "Inconsistent references to parameter %d: %c vs %c", pos+1, old, type),
                        src, srcOffset);
            }
        }
        _types.setCharAt(pos, type);
    }

    public ParameterDeclarations constructDeclarations() {
        if (_types.length() != _count) {
            _types.setLength(_count);
        }
        return new ParameterDeclarations(_types.toString());
    }
}
