package com.sun.squawk.suite;

import java.io.*;

public class ExceptionHandler {

    public final char    from;
    public final char    to;
    public final char    entryPoint;
    public final char    type;

    public ExceptionHandler(char from, char to, char entryPoint, char type) {
        this.from       = from;
        this.to         = to;
        this.entryPoint = entryPoint;
        this.type       = type;
    }
}


