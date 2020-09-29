package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class Return extends BasicBlockExitDelimiter {

    /** The returned value or null if this is a void return. */
    private Instruction value;

    Return(Type type, Instruction value) {
        super(type);
        this.value = value;
    }

    public Instruction   value()        { return value;        }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doReturn(this);
    }

    public void visit(ParameterVisitor visitor) {
        if (value != null) {
            value = visitor.doParameter(this, value);
        }
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
        return Mnemonics.OPCODES[OPC.RETURN] + (value == null ? "" : " "+value.type().toSignature(true, true));
    }
}
