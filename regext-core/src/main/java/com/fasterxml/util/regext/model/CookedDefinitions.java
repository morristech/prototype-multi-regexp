package com.fasterxml.util.regext.model;

import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.util.regext.DefinitionParseException;
import com.fasterxml.util.regext.RegExtractor;
import com.fasterxml.util.regext.autom.PolyMatcher;
import com.fasterxml.util.regext.io.InputLine;
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
                // Should never happen, should have been checked earlier...
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
            } else if (def instanceof TemplateParameterReference) {
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

            // last null -> no bindings from within extraction declaration
            _resolveExtraction(template, automatonInput, regexpInput, extractorNameSet, null);
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
    
    private void _resolveExtraction(DefPieceAppendable template,
            StringBuilder automatonInput, StringBuilder regexpInput,
            Collection<String> extractorNames,
            ParameterBindings activeBindings)
        throws DefinitionParseException
    {
        for (DefPiece part : template.getParts()) {
            if (_resolveLiteral(part, automatonInput, regexpInput)
                    || _resolveExtractor(part, automatonInput, regexpInput, extractorNames, activeBindings)) {
                continue;
            }
            if (part instanceof TemplateReference) {
                _resolveTemplateRefFromExtraction((TemplateReference) part,
                        automatonInput, regexpInput, extractorNames, activeBindings);
            } else if (part instanceof TemplateParameterReference) {
                // Should not occur at this point; should resolve via TemplateReference above
                TemplateParameterReference var = (TemplateParameterReference) part;
                part.getSource().reportError(part.getSourceOffset(),
                        "Internal error: should not encounter template parameter %s#%d",
                        var.getParentId(), var.getPosition());
            } else {
                part.getSource().reportError(part.getSourceOffset(),
                        "Internal error: unrecognized DefPiece %s", part.getClass().getName());
            }
        }
    }

    /**
     * Method called directly from template section of an extraction rule.
     */
    private void _resolveTemplateRefFromExtraction(TemplateReference ref,
            StringBuilder automatonInput, StringBuilder regexpInput,
            Collection<String> extractorNames,
            ParameterBindings incomingBindings)
        throws DefinitionParseException
    {
        final InputLine src = ref.getSource();
        CookedTemplate template = _templates.get(ref.getName());
        if (template == null) { // should never occur but
            src.reportError(ref.getSourceOffset(),
                    "Internal error: reference to unknown template '@%s'", ref.getName());
        }

        // at this point, main-level template references have been flattened, but not
        // necessarily templates within parametric template parameter lists. So...
        ParameterBindings bindings = null;
        if (template.hasParameters()) {
            // Ok, then, create bindings. But first, ensure actual/expected member matches
            List<DefPiece> paramRefs = ref.getParameters();
            ParameterDeclarations paramDecls = template.getParameterDeclarations();
            final int pcount = paramDecls.size();
            if (paramRefs.size() != pcount) {
                src.reportError(ref.getSourceOffset(),
                        "Parameter mismatch: template '@%s' expects %d parameters; %d passed",
                            ref.getName(), pcount, paramRefs.size());
            }
    
            // For bindings need to resolve parameters
            bindings = new ParameterBindings(paramDecls);
            int i = 0;
            for (DefPiece piece : paramRefs) {
                char exp = paramDecls.getType(++i);
                if (!_paramCompatible(exp, piece)) {
                    src.reportError(ref.getSourceOffset(),
                            "Parameter mismatch: template '@%s' expects type '%c' parameter, got %s",
                                ref.getName(),  exp, piece.getClass().getName());
                }
                bindings.addBound(_resolveParameters(piece, incomingBindings));
            }
        }
        _resolveTemplateFromExtraction(template, automatonInput, regexpInput, extractorNames,
                bindings);
    }

    private DefPiece _resolveParameters(DefPiece piece, ParameterBindings bindings)
        throws DefinitionParseException
    {
        // Should only really get references to templates, parameters (variables)
        // and parameter references
        if (piece instanceof TemplateParameterReference) {
            TemplateParameterReference var = (TemplateParameterReference) piece;
            DefPiece v = bindings.getParameter(var.getPosition());
            if (v == null) { // sanity check: out of bound
                piece.reportError("Invalid parameter variable reference @%d; template has %d parameters",
                        var.getPosition(), bindings.size());
            }
            return v;
        }
        if (piece instanceof TemplateReference) {
            TemplateReference templ = (TemplateReference) piece;
            List<DefPiece> params = templ.getParameters();
            if (params == null) {
                return templ;
            }
            List<DefPiece> newParams = new ArrayList<>();
            for (DefPiece p : params) {
                newParams.add(_resolveParameters(p, bindings));
            }
            return templ.withParameters(newParams);
        }
        if (piece instanceof ExtractorExpression) {
            ExtractorExpression extr = (ExtractorExpression) piece;
            List<DefPiece> newParts = new ArrayList<>();
            for (DefPiece p : extr.getParts()) {
                newParts.add(_resolveParameters(p, bindings));
            }
            return extr.withParts(newParts);
        }
        piece.reportError("Internal error: unexpected template parameter type %s", piece.getClass().getName());
        return piece;
    }
    
    private void _resolveTemplateFromExtraction(CookedTemplate template,
            StringBuilder automatonInput, StringBuilder regexpInput,
            Collection<String> extractorNames,
            ParameterBindings activeBindings)
        throws DefinitionParseException
    {
//        final InputLine src = template.getSource();

        for (DefPiece part : template.getParts()) {
            // First: parameters just expand to a single other thing
            if (part instanceof TemplateParameterReference) {
                TemplateParameterReference paramRef = (TemplateParameterReference) part;
                int pos = paramRef.getPosition();
                if (activeBindings == null){
                    part.reportError("Invalid parameter variable reference @%d; template takes no parameters", pos);
                }
                DefPiece param = (activeBindings == null) ? null : activeBindings.getParameter(pos);
                if (param == null) {
                    part.reportError("Invalid parameter variable reference @%d; template takes %d parameters",
                            pos, activeBindings.size());
                }
                part = param;
                // fall-through for further processing
            }

            if (_resolveLiteral(part, automatonInput, regexpInput)
                    || _resolveExtractor(part, automatonInput, regexpInput, extractorNames, activeBindings)) {
                continue;
            }
            
            if (part instanceof TemplateReference) {
                _resolveTemplateRefFromExtraction((TemplateReference) part,
                        automatonInput, regexpInput, extractorNames, activeBindings);
                continue;
            }
            part.getSource().reportError(part.getSourceOffset(),
                    "Internal error: unrecognized DefPiece %s", part.getClass().getName());
        }
    }

    /*
    
    private void _resolveTemplateContents(String name, Iterable<DefPiece> toResolve,
            DefPieceAppendable result,
            List<String> stack)
        throws DefinitionParseException
    {
        for (DefPiece def : toResolve) {
            if (_resolveLiteral(def, automatonInput, regexpInput)
                    || _resolveExtractor(def, automatonInput, regexpInput, extractorNames)) {
                continue;
            }

            if (def instanceof PatternReference) {
                String patternRef = def.getText();
                LiteralPattern p = _patterns.get(patternRef);
                // Should never happen, should have been checked earlier...
                if (p == null) {
                    throw new IllegalStateException(String.format(
                            "Internal error: non-existing pattern '%%%s' from template '%s'",
                            patternRef, name));
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
    */

    private boolean _resolveExtractor(DefPiece part,
            StringBuilder automatonInput, StringBuilder regexpInput,
            Collection<String> extractorNames,
            ParameterBindings activeBindings)
        throws DefinitionParseException
    {
        if (part instanceof ExtractorExpression) {
            ExtractorExpression extr = (ExtractorExpression) part;
            if (!extractorNames.add(extr.getName())) { // not allowed
                part.getSource().reportError(part.getSourceOffset(),
                        "Duplicate extractor name ($%s)", extr.getName());
            }
            // not sure if we need to enclose it for Automaton, but shouldn't hurt
            automatonInput.append('(');
            regexpInput.append('(');
            // and for "regular" Regexp package, must add to get group
            _resolveExtraction(extr, automatonInput, regexpInput, extractorNames, activeBindings);
            automatonInput.append(')');
            regexpInput.append(')');
            return true;
        }
        return false;
    }

    private boolean _resolveLiteral(DefPiece part,
            StringBuilder automatonInput, StringBuilder regexpInput)
        throws DefinitionParseException
    {
        if (part instanceof LiteralText) {
            String q = RegexHelper.quoteLiteralAsRegexp(part.getText());
            automatonInput.append(q);
            regexpInput.append(q);
            return true;
        }
        if (part instanceof LiteralPattern) {
            _resolvePattern(part, part.getText(), automatonInput, regexpInput);
            return true;
        }
        if (part instanceof PatternReference) {
            String patternRef = part.getText();
            LiteralPattern p = _patterns.get(patternRef);
            // Should never happen, should have been checked earlier...
            if (p == null) {
                throw new IllegalStateException(String.format(
                        "Internal error: non-existing pattern '%%%s', should have been caught earlier",
                        patternRef));
            }
            _resolvePattern(p, p.getText(), automatonInput, regexpInput);
            return true;
        }
        return false;
    }

    private void _resolvePattern(DefPiece part, String ptext,
            StringBuilder automatonInput, StringBuilder regexpInput)
        throws DefinitionParseException
    {
        try {
            RegexHelper.massageRegexpForAutomaton(ptext, automatonInput);
            RegexHelper.massageRegexpForJDK(ptext, regexpInput);
        } catch (Exception e) {
            part.getSource().reportError(part.getSourceOffset(),
                    "Invalid pattern definition, problem (%s): %s",
                    e.getClass().getName(), e.getMessage());
        }
    }

    private boolean _paramCompatible(char exp, DefPiece p)
    {
        switch (exp) {
        case '@':
            return p instanceof TemplateReference;
        case '$':
            // !!! TODO: do not yet have type for this!
    //        compatible = p instanceof ExtractorReference;
            return false;
        default:
            throw new IllegalStateException("Internal error: unrecognized template parameter type '"
                    +exp+"' (for actual parameter type of "+p.getClass().getName()+")");
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
