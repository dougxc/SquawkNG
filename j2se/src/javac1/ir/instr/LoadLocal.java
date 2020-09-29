/*
 * @(#)LoadLocal.java                   1.10 02/11/27
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
 * The instruction node for loading a local variable.
 *
 * @see      StoreLocal
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LoadLocal extends AccessLocal {
    /**
     * Constructs a new instruction node for loading a local variable.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the variable
     * @param  index  index into the local variable array
     */
    public LoadLocal(IRScope scope, ValueType type, int index) {
        super(scope, type, index);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!type.isConstant(), "type must not be a constant type");
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadLocal(this);
    }
}
