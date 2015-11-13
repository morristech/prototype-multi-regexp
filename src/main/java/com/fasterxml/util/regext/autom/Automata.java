package com.fasterxml.util.regext.autom;

import java.util.*;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.DkBricsAutomatonAccess;
import dk.brics.automaton.State;

/**
 * Abstraction for the state machine constructed from multiple {@link Automaton}s.
 */
public class Automata
{
    private final int[][] _accept;
    final boolean[] _atLeastOneAccept;
    private final int _stride;
    private final int[] _transitions;
    private final int[] _alphabet;
    private final int _nbPatterns;

    private Automata(final int[][] accept,
            final int[] transitions,
            final char[] points,
            final int nbPatterns)
    {
        _accept = accept;
        _transitions = transitions;
        _alphabet = alphabet(points);
        _stride = points.length;
        _atLeastOneAccept = new boolean[accept.length];
        for (int i=0; i<accept.length; i++) {
            _atLeastOneAccept[i] = (_accept[i].length > 0);
        }
        _nbPatterns = nbPatterns;
    }

    private static int[] alphabet(final char[] points) {
        final int size = Character.MAX_VALUE - Character.MIN_VALUE + 1;
        final int[] alphabet = new int[size];

        for (int i = 0, j = 0; j < size; ++j) {
            if (i + 1 < points.length && j == points[i + 1])
                i++;
            alphabet[j] = i;
        }
        return alphabet;
    }

    public static Automata construct(final List<Automaton> automata)
    {
        for (final Automaton automaton: automata) {
            automaton.determinize();
        }

        final char[] points = pointsUnion(automata);
        final int plen = points.length;

        // states that are still to be visited
        final Queue<PolyState> statesToVisits = new LinkedList<>();
        final PolyState initialState = initialState(automata);
        statesToVisits.add(initialState);

        final List<int[]> transitionList = new ArrayList<>();

        final Map<PolyState, Integer> multiStateIndex = new HashMap<>();
        multiStateIndex.put(initialState, 0);

        while (!statesToVisits.isEmpty()) {
            final PolyState visitingState = statesToVisits.remove();
            assert multiStateIndex.containsKey(visitingState);
            final int[] curTransitions = new int[plen];
            for (int c = 0; c < plen; ++c) {
                final char point = points[c];
                final PolyState destState = visitingState.step(point);
                if (destState.isNull()) {
                    curTransitions[c] = -1;
                }
                else {
                    final int destStateId;
                    if (!multiStateIndex.containsKey(destState)) {
                        statesToVisits.add(destState);
                        destStateId = multiStateIndex.size();
                        multiStateIndex.put(destState, destStateId);
                    }
                    else {
                        destStateId = multiStateIndex.get(destState);
                    }
                    curTransitions[c] = destStateId;
                }
            }
            transitionList.add(curTransitions);
        }

        if (transitionList.size() != multiStateIndex.size()) {
            throw new IllegalStateException(String.format(
                    "Internal error: transitionList.size(%d) != multiStateIndex.size(%d)",
                    transitionList.size(), multiStateIndex.size()));
        }
        final int nbStates = multiStateIndex.size();

        final int[] transitions = new int[nbStates * plen];
        for (int stateId=0; stateId<nbStates; stateId++) {
            for (int pointId = 0; pointId < plen; ++pointId) {
                transitions[stateId * plen + pointId] = transitionList.get(stateId)[pointId];
            }
        }

        final int[][] acceptValues = new int[nbStates][];
        for (final Map.Entry<PolyState, Integer> entry: multiStateIndex.entrySet()) {
            final Integer stateId = entry.getValue();
            final PolyState multiState = entry.getKey();
            acceptValues[stateId] = multiState.toAcceptValues();
        }

        return new Automata(acceptValues, transitions, points, automata.size());
    }

    public int step(final int state, final char c) {
        return _transitions[((state * _stride) + _alphabet[c - Character.MIN_VALUE])];
    }

    public int[] accept(int stateId) {
        return _accept[stateId];
    }

    // for testing
    public int getNbPatterns() {
        return _nbPatterns;
    }

    static PolyState initialState(List<Automaton> automata) {
        final State[] initialStates = new State[automata.size()];
        int c = 0;
        for (final Automaton automaton: automata) {
            initialStates[c] = automaton.getInitialState();
            c += 1;
        }
        return new PolyState(initialStates);
    }
    
    static char[] pointsUnion(final Iterable<Automaton> automata) {
        Set<Character> points = new TreeSet<>();
        // TODO: change to BitSet
        for (Automaton automaton: automata) {
            char[] sp = DkBricsAutomatonAccess.getStartPoints(automaton);
            for (int i = 0; i < sp.length; ++i) {
                char c = sp[i];
                points.add(c);
            }
        }
        char[] pointsArr = new char[points.size()];
        int i=0;
        for (Character c: points) {
            pointsArr[i] = c;
            i++;
        }
        return pointsArr;
    }
}
