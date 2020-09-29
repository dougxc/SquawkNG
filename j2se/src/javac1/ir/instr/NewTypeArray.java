/*
 * @(#)NewTypeArray.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for creating a new array with the specified element
 * type.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewTypeArray extends NewArray {
    /**
     * The basic element type of the new array.
     */
    private int elemType;

    /**
     * Constructs a new instruction node for creating an array.
     *
     * @param  scope     scope containing this instruction
     * @param  length    length of the new array
     * @param  elemType  <strong>basic</strong> element type
     */
    public NewTypeArray(IRScope scope, Instruction length, int elemType) {
        super(scope, length);
        this.elemType = elemType;
    }

    /**
     * Returns the element type of the new array.
     *
     * @return  the element type
     */
    public int getElemType() {
        return elemType;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNewTypeArray(this);
    }
}
