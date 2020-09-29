package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This represents the INSTANCEOF Java bytecode.
 */
public final class InstanceOf extends Instruction {

    /** The type being checked for. */
    private Type checkType;
    /** The reference being checked. */
    private Instruction value;

    /**
     * Create a new InstanceOf instruction.
     * @param checkType The type being checked for.
     * @param value The reference being checked.
     */
    InstanceOf(Type checkType, Instruction value) {
        super(checkType.vm().INT);
        this.checkType = checkType;
        this.value = value;

    }

    /**
     * Return the type being checked for.
     * @return the type being checked for.
     */
    public Type checkType() {
        return checkType;
    }

    public boolean constrainsStack() {
        return true;
    }

    /**
     * Return the reference being checked.
     * @return the reference being checked.
     */
    public Instruction value() {
        return value;
    }

    public Type getReferencedType() {
        return checkType;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doInstanceOf(this);
    }

    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.INSTANCEOF] + annotateType(checkType);
    }
}
