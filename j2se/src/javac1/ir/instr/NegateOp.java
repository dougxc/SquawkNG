/*
 * @(#)NegateOp.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;

/**
 * The instruction node for the arithmetic negation of a value. For integer
 * values, negation is the same as subtraction from zero. This is not the case
 * for float values, because if the value is a zero, the result of negation is
 * the zero of opposite sign.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NegateOp extends Instruction {
    /**
     * The value to negate.
     */
    private Instruction x;

    /**
     * Constructs a new instruction node for a negation.
     *
     * @param  scope  scope containing this instruction
     * @param  x      the operand
     */
    public NegateOp(IRScope scope, Instruction x) {
        super(scope, x.getType());
        this.x = x;
    }

    /**
     * Returns the value to negate.
     *
     * @return  the operand
     */
    public Instruction getX() {
        return x;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        x = vc.doValue(x);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNegateOp(this);
    }
}
