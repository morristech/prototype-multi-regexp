package com.fasterxml.util.regext.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.util.regext.io.InputLine;

public class TemplateReference extends DefPiece
    implements DefPieceAppendable
{
    protected List<DefPiece> _parameters;

    public TemplateReference(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }

    public boolean takesParameters() {
        return _parameters != null;
    }

    public List<DefPiece> getParameters() {
        return _parameters;
    }

    // // // DefPieceAppendable
    
    @Override
    public String getName() {
        return getText();
    }

    @Override
    public Iterable<DefPiece> getParts() {
        if (_parameters == null) {
            return Collections.emptyList();
        }
        return _parameters;
    }

    @Override
    public void append(DefPiece part) {
        if (_parameters == null) {
            _parameters = new ArrayList<>();
        }
        _parameters.add(part);
    }
}
