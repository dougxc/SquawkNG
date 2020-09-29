/*
 * @(#)DoubleConstant.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that represent a constant double-precision
 * floating-point value. This class extends its base class by an additional
 * instance field that stores the value of the constant.
 *
 * @see      javac1.ir.instr.Constant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class DoubleConstant extends DoubleType {
    /**
     * The value of the constant.
     */
    private double value;

    /**
     * Constructs a new double-precision floating-point constant with the
     * specified value.
     *
     * @param  value  the value of the constant
     */
    public DoubleConstant(double value) {
        this.value = value;
    }

    /**
     * Returns the value of this floating-point constant.
     *
     * @return  the value of this constant
     */
    public double getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }
}
