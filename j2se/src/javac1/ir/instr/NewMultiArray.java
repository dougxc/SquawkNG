/*
 * @(#)NewMultiArray.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.List;
import javac1.ci.Klass;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;

/**
 * The instruction node for creating a new multidimensional array.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewMultiArray extends NewArray {
    /**
     * The reference type of the array to be created.
     */
    private Klass klass;

    /**
     * The desired length of the new array per dimension.
     */
    private List dims;

    /**
     * Constructs a new instruction node for creating a multidimensional array.
     *
     * @param  scope  scope containing this instruction
     * @param  klass  type of the new array
     * @param  dims   length per dimension
     */
    public NewMultiArray(IRScope scope, Klass klass, List dims) {
        super(scope, (Instruction) dims.get(0));
        this.klass = klass;
        this.dims = dims;
    }

    /**
     * Returns the reference type of the array to be created.
     *
     * @return  type of the new array
     */
    public Klass getKlass() {
        return klass;
    }

    /**
     * Returns an array of values representing the desired length of the new
     * array per dimension.
     *
     * @return  length per dimension
     */
    public List getDims() {
        return dims;
    }

    /**
     * Returns the number of dimensions of the new array.
     *
     * @return  the rank of the new array
     */
    public int getRank() {
        return dims.size();
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        for (int i = 1; i < dims.size(); i++) {
            dims.set(i, vc.doValue((Instruction) dims.get(i)));
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNewMultiArray(this);
    }
}
