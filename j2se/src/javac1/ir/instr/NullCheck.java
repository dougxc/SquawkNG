/*
 * @(#)NullCheck.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ObjectType;

/**
 * The instruction node that tests for the null reference. If the use of an
 * object has been eliminated during optimization then this instruction can be
 * inserted to ensure at least that the reference is not null.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NullCheck extends Instruction {
    /**
     * The value to be checked against the null reference.
     */
    private Instruction obj;

    /**
     * Constructs a new instruction node that tests for the null reference.
     *
     * @param  scope  scope containing this instruction
     * @param  obj    the value to be checked
     */
    public NullCheck(IRScope scope, Instruction obj) {
        super(scope, obj.getType());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.getType() instanceof ObjectType, "must be applied to objects only");
        }
        this.obj = obj;
        setPinned(true);
    }

    /**
     * Returns the value to be checked against the null reference.
     *
     * @return  the value to be checked
     */
    public Instruction getObj() {
        return obj;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        obj = vc.doValue(obj);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNullCheck(this);
    }
}
