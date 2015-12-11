package com.fasterxml.util.regext;

import java.util.Map;

public class VarTemplateExtractionTest extends TestBase
{
   public void testSimple() throws Exception
   {
       final String DEF =
"pattern %word ([a-zA-Z]+)\n"+
"pattern %num ([0-9]+)\n"+
"pattern %ip [a-zA-Z\\.]+\n"+
"template @ip %ip\n"+
"template @port %num\n"+
"template @colonPair() @1:@2\n"+
"extract Net {  \n"+
"  template $endpoint(@colonPair(@ip,@port))/%word"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       RegExtractor def = defR.read();

       ExtractionResult result = def.extract("foo.bar.com:8080/user");
       assertNotNull(result);
       assertEquals("Net", result.getId());
       Map<String,Object> stuff = result.asMap();
       assertEquals("foo.bar.com:8080", stuff.get("endpoint"));
       assertEquals(1, stuff.size());
   }
}
