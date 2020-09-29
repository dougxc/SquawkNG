/*
 * @(#)Phi.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The placeholder for a value coming in from one or more predecessor blocks.
 * For the purpose of simple register allocation and code generation, none of
 * the instructions of one basic block refer directly to values in another one,
 * but only to phi instructions representing them.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Phi extends Instruction {
    /**
     * The index of the referred value.
     */
    private int index;

    /**
     * The size of the stack.
     */
    private int stackSize;

    /**
     * Constructs a new phi instruction that refers to the specified value.
     *
     * @param  scope      scope containing this instruction
     * @param  type       type of the value
     * @param  index      index of the referred value
     * @param  stackSize  the size of the stack
     */
    public Phi(IRScope scope, ValueType type, int index, int stackSize) {
        super(scope, type.getBase());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((index >= 0) && (index < stackSize), "illegal index or stack size");
        }
        this.index = index;
        this.stackSize = stackSize;
        setPinned(true);
    }

    /**
     * Returns the index of the referred value.
     *
     * @return  the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the size of the stack.
     *
     * @return  the stack size
     */
    public int getStackSize() {
        return stackSize;
    }

    public int hashCode() {
        return hash(index);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Phi) && (((Phi) obj).getIndex() == index);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doPhi(this);
    }
}
