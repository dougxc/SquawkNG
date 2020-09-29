package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * Represents an LDC Java bytecode instruction where the object loaded
 * is a String. It's also used to load other types of constants (such
 * as the constant array for the implementation of the Squawk 'lookup'
 * instruction.
 */
public final class LoadConstantObject extends Instruction {

    /** The constant loaded. */
    private Object value;

    /**
     *
     * @param type
     * @param value
     */
    LoadConstantObject(Type type, Object value) {
        super(type);
        this.value = value;
    }

    public String toString() {
        return Mnemonics.OPCODES[OPC.OBJECT] + (value instanceof String ? " \""+value+"\"" : value) + annotateType(type());
    }

    public Object value() { return value; }

    public Object getConstantObject() {
        return value;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLoadConstantObject(this);
    }
}
