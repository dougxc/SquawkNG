/*
 * @(#)AccessLoop.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions that mark the beginning or the end
 * of a loop. During optimization a simple algorithm is used to determine
 * innermost loops. This information may be used by the back end for better
 * register allocation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessLoop extends Instruction {
    /**
     * The identification number of the loop.
     */
    private int loopId;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope   scope containing this instruction
     * @param  loopId  the identification number of the loop
     */
    protected AccessLoop(IRScope scope, int loopId) {
        super(scope, ValueType.illegalType);
        this.loopId = loopId;
    }

    /**
     * Returns the unique identification number of the loop.
     *
     * @return  the identification number of the loop
     */
    public int getLoopId() {
        return loopId;
    }
}
