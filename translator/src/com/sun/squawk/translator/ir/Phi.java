package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.StackMap;
import com.sun.squawk.translator.util.BitSet;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This represents a control flow merge point. It is inserted between the
 * instruction at this address and its lexical predecessor.
 */
public final class Phi extends BasicBlockEntryDelimiter {

    Phi(Target target) {
        super(null, target);
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doPhi(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "phi ["+target().getStackMapEntry()+"]";
    }
}
