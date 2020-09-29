/*
 * @(#)AllocTable.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import javac1.Assert;
import javac1.backend.code.FrameMap;

/**
 * This class handles allocation and release of generic registers, which are
 * specified by their number.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class AllocTable {
    /**
     * The bit mask for each register. The mask for a register with a certain
     * number can be looked up in this array at the corresponding index instead
     * of shifting 1 by the appropriate number of bits.
     */
    private static final int[] REG_MASK = new int[RegAlloc.NUM_REGS];

    /**
     * The table of free registers. The value at any index indicates the
     * position of the first bit in the binary representation of the index that
     * is 0. Hence the element at the index representing the current state
     * equals the number of the first free register.
     */
    private static final int[] FREE_REG = new int[RegAlloc.NUM_REGS_EXP2];

    /**
     * The state when all registers are free.
     */
    private static final int ALL_FREE_STATE = 0;

    /**
     * The state when no register is free.
     */
    private static final int NONE_FREE_STATE = RegAlloc.NUM_REGS_EXP2 - 1;

    /**
     * Specifies which registers are free.
     */
    private int state;

    /**
     * Specifies which registers have been used.
     */
    private int orState;

    /**
     * The set of registers that must not be locked.
     */
    private RegMask lockout;

    /**
     * Initializes the register mask and the table of free registers.
     */
    static {
        for (int rnr = 0; rnr < RegAlloc.NUM_REGS; rnr++) {
            REG_MASK[rnr] = 1 << rnr;
        }
        FREE_REG[0] = FrameMap.reg2rnr(FrameMap.firstRegister());
        FREE_REG[RegAlloc.NUM_REGS_EXP2 - 1] = RegAlloc.NUM_REGS;
        for (int i = 1; i < RegAlloc.NUM_REGS_EXP2 - 1; i++) {
            while ((i & (1 << FREE_REG[i])) != 0) {
                FREE_REG[i]++;
            }
        }
    }

    /**
     * Constructs a new register allocation table.
     */
    public AllocTable() {
        state = ALL_FREE_STATE;
        orState = ALL_FREE_STATE;
        lockout = RegMask.EMPTY_SET;
    }

    /**
     * Returns whether or not all registers are free.
     *
     * @return  whether or not all registers are free
     */
    public boolean areAllFree() {
        return state == ALL_FREE_STATE;
    }

    /**
     * Returns whether or not at least one register is free.
     *
     * @return  whether or not one register is free
     */
    public boolean hasOneFree() {
        return state != NONE_FREE_STATE;
    }

    /**
     * Returns whether or not at least two registers are free.
     *
     * @return  whether or not two registers are free
     */
    public boolean hasTwoFree() {
        if (!hasOneFree()) {
            return false;
        }
        int reg = getFree();
        setLocked(reg);
        boolean hasTwo = hasOneFree();
        setFree(reg);
        return hasTwo;
    }

    /**
     * Tests if at least one register in the specified register set is free.
     *
     * @param   mask  set of registers to be examined
     * @return  whether or not one of the registers is free
     */
    public boolean hasOneFreeMasked(RegMask mask) {
        return (~state & mask.getMask()) != 0;
    }

    /**
     * Returns whether or not the register with the specified number is free.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register is free
     */
    public boolean isFree(int rnr) {
        return (state & REG_MASK[rnr]) == 0;
    }

    /**
     * Tests if the register with the specified number has been used.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register has been used
     */
    public boolean didUseRegister(int rnr) {
        return (orState & REG_MASK[rnr]) != 0;
    }

    /**
     * Tests if the two registers with the specified numbers are free.
     *
     * @param   r1  number of the first register
     * @param   r2  number of the second register
     * @return  whether or not the registers are free
     */
    public boolean areFree(int r1, int r2) {
        return isFree(r1) && isFree(r2);
    }

    /**
     * Releases the register with the specified number.
     *
     * @param  rnr  number of the register to be released
     */
    public void setFree(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFree(rnr), "register not locked");
        }
        state &= ~REG_MASK[rnr];
    }

    /**
     * Locks the register with the specified number.
     *
     * @param  rnr  number of the register to be locked
     */
    public void setLocked(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isFree(rnr), "register already locked");
        }
        state |= REG_MASK[rnr];
        orState |= state;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!lockout.contains(rnr), "lockout check failed");
        }
    }

    /**
     * Returns the number of any free register.
     *
     * @return  number of a free register
     */
    public int getFree() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(hasOneFree(), "no free register");
        }
        int rnr = FREE_REG[state];
        orState |= REG_MASK[rnr];
        return rnr;
    }

    /**
     * Returns the number of any free register in the specified register set.
     *
     * @param   mask  set of registers to be examined
     * @return  number of a free register in the set
     */
    public int getFreeMasked(RegMask mask) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(hasOneFreeMasked(mask), "no free register in the mask");
        }
        int tmpState = state | (~mask.getMask() & NONE_FREE_STATE);
        int rnr = FREE_REG[tmpState];
        orState |= REG_MASK[rnr];
        return rnr;
    }

    /**
     * Returns the set of registers that have been used at least once.
     *
     * @return  set of used registers
     */
    public RegMask usedRegisters() {
        RegMask mask = new RegMask();
        for (int rnr = 0; rnr < RegAlloc.NUM_REGS; rnr++) {
            if ((orState & REG_MASK[rnr]) != 0) {
                mask.addReg(rnr);
            }
        }
        return mask;
    }

    /**
     * Returns the set of all registers that have not been used yet.
     *
     * @return  set of unused registers
     */
    public RegMask freeRegisters() {
        RegMask mask = new RegMask();
        for (int rnr = 0; rnr < RegAlloc.NUM_REGS; rnr++) {
            if ((orState & REG_MASK[rnr]) == 0) {
                mask.addReg(rnr);
            }
        }
        return mask;
    }

    /**
     * Sets the set of registers that must not be locked.
     *
     * @param  lockout  set of registers to be locked out
     */
    public void setLockout(RegMask lockout) {
        this.lockout = lockout;
    }
}
