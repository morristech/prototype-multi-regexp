package com.fasterxml.util.regext.model;

import java.util.*;

/**
 * Encapsulation of pattern at point when it has not yet been fully processed
 * and may still contain references to other (uncooked) patterns.
 */
public class UncookedPattern
{
	protected String _name;

	/**
	 * Sequence of parts of this pattern instance.
	 */
	protected List<Segment> _parts = new LinkedList<Segment>();

	public UncookedPattern(String literal) {
		_parts.add(new Segment(literal, null));
	}

	public UncookedPattern append(String literal, String pattern) {
		_parts.add(new Segment(literal, pattern));
		return this;
	}

	static class Segment {
		public final String literal;
		public String pattern;

		public Segment(String lit, String p) {
			literal = lit;
			pattern = p;
		}
	}
}
