package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.OpConst;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This class represents all the conversion instructions in the IR.
 * These, in turn, correspond to all the Java bytecode conversion instructions:
 *
 *    I2L
 *    I2F
 *    I2D
 *    L2I
 *    L2F
 *    L2D
 *    F2I
 *    F2L
 *    F2D
 *    D2I
 *    D2L
 *    D2F
 *    I2B
 *    I2C
 *    I2S
 */
public final class ConvertOp extends Instruction {

    /** The conversion operation. */
    private OpConst op;
    /** The type being converted from. */
    private final Type fromType;
    /** Instruction that pushes the value to be converted. */
    private Instruction value;

    /**
     * Create a new ConvertOp representing a conversion instruction.
     * @param op The conversion operation.
     * @param fromType The type being converted from.
     * @param toType The type being converted to.
     * @param value Instruction that pushes the value to be converted.
     */
    ConvertOp(OpConst op, Type fromType, Type toType, Instruction value) {
        super(toType);
        this.op = op;
        this.value = value;
        this.fromType = fromType;
    }

    /**
     * Return the conversion operation operation represented by this instruction.
     * @return the conversion operation operation represented by this instruction.
     */
    public OpConst op() {
        return op;
    }

    /**
    * Return the type being converted from.
    * @return the type being converted from.
    */
    public Type fromType() {
        return fromType;
    }

    /**
     * Return the Instruction that pushes the value to be converted.
     * @return the Instruction that pushes the value to be converted.
     */
    public Instruction value(){
        return value;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doConvertOp(this);
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
        return op.toString() + annotateType(type());
    }
}
