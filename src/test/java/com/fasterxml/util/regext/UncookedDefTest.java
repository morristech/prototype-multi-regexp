package com.fasterxml.util.regext;

import java.io.IOException;
import java.util.*;

import com.fasterxml.util.regext.model.DefPiece;
import com.fasterxml.util.regext.model.LiteralPattern;
import com.fasterxml.util.regext.model.PatternReference;
import com.fasterxml.util.regext.model.UncookedExtractions;
import com.fasterxml.util.regext.model.UncookedPattern;

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
"pattern %host-name %\"phrase\"\n"
				;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedExtractions def = defR._uncooked;
        Map<String,UncookedPattern> patterns = def.getPatterns();

        /*
        for (String name : patterns.keySet()) {
            System.out.println("Pattern '"+name+"' -> "+patterns.get(name));
        }
        */
        
        assertEquals(5, patterns.size());

        assertTrue(patterns.containsKey("ws"));
        assertTrue(patterns.containsKey("optws"));
        assertTrue(patterns.containsKey("phrase"));
        assertTrue(patterns.containsKey("maybeUUID"));
        assertTrue(patterns.containsKey("host-name"));
    }

    public void testRefsInPatterns() throws Exception
    {
        final String DEF =
"pattern %wsChar \\s\n"+
"pattern %optws %wsChar*\n"+
"pattern %word ([a-z]+)\n"+
"pattern %phrase3   %word %word2%word3\n"
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedExtractions def = defR._uncooked;
        Map<String,UncookedPattern> patterns = def.getPatterns();

        assertEquals(4, patterns.size());
        List<DefPiece> parts;

        // Let's see handling of composite definition
        UncookedPattern optws = patterns.get("optws");
        assertNotNull(optws);

        parts = optws.getParts();
        assertEquals(2, parts.size());
        assertEquals(PatternReference.class, parts.get(0).getClass());
        assertEquals("wsChar", parts.get(0).getText());
        assertEquals(LiteralPattern.class, parts.get(1).getClass());
        assertEquals("*", parts.get(1).getText());
        
        UncookedPattern p3 = patterns.get("phrase3");
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
}
