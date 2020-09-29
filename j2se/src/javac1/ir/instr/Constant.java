/*
 * @(#)Constant.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;
import javac1.ir.types.IntConstant;

/**
 * The instruction node for loading a constant.
 *
 * @see      ValueType#isConstant()
 * @see      javac1.ir.types.AddressConstant
 * @see      javac1.ir.types.ArrayConstant
 * @see      javac1.ir.types.ClassConstant
 * @see      javac1.ir.types.DoubleConstant
 * @see      javac1.ir.types.FloatConstant
 * @see      javac1.ir.types.InstanceConstant
 * @see      javac1.ir.types.IntConstant
 * @see      javac1.ir.types.LongConstant
 * @see      javac1.ir.types.ObjectConstant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Constant extends Instruction {
    /**
     * Constructs a new instruction node for loading a constant.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the constant
     */
    public Constant(IRScope scope, ValueType type) {
        super(scope, type);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(type.isConstant(), "type must be a constant type");
        }
    }

    public int hashCode() {
        return (getType() instanceof IntConstant) ?
            hash(((IntConstant) getType()).getValue()) : 0;
    }

    public boolean equals(Object obj) {
        return (getType() instanceof IntConstant) && (obj instanceof Constant)
            && (getType().equals(((Constant) obj).getType()));
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doConstant(this);
    }
}
