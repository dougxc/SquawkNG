package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.StackMap;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This represents the point just before the first instruction in an exception handler.
 * This is required to ensure that the stack is in the state expected upon entry
 * to an exception handler. That is, the is a single object on the stack which
 * is the Throwable object caught by the handler.
 */
public final class LoadException extends BasicBlockEntryDelimiter {

    /**
     * Create a LoadException that represents an exception handler.
     * @param target The Target representing the address of this instruction.
     * @return the LoadException instruction.
     */
    LoadException(Target target) {
        super(target.getExceptionType(), target);
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLoadException(this);
    }

    public Type getReferencedType() {
        return target().getExceptionType();
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "catch"+annotateType(target().getExceptionType());
    }
}
