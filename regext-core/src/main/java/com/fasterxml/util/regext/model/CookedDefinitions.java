package com.fasterxml.util.regext.model;

import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.util.regext.DefinitionParseException;
import com.fasterxml.util.regext.RegExtractor;
import com.fasterxml.util.regext.autom.PolyMatcher;
import com.fasterxml.util.regext.util.RegexHelper;

public class CookedDefinitions
{
    protected HashMap<String,LiteralPattern> _patterns = new LinkedHashMap<>();

    protected HashMap<String,CookedTemplate> _templates = new LinkedHashMap<>();

    protected List<CookedExtraction> _extractions = new ArrayList<>();
    
    public CookedDefinitions() { }

    public LiteralPattern findPattern(String name) {
        return _patterns.get(name);
    }

    public CookedTemplate findTemplate(String name) {
        return _templates.get(name);
    }

    public Map<String,LiteralPattern> getPatterns() {
        return _patterns;
    }

    public Map<String,CookedTemplate> getTemplates() {
        return _templates;
    }

    public List<CookedExtraction> getExtractions() {
        return _extractions;
    }

    /*
    /**********************************************************************
    /* Resolution: patterns
    /**********************************************************************
     */

    /**
     * First part of resolution: resolving and flattening of pattern declarations.
     * After this step, 
     */
    public void resolvePatterns(UncookedDefinitions uncooked)  throws DefinitionParseException
    {
        Map<String,UncookedDefinition> uncookedPatterns = uncooked.getPatterns(); 
        for (UncookedDefinition pattern : uncookedPatterns.values()) {
            String name = pattern.getName();
            if (_patterns.containsKey(name)) { // due to recursion, may have done it already
                continue;
            }
            _patterns.put(name, _resolvePattern(uncookedPatterns, name, pattern, null));
        }
    }

    private LiteralPattern _resolvePattern(Map<String,UncookedDefinition> uncookedPatterns,
            String name, UncookedDefinition def,
            List<String> stack) throws DefinitionParseException
    {
        // Minor optimization: we might have just one part
        List<DefPiece> pieces = def.getParts();
        if (pieces.size() == 1) {
            DefPiece piece = pieces.get(0);
            if (piece instanceof LiteralPattern) {
                return (LiteralPattern) piece;
            }
            // must be reference (only literals and refs)
            if (stack == null) {
                stack = new LinkedList<>();
            }
            return _resolvePatternReference(uncookedPatterns, name, (PatternReference) piece, stack);
        }
        StringBuilder sb = new StringBuilder(100);
        for (DefPiece piece : pieces) {
            LiteralPattern lit;
            if (piece instanceof LiteralPattern) {
                lit = (LiteralPattern) piece;
            } else {
                // must be reference (only literals and refs)
                if (stack == null) {
                    stack = new LinkedList<>();
                }
                lit = _resolvePatternReference(uncookedPatterns, name, (PatternReference) piece, stack);
            }
            sb.append(lit.getText());
        }
        return new LiteralPattern(def.getSource(), pieces.get(0).getSourceOffset(), sb.toString());
    }

    private LiteralPattern _resolvePatternReference(Map<String,UncookedDefinition> uncookedPatterns,
            String fromName, PatternReference def,
            List<String> stack) throws DefinitionParseException
    {
        final String toName = def.getText();
        // very first thing: maybe already resolved?
        LiteralPattern res = findPattern(toName);
        if (res != null) {
            return res;
        }

        // otherwise verify that we have no loop
        stack.add(fromName);
        if (stack.contains(toName)) {
            def.reportError("Cyclic pattern reference to '%%%s' %s",
                    toName, _stackDesc("%", stack, toName));
        }
        UncookedDefinition raw = uncookedPatterns.get(toName);
        if (raw == null) {
            def.reportError("Referencing non-existing pattern '%%%s' %s",
                    toName, _stackDesc("%", stack, toName));
        }
        LiteralPattern p = _resolvePattern(uncookedPatterns, toName, raw, stack);
        _patterns.put(toName, p);
        // but remove from stack
        stack.remove(stack.size()-1);
        return p;
    }

    /*
    /**********************************************************************
    /* Resolution: templates
    /**********************************************************************
     */

    /**
     * Method called to essentially flatten template definitions as much as possible,
     * although leaving nested structure for parametric templates.
     */
    public void resolveTemplates(UncookedDefinitions uncooked)  throws DefinitionParseException
    {
        Map<String,UncookedDefinition> uncookedTemplates = uncooked.getTemplates();
        for (UncookedDefinition template : uncookedTemplates.values()) {
            String name = template.getName();
            if (_templates.containsKey(name)) { // due to recursion, may have done it already
                continue;
            }
            CookedTemplate result = CookedTemplate.construct(template);
            _resolveTemplateContents(uncookedTemplates,
                    template.getName(), template.getParts(), result, null, name);
            _templates.put(name, result);
        }
    }

