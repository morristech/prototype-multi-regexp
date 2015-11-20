package com.fasterxml.util.regext.util;

public class RegexHelper
{
    public static String quoteLiteralAsRegexp(String text)
    {
        final int end = text.length();
        
        StringBuilder sb = new StringBuilder(end + 8);

        for (int i = 0; i < end; ) {
            char c = text.charAt(i++);

            switch (c) {
            case ' ': // one of few special cases: collate, collapse into "one or more" style regexp
            case '\t':
                while ((i < end) && text.charAt(i) <= ' ') {
                    ++i;
                }
                sb.append("[ \t]+");
                break;

            case '.':
                sb.append("\\.");
                break;

                // Looks like we need to match not just open, but close parenthesis; probably same for others
            case '(':
            case ')':
            case '[':
            case ']':
            case '\\':
            case '{':
            case '}':
            case '|':
            case '*':
            case '?':
            case '+':
            case '$':
            case '^':
                // Automaton has heartburn with less-than
            case '<':
            case '>':
                // as well as with quoted entries (how about single quotes?)
            case '"':
                // and some other operators
            case '&':
                sb.append('\\');
                sb.append(c);
                break;
                
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
