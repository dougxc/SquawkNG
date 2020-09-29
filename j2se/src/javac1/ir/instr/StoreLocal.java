/*
 * @(#)StoreLocal.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for storing a value into a local variable.
 *
 * @see      LoadLocal
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class StoreLocal extends AccessLocal {
    /**
     * The value that has to be stored into the local variable.
     */
    private Instruction value;

    /**
     * Constructs a new instruction node for setting a local variable.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the variable
     * @param  index  index into the local variable array
     * @param  value  the new value of the local variable
     */
    public StoreLocal(IRScope scope, ValueType type, int index, Instruction value) {
        super(scope, type, index);
        this.value = value;
        setPinned(true);
    }

    /**
     * Returns the value that has to be stored into the local variable.
     *
     * @return  the new value of the local variable
     */
    public Instruction getValue() {
        return value;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        value = vc.doValue(value);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreLocal(this);
    }
}
