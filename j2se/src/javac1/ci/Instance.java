/*
 * @(#)Instance.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Represents an instance in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Instance extends Obj {
    /**
     * Constructs a new object representing an instance.
     *
     * @param  oop  the ordinary object pointer
     */
    protected Instance(Object oop) {
        super(oop);
    }
}
