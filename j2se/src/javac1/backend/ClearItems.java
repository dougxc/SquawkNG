/*
 * @(#)ClearItems.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import javac1.Assert;
import javac1.ir.BlockClosure;
import javac1.ir.ValueClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.Instruction;
import javac1.ir.instr.Phi;

/**
 * This class clears the items of all instructions and state values.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class ClearItems implements BlockClosure, ValueClosure {
    /**
     * Clears the items of all instructions in the specified basic block.
     *
     * @param  block  basic block whose items have to be cleared
     */
    public void doBlock(BlockBegin block) {
        block.doStateValues(this);
        for (Instruction x = block; x != null; x = x.getNext()) {
            x.clearItem();
        }
    }

    /**
     * Clears the item of the specified state value.
     *
     * @param  value  state value whose item has to be cleared
     */
    public Instruction doValue(Instruction value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(value instanceof Phi, "state must only contain phis");
        }
        value.clearItem();
        return value;
    }
}
