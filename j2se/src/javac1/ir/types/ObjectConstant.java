/*
 * @(#)ObjectConstant.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

import javac1.ci.Obj;

/**
 * The value type of instructions that represent a constant object. This class
 * extends its base class by an additional instance field that stores the value
 * of the constant. For reasons of comfort and efficiency a constant for the
 * special null value has been predefined.
 *
 * @see      javac1.ir.instr.Constant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ObjectConstant extends ObjectType {
    /**
     * The object constant for the null value representing the absence of a
     * reference.
     */
    public static final ObjectConstant NULL = new ObjectConstant(null);

    /**
     * The value of the constant.
     */
    private Obj value;

    /**
     * Constructs a new object constant with the specified value.
     *
     * @param  value  the value of the constant
     */
    public ObjectConstant(Obj value) {
        this.value = value;
    }

    /**
     * Returns the value of this object constant.
     *
     * @return  the value of this constant
     */
    public Obj getValue() {
        return value;
    }

    public boolean isConstant() {
        return true;
    }
}
