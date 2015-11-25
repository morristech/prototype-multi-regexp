package com.fasterxml.util.regext.model;

/**
 * Add-on interface for things onto which {@link DefPiece}s
 * may be appended; typically cooked definitions.
 */
public interface DefPieceAppendable {
    public String getName();
    public Iterable<DefPiece> getParts();

    public void append(DefPiece part);
}
