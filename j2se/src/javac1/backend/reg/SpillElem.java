/*
 * @(#)SpillElem.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import javac1.Assert;
import javac1.ir.instr.Instruction;

/**
 * Represents an element spilled on the stack. If there are not enough registers
 * during code generation, some values must be spilled. The amount of required
 * spill space is computed as a side effect of register allocation, so that all
 * stack offsets are known at the time of code generation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class SpillElem {
    /**
     * The value of the spill element.
     */
    private Instruction instr;

    /**
     * The reference count of the spill element.
     */
    private int refCount;

    /**
     * Whether or not the spill slot can contain an ordinary object pointer.
     */
    private boolean isOop;

    /**
     * Constructs a new spill element.
     */
    public SpillElem() {
       clear();
    }

    /**
     * Sets all attributes of this spill element in one go.
     *
     * @param  instr     value of the spill element
     * @param  refCount  reference count
     * @param  isOop     whether or not slot can contain object pointer
     */
    public void set(Instruction instr, int refCount, boolean isOop) {
        this.instr = instr;
        this.refCount = refCount;
        this.isOop = isOop;
    }

    /**
     * Sets the attributes of this spill element using the specified one.
     *
     * @param  spill  spill element to copy attributes from
     */
    public void setUsing(SpillElem spill) {
        set(spill.getInstr(), spill.getRefCount(), spill.isOop());
    }

    /**
     * Returns the value of this spill element.
     *
     * @return  value of the spill element
     */
    public Instruction getInstr() {
        return instr;
    }

    /**
     * Returns the reference count of this spill element.
     *
     * @return  reference count
     */
    public int getRefCount() {
        return refCount;
    }

    /**
     * Tests if the spill slot can contain an ordinary object pointer.
     *
     * @return  whether or not slot can contain object pointer
     */
    public boolean isOop() {
        return isOop;
    }

    /**
     * Returns whether or not this spill slot is free.
     *
     * @return  whether or not the spill slot is free
     */
    public boolean isFree() {
        return refCount == 0;
    }

    /**
     * Decrements the reference count of this spill element.
     */
    public void decRefCount() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(refCount > 0, "cannot decrement reference count");
        }
        refCount--;
    }

    /**
     * Sets the reference count of this spill element to 0.
     */
    public void clearRefCount() {
        refCount = 0;
    }

    /**
     * Clears the attributes of this spill element.
     */
    public void clear() {
        instr = null;
        refCount = 0;
        isOop = false;
    }
}
