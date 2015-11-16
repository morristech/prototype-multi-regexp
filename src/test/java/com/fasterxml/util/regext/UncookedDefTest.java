package com.fasterxml.util.regext;

import java.util.Map;

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
"pattern %hostname %\"phrase\"\n"
				;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedExtractions def = defR._uncooked;
        Map<String,UncookedPattern> patterns = def.getPatterns();
        
        for (String name : patterns.keySet()) {
            System.out.println("Pattern '"+name+"' -> "+patterns.get(name));
        }
        
        assertEquals(5, patterns.size());
    }
}
