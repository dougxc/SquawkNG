/*
 * @(#)LoadField.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for loading a static field or an instance variable.
 *
 * @see      StoreField
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LoadField extends AccessField {
    /**
     * Constructs a new instruction node for loading a field.
     *
     * @param  scope        scope containing this instruction
     * @param  obj          the parent object
     * @param  offset       offset of the field
     * @param  fieldType    <strong>basic</strong> field type
     * @param  isStatic     whether it is a static or an instance field
     * @param  loaded       whether or not the field is loaded
     * @param  initialized  whether or not the field is initialized
     */
    public LoadField(IRScope scope, Instruction obj, int offset, int fieldType,
            boolean isStatic, boolean loaded, boolean initialized) {
        super(scope, obj, offset, fieldType, isStatic, loaded, initialized);
    }

    public int hashCode() {
        return isLoaded() ? hash(getObj().getId(), getOffset()) : 0;
    }

    public boolean equals(Object obj) {
        return isLoaded() && (obj instanceof LoadField)
            && (((LoadField) obj).getObj() == getObj())
            && (((LoadField) obj).getOffset() == getOffset());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadField(this);
    }
}
