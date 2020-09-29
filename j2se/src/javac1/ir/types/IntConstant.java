/*
 * @(#)IntConstant.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that represent a constant integer value. This
 * class extends its base class by an additional instance field that stores the
 * value of the constant. For reasons of comfort and efficiency two constants
 * for the most common values 0 and 1 have been predefined.
 *
 * @see      javac1.ir.instr.Constant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class IntConstant extends IntType {
    /**
     * The integer constant zero.
     */
    public static final IntConstant ZERO = new IntConstant(0);

    /**
     * The integer constant one.
     */
    public static final IntConstant ONE = new IntConstant(1);

    /**
     * The value of the constant.
     */
    private int value;

    /**
     * Constructs a new integer constant with the specified value.
     *
     * @param  value  the value of the constant
     */
    public IntConstant(int value) {
        this.value = value;
    }

    /**
     * Returns the value of this integer constant.
     *
     * @return  the value of this constant
     */
    public int getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }

    /**
     * Compares this integer constant with the specified object. The two objects
     * are equal if and only if the specified object is an integer constant that
     * has the same value as this constant.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the constants are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof IntConstant)
            && (((IntConstant) obj).getValue() == value);
    }
}
