/*
 * @(#)AccessIndexed.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions loading or storing the component of
 * an array at a certain index.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessIndexed extends AccessArray {
    /**
     * The index of the element.
     */
    private Instruction index;

    /**
     * The basic element type.
     */
    private int elemType;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope     scope containing this instruction
     * @param  array     the array to be accessed
     * @param  index     index of the element
     * @param  elemType  <strong>basic</strong> element type
     */
    protected AccessIndexed(IRScope scope, Instruction array, Instruction index,
            int elemType) {
        super(scope, ValueType.valueOf(elemType), array);
        this.index = index;
        this.elemType = elemType;
    }

    /**
     * Returns the index of the element.
     *
     * @return  the index of the element
     */
    public Instruction getIndex() {
        return index;
    }

    /**
     * Returns the basic type of the array element.
     */
    public int getElemType() {
        return elemType;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        index = vc.doValue(index);
    }
}
