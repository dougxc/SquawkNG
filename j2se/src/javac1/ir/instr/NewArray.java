/*
 * @(#)NewArray.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions creating new one-dimensional or
 * multidimensional arrays.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class NewArray extends StateSplit {
    /**
     * The number of elements in the new array.
     */
    private Instruction length;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope   scope containing this instruction
     * @param  length  length of the new array
     */
    protected NewArray(IRScope scope, Instruction length) {
        super(scope, ValueType.objectType);
        this.length = length;
    }

    /**
     * Returns the number of elements in the new array.
     *
     * @return  length of the new array
     */
    public Instruction getLength() {
        return length;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        length = vc.doValue(length);
    }
}
