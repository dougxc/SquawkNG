/*
 * @(#)ArrayLength.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for determining the number of components in an array.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ArrayLength extends AccessArray {
    /**
     * Constructs a new instruction node for getting the length of an array.
     *
     * @param  scope  scope containing this instruction
     * @param  array  the array to be accessed
     */
    public ArrayLength(IRScope scope, Instruction array) {
        super(scope, ValueType.intType, array);
    }

    public int hashCode() {
        return hash(getArray().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof ArrayLength)
            && (((ArrayLength) obj).getArray() == getArray());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doArrayLength(this);
    }
}
