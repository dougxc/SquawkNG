package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * The IR instruction implementing the Java ARRAYLENGTH bytecode.
 */
public final class ArrayLength extends Instruction {

    /** The instruction that pushes the array. */
    private Instruction array;

    /**
     * Create an ArrayLength instruction.
     * @param array The instruction that pushes the array.
     */
    ArrayLength(Instruction array) {
        super(array.type().vm().INT);
        this.array = array;
    }

    /**
     * Return the instruction that pushes the array.
     * @return the instruction that pushes the array.
     */
    public Instruction array()  { return array; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doArrayLength(this);
    }

    /**
     * Entry point for a ParameterVisitor.
     * @param visitor The ParameterVisitor.
     */
    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.ARRAYLENGTH];
    }

}
