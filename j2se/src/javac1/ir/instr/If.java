/*
 * @(#)If.java                          1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for conditional jumps. It compares two operands and uses
 * the result to choose one of two alternative successors for execution.
 *
 * @see      CompareOp
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class If extends BlockEnd {
    /**
     * A condition code constant.
     */
    public static final int EQ = 0, NE = 1, LT = 2, LE = 3, GT = 4, GE = 5;

    /**
     * The mirrored condition codes.
     */
    public static final int[] MIRROR = new int[] {EQ, NE, GT, GE, LT, LE};

    /**
     * The negated condition codes.
     */
    public static final int[] NEGATE = new int[] {NE, EQ, GE, GT, LE, LT};

    /**
     * The value to compare.
     */
    private Instruction x;

    /**
     * The condition code.
     */
    private int cond;

    /**
     * The truth value of the condition if one of the operands is NaN.
     */
    private boolean unorderedIsTrue;

    /**
     * The value to be compared with.
     */
    private Instruction y;

    /**
     * Constructs a new instruction node for a conditional jump.
     *
     * @param  scope            scope containing this instruction
     * @param  x                operand to compare
     * @param  cond             condition code
     * @param  unorderedIsTrue  truth value if one of the operands is NaN
     * @param  y                operand to be compared with
     * @param  tsux             successor executed if condition is true
     * @param  fsux             successor executed if condition is false
     */
    public If(IRScope scope, Instruction x, int cond, boolean unorderedIsTrue,
            Instruction y, BlockBegin tsux, BlockBegin fsux) {
        super(scope, ValueType.illegalType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.getType().getTag() == y.getType().getTag(), "types must match");
        }
        this.x = x;
        this.cond = cond;
        this.unorderedIsTrue = unorderedIsTrue;
        this.y = y;
        addSux(tsux);
        addSux(fsux);
    }

    /**
     * Returns the operand to compare
     *
     * @return  operand to compare
     */
    public Instruction getX() {
        return x;
    }

    /**
     * Returns the condition code.
     *
     * @return  condition code
     */
    public int getCond() {
        return cond;
    }

    /**
     * Returns the operand to be compared with.
     *
     * @return  operand to be compared with
     */
    public Instruction getY() {
        return y;
    }

    /**
     * Swaps the two operands and mirrors the condition code.
     */
    public void swapOperands() {
        Instruction t = x;
        x = y;
        cond = MIRROR[cond];
        y = t;
    }

    /**
     * Swaps the successors and negates the condition code.
     */
    public void swapSuccessors() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(countSux() == 2, "wrong number of successors");
        }
        setSux(1, setSux(0, suxAt(1)));
        cond = NEGATE[cond];
        unorderedIsTrue = !unorderedIsTrue;
    }

    /**
     * Returns the successor that will be executed if the condition evaluates
     * to the specified truth value.
     *
     * @param   isTrue  truth value
     * @return  the corresponding successor
     */
    public BlockBegin suxFor(boolean isTrue) {
        return suxAt(isTrue ? 0 : 1);
    }

    /**
     * Returns the successor that will be executed if the condition is true.
     *
     * @return  successor executed if condition is true
     */
    public BlockBegin trueSux() {
        return suxFor(true);
    }

    /**
     * Returns the successor that will be executed if the condition is false.
     *
     * @return  successor executed if condition is false
     */
    public BlockBegin falseSux() {
        return suxFor(false);
    }

    /**
     * Returns the successor that will be executed if at least one of the
     * operands is NaN.
     *
     * @return  successor executed if one of the operands is NaN
     */
    public BlockBegin unorderedSux() {
        return suxFor(unorderedIsTrue);
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        x = vc.doValue(x);
        y = vc.doValue(y);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doIf(this);
    }
}
