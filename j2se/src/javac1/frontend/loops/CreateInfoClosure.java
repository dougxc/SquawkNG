/*
 * @(#)CreateInfoClosure.java           1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.loops;

import javac1.Assert;
import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;

/**
 * Creates an information object for each basic block in the control flow graph.
 *
 * @see      BlockLoopInfo
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class CreateInfoClosure implements BlockClosure {
    /**
     * The loop finder that stores the block loop information objects.
     */
    private LoopFinder lf;

    /**
     * Constructs a new block closure for creating information objects.
     *
     * @param  lf  loop finder that stores the information objects
     */
    public CreateInfoClosure(LoopFinder lf) {
        this.lf = lf;
    }

    /**
     * Creates a new information object for the specified basic block.
     *
     * @param  block  basic block to be described
     */
    public void doBlock(BlockBegin block) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(lf.getBlockInfo(block) == null, "information already allocated");
        }
        BlockLoopInfo bli = new BlockLoopInfo(block, lf.getMaxBlocks());
        lf.setBlockInfo(block, bli);
    }
}
