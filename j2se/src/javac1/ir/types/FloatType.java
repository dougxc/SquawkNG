/*
 * @(#)FloatType.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that produce a single-precision floating-point
 * value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class FloatType extends ValueType {
    public int getTag() {
        return ValueType.floatTag;
    }

    public int getSize() {
        return 1;
    }

    public ValueType getBase() {
        return ValueType.floatType;
    }

    public char getTypeChar() {
        return 'f';
    }

    public String toString() {
        return "float";
    }
}
