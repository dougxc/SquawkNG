package com.sun.squawk.translator.loader;

/**
 * This class represents a single exception handler table entry.
 */
public class ExceptionHandler {
    public final int start;
    public final int end;
    public final int handler;
    public final int catchType;

    /**
     * Constructor.
     */
    public ExceptionHandler(int start, int end, int handler, int catchType) {
        this.start     = start;
        this.end       = end;
        this.handler   = handler;
        this.catchType = catchType;
    }
}

