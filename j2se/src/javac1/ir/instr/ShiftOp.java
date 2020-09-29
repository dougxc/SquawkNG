/*
 * @(#)ShiftOp.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for shifting a value left or right by the specified
 * number of bits. There are two variants of the right shift. The arithmetic
 * right shift uses sign extension, that is ones are inserted at the higher
 * order bits if the value is negative. The logical variant uses zero extension
 * and inserts always zeroes regardless of the sign.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ShiftOp extends Op2 {
    /**
     * Constructs a new instruction node for a shift operation.
     *
     * @param  scope  scope containing this instruction
     * @param  op     the operation code
     * @param  x      value to shift
     * @param  s      shift distance
     */
    public ShiftOp(IRScope scope, int op, Instruction x, Instruction s) {
        super(scope, x.getType().getBase(), op, x, s);
    }

    public int hashCode() {
        return hash(getOp(), getX().getId(), getY().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Op2) && (((Op2) obj).getOp() == getOp())
            && (((Op2) obj).getX() == getX()) && (((Op2) obj).getY() == getY());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doShiftOp(this);
    }
}
