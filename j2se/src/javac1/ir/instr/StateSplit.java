/*
 * @(#)StateSplit.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.ValueStack;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for all instructions that cause a state split.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class StateSplit extends Instruction {
    /**
     * The state array of this node.
     */
    private ValueStack state;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the instruction
     */
    protected StateSplit(IRScope scope, ValueType type) {
        super(scope, type);
        state = null;
        setPinned(true);
    }

    /**
     * Sets the state array of this node.
     *
     * @param  state  the state array
     */
    public void setState(ValueStack state) {
        this.state = state;
    }

    /**
     * Returns the state array of this node.
     *
     * @return  the state array
     */
    public ValueStack getState() {
        return state;
    }

    public void doStateValues(ValueClosure vc) {
        super.doStateValues(vc);
        if (state != null) {
            state.doValues(vc);
        }
    }
}
