package com.fasterxml.util.regext.model;

public interface DefPieceContainer
{
    public String getName();

    public void appendLiteralPattern(String literal, int offset);
    public void appendLiteralText(String literal, int offset);

    public void appendPatternRef(String name, int offset);
    public void appendTemplateRef(String name, int offset);

    public void appendTemplateVariable(int varPos, int offset);

    public ExtractorExpression appendExtractor(String name, int offset);

    /**
     * Method for appending extractor whose name is not known, but will be
     * passed in as a template name parameter (variable)
     */
    public ExtractorExpression appendVariableExtractor(int varPos, int offset);
}

