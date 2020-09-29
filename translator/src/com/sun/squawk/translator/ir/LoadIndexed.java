package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.vm.*;

/**
 * This represents an array load instruction.
 */
public class LoadIndexed extends Trappable {

    private Instruction array;
    private Instruction index;

    LoadIndexed(Type type, Instruction array, Instruction index) {
        super(type);
        this.array = array;
        this.index = index;
    }

    /**
     * Return the instruction that pushed the array onto the stack.
     * @return the instruction that pushed the array onto the stack.
     */
    public Instruction array() {
        return array;
    }

    /**
     * Return the instruction that pushed the index onto the stack.
     * @return the instruction that pushed the index onto the stack.
     */
    public Instruction index() {
        return index;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {

        visitor.doLoadIndexed(this);
    }

    /**
     * Entry point for a visit from a ParameterVisitor object.
     * @param visitor The ParameterVisitor object.
     */
    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        index = visitor.doParameter(this, index);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.ALOAD] + annotateType(type());
    }
}
