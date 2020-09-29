package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * The IR instruction representing a store into an array.
 * This represents the following Java bytecode instructions:
 *
 *    IASTORE
 *    LASTORE
 *    FASTORE
 *    DASTORE
 *    AASTORE
 *    BASTORE
 *    CASTORE
 *    SASTORE
 */
public final class StoreIndexed extends Trappable {

    /** The instruction that pushed the array object. */
    private Instruction array;
    /** The instruction that pushed the index at which the value is to be stored. */
    private Instruction index;
    /** The instruction that pushed the value to be stored into the array. */
    private Instruction value;

    /**
     * The basic type of the array. This specifies whether this is a reference
     * array or a primitive array and if it is the latter, which primitive type
     * its elements are. It must be one of the following:
     *
     *     INT_ARRAY
     *     LONG_ARRAY
     *     FLOAT_ARRAY
     *     DOUBLE_ARRAY
     *     OBJECT_ARRAY
     *     BYTE_ARRAY
     *     CHAR_ARRAY
     *     SHORT_ARRAY
     */
    private Type basicType;

    /**
     * Create a StoreIndexed instruction.
     * @param array The instruction that pushed the array object.
     * @param index The instruction that pushed the index at which the value is to be stored.
     * @param value The instruction that pushed the value to be stored into the array.
     * @param basicType The basic type of the array.
     */
    StoreIndexed(Instruction array, Instruction index, Instruction value, Type basicType) {
        super(null);
        this.array     = array;
        this.index     = index;
        this.value     = value;
        this.basicType = basicType;
    }

    /**
     * Return the instruction that pushed the array object.
     * @return the instruction that pushed the array object.
     */
    public Instruction array()  { return array; }

    /**
     * Return the instruction that pushed the index at which the value is to be stored.
     * @return the instruction that pushed the index at which the value is to be stored.
     */
    public Instruction index()  { return index; }

    /**
     * Return the instruction that pushed the value to be stored into the array.
     * @return the instruction that pushed the value to be stored into the array.
     */
    public Instruction value()  { return value; }

    /**
     * Return the basic type of the array. This will be one of the following:
     *
     *     INT_ARRAY
     *     LONG_ARRAY
     *     FLOAT_ARRAY
     *     DOUBLE_ARRAY
     *     OBJECT_ARRAY
     *     BYTE_ARRAY
     *     CHAR_ARRAY
     *     SHORT_ARRAY
     * @return the basic type of the array.
     */
    public Type basicType()     { return basicType; }

    /**
     * A non-primitive array store requires the stack to be empty as it
     * performs runtime checks that may involve calling Java code.
     * @return
     */
    public boolean constrainsStack() {
        return !(value.type().isPrimitive());
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
        /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {

        visitor.doStoreIndexed(this);
    }

    /**
     * Entry point for a ParameterVisitor.
     * @param visitor The ParameterVisitor.
     */
    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        index = visitor.doParameter(this, index);
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.ASTORE] + annotateType(value.type());
    }
}
