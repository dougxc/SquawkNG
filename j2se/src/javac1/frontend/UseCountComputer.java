/*
 * @(#)UseCountComputer.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import javac1.ir.BlockClosure;
import javac1.ir.ValueClosure;
import javac1.ir.ValueStack;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;
import javac1.ir.instr.Instruction;
import javac1.ir.instr.Jsr;

/**
 * Computes the number of uses of each value in the intermediate representation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class UseCountComputer implements BlockClosure, ValueClosure {
    /**
     * Updates the use counts of the input and state values of the specified
     * instruction.
     *
     * @param  instr  instruction to be updated
     */
    private void doUses(Instruction instr) {
        instr.doInputValues(this);
        if ((instr instanceof Jsr) || (instr instanceof BlockEnd)) {
            instr.doStateValues(this);
        }
    }

    /**
     * Updates the use counts of values used by pinned instructions in the
     * specified block.
     *
     * @param  block  basic block to compute use counts for
     */
    private void basicComputeUseCount(BlockBegin block) {
        for (Instruction x = block; x != null; x = x.getNext()) {
            if (x.isPinned()) {
                doUses(x);
            }
        }
    }

    /**
     * Pins all instructions in the specified block that are not used but can
     * trap.
     *
     * @param   block  basic block to be updated
     * @return  whether or not any instruction has been pinned
     */
    private boolean updatedPinning(BlockBegin block) {
        boolean updated = false;
        for (Instruction x = block; x != null; x = x.getNext()) {
            if ((x.getUseCount() == 0) && x.canTrap()) {
                x.setPinned(true);
                updated = true;
            }
        }
        return updated;
    }

    /**
     * Clears the use counts of all values in the specified basic block.
     *
     * @param  block  basic block to clear use counts for
     */
    private void clearUseCount(BlockBegin block) {
        ValueStack stack = block.getState();
        int i = 0;
        while (i < stack.getStackSize()) {
            Instruction instr = stack.stackAt(i);
            instr.setUseCount(0);
            i += instr.getType().getSize();
        }
        for (Instruction x = block; x != null; x = x.getNext()) {
            x.setUseCount(0);
        }
    }

    /**
     * Computes the use count for each value in the specified basic block. At
     * first the use counts of values used by pinned instructions in the block
     * is computed. If the block afterwards contains unused instructions that
     * can trap then these instructions will be pinned and the use counts of all
     * values will be cleared and computed once more.
     *
     * @param  block  basic block to compute use counts for
     */
    public void doBlock(BlockBegin block) {
        basicComputeUseCount(block);
        if (updatedPinning(block)) {
            clearUseCount(block);
            basicComputeUseCount(block);
        }
    }

    /**
     * Increments the use count of the specified value. If the value is not
     * pinned and has not been touched before then its input and state values
     * will also be updated.
     *
     * @param   value  value to be updated
     * @return  the unchanged reference only to meet the interface
     */
    public Instruction doValue(Instruction value) {
        if (!value.isPinned() && (value.getUseCount() == 0)) {
            doUses(value);
        }
        value.use();
        return value;
    }
}
