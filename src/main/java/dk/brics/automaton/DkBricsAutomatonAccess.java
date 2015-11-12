package dk.brics.automaton;

/**
 * Helper class only needed to get around access restrictions, namely that
 * of <code>Automaton</code> not exposing its start points information.
 */
public class DkBricsAutomatonAccess
{
    public static char[] getStartPoints(Automaton a) {
        return a.getStartPoints();
    }
}
