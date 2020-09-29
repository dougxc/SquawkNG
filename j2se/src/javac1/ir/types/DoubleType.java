/*
 * @(#)DoubleType.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that produce a double-precision floating-point
 * value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class DoubleType extends ValueType {
    public int getTag() {
        return ValueType.doubleTag;
    }

    public int getSize() {
        return 2;
    }

    public ValueType getBase() {
        return ValueType.doubleType;
    }

    public char getTypeChar() {
        return 'd';
    }

    public String toString() {
        return "double";
    }
}
