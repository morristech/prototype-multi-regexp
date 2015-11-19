package com.fasterxml.util.regext;

import java.io.IOException;

/**
 * Exception used to indicate a problem in executing extraction.
 */
public class ExtractionException extends IOException
{
    private static final long serialVersionUID = 1L;

    protected final String _input;

    public ExtractionException(String input, String msg) {
        super(msg);
        _input = input;
    }
    
    public ExtractionException(String input, String msg, Exception e) {
        super(msg, e);
        _input = input;
    }

    public String getInput() {
        return _input;
    }
}