    private void _resolveTemplateContents(Map<String,UncookedDefinition> uncookedTemplates,
            String name, Iterable<DefPiece> toResolve, DefPieceAppendable result,
            List<String> stack, String topName)
        throws DefinitionParseException
    {
        for (DefPiece def : toResolve) {
            if (def instanceof LiteralPiece) { // literals fine as-is
                result.append(def);
            } else if (def instanceof PatternReference) {
                String patternRef = def.getText();
                LiteralPattern p = _patterns.get(patternRef);
                // 15-Dec-2015, tatu: Should never happen, should have been checked earlier...
                if (p == null) {
                    def.reportError("Referencing non-existing pattern '%%%s' from template '%s' %s",
                            patternRef, topName, _stackDesc("@", stack, result.getName()));
                }
                result.append(p);
            } else if (def instanceof TemplateReference) {
                TemplateReference refdTemplate = (TemplateReference) def;
                // Can only flatten non-parametric templates; others left as is
                if (refdTemplate.takesParameters()) {
                    result.append(refdTemplate);
                } else {
                    if (stack == null) {
                        stack = new LinkedList<>();
                    }
                    CookedTemplate tmpl = _resolveTemplateReference(uncookedTemplates,
                            name, refdTemplate, stack, topName);
                    // And for proper flattening, we'll just take out contents
                    for (DefPiece p : tmpl.getParts()) {
                        result.append(p);
                    }
                }
            } else if (def instanceof ExtractorExpression) {
                ExtractorExpression raw = (ExtractorExpression) def;
                ExtractorExpression resolved = raw.empty();
                if (stack == null) {
                    stack = new LinkedList<>();
                }
                // pass same name as we got, since we are not resolving other template
                _resolveTemplateContents(uncookedTemplates, name,
                        raw.getParts(), resolved, stack, topName);
                result.append(resolved);
            } else if (def instanceof TemplateVariable) {
                // 15-Dec-2015, tatu: Pass as-is, for now?
                result.append(def);
            } else {
                _unrecognizedPiece(def, "template definition '"+topName+"'");
            }
        }
    }

    private CookedTemplate _resolveTemplateReference(Map<String,UncookedDefinition> uncookedTemplates,
            String fromName, TemplateReference ref,
            List<String> stack, String topName) throws DefinitionParseException
    {
        final String toName = ref.getText();
        // very first thing: maybe already resolved?
        CookedTemplate res = _templates.get(toName);
        if (res != null) {
            return res;
        }
        // otherwise verify that we have no loop
        stack.add(fromName);
        if (stack.contains(toName)) {
            ref.reportError("Cyclic template reference to '%%%s' %s",
                    toName, _stackDesc("@", stack, toName));
        }
        UncookedDefinition raw = uncookedTemplates.get(toName);
        if (raw == null) {
            ref.reportError("Referencing non-existing template '%%%s' %s",
                    toName, _stackDesc("@", stack, toName));
        }
        CookedTemplate result = CookedTemplate.construct(raw);
        _resolveTemplateContents(uncookedTemplates,
                result.getName(), result.getParts(), result, stack, topName);
        _templates.put(toName, result);
        // but remove from stack
        stack.remove(stack.size()-1);
        return result;
    }

    /*
    /**********************************************************************
    /* Resolution: extractions
    /**********************************************************************
     */

    /**
     * Final resolution method called when all named patterns, templates and inline extractors
     * have been resolved, flattened (to the degree they can be: extractors can be nested).
     * At this point translation into physical regexp input is needed.
     */
    public RegExtractor resolveExtractions(UncookedDefinitions uncooked)
        throws DefinitionParseException
    {
        Map<String, UncookedExtraction> uncookedTemplates = uncooked.getExtractions();
        final int ecount = uncookedTemplates.size();
        List<String> automatonInputs = new ArrayList<>(ecount);

        for (UncookedExtraction rawExtr : uncookedTemplates.values()) {
            UncookedDefinition rawTemplate = rawExtr.getTemplate();
            String name = rawTemplate.getName();
            CookedTemplate template = CookedTemplate.construct(rawTemplate);
            _resolveTemplateContents(Collections.<String,UncookedDefinition>emptyMap(),
                    rawTemplate.getName(), rawTemplate.getParts(), template, null, name);
/*
    System.err.println("Resolved extractions for: "+rawExtr.getName());
for (DefPiece piece : template.getParts()) {
    System.err.println(" Piece ("+piece.getClass().getName()+"): "+piece.getText());
}
*/            
            // Ok. And then heavy-lifting... two main parts; conversion of regexps (automaton
            // for multi-match; another regexp for actual extraction), then building
            // of multi-matcher

            StringBuilder automatonInput = new StringBuilder();
            StringBuilder regexpInput = new StringBuilder();

            // Use set to efficiently catch duplicate extractor names
            Set<String> extractorNameSet = new LinkedHashSet<>();

            _resolveRegexps(template, automatonInput, regexpInput, extractorNameSet);
            automatonInputs.add(automatonInput.toString());

            // Start with regexp itself
            String[] extractorNames = extractorNameSet.toArray(new String[extractorNameSet.size()]);
            final String regexpSource = regexpInput.toString();
            Pattern regexp = null;
            try {
                regexp = Pattern.compile(regexpSource);
            } catch (Exception e) {
                rawTemplate.reportError("Invalid regular expression segment: %s", e.getMessage());
            }
            
            int index = _extractions.size();
            _extractions.add(CookedExtraction.construct(index, rawExtr, template,
                    regexp, regexpSource, extractorNames));
        }
        
        // With that, can try constructing multi-matcher
        PolyMatcher poly = null;
        try {
            poly = PolyMatcher.create(automatonInputs);
        } catch (Exception e) {
            DefinitionParseException pe = DefinitionParseException.construct(
                    "Internal error: problem with PolyMatcher construction: "+ e.getMessage(),
                    null, 0);
            pe.initCause(e);
            throw pe;
        }
        return RegExtractor.construct(this, poly);
    }

