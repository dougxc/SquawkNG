package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This class represents the POP and POP2 Java bytecode instructions.
 */
public final class Pop extends Instruction {

    /** Instruction that pushed the value to be popped. */
    private Instruction value;

    Pop(Instruction value) {
        super(null);
        this.value = value;
    }

    public Instruction value() { return value; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doPop(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.POP];
    }
}
