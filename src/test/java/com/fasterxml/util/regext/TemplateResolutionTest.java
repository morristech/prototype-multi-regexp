package com.fasterxml.util.regext;

import java.util.List;
import java.util.Map;

import com.fasterxml.util.regext.model.*;

public class TemplateResolutionTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %a a\n"+
"template @base (%a:foo)\n"+
"template @full @base...%{[.*{2}]}--%a\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity check
        assertEquals(1, defR._uncooked.getPatterns().size());
        assertEquals(2, defR._uncooked.getTemplates().size());
        
        defR.resolvePatterns();
        defR.resolveTemplates();

        CookedDefinitions cooked = defR._cooked;

        // sanity check for pattern(s)
        Map<String, LiteralPattern> patterns = cooked.getPatterns();
        assertEquals(1, patterns.size());
        assertEquals("a", patterns.get("a").getText());

        // and then the beef, templates:
        List<DefPiece> parts;

        CookedTemplate t = cooked.findTemplate("base");
        assertNotNull(t);
        parts = t.getParts();
        assertEquals(3, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");
        
        t = cooked.findTemplate("full");
        assertNotNull(t);
        parts = t.getParts();

        // First three same as above
        assertEquals(7, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");

        _assertPart(parts.get(3), LiteralText.class, "...");
        _assertPart(parts.get(4), LiteralPattern.class, "[.*{2}]");
        _assertPart(parts.get(5), LiteralText.class, "--");
        _assertPart(parts.get(6), LiteralPattern.class, "a");
    }

    private void _assertPart(DefPiece part, Class<?> expClass, String expText) {
        assertEquals(expClass, part.getClass());
        assertEquals(expText, part.getText());
    }
}
