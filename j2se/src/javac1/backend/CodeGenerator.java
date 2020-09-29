/*
 * @(#)CodeGenerator.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import javac1.Assert;
import javac1.backend.reg.RegAlloc;
import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.Instruction;

/**
 * Generates code for each block in the control flow graph.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class CodeGenerator implements BlockClosure {
    /**
     * The value generator.
     */
    private ValueGen gen;

    /**
     * The invariant data.
     */
    private ValueGenInvariant vgi;

    /**
     * The maximum number of values to be spilled on the stack.
     */
    private int maxSpills;

    /**
     * Constructs a new code generator.
     *
     * @param  gen  the value generator
     * @param  vgi  the invariant data
     */
    public CodeGenerator(ValueGen gen, ValueGenInvariant vgi) {
        this.gen = gen;
        this.vgi = vgi;
        this.maxSpills = 0;
    }

    /**
     * Prepares the specified basic block for code generation.
     *
     * @param  block  basic block to be prepared
     */
    private void doProlog(BlockBegin block) {
        gen.alignBlock(block);
        gen.bindLabel(block.getLabel());
        if (block.isFlagSet(BlockBegin.EXCEPTION_ENTRY_FLAG)) {
            gen.exceptionHandlerStart(block.getBci());
        }
    }

    /**
     * Finishes code generation for the specified basic block.
     *
     * @param  block  basic block to be finished
     */
    private void doEpilog(BlockBegin block) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(gen.isCpuStackFree() && gen.isFpuStackFree(), "still some values on the stack");
            Assert.that(vgi.getRegAlloc().areAllFree(), "not everything released at block end");
        }
        maxSpills = Math.max(maxSpills, vgi.getRegAlloc().getMaxSpills());
    }

    /**
     * Generates code for the specified basic block. This method iterates over
     * the root instructions of the block and generates code for them.
     *
     * @param  block  basic block to generate code for
     */
    public void doBlock(BlockBegin block) {
        gen.setInvariant(vgi);
        doProlog(block);
        gen.blockProlog(block);
        gen.setBlock(block);
        for (Instruction x = block; x != null; x = x.getNext()) {
            if (x.isRoot()) {
                gen.doRoot(x);
            }
        }
        gen.setBlock(null);
        gen.blockEpilog(block);
        doEpilog(block);
    }

    /**
     * Returns the maximum number of values to be spilled on the stack.
     *
     * @return  maximum number of spill elements
     */
    public int getMaxSpills() {
        return maxSpills;
    }

    /**
     * Tests if any value has to be spilled on the stack.
     *
     * @return  whether or not any value has to be spilled
     */
    public boolean hasSpills() {
        return maxSpills > 0;
    }
}
