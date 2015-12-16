package com.fasterxml.util.regext.model;

public class ParameterDeclarations
{
    private final String _types;

    public ParameterDeclarations(String types) {
        _types = types;
    }

    public int size() {
        return _types.length();
    }

    public String getTypes() {
        return _types;
    }
    
    /**
     * @param index 1-based index
     */
    public char getType(int index) {
        if ((index < 1) || (index > _types.length())) {
            throw new IllegalArgumentException("Invalid type index "+index+"; valid indexes [1.."
                    +_types.length()+"]");
        }
        return _types.charAt(index-1);
    }
}
