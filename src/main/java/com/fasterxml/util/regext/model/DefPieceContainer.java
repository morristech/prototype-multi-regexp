package com.fasterxml.util.regext.model;

public interface DefPieceContainer
{
    public String getName();

    public void appendLiteralPattern(String literal, int offset);
    public void appendLiteralText(String literal, int offset);
    public void appendPatternRef(String name, int offset);
    public void appendTemplateRef(String name, int offset);
    public ExtractorExpression appendExtractor(String name, int offset);
}
