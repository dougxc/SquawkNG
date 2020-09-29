/*
 * @(#)Address.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;

/**
 * This class represents an address. It is an abstraction used to represent a
 * memory location using any of the x86 addressing modes with one object. A
 * register location is represented via {@link Register}, not via this class
 * for efficiency and simplicity reasons.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Address {
    /**
     * A scaling factor constant.
     */
    public static final int
            NO_SCALE = -1, TIMES_1  =  0, TIMES_2  =  1, TIMES_4  =  2,
            TIMES_8  =  3;

    /**
     * The base register.
     */
    private Register base;

    /**
     * The index register.
     */
    private Register index;

    /**
     * The scaling factor.
     */
    private int scale;

    /**
     * The displacement.
     */
    private int disp;

    /**
     * The relocation type.
     */
    private int rtype;

    /**
     * Constructs an address with the value 0.
     */
    public Address() {
        this(Register.NO_REG, Register.NO_REG, NO_SCALE, 0);
    }

    /**
     * Constructs an address of the form <b>disp</b>.
     *
     * @param  disp   the displacement
     * @param  rtype  the relocation type
     */
    public Address(int disp, int rtype) {
        this(Register.NO_REG, Register.NO_REG, NO_SCALE, disp);
        this.rtype = rtype;
    }

    /**
     * Constructs an address of the form <b>[base][index*scale]</b>.
     *
     * @param  base   the base register
     * @param  index  the index register
     * @param  scale  the scaling factor
     */
    public Address(Register base, Register index, int scale) {
        this(base, index, scale, 0);
    }

    /**
     * Constructs an address of the form <b>disp[base]</b>.
     *
     * @param  base  the base register
     * @param  disp  the displacement
     */
    public Address(Register base, int disp) {
        this(base, Register.NO_REG, NO_SCALE, disp);
    }

    /**
     * Constructs an address of the form <b>[base]</b>.
     *
     * @param  base  the base register
     */
    public Address(Register base) {
        this(base, Register.NO_REG, NO_SCALE, 0);
    }

    /**
     * Constructs an address of the form <b>disp[base][index*scale]</b>.
     *
     * @param  base   the base register
     * @param  index  the index register
     * @param  scale  the scaling factor
     * @param  disp   the displacement
     */
    public Address(Register base, Register index, int scale, int disp) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(index.equals(Register.NO_REG) == (scale == NO_SCALE), "inconsistent address");
        }
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.disp = disp;
        this.rtype = RelocInfo.NONE;
    }

    /**
     * Returns the base register.
     *
     * @return  the base regsiter
     */
    public Register getBase() {
        return base;
    }

    /**
     * Returns the index register.
     *
     * @return  the index register
     */
    public Register getIndex() {
        return index;
    }

    /**
     * Returns the scaling factor.
     *
     * @return  the scaling factor
     */
    public int getScale() {
        return scale;
    }

    /**
     * Returns the displacement.
     *
     * @return  the displacement
     */
    public int getDisp() {
        return disp;
    }

    /**
     * Returns the relocation type.
     *
     * @return  the relocation type
     * @see     RelocInfo
     */
    public int getRelocType() {
        return rtype;
    }

    /**
     * Tests if this address uses the specified register.
     *
     * @param   reg  register to look for
     * @return  whether or not this address uses the register
     */
    public boolean uses(Register reg) {
        return base.equals(reg) || index.equals(reg);
    }
}
