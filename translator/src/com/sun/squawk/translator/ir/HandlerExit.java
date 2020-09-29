package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This is a pseudo instruction representing the end of a range of code protected by
 * a single exception handler. It corresponds with the closing "}" of Java source level
 * "try" construct.
 */
public final class HandlerExit extends Instruction implements PseudoInstruction {

    /** The entry address of the exception handler corresponding to this try block end. */
    private Target target;

    /**
     * Create a HandlerExit to represent the end of a range of code protected by
     * a single exception handler.
     * @param target The entry address of the exception handler corresponding to this try block end.
     */
    HandlerExit(Target target) {
        super(null);
        this.target = target;
    }

    /**
     * Return the entry address of the exception handler corresponding to this try block end.
     * @return the entry address of the exception handler corresponding to this try block end.
     */
    public Target target() { return target; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doHandlerExit(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "} endtry handler="+target.getInstruction().getRelocIP();
    }
}
