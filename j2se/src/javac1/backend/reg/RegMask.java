/*
 * @(#)RegMask.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;

/**
 * This class represents a set of word registers. A register is a member of the
 * set if and only if the bit corresponding to its number is set in the integer
 * mask.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class RegMask {
    /**
     * The set containing no registers.
     */
    public static final RegMask EMPTY_SET = new RegMask();

    /**
     * The integer mask representing the set of registers.
     */
    private int mask;

    /**
     * Constructs a new empty register set.
     */
    public RegMask() {
        mask = 0;
    }

    /**
     * Constructs a new register set and adds the specified word register to it.
     *
     * @param  reg  register to be added
     */
    public RegMask(RInfo reg) {
        this();
        addReg(reg);
    }

    /**
     * Adds the register with the specified number to the set.
     *
     * @param  rnr  number of the register to be added
     */
    public void addReg(int rnr) {
        mask |= 1 << rnr;
    }

    /**
     * Adds the specified word register to the set.
     *
     * @param  reg  register to be added
     */
    public void addReg(RInfo reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg.isWord(), "only cpu registers");
        }
        addReg(reg.reg());
    }

    /**
     * Removes the register with the specified number from the set.
     *
     * @param  rnr  number of the register to be removed
     */
    public void removeReg(int rnr) {
        mask &= ~(1 << rnr);
    }

    /**
     * Removes the specified word register from the set.
     *
     * @param  reg  register to be removed
     */
    public void removeReg(RInfo reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg.isWord(), "only cpu registers");
        }
        removeReg(reg.reg());
    }

    /**
     * Tests if the register with the specified number is an element of this
     * set.
     *
     * @param   rnr  number of the register to look for
     * @return  whether or not the register is an element of this set
     */
    public boolean contains(int rnr) {
        return (mask & (1 << rnr)) != 0;
    }

    /**
     * Tests if the specified word register is an element of this set.
     *
     * @param   reg  register to look for
     * @return  whether or not the register is an element of this set
     */
    public boolean contains(RInfo reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg.isWord(), "only cpu registers");
        }
        return contains(reg.reg());
    }

    /**
     * Returns the integer mask representing this set of registers.
     *
     * @return  the register mask
     */
    public int getMask() {
        return mask;
    }

    /**
     * Returns the register with the lowest number in this set.
     *
     * @return  the first register
     */
    public RInfo getFirstReg() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isEmpty(), "set is empty");
        }
        int rnr = 0;
        for (int x = mask; (x & 1) == 0; x >>>= 1) {
            rnr++;
        }
        return RInfo.wordReg(rnr);
    }

    /**
     * Converts this set of registers to a collection of register information
     * objects.
     *
     * @return  collection of register information objects
     */
    public List getRInfoCollection() {
        List infos = new ArrayList();
        for (int x = mask, rnr = 0; x != 0; x >>>= 1, rnr++) {
            if ((x & 1) != 0) {
                infos.add(RInfo.wordReg(rnr));
            }
        }
        return infos;
    }

    /**
     * Tests if this set does not contain any elements.
     *
     * @return  whether or not the set is empty
     */
    public boolean isEmpty() {
        return mask == 0;
    }
}
