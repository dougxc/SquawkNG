package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This instruction represents an unconditional branch instruction (i.e. GOTO).
 * It's also the base class for the conditional branch instructions.
 */
public class Goto extends BasicBlockExitDelimiter {

    /** The target of the branch. */
    private final Target target;

    /**
     * Create a Goto instruction.
     * @param target The target of the branch.
     */
    Goto(Target target) {
        super(null);
        this.target = target;
    }

    /**
     * Return the target of the branch.
     * @return the target of the branch.
     */
    public Target target() {
        return target;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doGoto(this);
    }

    /**
     * Implementation of BasicBlockEndDelimiter.
     * @return
     */
    protected Instruction[] createSuccessors() {
        return new Instruction[] { target.getInstruction() };
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.GOTO] + " " + target.getInstruction().getRelocIP();
    }
}
