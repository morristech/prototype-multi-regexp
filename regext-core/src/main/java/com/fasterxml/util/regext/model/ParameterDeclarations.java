package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

public class ParameterDeclarations
{
    private final String _types;

    public ParameterDeclarations(String types) {
        _types = types;
    }

    public int size() {
        return _types.length();
    }

    public char getType(InputLine src, int srcOffset, int pos) {
        return _types.charAt(pos-1);
    }
}
