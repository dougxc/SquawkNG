/*
 * @(#)InstanceConstant.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

import javac1.ci.Instance;

/**
 * The value type of instructions that represent a constant instance. This class
 * extends its base class by an additional instance field that stores the value
 * of the constant.
 *
 * @see      javac1.ir.instr.Constant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class InstanceConstant extends InstanceType {
    /**
     * The value of the constant.
     */
    private Instance value;

    /**
     * Constructs a new instance constant with the specified value.
     *
     * @param  value  the value of the constant
     */
    public InstanceConstant(Instance value) {
        this.value = value;
    }

    /**
     * Returns the value of this instance constant.
     *
     * @return  the value of this constant
     */
    public Instance getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }
}
