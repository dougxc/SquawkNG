/*
 * @(#)StoreIndexed.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.ValueStack;
import javac1.ir.types.ObjectType;

/**
 * The instruction node for storing a value into an array.
 *
 * @see      LoadIndexed
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class StoreIndexed extends AccessIndexed {
    /**
     * The value that has to be stored into the array.
     */
    private Instruction value;

    /**
     * A copy of the current state array.
     */
    private ValueStack state;

    /**
     * Constructs a new instruction node for setting an element.
     *
     * @param  scope     scope containing this instruction
     * @param  array     the array to be accessed
     * @param  index     index of the element
     * @param  elemType  <strong>basic</strong> element type
     * @param  value     the new value of the indexed component
     */
    public StoreIndexed(IRScope scope, Instruction array, Instruction index,
            int elemType, Instruction value) {
        super(scope, array, index, elemType);
        this.value = value;
        this.state = null;
        setPinned(true);
    }

    /**
     * Returns the value that has to be stored into the array.
     *
     * @return  the new value of the indexed component
     */
    public Instruction getValue() {
        return value;
    }

    /**
     * Returns whether or not a write barrier is needed.
     *
     * @return  whether or not a write barrier is needed
     */
    public boolean needsWriteBarrier() {
        return getType() instanceof ObjectType;
    }

    /**
     * Returns whether or not a store check is needed.
     *
     * @return  whether or not a store check is needed
     */
    public boolean needsStoreCheck() {
        return getType() instanceof ObjectType;
    }

    /**
     * Saves a copy of the current state array because an array store check may
     * be required.
     */
    public void setState(ValueStack state) {
        this.state = state;
    }

    /**
     * Returns the saved copy of the state array.
     *
     * @return  the saved state array
     */
    public ValueStack getState() {
        return state;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        value = vc.doValue(value);
    }

    public void doStateValues(ValueClosure vc) {
        super.doStateValues(vc);
        if (state != null) {
            state.doValues(vc);
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreIndexed(this);
    }
}
