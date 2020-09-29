package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

public class NewMultiArray extends Trappable {

    private Instruction[] dimList;

    NewMultiArray(Type type, Instruction[] dimList) {
        super(type);
        this.dimList = dimList;
    }

    public Instruction[] dimList() { return dimList;   }

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
        visitor.doNewMultiArray(this);
    }

    public void visit(ParameterVisitor visitor) {
        for (int i = 0 ; i < dimList.length ; i++) {
            dimList[i] = visitor.doParameter(this, dimList[i]);
        }
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "multianewarray dims="+dimList.length+" "+type().toSignature(true, true);
    }
}
