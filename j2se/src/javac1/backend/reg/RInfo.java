/*
 * @(#)RInfo.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import javac1.Assert;
import javac1.backend.code.FrameMap;
import javac1.backend.code.Register;

/**
 * This class represents a register that is either in the CPU or in the FPU. It
 * also represents long registers, which are composed of two general-purpose
 * registers.
 *
 * @see      Register
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class RInfo {
    /**
     * The type constant for word registers.
     */
    public static final int WORD_REG_TYPE = 0;

    /**
     * The type constant for double-word registers.
     */
    public static final int LONG_REG_TYPE = 1;

    /**
     * The type constant for single-precision floating-point registers.
     */
    public static final int FLOAT_REG_TYPE = 2;

    /**
     * The type constant for double-precision floating-point registers.
     */
    public static final int DOUBLE_REG_TYPE = 3;

    /**
     * The number of illegal registers.
     */
    public static final int ILLEGAL_REG_NUMBER = -1;

    /**
     * The constant used to indicate that the desired register does not exist.
     */
    public static final RInfo NO_RINFO = new RInfo();

    /**
     * The number of bits that encode the type of the register.
     */
    private static final int TYPE_BITS = 2;

    /**
     * The number of bits that encode one physical register in use.
     */
    private static final int REG_BITS = 5;

    /**
     * The distance that the second register number is shifted to the left.
     */
    private static final int REG2_SHIFT = TYPE_BITS + REG_BITS;

    /**
     * The bit mask used to extract the type of the register.
     */
    private static final int TYPE_MASK = (1 << TYPE_BITS) - 1;

    /**
     * The bit mask used to extract one of the physical registers in use.
     */
    private static final int REG_MASK = (1 << REG_BITS) - 1;

    /**
     * The bit field containing both the type and the physical registers used.
     */
    private int number;

    /**
     * Constructs a new register information object with the specified number.
     *
     * @param  number  the number of the register
     */
    public RInfo(int number) {
        this.number = number;
    }

    /**
     * Constructs a new register information object. Afterwards the number of
     * the register still must be set by calling one of the access methods
     * according to the type of the register.
     */
    public RInfo() {
        this(ILLEGAL_REG_NUMBER);
    }

    /**
     * Constructs a new information object for the word register with the
     * specified number.
     *
     * @param  rnr  number of the word register
     */
    public static RInfo wordReg(int rnr) {
        RInfo rinfo = new RInfo();
        rinfo.setWordReg(rnr);
        return rinfo;
    }

    /**
     * Returns the number that encodes the type and the physical registers used.
     *
     * @return  the number of this register
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the type of this register.
     *
     * @return  the type of this register
     */
    public int getType() {
        return (number & TYPE_MASK);
    }

    /**
     * Tests if this is a word register.
     *
     * @return  whether or not this is a word register
     */
    public boolean isWord() {
        return getType() == WORD_REG_TYPE;
    }

    /**
     * Tests if this is a double-word register.
     *
     * @return  whether or not this is a long register
     */
    public boolean isLong() {
        return getType() == LONG_REG_TYPE;
    }

    /**
     * Tests if this is a single-precision floating-point register.
     *
     * @return  whether or not this is a single-precision floating-point register
     */
    public boolean isFloat() {
        return getType() == FLOAT_REG_TYPE;
    }

    /**
     * Tests if this is a double-precision floating-point register.
     *
     * @return  whether or not this is a double-precision floating-point register
     */
    public boolean isDouble() {
        return getType() == DOUBLE_REG_TYPE;
    }

    /**
     * Tests if this is a floating-point register.
     *
     * @return  whether or not this is a floating-point register
     */
    public boolean isFloatKind() {
        return isFloat() || isDouble();
    }

    /**
     * Tests if this is an illegal register.
     *
     * @return  whether or not this is an illegal register
     */
    public boolean isIllegal() {
        return number == ILLEGAL_REG_NUMBER;
    }

    /**
     * Tests if this register has a valid number.
     *
     * @return  whether or not this is a valid register
     */
    public boolean isValid() {
        return number != ILLEGAL_REG_NUMBER;
    }

    /**
     * Returns the number of the used general-purpose register.
     *
     * @return  number of the general-purpose register
     */
    public int reg() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isWord(), "invalid type");
        }
        return (number >>> TYPE_BITS) & REG_MASK;
    }

    /**
     * Returns the number of the register that stores the low word.
     *
     * @return  number of the general-purpose register
     */
    public int regLo() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isLong(), "invalid type");
        }
        return (number >>> TYPE_BITS) & REG_MASK;
    }

    /**
     * Returns the number of the register that stores the high word.
     *
     * @return  number of the general-purpose register
     */
    public int regHi() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isLong(), "invalid type");
        }
        return (number >>> REG2_SHIFT) & REG_MASK;
    }

    /**
     * Returns the number of the used single-precision floating-point register.
     *
     * @return  number of the floating-point register
     */
    public int floatReg() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isFloat(), "invalid type");
        }
        return (number >>> TYPE_BITS) & REG_MASK;
    }

    /**
     * Returns the number of the used double-precision floating-point register.
     *
     * @return  number of the floating-point register
     */
    public int doubleReg() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isDouble(), "invalid type");
        }
        return (number >>> TYPE_BITS) & REG_MASK;
    }

    /**
     * Returns the number of the used floating-point register.
     *
     * @return  number of the floating-point register
     */
    public int fpu() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isFloat() || isDouble(), "invalid type");
        }
        return (number >>> TYPE_BITS) & REG_MASK;
    }

    /**
     * Returns the underlying general-purpose register.
     *
     * @return  the general-purpose register
     */
    public Register getRegister() {
        return FrameMap.rnr2reg(reg());
    }

    /**
     * Returns the general-purpose register that stores the low word.
     *
     * @return  the general-purpose register
     */
    public Register getRegisterLo() {
        return FrameMap.rnr2reg(regLo());
    }

    /**
     * Returns the general-purpose register that stores the high word.
     *
     * @return  the general-purpose register
     */
    public Register getRegisterHi() {
        return FrameMap.rnr2reg(regHi());
    }

    /**
     * Sets the number of this register to the specified word register.
     *
     * @param  rnr  number of the word register
     */
    public void setWordReg(int rnr) {
        number = (rnr << TYPE_BITS) | WORD_REG_TYPE;
    }

    /**
     * Sets the number of this register to the specified pair of registers.
     *
     * @param  lo  number of the register for the low word
     * @param  hi  number of the register for the high word
     */
    public void setLongReg(int lo, int hi) {
        number = (hi << REG2_SHIFT) | (lo << TYPE_BITS) | LONG_REG_TYPE;
    }

    /**
     * Sets the number of this register to the specified single-precision
     * floating-point register.
     *
     * @param  rnr  number of the floating-point register
     */
    public void setFloatReg(int rnr) {
        number = (rnr << TYPE_BITS) | FLOAT_REG_TYPE;
    }

    /**
     * Sets the number of this register to the specified double-precision
     * floating-point register.
     *
     * @param  reg  number of the floating-point register
     */
    public void setDoubleReg(int rnr) {
        number = (rnr << TYPE_BITS) | DOUBLE_REG_TYPE;
    }

    /**
     * Sets the number of this register to an illegal value.
     */
    public void setNoRInfo() {
        number = ILLEGAL_REG_NUMBER;
    }

    /**
     * Tests if this register and the specified register overlap.
     *
     * @param   other  the other register
     * @return  whether or not the registers overlap
     */
    public boolean overlaps(RInfo other) {
        if (isWord()) {
            if (other.isWord()) {
                return reg() == other.reg();
            } else if (other.isLong()) {
                return (reg() == other.regLo()) || (reg() == other.regHi());
            } else {
                return false;
            }
        } else if (isLong()) {
            if (other.isWord()) {
                return (regLo() == other.reg()) || (regHi() == other.reg());
            } else if (other.isLong()) {
                return (regLo() == other.regLo()) || (regLo() == other.regHi())
                    || (regHi() == other.regLo()) || (regHi() == other.regHi());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Compares this register with the specified object for equality.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the registers are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof RInfo) && (((RInfo) obj).getNumber() == number);
    }
}
