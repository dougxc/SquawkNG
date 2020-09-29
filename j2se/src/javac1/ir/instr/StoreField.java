/*
 * @(#)StoreField.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ObjectType;

/**
 * The instruction node for setting a static field or an instance variable.
 *
 * @see      LoadField
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class StoreField extends AccessField {
    /**
     * The value that has to be stored into the field.
     */
    private Instruction value;

    /**
     * Constructs a new instruction node for setting a field.
     *
     * @param  scope        scope containing this instruction
     * @param  obj          the parent object
     * @param  offset       offset of the field
     * @param  fieldType    <strong>basic</strong> field type
     * @param  value        the new value of the field
     * @param  isStatic     whether it is a static or an instance field
     * @param  loaded       whether or not the field is loaded
     * @param  initialized  whether or not the field is initialized
     */
    public StoreField(IRScope scope, Instruction obj, int offset, int fieldType,
            Instruction value, boolean isStatic, boolean loaded,
            boolean initialized) {
        super(scope, obj, offset, fieldType, isStatic, loaded, initialized);
        this.value = value;
        setPinned(true);
    }

    /**
     * Returns whether or not a write barrier is needed.
     *
     * @return  whether or not a write barrier is needed
     */
    public boolean needsWriteBarrier() {
        return getType() instanceof ObjectType;
    }

    /**
     * Returns the value that has to be stored into the field.
     *
     * @return  the new value of the field
     */
    public Instruction getValue() {
        return value;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        value = vc.doValue(value);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doStoreField(this);
    }
}
