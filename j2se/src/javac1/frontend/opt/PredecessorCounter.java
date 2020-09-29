/*
 * @(#)PredecessorCounter.java          1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.opt;

import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;

/**
 * Counts the predecessors of each basic block in the control flow graph. The
 * results are used to determine if two basic blocks can be merged.
 *
 * @see      BlockMerger
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class PredecessorCounter implements BlockClosure {
    /**
     * The number of predecessors per basic block.
     */
    private int[] count;

    /**
     * Constructs a new predecessor counter.
     *
     * @param  size  the total number of basic blocks
     */
    public PredecessorCounter(int size) {
        count = new int[size];
    }

    /**
     * Increments the counter of predecessors of the specified basic block.
     *
     * @param  block  the basic block
     */
    private void addPred(BlockBegin block) {
        count[block.getBlockId()]++;
    }

    /**
     * Returns the number of predecessors of the specified basic block.
     *
     * @param   block  the basic block
     * @return  the number of predecessors
     */
    public int getPredCount(BlockBegin block) {
        return count[block.getBlockId()];
    }

    /**
     * Updates the counters of the blocks that can be reached directly from the
     * specified basic block.
     *
     * @param  block  the basic block
     */
    public void doBlock(BlockBegin block) {
        for (int i = block.getEnd().countSux() - 1; i >= 0; i--) {
            addPred(block.getEnd().suxAt(i));
        }
        for (int j = block.countSubroutines() - 1; j >= 0; j--) {
            addPred(block.subroutineAt(j));
        }
        for (int k = block.countExceptionHandlers() - 1; k >= 0; k--) {
            addPred(block.exceptionHandlerAt(k));
        }
    }
}
