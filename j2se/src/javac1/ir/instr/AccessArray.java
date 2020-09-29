/*
 * @(#)AccessArray.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions determining the length of an array
 * or accessing its elements.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessArray extends Instruction {
    /**
     * The array to be accessed.
     */
    private Instruction array;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   the element type, or integer type when getting the length
     * @param  array  the array to be accessed
     */
    protected AccessArray(IRScope scope, ValueType type, Instruction array) {
        super(scope, type);
        this.array = array;
    }

    /**
     * Returns the array to be accessed.
     *
     * @return  the array to be accessed
     */
    public Instruction getArray() {
        return array;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        array = vc.doValue(array);
    }
}
