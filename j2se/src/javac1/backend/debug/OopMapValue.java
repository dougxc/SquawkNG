/*
 * @(#)OopMapValue.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.debug;

import javac1.Assert;
import javac1.backend.code.FrameMap;

/**
 * Represents a single entry in a frame map. A frame map describes for a
 * specific code position what each register and frame stack slot contains.
 *
 * @see      OopMap
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class OopMapValue {
    /**
     * The constant for unused locations.
     */
    public static final int UNUSED_VALUE = 0;

    /**
     * The constant for locations that contain object pointers.
     */
    public static final int OOP_VALUE = 1;

    /**
     * The constant for locations that contain live integer values.
     */
    public static final int VALUE_VALUE = 2;

    /**
     * The constant for dead locations that can be zapped for debugging.
     */
    public static final int DEAD_VALUE = 4;

    /**
     * The constant for callee saved locations.
     */
    public static final int CALLEE_SAVED_VALUE = 8;

    /**
     * The constant for locations that contain derived object pointers.
     */
    public static final int DERIVED_OOP_VALUE = 16;

    /**
     * The number of bits that indicate the content type of the location.
     */
    private static final int TYPE_BITS = 5;

    /**
     * The number of bits that encode the register number.
     */
    private static final int REGISTER_BITS = 11;

    /**
     * The bit mask that selects the content type of the location.
     */
    private static final int TYPE_MASK = (1 << TYPE_BITS) - 1;

    /**
     * The bit mask that selects the register number.
     */
    private static final int REGISTER_MASK = (1 << REGISTER_BITS) - 1;

    /**
     * The value that encodes information about this location.
     */
    private int value;

    /**
     * The number of the content register.
     */
    public int contentReg;

    /**
     * Constructs a new entry for a frame map.
     */
    public OopMapValue() {
        value = 0;
        contentReg = 0;
    }

    /**
     * Constructs a new entry for a frame map.
     *
     * @param  reg   number of the register
     * @param  type  the content type
     */
    public OopMapValue(int reg, int type) {
        this();
        setReg(reg);
        setType(type);
    }

    /**
     * Sets the content type of this location.
     *
     * @param  type  the content type
     */
    public void setType(int type) {
        value = (value & ~TYPE_MASK) | type;
    }

    /**
     * Returns the content type of this location.
     *
     * @return  the content type
     */
    public int getType() {
        return value & TYPE_MASK;
    }

    /**
     * Sets the register number of this location.
     *
     * @param  reg  number of the register
     */
    public void setReg(int reg) {
        value = (value & TYPE_MASK) | (reg << TYPE_BITS);
    }

    /**
     * Returns the register number of this location.
     *
     * @return  number of the register
     */
    public int getReg() {
        return (value >>> TYPE_BITS) & REGISTER_MASK;
    }

    /**
     * Sets the number of the content register.
     *
     * @param  contentReg  number of the content register
     */
    public void setContentReg(int contentReg) {
        this.contentReg = contentReg;
    }

    /**
     * Returns the number of the content register.
     *
     * @return  number of the content register
     */
    public int getContentReg() {
        return contentReg;
    }

    /**
     * Tests if this location is a physical register.
     *
     * @return  whether or not this is a physical register
     */
    public boolean isRegisterLoc() {
        return getReg() < FrameMap.STACK_0;
    }

    /**
     * Tests if this location is a stack slot.
     *
     * @return  whether or not this is a stack slot
     */
    public boolean isStackLoc() {
        return getReg() >= FrameMap.STACK_0;
    }

    /**
     * Returns the stack pointer offset of this location.
     *
     * @return  stack pointer offset
     */
    public int getStackOffset() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isStackLoc(), "must be stack location");
        }
        return getReg() - FrameMap.STACK_0;
    }
}
