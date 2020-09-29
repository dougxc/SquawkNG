/*
 * @(#)CompareOp.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Bytecodes;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for comparing two values of compatible but arbitrary
 * types.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CompareOp extends Op2 {
    /**
     * Constructs a new instruction node for comparing two values.
     *
     * @param  scope  scope containing this instruction
     * @param  op     the operation code
     * @param  x      operand to compare
     * @param  y      operand to be compared with
     */
    public CompareOp(IRScope scope, int op, Instruction x, Instruction y) {
        super(scope, ValueType.intType, op, x, y);
    }

    /**
     * Tests for floating-point operations if the first operand has to be
     * considered to be less than the second one if at least one of the operands
     * is NaN.
     *
     * @return  whether or not unordered means less
     */
    public boolean isUnorderedLess() {
        return (getOp() == Bytecodes._fcmpl) || (getOp() == Bytecodes._dcmpl);
    }

    public int hashCode() {
        return hash(getOp(), getX().getId(), getY().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Op2) && (((Op2) obj).getOp() == getOp())
            && (((Op2) obj).getX() == getX()) && (((Op2) obj).getY() == getY());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doCompareOp(this);
    }
}
