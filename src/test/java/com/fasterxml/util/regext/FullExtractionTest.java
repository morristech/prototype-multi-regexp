package com.fasterxml.util.regext;

import java.util.Map;

public class FullExtractionTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %word ([a-zA-Z]+)\n"+
"template @base %word\n"+
"extract double {  \n"+
"  template @base value=$value(%word) value2=$value2(%word)\n"+
"}\n"+
"extract single {  \n"+
"  template value=$value(%word)\n"+
"}\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        ExtractionDefinition def = defR.read();

        ExtractionResult result = def.match("value=foobar");
        assertNotNull(result);
        assertEquals("single", result.getId());
        Map<String,Object> stuff = result.asMap("id");
        assertEquals("single", stuff.get("id"));
        assertEquals("foobar", stuff.get("value"));
        assertEquals(2, stuff.size());

        result = def.match("prefix value=a value2=b");
        assertNotNull(result);
        assertEquals("double", result.getId());
        stuff = result.asMap("id");
        assertEquals("double", stuff.get("id"));
        assertEquals("a", stuff.get("value"));
        assertEquals("b", stuff.get("value2"));
        assertEquals(3, stuff.size());
    }
}
