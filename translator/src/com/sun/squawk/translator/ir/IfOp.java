package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.OpConst;
import com.sun.squawk.translator.util.BitSet;
import com.sun.squawk.translator.loader.StackMap;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This represents all the conditional branch Java bytecode instructions:
 *
 *    IFEQ
 *    IFNE
 *    IFLT
 *    IFGE
 *    IFGT
 *    IFLE
 *    IF_ICMPEQ
 *    IF_ICMPNE
 *    IF_ICMPLT
 *    IF_ICMPGE
 *    IF_ICMPGT
 *    IF_ICMPLE
 *    IF_ACMPEQ
 *    IF_ACMPNE
 *    IFNULL
 *    IFNONNULL
 */
public final class IfOp extends Goto {

    /** The conditional operation. */
    private final OpConst op;
    /** The instruction that pushes the left operand or null if this is a compare with zero/null. */
    private Instruction left;
    /** The instruction that pushes the right operand. */
    private Instruction right;

    /**
     * Create a new conditional instruction.
     * @param op The conditional operation.
     * @param left The instruction that pushes the left operand or null if this is a compare with zero/null.
     * @param right The instruction that pushes the right operand.
     * @param target The target of the branch.
     */
    IfOp(OpConst op, Instruction left, Instruction right, Target target) {
        super(target);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    /**
     * Return the conditional operation.
     * @return the conditional operation.
     */
    public OpConst op() {
        return op;
    }

    /**
     * Return the instruction that pushes the left operand or null if this is a compare with zero/null..
     * @return the instruction that pushes the left operand or null if this is a compare with zero/null..
     */
    public Instruction left() {
        return left;
    }

    /**
     * Return the instruction that pushes the right operand.
     * @return the instruction that pushes the right operand.
     */
     public Instruction right() {
        return right;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {

        visitor.doIfOp(this);
    }

    /**
     * Entry point for a ParameterVisitor.
     * @param visitor The ParameterVisitor.
     */
    public void visit(ParameterVisitor visitor) {
        if (left != null) {
            left  = visitor.doParameter(this, left);
        }
        right = visitor.doParameter(this, right);
    }

    /**
     * Implementation of BasicBlockEndDelimiter.
     * @return
     */
    protected Instruction[] createSuccessors() {
        return new Instruction[] { target().getInstruction(), getNext() };
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[op.ifBytecode(left == null)] + " " + target().getInstruction().getRelocIP();
    }
}
