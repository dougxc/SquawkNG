/*
 * @(#)LogicOp.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for executing a binary bitwise logical operation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LogicOp extends Op2 {
    /**
     * Constructs a new instruction node for a logical operation.
     *
     * @param  scope  scope containing this instruction
     * @param  op     the operation code
     * @param  x      the first operand
     * @param  y      the second operand
     */
    public LogicOp(IRScope scope, int op, Instruction x, Instruction y) {
        super(scope, x.getType().meet(y.getType()), op, x, y);
    }

    public boolean isCommutative() {
        return true;
    }

    public int hashCode() {
        return hash(getOp(), getX().getId(), getY().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Op2) && (((Op2) obj).getOp() == getOp())
            && (((Op2) obj).getX() == getX()) && (((Op2) obj).getY() == getY());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLogicOp(this);
    }
}
