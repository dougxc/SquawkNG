/*
 * @(#)BailOut.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

/**
 * Thrown to indicate that the current method cannot be compiled and must be
 * interpreted instead.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BailOut extends RuntimeException {
    /**
     * Constructs a new exception with no detail message.
     */
    public BailOut() {
        super();
    }
    
    /**
     * Constructs a new compiler exception with the specified detail message.
     *
     * @param  msg  the detail message
     */
    public BailOut(String msg) {
        super(msg);
    }
}
