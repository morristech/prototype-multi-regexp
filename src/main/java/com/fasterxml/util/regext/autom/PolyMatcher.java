package com.fasterxml.util.regext.autom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

//Note: Copied from Multiregexp package
public class PolyMatcher
{
    private final int[] NO_MATCH = {};
    private final Automata automata;
    
    /**
     * Most of the extra flags are not applicable, partly since they
     * use syntax that is different "standard" regexp, and as such would
     * otherwise need to be translated. Extra operators do not seem particularly
     * useful either, so removing them should make things bit safer and
     * possibly more efficient.
     */
    private final static int FLAGS = RegExp.NONE;

    protected PolyMatcher(Automata a) {
        automata = a;
    }

    public static PolyMatcher create(String... patterns) {
        return create(Arrays.asList(patterns));
    }

    public static PolyMatcher create(List<String> patterns) {
        return new PolyMatcher(createAutomaton(patterns));
    }

    private static Automata createAutomaton(List<String> patterns) {
        final List<Automaton> automata = new ArrayList<>();
        for (String ptn: patterns) {
            try {
                Automaton automaton = new RegExp(ptn, FLAGS).toAutomaton();
                automaton.minimize();
                automata.add(automaton);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid regexp, "+e.getMessage()+", source: "+ptn);
            }
        }
        return Automata.construct(automata);
    }
    
    /**
     * @return Indexes of all patterns that matched.
     */
    public int[] match(CharSequence s) {
        int p = 0;
        final int l = s.length();
        for (int i = 0; i < l; ++i) {
            p = automata.step(p, s.charAt(i));
            if (p == -1) {
                return NO_MATCH;
            }
        }
        return automata.accept(p);
    }
}
