package com.fasterxml.util.regext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.util.regext.model.UncookedExtractions;
import com.fasterxml.util.regext.model.UncookedPattern;
import com.fasterxml.util.regext.model.UncookedPattern.Segment;

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
"pattern %phrase3 %word %word%word\n"
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedExtractions def = defR._uncooked;
        Map<String,UncookedPattern> patterns = def.getPatterns();

        assertEquals(4, patterns.size());

        // Let's see handling of composite definition
        UncookedPattern p3 = patterns.get("phrase3");
        assertNotNull(p3);
        List<Segment> segs = p3.getParts();
        assertEquals(3, segs.size());
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
