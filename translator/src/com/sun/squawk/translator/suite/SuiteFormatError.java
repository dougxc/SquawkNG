package com.sun.squawk.translator.suite;

/**
 * Thrown by the SuiteProducer when a given suite cannot be encoded within a
 * suite file format.
 */

public class SuiteFormatError extends LinkageError {

    public SuiteFormatError(String msg) {
        super(msg);
    }
}