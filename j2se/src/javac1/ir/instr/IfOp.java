/*
 * @(#)IfOp.java                        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.Bytecodes;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;

/**
 * The instruction node for a conditional expression. It compares two operands
 * and uses the result to choose one of two alternative values. This instruction
 * appears only as a result of optimizations.
 *
 * @see      CompareOp
 * @see      If
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class IfOp extends Op2 {
    /**
     * The condition code of the comparison.
     */
    private int cond;

    /**
     * The value that is chosen if the condition is true.
     */
    private Instruction tval;

    /**
     * The value that is chosen if the condition is false.
     */
    private Instruction fval;

    /**
     * Constructs a new instruction node for a conditional expression.
     *
     * @param  scope  scope containing this instruction
     * @param  x      operand to compare
     * @param  cond   the condition code
     * @param  y      operand to be compared with
     * @param  tval   result value if condition is true
     * @param  fval   result value if condition is false
     */
    public IfOp(IRScope scope, Instruction x, int cond, Instruction y,
            Instruction tval, Instruction fval) {
        super(scope, tval.getType().meet(fval.getType()), Bytecodes._illegal, x, y);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(tval.getType().getTag() == fval.getType().getTag(), "types must match");
        }
        this.cond = cond;
        this.tval = tval;
        this.fval = fval;
    }

    /**
     * Returns the condition code of the comparison.
     *
     * @return  the condition code
     */
    public int getCond() {
        return cond;
    }

    /**
     * Returns the value that is chosen if the condition is true.
     *
     * @return  the value if condition is true
     */
    public Instruction getTrueVal() {
        return tval;
    }

    /**
     * Returns the value that is chosen if the condition is false.
     *
     * @return  the value if condition is false
     */
    public Instruction getFalseVal() {
        return fval;
    }

    public boolean isCommutative() {
        return (cond == If.EQ) || (cond == If.NE);
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        tval = vc.doValue(tval);
        fval = vc.doValue(fval);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doIfOp(this);
    }
}
