/*
 * @(#)LoadIndexed.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for loading a value from an array.
 *
 * @see      StoreIndexed
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LoadIndexed extends AccessIndexed {
    /**
     * Constructs a new instruction node for loading an element.
     *
     * @param  scope     scope containing this instruction
     * @param  array     the array to be accessed
     * @param  index     index of the element
     * @param  elemType  <strong>basic</strong> element type
     */
    public LoadIndexed(IRScope scope, Instruction array, Instruction index,
            int elemType) {
        super(scope, array, index, elemType);
    }

    public int hashCode() {
        return hash(getArray().getId(), getIndex().getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof LoadIndexed)
            && (((LoadIndexed) obj).getArray() == getArray())
            && (((LoadIndexed) obj).getIndex() == getIndex());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoadIndexed(this);
    }
}
