/*
 * @(#)ArithmeticOp.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Bytecodes;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for executing a binary arithmetic operation. Both
 * additive and multiplicative operations are allowed.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ArithmeticOp extends Op2 {
    /**
     * Constructs a new instruction node for an arithmetic operation.
     *
     * @param  scope  scope containing this instruction
     * @param  op     the operation code
     * @param  x      the first operand
     * @param  y      the second operand
     */
    public ArithmeticOp(IRScope scope, int op, Instruction x, Instruction y) {
        super(scope, x.getType().meet(y.getType()), op, x, y);
        if (canTrap()) {
            setPinned(true);
        }
    }

    public boolean isCommutative() {
        switch (getOp()) {
        case Bytecodes._iadd:
            /* falls through */
        case Bytecodes._ladd:
            /* falls through */
        case Bytecodes._fadd:
            /* falls through */
        case Bytecodes._dadd:
            /* falls through */
        case Bytecodes._imul:
            /* falls through */
        case Bytecodes._lmul:
            /* falls through */
        case Bytecodes._fmul:
            /* falls through */
        case Bytecodes._dmul:
            return true;
        default:
            return false;
        }
    }

    public boolean canTrap() {
        return Bytecodes.canTrap(getOp());
    }

    public int hashCode() {
        return hash(getOp(), getX().getId(), getY().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Op2) && (((Op2) obj).getOp() == getOp())
            && (((Op2) obj).getX() == getX()) && (((Op2) obj).getY() == getY());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doArithmeticOp(this);
    }
}
