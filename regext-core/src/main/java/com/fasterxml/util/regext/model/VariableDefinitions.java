package com.fasterxml.util.regext.model;

import com.fasterxml.util.regext.io.InputLine;

public class VariableDefinitions
{
    private final String _types;

    public VariableDefinitions(String types) {
        _types = types;
    }

    public int size() {
        return _types.length();
    }

    public char getType(InputLine src, int srcOffset, int pos) {
        return _types.charAt(pos-1);
    }
}
