/*
 * @(#)LookupRange.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import javac1.ir.instr.BlockBegin;

/**
 * Represents a range of consecutive keys in a jump table.
 *
 * @see      javac1.ir.instr.LookupSwitch
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class LookupRange {
    /**
     * The lowest key of this range.
     */
    private int lowKey;

    /**
     * The highest key of this range.
     */
    private int highKey;

    /**
     * The successor associated to this range.
     */
    private BlockBegin sux;

    /**
     * Constructs a new range of consecutive keys.
     *
     * @param  lowKey  the lowest key
     * @param  sux     the successor
     */
    public LookupRange(int lowKey, BlockBegin sux) {
        this.lowKey = lowKey;
        this.highKey = lowKey;
        this.sux = sux;
    }

    /**
     * Returns the lowest key of this range.
     *
     * @return  the lowest key
     */
    public int getLowKey() {
        return lowKey;
    }

    /**
     * Sets the highest key of this range.
     *
     * @param  key  the new highest key
     */
    public void setHighKey(int key) {
        highKey = key;
    }

    /**
     * Returns the highest key of this range.
     *
     * @return  the highest key
     */
    public int getHighKey() {
        return highKey;
    }

    /**
     * Returns the successor associated to this range.
     *
     * @return  the successor
     */
    public BlockBegin getSux() {
        return sux;
    }
}
