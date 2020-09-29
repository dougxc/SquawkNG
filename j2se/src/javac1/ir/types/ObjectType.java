/*
 * @(#)ObjectType.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that produce an object. This class is the
 * base class of all reference types.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ObjectType extends ValueType {
    public int getTag() {
        return ValueType.objectTag;
    }

    public int getSize() {
        return 1;
    }

    public ValueType getBase() {
        return ValueType.objectType;
    }

    public char getTypeChar() {
        return 'a';
    }

    public String toString() {
        return "object";
    }
}
