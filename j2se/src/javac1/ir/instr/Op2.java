/*
 * @(#)Op2.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for all binary operations. There are concrete
 * subclasses for arithmetic, comparing, logical and shift operations.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class Op2 extends Instruction {
    /**
     * The operation code.
     */
    private int op;

    /**
     * The operand to the left of the operator.
     */
    private Instruction x;

    /**
     * The operand to the right of the operator.
     */
    private Instruction y;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the operands
     * @param  op     the operation code
     * @param  x      the first operand
     * @param  y      the second operand
     */
    protected Op2(IRScope scope, ValueType type, int op, Instruction x,
            Instruction y) {
        super(scope, type);
        this.op = op;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the operand to the left of the operator.
     *
     * @return  the first operand
     */
    public Instruction getX() {
        return x;
    }

    /**
     * Returns the operation code.
     *
     * @return  the operation code
     */
    public int getOp() {
        return op;
    }

    /**
     * Returns the operand to the right of the operator.
     *
     * @return  the second operand
     */
    public Instruction getY() {
        return y;
    }

    /**
     * Swaps the operands, provided that the operation is commutative.
     */
    public void swapOperands() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isCommutative(), "operation must be commutative");
        }
        Instruction t = x;
        x = y;
        y = t;
    }

    /**
     * Returns whether or not the operation is commutative. In this case the
     * operands can be swapped.
     *
     * @return  whether or not the operation is commutative
     */
    public boolean isCommutative() {
        return false;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        x = vc.doValue(x);
        y = vc.doValue(y);
    }
}
