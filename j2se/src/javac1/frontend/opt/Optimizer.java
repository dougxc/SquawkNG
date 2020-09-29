/*
 * @(#)Optimizer.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.opt;

import javac1.Assert;
import javac1.ir.IR;
import javac1.ir.instr.BlockBegin;

/**
 * The optimizer eliminates conditional expressions and merges basic blocks.
 *
 * @see      BlockMerger
 * @see      CEEliminator
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Optimizer {
    /**
     * The intermediate representation to be optimized.
     */
    private IR ir;

    /**
     * Constructs a new optimizer.
     *
     * @param  ir  the intermediate representation to be optimized
     */
    public Optimizer(IR ir) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ir.isValid(), "ir must be valid");
        }
        this.ir = ir;
    }

    /**
     * Looks for conditional expressions and replaces them by <code>IfOp</code>
     * instructions.
     *
     * @see  javac1.ir.instr.IfOp
     */
    public void eliminateConditionalExpressions() {
        CEEliminator ce = new CEEliminator();
        ir.iteratePreorder(ce);
    }

    /**
     * Determines the predecessor count for each block and merges blocks if
     * possible.
     */
    public void eliminateBlocks() {
        int count = ir.countBlocks();
        PredecessorCounter pc = new PredecessorCounter(count);
        ir.iteratePreorder(pc);
        BlockMerger bm = new BlockMerger(pc);
        ir.iteratePreorder(bm);
    }
}
