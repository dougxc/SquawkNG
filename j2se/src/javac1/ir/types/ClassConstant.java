/*
 * @(#)ClassConstant.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

import javac1.ci.InstanceKlass;

/**
 * The value type of instructions that represent a constant class. This class
 * extends its base class by an additional instance field that stores the value
 * of the constant.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ClassConstant extends ClassType {
    /**
     * The value of the constant.
     */
    private InstanceKlass value;

    /**
     * Constructs a new class constant with the specified value.
     *
     * @param  value  the value of the constant
     */
    public ClassConstant(InstanceKlass value) {
        this.value = value;
    }

    /**
     * Returns the value of this class constant.
     *
     * @return  the value of this constant
     */
    public InstanceKlass getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }
}
