package com.fasterxml.util.regext.util;

/**
 * Silly little 2-String tuple class used for returning results of simplest
 * decoding/parsing operations
 */
public class StringPair
{
	protected final int _leftOffset, _rightOffset;
	protected final String _left, _right;

	public StringPair(int lo, String l, int ro, String r) {
		_leftOffset = lo;
		_left = l;
		_rightOffset = ro;
		_right = r;
	}

	public String left() { return _left; }
	public String right() { return _right; }

	public int leftOffset() { return _leftOffset; }
	public int rightOffset() { return _rightOffset; }
}
