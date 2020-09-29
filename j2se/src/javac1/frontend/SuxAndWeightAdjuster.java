/*
 * @(#)SuxAndWeightAdjuster.java        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;
import javac1.ir.instr.If;

/**
 * Adjusts the successors and the weight of basic blocks. This block closure
 * reassigns the weights so that they are increasing by 1 and starting with 0
 * for the first block and it swaps the successors of branches if the default
 * successor is not the block immediately following.
 *
 * @see      BlockBegin
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class SuxAndWeightAdjuster implements BlockClosure {
    /**
     * The previously adjusted basic block.
     */
    private BlockBegin prev;

    /**
     * Constructs a new block closure adjusting weight and successors.
     */
    public SuxAndWeightAdjuster() {
        prev = null;
    }

    /**
     * Adjusts the weight of the specified basic block and the successors of the
     * previous one.
     *
     * @param  block  basic block to be adjusted
     */
    public void doBlock(BlockBegin block) {
        if (prev == null) {
            block.setWeight(0);
        } else {
            block.setWeight(prev.getWeight() + 1);
            BlockEnd end = prev.getEnd();
            if ((end.countSux() == 2) && (end.defaultSux() != block)
                    && (end.suxIndex(block) >= 0) && (end instanceof If)) {
                ((If) end).swapSuccessors();
            }
        }
        prev = block;
    }
}
