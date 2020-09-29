/*
 * @(#)SetPredsClosure.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.loops;

import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;

/**
 * Determines the predecessors for each block in the control flow graph.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class SetPredsClosure implements BlockClosure {
    /**
     * The loop finder that holds the block loop information objects.
     */
    private LoopFinder lf;

    /**
     * Constructs a new block closure for determining the predecessors.
     *
     * @param  lf  loop finder that holds the block loop information objects
     */
    public SetPredsClosure(LoopFinder lf) {
        this.lf = lf;
    }

    /**
     * Adds the specified block to its successors' lists of predecessors. This
     * method is called for each basic block in the control flow graph.
     *
     * @param  block  the current basic block
     */
    public void doBlock(BlockBegin block) {
        BlockEnd end = block.getEnd();
        int max = end.countSux();
        for (int i = 0; i < max; i++) {
            BlockBegin sux = end.suxAt(i);
            BlockLoopInfo bli = lf.getBlockInfo(sux);
            bli.addPredecessor(block);
        }
    }
}
