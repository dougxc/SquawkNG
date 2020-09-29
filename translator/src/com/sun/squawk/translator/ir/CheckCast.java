package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * The checkcast IR instruction. This instruction implements the Java
 * bytecode CHECKCAST instruction. That is, it pops
 * a value off the stack, checks that it can be cast to another type and
 * pushes back on a value of that type.
 */
public final class CheckCast extends Trappable {

    /** The type being checked for. */
    private Type checkType;
    /** The instruction that pushed the value being checked. */
    private Instruction value;

    /**
     * Create a CheckCast instruction.
     * @param checkType The type being checked for.
     * @param value The instruction that pushed the value being checked.
     */
    CheckCast(Type checkType, Instruction value) {
        super(checkType);
        this.checkType = checkType;
        this.value = value;
    }

    /**
     * Return the type being checked for.
     * @return the type being checked for.
     */
    public Type checkType()     { return checkType; }

    /**
     * Return the instruction that pushed the value being checked.
     * @return the instruction that pushed the value being checked.
     */
    public Instruction value()  { return value;     }

    public boolean constrainsStack() {
        return true;
    }

    /**
     * Return the class referenced by this instruction which is the type being checked for.
     * @return the class referenced by this instruction which is the type being checked for.
     */
    public Type getReferencedType() {
        return checkType;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doCheckCast(this);
    }

    /**
     * Entry point for a ParameterVisitor.
     * @param visitor The ParameterVisitor.
     */
    public void visit(ParameterVisitor visitor) {
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.CHECKCAST] + annotateType(checkType);
    }
}
