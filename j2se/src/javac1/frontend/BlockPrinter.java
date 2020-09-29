/*
 * @(#)BlockPrinter.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import javac1.JavaC1;
import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.Instruction;

/**
 * Prints the basic blocks of the intermediate representation.
 *
 * @see      InstructionPrinter
 * @see      BlockBegin
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class BlockPrinter implements BlockClosure {
    /**
     * The instruction printer used for printing the instructions of a block.
     */
    private InstructionPrinter ip;

    /**
     * Whether or not to print the control flow graph only.
     */
    private boolean cfgOnly;

    /**
     * Constructs a new block printer.
     *
     * @param  ip       the instruction printer
     * @param  cfgOnly  whether or not to print the control flow graph only
     */
    public BlockPrinter(InstructionPrinter ip, boolean cfgOnly) {
        this.ip = ip;
        this.cfgOnly = cfgOnly;
    }

    /**
     * Prints the specified basic block together with its instructions.
     *
     * @param  block  the basic block to be printed
     */
    private void printBlock(BlockBegin block) {
        ip.printInstr(block);
        JavaC1.out.println();
        ip.printStack(block.getState());
        JavaC1.out.println();
        ip.printHead();
        for (Instruction x = block.getNext(); x != null; x = x.getNext()) {
            ip.printLine(x);
        }
        JavaC1.out.println();
    }

    /**
     * Prints the specified basic block. This method is called for each basic
     * block in the control flow graph.
     *
     * @param  block  the basic block to be printed
     */
    public void doBlock(BlockBegin block) {
        if (cfgOnly) {
            ip.printInstr(block);
            JavaC1.out.println();
        } else {
            printBlock(block);
        }
    }
}
