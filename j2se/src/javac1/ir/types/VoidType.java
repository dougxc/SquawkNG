/*
 * @(#)VoidType.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that invoke a method whose return type is
 * void.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class VoidType extends ValueType {
    public int getTag() {
        return ValueType.voidTag;
    }

    public int getSize() {
        return 0;
    }

    public ValueType getBase() {
        return ValueType.voidType;
    }

    public char getTypeChar() {
        return 'v';
    }

    public String toString() {
        return "void";
    }
}
