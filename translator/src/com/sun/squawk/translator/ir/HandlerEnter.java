package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This is a pseudo instruction representing the start of a range of code protected by
 * a single exception handler. It corresponds with the Java source level
 * "try" construct.
 */
public final class HandlerEnter extends Instruction implements PseudoInstruction {

    /** The entry address of the exception handler corresponding to this try block start. */
    private Target target;

    /**
     * Create a HandlerEnter to represent the start of a range of code protected by
     * a single exception handler.
     * @param target The entry address of the exception handler corresponding to this try block start.
     */
    HandlerEnter(Target target) {
        super(null);
        this.target = target;
    }

    /**
     * Return the entry address of the exception handler corresponding to this try block start.
     * @return the entry address of the exception handler corresponding to this try block start.
     */
    public Target target() { return target; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doHandlerEnter(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "try handler="+target.getInstruction().getRelocIP() + " {";
    }
}