    private void _resolveRegexps(DefPieceAppendable template,
            StringBuilder automatonInput, StringBuilder regexpInput,
            Collection<String> extractorNames)
        throws DefinitionParseException
    {
        for (DefPiece part : template.getParts()) {
            if (part instanceof LiteralText) {
                String q = RegexHelper.quoteLiteralAsRegexp(part.getText());
                automatonInput.append(q);
                regexpInput.append(q);
            } else if (part instanceof LiteralPattern) {
                String ptext = part.getText();
                try {
                    RegexHelper.massageRegexpForAutomaton(ptext, automatonInput);
                    RegexHelper.massageRegexpForJDK(ptext, regexpInput);
                } catch (Exception e) {
                    part.getSource().reportError(part.getSourceOffset(),
                            "Invalid pattern definition, problem (%s): %s",
                            e.getClass().getName(), e.getMessage());
                }
            } else if (part instanceof ExtractorExpression) {
                ExtractorExpression extr = (ExtractorExpression) part;
                if (!extractorNames.add(extr.getName())) { // not allowed
                    part.getSource().reportError(part.getSourceOffset(),
                            "Duplicate extractor name ($%s)", extr.getName());
                }
                // not sure if we need to enclose it for Automaton, but shouldn't hurt
                automatonInput.append('(');
                regexpInput.append('(');
                // and for "regular" Regexp package, must add to get group
                _resolveRegexps(extr, automatonInput, regexpInput, extractorNames);
                automatonInput.append(')');
                regexpInput.append(')');
            } else if (part instanceof TemplateVariable) {
                TemplateVariable var = (TemplateVariable) part;
                part.getSource().reportError(part.getSourceOffset(),
                        "Internal error: can not yet resolve parameter %s#%d",
                        var.getParentId(), var.getPosition());

            } else if (part instanceof TemplateReference) {
                TemplateReference ref = (TemplateReference) part;
                CookedTemplate res = _templates.get(ref.getName());
                if (res == null) { // should never occur but
                    part.getSource().reportError(part.getSourceOffset(),
                            "Internal error: reference to unknown template '@%s'", ref.getName());
                }
                // at this point, MUST be parametric, non-parametric already flattened.. but verify
                if (!res.hasParameters()) {
                    part.getSource().reportError(part.getSourceOffset(),
                            "Internal error: unresolved non-parametric template '@%s'", ref.getName());
                }

                // Ok, then, create bindings. But first, ensure actual/expected mumber matches
                List<DefPiece> paramRefs = ref.getParameters();
                ParameterDeclarations paramDecls = res.getParameterDeclarations();
                if (paramRefs.size() != paramDecls.size()) {
                    part.getSource().reportError(part.getSourceOffset(),
                            "Parameter mismatch: template '@%s' expects %d parameters; %d passed",
                                ref.getName(), paramDecls.size(), paramRefs.size());
                }
                
                part.getSource().reportError(part.getSourceOffset(),
                        "Internal error: can not yet resolve parametric template '@%s'", ref.getName());
            } else {
                part.getSource().reportError(part.getSourceOffset(),
                        "Internal error: unrecognized DefPiece %s", part.getClass().getName());
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private String _stackDesc(String marker, List<String> stack, String last) {
        if (stack == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(100);
        sb.append('(');
        for (String str : stack) {
            sb.append(marker).append(str);
            sb.append("->");
        }
        sb.append(marker).append(last);
        sb.append(')');
        return sb.toString();
    }

    private void _unrecognizedPiece(DefPiece def, String type) throws DefinitionParseException{
        def.getSource().reportError(0, "Internal error: unexpected definition type %s when resolving %s",
                def.getClass().getName(), type);
    }
}
