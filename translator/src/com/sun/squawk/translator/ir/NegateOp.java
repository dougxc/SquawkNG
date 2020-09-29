package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class NegateOp extends Instruction {

    private Instruction value;

    NegateOp(Instruction value) {
        super(value.type());
        this.value = value;
    }

    public Instruction value() { return value; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {

        visitor.doNegateOp(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.NEG];
    }
}
