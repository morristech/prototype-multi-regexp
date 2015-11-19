package com.fasterxml.util.regext;

import com.fasterxml.util.regext.autom.PolyMatcher;

/**
 * Test that verifies that Polymatcher constructed from definitions seems
 * to work to basic level.
 */
public class PolyMatchTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %word ([a-zA-Z]+)\n"+
"template @base %word\n"+
"extract rule1 {  \n"+
"  template @base value=$value(%word) value2=$value2(%word)\n"+
"}\n"+
"extract rule2 {  \n"+
"  template value=%word\n"+
"}\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        ExtractionDefinition def = defR.read();
        PolyMatcher matcher = def.getMatcher();

        int[] matches = matcher.match("value=stuff");
        assertEquals(1, matches.length);
        assertEquals(1, matches[0]);

        matches = matcher.match("prefix value=a value2=b");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0]);
    }
}
