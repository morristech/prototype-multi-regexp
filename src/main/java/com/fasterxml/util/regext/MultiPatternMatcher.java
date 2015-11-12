package com.fasterxml.util.regext;

//Note: Copied from Multiregexp package
public class MultiPatternMatcher
{
    private final int[] NO_MATCH = {};
    private final MultiPatternAutomaton automaton;

    public MultiPatternMatcher(MultiPatternAutomaton automaton) {
        this.automaton = automaton;
    }

    /**
     * @return Indexes of all patterns that matched.
     */
    public int[] match(CharSequence s) {
        int p = 0;
        final int l = s.length();
        for (int i = 0; i < l; ++i) {
            p = automaton.step(p, s.charAt(i));
            if (p == -1) {
                return NO_MATCH;
            }
        }
        return automaton._accept[p];
    }

}
