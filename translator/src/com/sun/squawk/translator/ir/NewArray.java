package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class NewArray extends Trappable {

    private Instruction size;

    NewArray(Type type, Instruction size) {
        super(type);
        this.size = size;
    }

    public Instruction size() { return size; }

    public boolean constrainsStack() {
        return true;
    }

    public Type getReferencedType() {
        return type();
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doNewArray(this);
    }

    public void visit(ParameterVisitor visitor) {
        size  = visitor.doParameter(this, size);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.NEWARRAY] + " {"+type().toSignature(true, true)+"}";
    }
}
