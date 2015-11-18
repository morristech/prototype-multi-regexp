package com.fasterxml.util.regext;

import java.io.IOException;
import java.util.*;

import com.fasterxml.util.regext.model.DefPiece;
import com.fasterxml.util.regext.model.LiteralPattern;
import com.fasterxml.util.regext.model.LiteralText;
import com.fasterxml.util.regext.model.PatternReference;
import com.fasterxml.util.regext.model.TemplateReference;
import com.fasterxml.util.regext.model.UncookedDefinitions;
import com.fasterxml.util.regext.model.UncookedDefinition;

public class UncookedDefTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %ws \\s+\n"+
"pattern %optws \\s*\n"+
"pattern %'phrase' \\S+\n"+
"pattern %\"maybeUUID\" %'phrase'\n"+
"# in addition to Java id chars, hyphen is also valid:\n"+
"pattern %host-name %\"phrase\"\n"+
"\n"+
"template @simple Prefix:\n"+
"template @'base' %phrase%optws(sic!) @simple %host-name\n"+

				"";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;

        Map<String,UncookedDefinition> patterns = def.getPatterns();
        
        assertEquals(5, patterns.size());

        assertTrue(patterns.containsKey("ws"));
        assertTrue(patterns.containsKey("optws"));
        assertTrue(patterns.containsKey("phrase"));
        assertTrue(patterns.containsKey("maybeUUID"));
        assertTrue(patterns.containsKey("host-name"));

        Map<String,UncookedDefinition> templates = def.getTemplates();
        assertEquals(2, templates.size());
        assertTrue(templates.containsKey("simple"));
        assertTrue(templates.containsKey("base"));
    }

    public void testPatternRefsInPatterns() throws Exception
    {
        final String DEF =
"pattern %wsChar \\s\n"+
"pattern %optws %wsChar*%%\n"+
"pattern %word ([a-z]+)\n"+
"pattern %phrase3   %word %word2%word3\n"
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> patterns = def.getPatterns();

        assertEquals(4, patterns.size());
        List<DefPiece> parts;

        // Let's see handling of composite definition
        UncookedDefinition optws = patterns.get("optws");
        assertNotNull(optws);

        parts = optws.getParts();
        assertEquals(2, parts.size());
        assertEquals(PatternReference.class, parts.get(0).getClass());
        assertEquals("wsChar", parts.get(0).getText());
        assertEquals(LiteralPattern.class, parts.get(1).getClass());
        assertEquals("*%", parts.get(1).getText());
        
        UncookedDefinition p3 = patterns.get("phrase3");
        assertNotNull(p3);
        
        parts = p3.getParts();
        assertEquals(4, parts.size());
        assertEquals(PatternReference.class, parts.get(0).getClass());
        assertEquals("word", parts.get(0).getText());
        assertEquals(LiteralPattern.class, parts.get(1).getClass());
        assertEquals(" ", parts.get(1).getText());
        assertEquals(PatternReference.class, parts.get(2).getClass());
        assertEquals("word2", parts.get(2).getText());
        assertEquals(PatternReference.class, parts.get(3).getClass());
        assertEquals("word3", parts.get(3).getText());
    }

    public void testTemplateRefsInPatterns() throws Exception
    {
        final String DEF =
"pattern %wsChar \\s\n"+
"\n"+
"template @base Stuff:\n"+
"template @actual @'base'%'wsChar'and%{\\s}more\n"+
""
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> templates = def.getTemplates();

        assertEquals(2, templates.size());
        List<DefPiece> parts;

        // Let's see handling of composite definition
        UncookedDefinition base = templates.get("base");
        assertNotNull(base);

        parts = base.getParts();
        assertEquals(1, parts.size());
        assertEquals(LiteralText.class, parts.get(0).getClass());
        assertEquals("Stuff:", parts.get(0).getText());
        
        UncookedDefinition p5 = templates.get("actual");
        assertNotNull(p5);
        
        parts = p5.getParts();
        assertEquals(5, parts.size());
        assertEquals(TemplateReference.class, parts.get(0).getClass());
        assertEquals("base", parts.get(0).getText());
        assertEquals(PatternReference.class, parts.get(1).getClass());
        assertEquals("wsChar", parts.get(1).getText());
        assertEquals(LiteralText.class, parts.get(2).getClass());
        assertEquals("and", parts.get(2).getText());
        assertEquals(LiteralPattern.class, parts.get(3).getClass());
        assertEquals("\\s", parts.get(3).getText());
        assertEquals(LiteralText.class, parts.get(4).getClass());
        assertEquals("more", parts.get(4).getText());
    }
    
    // // // // Tests for failure handling
    
    public void testDupPatternName() throws Exception
    {
        final String DEF =
"pattern %'ws' \\s+\n"+
"pattern %optws \\s*\n"+
"pattern %ws \\S+\n"
        ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.readUncooked();
            fail("Should have detected duplicate name");
        } catch (IOException e) {
            verifyException(e, "duplicate");
        }
    }

    public void testOrphanPercent() throws Exception
    {
        final String DEF =
"pattern %'ws' \\s+%\n"
        ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.readUncooked();
            fail("Should have detected duplicate name");
        } catch (IOException e) {
            verifyException(e, "Orphan '%'");
        }
    }
}
