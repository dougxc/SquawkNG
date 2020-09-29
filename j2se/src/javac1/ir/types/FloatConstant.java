/*
 * @(#)FloatConstant.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that represent a constant single-precision
 * floating-point value. This class extends its base class by an additional
 * instance field that stores the value of the constant.
 *
 * @see      javac1.ir.instr.Constant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class FloatConstant extends FloatType {
    /**
     * The value of the constant.
     */
    private float value;

    /**
     * Constructs a new single-precision floating-point constant with the
     * specified value.
     *
     * @param  value  the value of the constant
     */
    public FloatConstant(float value) {
        this.value = value;
    }

    /**
     * Returns the value of this floating-point constant.
     *
     * @return  the value of this constant
     */
    public float getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }
}
