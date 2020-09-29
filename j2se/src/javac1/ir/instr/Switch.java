/*
 * @(#)Switch.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.List;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for accessing a jump table. The controlling value is
 * used to select from among a number of different execution paths.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class Switch extends BlockEnd {
    /**
     * The value used to select one of the successors for execution.
     */
    private Instruction tag;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  tag   the controlling value
     * @param  sux   the list of successors
     */
    protected Switch(IRScope scope, Instruction tag, List sux) {
        super(scope, ValueType.illegalType);
        this.tag = tag;
        setSuccessors(sux);
    }

    /**
     * Returns the value that is used to select one of the successors for
     * execution.
     *
     * @return  the controlling value
     */
    public Instruction getTag() {
        return tag;
    }

    /**
     * Returns the number of alternative execution paths without the default
     * successor.
     *
     * @return  number of entries in the jump table
     */
    public int getLength() {
        return countSux() - 1;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        tag = vc.doValue(tag);
    }
}
