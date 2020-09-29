/*
 * @(#)AccessLocal.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions loading or storing a local variable.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessLocal extends Instruction {
    /**
     * The index of the local variable.
     */
    private int index;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the variable
     * @param  index  index into the local variable array
     */
    protected AccessLocal(IRScope scope, ValueType type, int index) {
        super(scope, type);
        this.index = index;
    }

    /**
     * Returns the index of the local variable.
     *
     * @return  the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Tests if this value is pinned by store.
     *
     * @return  whether or not it is pinned by store
     */
    public boolean isPinnedByStore() {
        return true;
    }
}
