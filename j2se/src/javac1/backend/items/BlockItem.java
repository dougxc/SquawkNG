/*
 * @(#)BlockItem.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.items;

import java.util.BitSet;
import java.util.List;
import javac1.backend.reg.RegMask;
import javac1.backend.reg.RInfo;

/**
 * Maps the indices of cached local variables to registers.
 *
 * @see      javac1.backend.LocalCaching
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BlockItem {
    /**
     * The mapping of local variables to registers.
     */
    private List mapping;

    /**
     * Specifies which of the cached variables can contain object pointers.
     */
    private BitSet oops;

    /**
     * The registers used for caching of local variables in the block.
     */
    private RegMask lockout;

    /**
     * Specifies the precision of floating-point calculations in the block.
     */
    private boolean is32bit;

    /**
     * Constructs a new block item with the specified mapping.
     *
     * @param  map      mapping of variable indices to registers
     * @param  oops     bit set of object pointers
     * @param  lockout  registers used for caching
     * @param  is32bit  specifies the floating-point precision
     */
    public BlockItem(List map, BitSet oops, RegMask lockout, boolean is32bit) {
        this.mapping = map;
        this.oops = oops;
        this.lockout = lockout;
        this.is32bit = is32bit;
    }

    /**
     * Returns the register that is mapped to the specified index.
     *
     * @param   index  index of the local variable
     * @return  register that the index is mapped to
     */
    public RInfo getCacheReg(int index) {
        if (index < mapping.size()) {
            return (RInfo) mapping.get(index);
        } else {
            return RInfo.NO_RINFO;
        }
    }

    /**
     * Tests if the cached variable at the specified index can contain an object
     * pointer.
     *
     * @param   index  index of the local variable
     * @return  whether or not the variable can contain object pointer
     */
    public boolean isOop(int index) {
        return (oops != null) && oops.get(index);
    }

    /**
     * Returns the maximum number of local variables mapped to registers.
     *
     * @return  maximum number of mapped indices
     */
    public int mapLength() {
        return mapping.size();
    }

    /**
     * Sets the registers used for caching of local variables in the block.
     *
     * @param  lockout  registers used for caching
     */
    public void setLockout(RegMask lockout) {
        this.lockout = lockout;
    }

    /**
     * Returns the registers used for caching of local variables in the block.
     *
     * @return  the registers used for caching
     */
    public RegMask getLockout() {
        return lockout;
    }

    /**
     * Sets the precision of floating-point calculations in the block.
     *
     * @param  is32bit  specifies the floating-point precision
     */
    public void set32bitPrecision(boolean is32bit) {
        this.is32bit = is32bit;
    }

    /**
     * Specifies the precision of floating-point calculations in the block.
     *
     * @return  precision of floating-point calculations
     */
    public boolean is32bitPrecision() {
        return is32bit;
    }
}
