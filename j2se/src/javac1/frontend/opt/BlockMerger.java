/*
 * @(#)BlockMerger.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.opt;

import javac1.Assert;
import javac1.JavaC1;
import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;
import javac1.ir.instr.Goto;
import javac1.ir.instr.Instruction;
import javac1.ir.instr.Phi;

/**
 * The block merger class is used to merge basic blocks. If a basic block has
 * exactly one successor and the successor can be reached only from this block
 * then the block and its successor can be merged. If such a pair of basic
 * blocks occurs anywhere in the control flow graph then the jump instruction at
 * the end of the first block will be eliminated and the instruction lists of
 * the blocks will be joined.
 *
 * @see      Optimizer
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class BlockMerger implements BlockClosure {
    /**
     * The object that stores the number of predecessors per block.
     */
    private PredecessorCounter pc;

    /**
     * The number of block pairs successfully merged.
     */
    private int mergeCount;

    /**
     * Constructs a new block merger.
     *
     * @param  pc  the predecessor counter
     */
    public BlockMerger(PredecessorCounter pc) {
        this.pc = pc;
        this.mergeCount = 0;
    }

    /**
     * Tries to merge the specified basic block and its direct successor.
     *
     * @param   block  the basic block to be examined
     * @return  whether or not the blocks have been merged
     */
    private boolean tryMerge(BlockBegin block) {
        BlockEnd end = block.getEnd();
        if (end instanceof Goto) {
            BlockBegin sux = end.defaultSux();
            if ((pc.getPredCount(sux) == 1) && !sux.isEntryBlock()) {
                int stackSize = sux.getState().getStackSize();
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(end.getState().getStackSize() == stackSize, "stack sizes must match");
                }
                Instruction prev = end.getPrev(block);
                Instruction next = sux.getNext();
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!(prev instanceof BlockEnd), "must not be block end");
                }
                prev.setNext(next);
                block.setEnd(sux.getEnd());
                for (int j = 0; j < sux.countSubroutines(); j++) {
                    block.addSubroutine(sux.subroutineAt(j));
                }
                for (int k = 0; k < sux.countExceptionHandlers(); k++) {
                    block.addExceptionHandler(sux.exceptionHandlerAt(k));
                }
                if (stackSize > 0) {
                    int i = 0;
                    while (i < stackSize) {
                        Instruction instr = sux.getState().stackAt(i);
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that(instr instanceof Phi, "must be a phi node");
                        }
                        instr.setSubst(end.getState().stackAt(i));
                        i += instr.getType().getSize();
                    }
                    sux.resolveSubstitution();
                }
                mergeCount++;
                if (javac1.Flags.PrintBlockElimination) {
                    JavaC1.out.print(mergeCount + ". merged B");
                    JavaC1.out.print(block.getBlockId() + " & B");
                    JavaC1.out.print(sux.getBlockId() + " (stack size = ");
                    JavaC1.out.println(stackSize + ")");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to merge the specified basic block and its successor. The attempt
     * to merge blocks is repeated as long as blocks have successfully been
     * merged because the resulting block and the next successor may merge
     * again.
     *
     * @param  block  the basic block to be examined
     */
    public void doBlock(BlockBegin block) {
        while (tryMerge(block)) {}
    }
}
