package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.OpConst;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This class represents all the arithmetic, logical and comparison instructions in the IR.
 * These, in turn, correspond to all the Java bytecode arithmetic instructions:
 *
 *    IADD
 *    LADD
 *    FADD
 *    DADD
 *    ISUB
 *    LSUB
 *    FSUB
 *    DSUB
 *    IMUL
 *    LMUL
 *    FMUL
 *    DMUL
 *    IDIV
 *    LDIV
 *    FDIV
 *    DDIV
 *    IREM
 *    LREM
 *    FREM
 *    DREM
 *
 * , logical instructions:
 *
 *    IAND
 *    LAND
 *    IOR
 *    LOR
 *    IXOR
 *    LXOR
 *
 * and comparison instructions:
 *
 *    LCMP
 *    FCMPL
 *    FCMPG
 *    DCMPL
 *    DCMPG
 *
 */
public final class ArithmeticOp extends Trappable {

    /** The arithmetic, comparison or logical operation. */
    private final OpConst op;
    /** The typed bytecode that will be generated for this operation. */
    private final int opcode;
    /** Instruction that pushes the left operand. */
    private Instruction left;
    /** Instruction that pushes the right operand. */
    private Instruction right;
    /** Flags whether this is a comparison operation. */
    private final boolean isComparison;

    /**
     * Create a new ArithmeticOp instruction.
     * @param op The operation.
     * @param opcode The typed bytecode that will be generated for this operation.
     * @param left Instruction that generates the left operand.
     * @param right Instruction that generates the left operand.
     * @param isComparison Flags whether this is a comparison operation.
     */
    ArithmeticOp(Type type,
                 OpConst op,
                 int opcode,
                 Instruction left,
                 Instruction right,
                 boolean isComparison) {
        super(type);
        this.op           = op;
        this.opcode       = opcode;
        this.isComparison = isComparison;
        this.left         = left;
        this.right        = right;
    }

    /**
     * Return the constant specifying the exact semantics of this instruction.
     * @return the constant specifying the exact semantics of this instruction.
     */
    public OpConst op()             { return op;    }

    public int opcode()         { return opcode; }

    /**
     * Return the instruction that pushes the left operand.
     * @return the instruction that pushes the left operand.
     */
    public Instruction left()   { return left;  }

    /**
     * Return the instruction that pushes the left operand.
     * @return the instruction that pushes the left operand.
     */
    public Instruction right()  { return right; }

    public boolean isComparison() {
        return isComparison;
    }

    /**
     * Return whether or not this instruction can raise an exception.
     * @return true if this instruction can raise an exception.
     */
    public boolean canTrap() {
        return op == OpConst.DIV || op == OpConst.REM;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doArithmeticOp(this);
    }

    /**
     * Entry point for a ParameterVisitor.
     * @param visitor The ParameterVisitor.
     */
    public void visit(ParameterVisitor visitor) {
        left  = visitor.doParameter(this, left);
        right = visitor.doParameter(this, right);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return op.toString();
    }
}


