/*
 * @(#)LongType.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that produce a long integer value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LongType extends ValueType {
    public int getTag() {
        return ValueType.longTag;
    }

    public int getSize() {
        return 2;
    }

    public ValueType getBase() {
        return ValueType.longType;
    }

    public char getTypeChar() {
        return 'l';
    }

    public String toString() {
        return "long";
    }
}
