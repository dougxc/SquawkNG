/*
 * @(#)NewObjectArray.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ci.Klass;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for creating a new one-dimensional array of object
 * references.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewObjectArray extends NewArray {
    /**
     * The reference type of the array to be created.
     */
    private Klass klass;

    /**
     * Constructs a new instruction node for creating a one-dimensional array.
     *
     * @param  scope  scope containing this instruction
     * @param  klass  type of the new array
     * @param  count  length of the new array
     */
    public NewObjectArray(IRScope scope, Klass klass, Instruction length) {
        super(scope, length);
        this.klass = klass;
    }

    /**
     * Returns the reference type of the array to be created.
     *
     * @return  type of the new array
     */
    public Klass getKlass() {
        return klass;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNewObjectArray(this);
    }
}
