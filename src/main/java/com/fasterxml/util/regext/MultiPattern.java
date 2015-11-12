package com.fasterxml.util.regext;

import java.util.*;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

public class MultiPattern {

    private final List<String> patterns;

    private MultiPattern(List<String> patterns) {
        this.patterns = new ArrayList<>(patterns);
    }

    public static MultiPattern of(List<String> patterns) {
        return new MultiPattern(patterns);
    }

    public static MultiPattern of(String... patterns) {
        return new MultiPattern(Arrays.asList(patterns));
    }

    /**
     * Equivalent of Pattern.compile, but the result is only valid for full string matching.
     *
     * If more than one pattern are matched, with a match ending at the same offset,
     * return all of the pattern ids in a sorted array.
     *
     * This operation is costly, make sure to cache its result when performing
     * search with the same patterns against the different strings.
     *
     * @return A searcher object
     */
    public MultiPatternMatcher matcher() {
        final MultiPatternAutomaton matcherAutomaton = makeAutomaton();
        return new MultiPatternMatcher(matcherAutomaton);
    }
    
    public MultiPatternAutomaton makeAutomaton() {
        final List<Automaton> automata = new ArrayList<>();
        for (String ptn: patterns) {
            final Automaton automaton = new RegExp(ptn).toAutomaton();
            automaton.minimize();
            automata.add(automaton);
        }
        return MultiPatternAutomaton.make(automata);
    }
}
