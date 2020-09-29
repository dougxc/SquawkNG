package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * Instances of this class are created as 'place holders' for stack slots
 * that are live at a merge point (i.e. a Phi instruction) where that merge
 * point is the target of a backwards branch or is the entry point to an
 * unreachable piece of code.
 */
public class LoadSynthetic extends Instruction {

    /**
     * Create a LoadDummy object to represent a type on the operand stack.
     * @param type The type pushed.
     */
    LoadSynthetic(Type type) {
        super(type);
        // This is not a real instruction
    }

    /**
     * Entry point for a visit from an InstructionVisitor object.
     * @param visitor The InstructionVisitor object.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        Assert.shouldNotReachHere();
    }

    /**
     * Return true if this instruction wrote to a stack slot that was written
     * to by another instruction on a different control flow path that merges
     * with the control flow path of this instruction.
     *
     * This is always true for LoadSynthetic instructions.
     *
     * @return true
     */
    public boolean wasMerged() {
        return true;
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "{synthetic load of "+type().toSignature(true, true)+"}";
    }
}
