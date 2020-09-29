package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

public class NewObject extends Trappable {

    NewObject(Type type) {
        super(type);
    }

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
        visitor.doNewObject(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return "new "+type().toSignature(true, true);
    }
}
