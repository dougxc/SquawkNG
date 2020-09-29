/*
 * @(#)FrameMap.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;
import javac1.backend.debug.OopMap;
import javac1.backend.reg.CachedLocals;
import javac1.backend.reg.RegMask;
import javac1.backend.reg.RInfo;

/**
 * Maps local variables, monitors, and spill slots to their frame location. On
 * Intel processors the frame for standard methods looks as follows:<p>
 *
 * <table align="center" border="1" style="font-family:sans-serif;font-size:10pt"><tr>
 * <td nowrap>&nbsp;arguments&nbsp;</td>
 * <td nowrap>&nbsp;return address&nbsp;</td>
 * <td nowrap>&nbsp;old ebp&nbsp;</td>
 * <td nowrap>&nbsp;local variables&nbsp;</td>
 * <td nowrap>&nbsp;monitors&nbsp;</td>
 * <td nowrap>&nbsp;spill area&nbsp;</td>
 * </tr></table>
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class FrameMap {
    /**
     * The array of general-purpose registers.
     */
    private static final Register[] CPU_REGS = new Register[] {
            Register.ESI, Register.EDI, Register.EBX,
            Register.EAX, Register.EDX, Register.ECX};

    /**
     * The set of byte registers.
     */
    public static final RegMask BYTE_REG_MASK = new RegMask();

    /**
     * The constant used to indicate that the desired register does not exist.
     */
    public static final RInfo NO_RINFO = RInfo.NO_RINFO;

    /**
     * The ESI register information constant.
     */
    public static final RInfo ESI_RINFO = RInfo.wordReg(0);

    /**
     * The EDI register information constant.
     */
    public static final RInfo EDI_RINFO = RInfo.wordReg(1);

    /**
     * The EBX register information constant.
     */
    public static final RInfo EBX_RINFO = RInfo.wordReg(2);

    /**
     * The EAX register information constant.
     */
    public static final RInfo EAX_RINFO = RInfo.wordReg(3);

    /**
     * The EDX register information constant.
     */
    public static final RInfo EDX_RINFO = RInfo.wordReg(4);

    /**
     * The ECX register information constant.
     */
    public static final RInfo ECX_RINFO = RInfo.wordReg(5);

    /**
     * The EDX:EAX register information constant.
     */
    public static final RInfo EAX_EDX_RINFO = new RInfo();

    /**
     * The constant for the first single-precision floating-pointer register.
     */
    public static final RInfo F0_RINFO = new RInfo();

    /**
     * The constant for the first double-precision floating-pointer register.
     */
    public static final RInfo D0_RINFO = new RInfo();

    /**
     * The first register number that refers to a stack slot.
     */
    public static final int STACK_0 = 32;

    /**
     * Completes initialization of the constants.
     */
    static {
        EAX_EDX_RINFO.setLongReg(3, 4);
        F0_RINFO.setFloatReg(0);
        D0_RINFO.setDoubleReg(0);
        BYTE_REG_MASK.addReg(EAX_RINFO);
        BYTE_REG_MASK.addReg(ECX_RINFO);
        BYTE_REG_MASK.addReg(EDX_RINFO);
        BYTE_REG_MASK.addReg(EBX_RINFO);
    }

    /**
     * The total size of local variables in the frame.
     */
    private int sizeLocals;

    /**
     * The total size of monitors in the frame.
     */
    private int sizeMonitors;

    /**
     * The total size of arguments in the frame.
     */
    private int sizeArguments;

    /**
     * The total size of spill slots in the frame.
     */
    private int sizeSpills;

    /**
     * The fixed frame size.
     */
    private int fixedFrameSize;

    /**
     * The maximum size of the expression stack in the frame.
     */
    private int maxStackSize;

    /**
     * The list of local variables that are cached in registers.
     */
    private CachedLocals cachedLocals;

    /**
     * Constructs a new frame map.
     *
     * @param  sizeSpills    size of the spill area
     * @param  cachedLocals  list of cached local variables
     */
    public FrameMap(int sizeSpills, CachedLocals cachedLocals) {
        this.sizeLocals = 0;
        this.sizeMonitors = 0;
        this.sizeArguments = 0;
        this.sizeSpills = sizeSpills;
        this.fixedFrameSize = -1;
        this.maxStackSize = 0;
        this.cachedLocals = cachedLocals;
    }

    /**
     * Returns the general-purpose register with the specified number.
     *
     * @param   rnr  number of the desired register
     * @return  the register with the specified number
     */
    public static Register rnr2reg(int rnr) {
        return CPU_REGS[rnr];
    }

    /**
     * Returns the number of the specified general-purpose register.
     *
     * @param   reg  register to look for
     * @return  the number of the specified register
     */
    public static int reg2rnr(Register reg) {
        for (int i = 0; i < CPU_REGS.length; i++) {
            if (reg.equals(CPU_REGS[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts the specified stack slot index to a register name.
     *
     * @param   index  stack slot index
     * @return  corresponding register name
     */
    public static int stack2reg(int index) {
        return STACK_0 + index;
    }

    /**
     * Converts the specified register name to a stack slot index.
     *
     * @param   reg  register name
     * @return  corresponding stack slot index
     */
    public static int reg2stack(int reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg >= STACK_0, "not a stack-based register");
        }
        return reg - STACK_0;
    }

    /**
     * Returns the first general-purpose register.
     *
     * @return  the first register
     */
    public static Register firstRegister() {
        return CPU_REGS[0];
    }

    /**
     * Returns the first byte register.
     *
     * @return  the first byte register
     */
    public static Register firstByteRegister() {
        return CPU_REGS[2];
    }

    /**
     * Tests if the specified register information denotes a byte register.
     *
     * @param   reg  register information object
     * @return  whether or not the register is a byte register
     */
    public static boolean isByteRInfo(RInfo reg) {
        return reg.isWord() && CPU_REGS[reg.reg()].hasByteRegister();
    }

    /**
     * Returns the total size of the current stack frame.
     *
     * @param   espOffset  offset of the stack pointer
     * @return  the total frame size
     */
    public int getFrameSize(int espOffset) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(fixedFrameSize == sizeLocals + sizeSpills + sizeMonitors + 2 - sizeArguments, "frame size not set properly");
        }
        return fixedFrameSize + espOffset;
    }

    /**
     * Maps the specified Java index to a stack offset. The offset relates to
     * the EBP for standard methods and to the ESP for simple leaf methods.
     *
     * @param   javaIndex  index into the array of arguments and local variables
     * @return  stack offset for the specified variable
     */
    private int realIndex(int javaIndex) {
        return sizeArguments - javaIndex + (javaIndex < sizeArguments ? 1 : -1);
    }

    /**
     * Tests if the real indices of the specified Java indices are adjacent.
     *
     * @param   jix1  first Java index
     * @param   jix2  second Java index
     * @return  whether or not the real indices are adjacent
     */
    public boolean areAdjacentIndices(int jix1, int jix2) {
        return Math.abs(realIndex(jix1) - realIndex(jix2)) <= 1;
    }

    /**
     * Returns the address of the frame slot at the specified index.
     *
     * @param   slotIndex  frame slot index
     * @return  address of the specified slot
     */
    private Address frameSlotAddress(int slotIndex) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(slotIndex >= 0, "wrong slot index");
        }
        return new Address(Register.EBP, realIndex(slotIndex) * 4);
    }

    /**
     * Returns the address of the local variable at the specified index.
     *
     * @param   localIndex  index into the array of local variables
     * @param   isTwoWord   whether the variable occupies two words
     * @param   forHiWord   whether the high word address should be returned
     * @return  address of the local variable
     */
    public Address localAddress(int localIndex, boolean isTwoWord,
            boolean forHiWord) {
        if (isTwoWord && !forHiWord) {
            return frameSlotAddress(localIndex + 1);
        } else {
            return frameSlotAddress(localIndex);
        }
    }

    /**
     * Returns the address of the local variable at the specified index.
     *
     * @see  #localAddress(int, boolean, boolean)
     */
    public Address localAddress(int localIndex, boolean isTwoWord) {
        return localAddress(localIndex, isTwoWord, false);
    }

    /**
     * Returns the address of the monitor with the specified index.
     *
     * @param   monitorIndex  index of the monitor
     * @return  address of the monitor
     */
    public Address monitorAddress(int monitorIndex) {
       return frameSlotAddress(monitorIndex + sizeLocals);
    }

    /**
     * Returns the address of the spill slot at the specified index.
     *
     * @param   spillIndex  index of the spill slot
     * @param   isTwoWord   whether the spill slot occupies two words
     * @param   forHiWord   whether the high word address should be returned
     * @return  address of the spill slot
     */
    public Address spillAddress(int spillIndex, boolean isTwoWord,
            boolean forHiWord) {
        int index = sizeLocals + sizeMonitors + spillIndex;
        if (isTwoWord && !forHiWord) {
            return frameSlotAddress(index + 1);
        } else {
            return frameSlotAddress(index);
        }
    }

    /**
     * Returns the register name for the specified index.
     *
     * @param   inIndex    index of a variable or spill slot
     * @param   espOffset  offset of the stack pointer
     * @return  corresponding register name
     */
    private int regnameFor(int inIndex, int espOffset) {
        int index = -1;
        if (inIndex < sizeArguments) {
            index = sizeArguments - inIndex - 1;
            if (Assert.ASSERTS_ENABLED) {
                Assert.that((index >= 0) && (index < sizeArguments), "wrong index");
            }
        } else {
            index = 2 * sizeArguments - 3 + getFrameSize(espOffset) - inIndex;
            if (Assert.ASSERTS_ENABLED) {
                Assert.that((index >= sizeArguments + espOffset) && (index < getFrameSize(espOffset) + sizeArguments), "wrong index");
            }
        }
        return stack2reg(index);
    }

    /**
     * Returns the register name of the specified local variable.
     *
     * @param   localIndex  index into the array of local variables
     * @param   espOffset   offset of the stack pointer
     * @return  register name of the specified variable
     */
    public int localRegname(int localIndex, int espOffset) {
        return regnameFor(localIndex, espOffset);
    }

    /**
     * Returns the register name of the specified spill slot index.
     *
     * @param   spillIndex  index of the spill slot
     * @param   espOffset   offset of the stack pointer
     * @return  register name of the specified spill slot
     */
    public int spillRegname(int spillIndex, int espOffset) {
        int index = sizeLocals + sizeMonitors + spillIndex;
        return regnameFor(index, espOffset);
    }

    /**
     * Returns the register name of the specified physical register.
     *
     * @param   reg        the physical register
     * @param   espOffset  offset of the stack pointer
     * @return  register name of the specified physical register
     */
    public int  registerRegname(RInfo reg, int espOffset) {
        return reg.getRegister().getNumber();
    }

    /**
     * Constructs a new map of ordinary object pointers.
     *
     * @param   espOffset  offset of the stack pointer
     * @return  newly created pointer map
     */
    public OopMap newOopMap(int espOffset) {
        return new OopMap(getFrameSize(espOffset), sizeArguments);
    }

    /**
     * Sets the total size of local variables in the frame.
     *
     * @param  sizeLocal  size of the local variables
     */
    public void setSizeLocals(int sizeLocals) {
        this.sizeLocals = sizeLocals;
    }

    /**
     * Returns the total size of local variables in the frame.
     *
     * @return  size of the local variables
     */
    public int getSizeLocals() {
        return sizeLocals;
    }

    /**
     * Sets the total size of arguments in the frame.
     *
     * @param  sizeArguments  size of the arguments
     */
    public void setSizeArguments(int sizeArguments) {
        this.sizeArguments = sizeArguments;
    }

    /**
     * Returns the total size of arguments in the frame.
     *
     * @return  size of the arguments
     */
    public int getSizeArguments() {
        return sizeArguments;
    }

    /**
     * Sets the total size of monitors in the frame.
     *
     * @param  sizeMonitors  size of the monitors
     */
    public void setSizeMonitors(int sizeMonitors) {
        this.sizeMonitors = sizeMonitors;
    }

    /**
     * Returns the total size of monitors in the frame.
     *
     * @return  size of the monitors
     */
    public int getSizeMonitors() {
        return sizeMonitors;
    }

    /**
     * Returns the total size of spill slots in the frame.
     *
     * @return  size of the spill slots
     */
    public int getSizeSpills() {
        return sizeSpills;
    }

    /**
     * Sets the maximum size of the expression stack in the frame.
     *
     * @param  maxStackSize  maximum stack size
     */
    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    /**
     * Returns the maximum size of the expression stack in the frame.
     *
     * @return  maximum stack size
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * Returns the list of local variables that are cached in registers.
     *
     * @return  list of cached local variables
     */
    public CachedLocals getCachedLocals() {
        return cachedLocals;
    }

    /**
     * Fixes the current frame size.
     */
    public void setFixedFrameSize() {
        fixedFrameSize = sizeLocals + sizeSpills + sizeMonitors + 2 - sizeArguments;
    }

    /**
     * Specifies the ESP decrement needed to build the frame.
     *
     * @return  size of stack decrement in words
     */
    public int sizeOfStackDecrement() {
        int size = sizeLocals - sizeArguments + sizeMonitors;
        return (size >= 0) ? size : 0;
    }
}
