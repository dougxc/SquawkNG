package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class Throw extends BasicBlockExitDelimiter {

    /** The Throwable instance. */
    private Instruction value;

    Throw(Instruction value) {
        super(null);
        this.value = value;
    }

    public Instruction   value()        { return value;        }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {

        visitor.doThrow(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }

    /**
     * Implementation of BasicBlockEndDelimiter.
     * @return
     */
    protected Instruction[] createSuccessors() {
        return null;
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.THROW];
    }
}
