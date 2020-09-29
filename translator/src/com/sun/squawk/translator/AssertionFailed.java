/*
 * @(#)AssertionFailed.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.translator;

/**
 * Thrown to indicate that an assertion has failed. This exception usually shows
 * up a bug in the source code of the compiler.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class AssertionFailed extends RuntimeException {
    /**
     * Constructs a new exception with no detail message.
     */
    public AssertionFailed() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  msg  the detail message
     */
    public AssertionFailed(String msg) {
        super(msg);
    }

    /**
     * Add extra context to the message returned by getMessage.
     *
     * @param context
     */
    public void addContext(String context) {
        if (this.context == null) {
            this.context = context;
        }
        else {
            if (!this.context.startsWith(context)) {
                this.context = context + ": " + this.context;
            }
        }
    }

    /**
     * Return the detail message for the exception with the extra context (if any) prepended.
     *
     * @return
     */
    public String getMessage() {
        if (context != null) {
            return context + ": " + super.getMessage();
        }
        else {
            return super.getMessage();
        }
    }

    private String context;

}
