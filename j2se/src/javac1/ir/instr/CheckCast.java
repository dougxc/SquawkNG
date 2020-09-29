/*
 * @(#)CheckCast.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ci.Klass;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for checking whether an object is of a given type. The
 * <code>CheckCast</code> instruction differs from <code>InstanceOf</code> only
 * in its treatment of <code>null</code> and its behavior when the test fails.
 *
 * @see      InstanceOf
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CheckCast extends TypeCheck {
    /**
     * Constructs a new instruction node for a type check.
     *
     * @param  scope  scope containing this instruction
     * @param  clazz  reference type to test towards
     * @param  obj    object to check
     */
    public CheckCast(IRScope scope, Klass klass, Instruction obj) {
        super(scope, klass, obj, ValueType.objectType);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doCheckCast(this);
    }
}
