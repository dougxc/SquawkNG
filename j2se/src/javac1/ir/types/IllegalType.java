/*
 * @(#)IllegalType.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

import javac1.Assert;

/**
 * The value type of instructions that do not produce or represent any value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class IllegalType extends ValueType {
    public int getTag() {
        return ValueType.illegalTag;
    }

    public int getSize() {
        Assert.shouldNotReachHere();
        return 0;
    }

    public ValueType getBase() {
        return ValueType.illegalType;
    }

    public char getTypeChar() {
        return ' ';
    }

    public String toString() {
        return "illegal";
    }
}
