/*
 * @(#)RegAlloc.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.JavaC1;
import javac1.backend.items.Item;
import javac1.ir.instr.Instruction;
import javac1.ir.types.ArrayType;
import javac1.ir.types.DoubleType;
import javac1.ir.types.FloatType;
import javac1.ir.types.LongType;
import javac1.ir.types.ObjectType;
import javac1.ir.types.ValueType;

/**
 * This class is used to handle register allocation and spilling for the
 * compiler. It maintains allocated registers, the reference counts of registers
 * and the root instructions that have locked the registers. If a spill lock
 * counter is greater than zero then the corresponding register is spill-locked
 * and hence must not be spilled.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class RegAlloc {
    /**
     * The number of general-purpose registers.
     */
    public static final int NUM_CPU_REGS = 6;

    /**
     * The number of floating-point registers.
     */
    public static final int NUM_FPU_REGS = 6;

    /**
     * The number of registers.
     */
    public static final int NUM_REGS = NUM_CPU_REGS;

    /**
     * Two to the power of the number of registers.
     */
    public static final int NUM_REGS_EXP2 = 1 << NUM_REGS;

    /**
     * The reference counts of the general-purpose registers.
     */
    private int[] refCountCPU;

    /**
     * The reference counts of the floating-point registers.
     */
    private int[] refCountFPU;

    /**
     * The spill lock counter for the general-purpose registers.
     */
    private int[] spillLockCPU;

    /**
     * The spill lock counter for the floating-point registers.
     */
    private int[] spillLockFPU;

    /**
     * The instructions that have locked a general-purpose register.
     */
    private Instruction[] valueCPU;

    /**
     * The instructions that have locked a floating-point register.
     */
    private Instruction[] valueFPU;

    /**
     * The allocation table for the general-purpose registers.
     */
    private AllocTable allocTableCPU;

    /**
     * The allocation table for the floating-point registers.
     */
    private AllocTable allocTableFPU;

    /**
     * The set of spilled elements.
     */
    private List spillArea;

    /**
     * Whether or not the floating-point precision is 32 bit.
     */
    private boolean is32bitPrecision;

    /**
     * Whether or not locking is allowed.
     */
    private boolean lockingLocked;

    /**
     * Constructs a new register allocator.
     */
    public RegAlloc() {
        refCountCPU = new int[NUM_CPU_REGS];
        refCountFPU = new int[NUM_FPU_REGS];
        spillLockCPU = new int[NUM_CPU_REGS];
        spillLockFPU = new int[NUM_FPU_REGS];
        valueCPU = new Instruction[NUM_CPU_REGS];
        valueFPU = new Instruction[NUM_FPU_REGS];
        allocTableCPU = new AllocTable();
        allocTableFPU = new AllocTable();
        spillArea = new ArrayList();
        is32bitPrecision = false;
        lockingLocked = false;
    }

    /**
     * Locks the general-purpose register with the specified number.
     *
     * @param  rnr    number of the register to be locked
     * @param  instr  instruction that wants to lock the register
     * @param  rc     reference count
     */
    private void setLockedCPU(int rnr, Instruction instr, int rc) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!lockingLocked, "cannot lock");
            Assert.that(rc > 0, "wrong reference count");
        }
        allocTableCPU.setLocked(rnr);
        refCountCPU[rnr] = rc;
        valueCPU[rnr] = instr;
    }

    /**
     * Locks the floating-point register with the specified number.
     *
     * @param  rnr    number of the register to be locked
     * @param  instr  instruction that wants to lock the register
     * @param  rc     reference count
     */
    private void setLockedFPU(int rnr, Instruction instr, int rc) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!lockingLocked, "cannot lock");
            Assert.that(rc > 0, "wrong reference count");
        }
        allocTableFPU.setLocked(rnr);
        refCountFPU[rnr] = rc;
        valueFPU[rnr] = instr;
    }

    /**
     * Locks the specified register. Instructions whose use count equals zero
     * still allocate registers, as they are released later.
     *
     * @param  reg    register to be locked
     * @param  instr  instruction that wants to lock the register
     * @param  rc     reference count
     */
    public void lockReg(RInfo reg, Instruction instr, int rc) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isFreeReg(reg), "register already locked");
        }
        if (rc == 0) {
            rc = 1;
        }
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            setLockedCPU(reg.reg(), instr, rc);
            break;
        case RInfo.LONG_REG_TYPE:
            setLockedCPU(reg.regLo(), instr, rc);
            setLockedCPU(reg.regHi(), instr, rc);
            break;
        case RInfo.FLOAT_REG_TYPE:
            setLockedFPU(reg.floatReg(), instr, rc);
            break;
        case RInfo.DOUBLE_REG_TYPE:
            setLockedFPU(reg.doubleReg(), instr, rc);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Releases the general-purpose register with the specified number.
     *
     * @param  rnr  number of the register to be released
     */
    private void setFreeCPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(refCountCPU[rnr] != 0, "wrong reference count");
        }
        refCountCPU[rnr]--;
        if (refCountCPU[rnr] == 0) {
            allocTableCPU.setFree(rnr);
            valueCPU[rnr] = null;
        }
    }

    /**
     * Releases the floating-point register with the specified number.
     *
     * @param  rnr  number of the register to be released
     */
    private void setFreeFPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(refCountFPU[rnr] != 0, "wrong reference count");
        }
        refCountFPU[rnr]--;
        if (refCountFPU[rnr] == 0) {
            allocTableFPU.setFree(rnr);
            valueFPU[rnr] = null;
        }
    }

    /**
     * Releases the specified register.
     *
     * @param  reg  register to be released
     */
    public void freeReg(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            setFreeCPU(reg.reg());
            break;
        case RInfo.LONG_REG_TYPE:
            setFreeCPU(reg.regLo());
            setFreeCPU(reg.regHi());
            break;
        case RInfo.FLOAT_REG_TYPE:
            setFreeFPU(reg.floatReg());
            break;
        case RInfo.DOUBLE_REG_TYPE:
            setFreeFPU(reg.doubleReg());
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Sets the reference count and the value for a locked general-purpose
     * register.
     *
     * @param  rnr    number of the register to be modified
     * @param  rc     new reference count
     * @param  value  new value of the register
     */
    private void setRegCPU(int rnr, int rc, Instruction value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeCPU(rnr), "register is not locked");
            Assert.that(rc > 0, "wrong reference count");
        }
        refCountCPU[rnr] = rc;
        valueCPU[rnr] = value;
    }

    /**
     * Sets the reference count and the value for a locked floating-point
     * register.
     *
     * @param  rnr    number of the register to be modified
     * @param  rc     new reference count
     * @param  value  new value of the register
     */
    private void setRegFPU(int rnr, int rc, Instruction value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeFPU(rnr), "register is not locked");
            Assert.that(rc > 0, "wrong reference count");
        }
        refCountFPU[rnr] = rc;
        valueFPU[rnr] = value;
    }

    /**
     * Sets the reference count and the value for a locked register.
     *
     * @param  reg    register to be modified
     * @param  rc     new reference count
     * @param  value  new value of the register
     */
    public void setReg(RInfo reg, int rc, Instruction value) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            setRegCPU(reg.reg(), rc, value);
            break;
        case RInfo.LONG_REG_TYPE:
            setRegCPU(reg.regLo(), rc, value);
            setRegCPU(reg.regHi(), rc, value);
            break;
        case RInfo.FLOAT_REG_TYPE:
            setRegFPU(reg.floatReg(), rc, value);
            break;
        case RInfo.DOUBLE_REG_TYPE:
            setRegFPU(reg.doubleReg(), rc, value);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Tests if the general-purpose register with the specified number is free.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register is free
     */
    private boolean isFreeCPU(int rnr) {
        return allocTableCPU.isFree(rnr);
    }

    /**
     * Tests if the floating-point register with the specified number is free.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register is free
     */
    private boolean isFreeFPU(int rnr) {
        return allocTableFPU.isFree(rnr);
    }

    /**
     * Tests if the specified register is free.
     *
     * @param   reg  register to be examined
     * @return  whether or not the register is free
     */
    public boolean isFreeReg(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            return isFreeCPU(reg.reg());
        case RInfo.LONG_REG_TYPE:
            return isFreeCPU(reg.regLo()) && isFreeCPU(reg.regHi());
        case RInfo.FLOAT_REG_TYPE:
            return isFreeFPU(reg.floatReg());
        case RInfo.DOUBLE_REG_TYPE:
            return isFreeFPU(reg.doubleReg());
        default:
            Assert.shouldNotReachHere();
            return false;
        }
    }

    /**
     * Returns the reference count of the general-purpose register with the
     * specified number.
     *
     * @param   rnr  number of the register to be examined
     * @return  the reference count
     */
    private int getRefCountCPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeCPU(rnr), "register is not locked");
        }
        return refCountCPU[rnr];
    }

    /**
     * Returns the reference count of the floating-point register with the
     * specified number.
     *
     * @param   rnr  number of the register to be examined
     * @return  the reference count
     */
    private int getRefCountFPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeFPU(rnr), "register is not locked");
        }
        return refCountFPU[rnr];
    }

    /**
     * Returns the reference count of the specified register.
     *
     * @param   reg  register to be examined
     * @return  the reference count
     */
    public int getRefCountReg(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            return getRefCountCPU(reg.reg());
        case RInfo.LONG_REG_TYPE:
            return getRefCountCPU(reg.regLo());
        case RInfo.FLOAT_REG_TYPE:
            return getRefCountFPU(reg.floatReg());
        case RInfo.DOUBLE_REG_TYPE:
            return getRefCountFPU(reg.doubleReg());
        default:
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Returns the value of the general-purpose register with the specified
     * number.
     *
     * @param   rnr  number of the register to be examined
     * @return  the value of the register
     */
    private Instruction getValueCPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeCPU(rnr), "register is not locked");
        }
        return valueCPU[rnr];
    }

    /**
     * Returns the value of the floating-point register with the specified
     * number.
     *
     * @param   rnr  number of the register to be examined
     * @return  the value of the register
     */
    private Instruction getValueFPU(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isFreeFPU(rnr), "register is not locked");
        }
        return valueFPU[rnr];
    }

    /**
     * Returns the value of the specified register.
     *
     * @param   reg  register to be examined
     * @return  the value of the register
     */
    public Instruction getValueReg(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            return isFreeCPU(reg.reg()) ? null : getValueCPU(reg.reg());
        case RInfo.LONG_REG_TYPE:
            if (!isFreeCPU(reg.regLo())) {
                return getValueCPU(reg.regLo());
            } else if (!isFreeCPU(reg.regHi())) {
                return getValueCPU(reg.regHi());
            } else {
                return null;
            }
        case RInfo.FLOAT_REG_TYPE:
            return isFreeFPU(reg.floatReg()) ? null : getValueFPU(reg.floatReg());
        case RInfo.DOUBLE_REG_TYPE:
            return isFreeFPU(reg.doubleReg()) ? null : getValueFPU(reg.doubleReg());
        default:
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Tests if any register for the specified type is free.
     *
     * @param   type  type of the register to look for
     * @return  whether or not an appopriate register is free
     */
    public boolean hasFreeReg(ValueType type) {
        if ((type instanceof FloatType) || (type instanceof DoubleType)) {
            return allocTableFPU.hasOneFree();
        } else if (type instanceof LongType) {
            return allocTableCPU.hasTwoFree();
        } else {
            return allocTableCPU.hasOneFree();
        }
    }

    /**
     * Tests if at least one register in the specified register set is free.
     *
     * @param   mask  set of registers to be examined
     * @return  whether or not one of the registers is free
     */
    public boolean hasFreeReg(RegMask mask) {
        return allocTableCPU.hasOneFreeMasked(mask);
    }

    /**
     * Returns a free register for the specified type.
     *
     * @param   type  type of the register to look for
     * @return  free register for the specified type
     */
    public RInfo getFreeReg(ValueType type) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(hasFreeReg(type), "no free register");
        }
        RInfo reg = new RInfo();
        switch (type.getTag()) {
        case ValueType.intTag:
            /* falls through */
        case ValueType.objectTag:
            /* falls through */
        case ValueType.addressTag:
            reg.setWordReg(allocTableCPU.getFree());
            break;
        case ValueType.longTag:
            int lo = allocTableCPU.getFree();
            setLockedCPU(lo, null, 1);
            int hi = allocTableCPU.getFree();
            setFreeCPU(lo);
            reg.setLongReg(lo, hi);
            break;
        case ValueType.floatTag:
            reg.setFloatReg(allocTableFPU.getFree());
            break;
        case ValueType.doubleTag:
            reg.setDoubleReg(allocTableFPU.getFree());
            break;
        default:
            Assert.shouldNotReachHere();
        }
        return reg;
    }

    /**
     * Locks and returns a free register for the specified instruction and
     * type. If the instruction's use count equals zero then it must be a
     * pinned instruction with side effect.
     *
     * @param   instr  instruction to get register for
     * @param   type   requested type of the register
     * @return  locked register for the specified instruction
     */
    public RInfo getLockReg(Instruction instr, ValueType type) {
        int useCount;
        RInfo reg = getFreeReg(type);
        if ((instr != null) && (instr.getUseCount() > 0)) {
            useCount = instr.getUseCount();
        } else {
            useCount = 1;
        }
        switch (type.getTag()) {
        case ValueType.intTag:
            /* falls through */
        case ValueType.objectTag:
            /* falls through */
        case ValueType.addressTag:
            setLockedCPU(reg.reg(), instr, useCount);
            break;
        case ValueType.longTag:
            setLockedCPU(reg.regLo(), instr, useCount);
            setLockedCPU(reg.regHi(), instr, useCount);
            break;
        case ValueType.floatTag:
            setLockedFPU(reg.floatReg(), instr, useCount);
            break;
        case ValueType.doubleTag:
            setLockedFPU(reg.doubleReg(), instr, useCount);
            break;
        default:
            Assert.shouldNotReachHere();
        }
        return reg;
    }

    /**
     * Returns a free register from the specified register set.
     *
     * @param   mask  set of registers to be examined
     * @return  free register in the specified set
     */
    public RInfo getFreeRegFromMask(RegMask mask) {
        RInfo reg = new RInfo();
        int rnr = allocTableCPU.getFreeMasked(mask);
        reg.setWordReg(rnr);
        return reg;
    }

    /**
     * Tests if the specified register has already been used.
     *
     * @param   reg  register to be examined
     * @return  whether or not the register has been used
     */
    public boolean didUseRegister(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            return allocTableCPU.didUseRegister(reg.reg());
        default:
            Assert.shouldNotReachHere();
            return false;
        }
    }

    /**
     * Returns the set of general-purpose registers that have already been used.
     *
     * @return  set of used registers
     */
    public RegMask getUsedRegisters() {
        return allocTableCPU.usedRegisters();
    }

    /**
     * Returns the set of general-purpose registers that have not been used yet.
     *
     * @return  set of unused registers
     */
    public RegMask getFreeRegisters() {
        return allocTableCPU.freeRegisters();
    }

    /**
     * Tests if both all general-purpose and all floating-point registers are
     * free.
     *
     * @return  whether all registers are free
     */
    public boolean areAllRegsFree() {
        return allocTableCPU.areAllFree() && allocTableFPU.areAllFree();
    }

    /**
     * Returns the list of registers that contain ordinary object pointers.
     *
     * @return  list of registers that contain object pointers
     */
    public List oopsInRegisters() {
        List oops = new ArrayList();
        for (int i = 0; i < NUM_CPU_REGS; i++) {
            if (!isFreeCPU(i) && (valueCPU[i] != null)
                    && (valueCPU[i].getType() instanceof ObjectType)) {
                oops.add(RInfo.wordReg(i));
            }
        }
        return oops;
    }

    /**
     * Extends the spill area to the specified length.
     *
     * @param  len  required length
     */
    private void extendSpillArea(int len) {
        int delta = len - spillArea.size();
        for (int i = 0; i < delta; i++) {
            spillArea.add(new SpillElem());
        }
    }

    /**
     * Locks the spill slot at the specified index.
     *
     * @param  instr    instruction that wants to lock the spill slot
     * @param  spillIx  index of the spill slot to be locked
     * @param  rc       reference count
     */
    public void lockSpill(Instruction instr, int spillIx, int rc) {
        ValueType type = instr.getType();
        boolean isOop = (type instanceof ObjectType) || (type instanceof ArrayType);
        int len = spillIx + type.getSize();
        if (len > spillArea.size()) {
            extendSpillArea(len);
        }
        SpillElem spill = (SpillElem) spillArea.get(spillIx);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(spill.getRefCount() == 0, "slot has already been locked");
        }
        spill.set(instr, rc, isOop);
        if (type.isDoubleWord()) {
            spill = (SpillElem) spillArea.get(spillIx + 1);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(spill.getRefCount() == 0, "slot has already been locked");
            }
            spill.set(instr, rc, isOop);
        }
    }

    /**
     * Releases the spill slot at the specified index.
     *
     * @param  idx   index of the spill slot to be released
     * @param  type  type of the spill slot
     */
    public void freeSpill(int spillIx, ValueType type) {
        SpillElem spill = (SpillElem) spillArea.get(spillIx);
        spill.decRefCount();
        if (type.isDoubleWord()) {
            spill = (SpillElem) spillArea.get(spillIx + 1);
            spill.decRefCount();
        }
    }

    /**
     * Tests if the general-purpose register with the specified number is spill
     * locked.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register is spill locked
     */
    private boolean isSpillLockedCPU(int rnr) {
        return spillLockCPU[rnr] != 0;
    }

    /**
     * Tests if the floating-point register with the specified number is spill
     * locked.
     *
     * @param   rnr  number of the register to be examined
     * @return  whether or not the register is spill locked
     */
    private boolean isSpillLockedFPU(int rnr) {
        return spillLockFPU[rnr] != 0;
    }

    /**
     * Tests if the specified register is spill locked.
     *
     * @param   reg  register to be examined
     * @return  whether or not the register is spill locked
     */
    public boolean isSpillLocked(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            return isSpillLockedCPU(reg.reg());
        case RInfo.LONG_REG_TYPE:
            return isSpillLockedCPU(reg.regLo()) || isSpillLockedCPU(reg.regHi());
        case RInfo.FLOAT_REG_TYPE:
            return isSpillLockedFPU(reg.floatReg());
        case RInfo.DOUBLE_REG_TYPE:
            return isSpillLockedFPU(reg.doubleReg());
        default:
            Assert.shouldNotReachHere();
            return false;
        }
    }

    /**
     * Changes the spill count of a general-purpose register by the specified
     * value.
     *
     * @param  rnr    number of the register to be modified
     * @param  delta  value to change spill count by
     */
    private void changeSpillCountCPU(int rnr, int delta) {
        spillLockCPU[rnr] += delta;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(spillLockCPU[rnr] >= 0, "illegal spill count");
        }
    }

    /**
     * Changes the spill count of a floating-point register by the specified
     * value.
     *
     * @param  rnr    number of the register to be modified
     * @param  delta  value to change spill count by
     */
    private void changeSpillCountFPU(int rnr, int delta) {
        spillLockFPU[rnr] += delta;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(spillLockFPU[rnr] >= 0, "illegal spill count");
        }
    }

    /**
     * Increments the number of spill locks for the specified register.
     *
     * @param  reg  register to be modified
     */
    public void incrSpillLock(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            changeSpillCountCPU(reg.reg(), 1);
            break;
        case RInfo.LONG_REG_TYPE:
            changeSpillCountCPU(reg.regLo(), 1);
            changeSpillCountCPU(reg.regHi(), 1);
            break;
        case RInfo.FLOAT_REG_TYPE:
            changeSpillCountFPU(reg.floatReg(), 1);
            break;
        case RInfo.DOUBLE_REG_TYPE:
            changeSpillCountFPU(reg.doubleReg(), 1);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Decrements the number of spill locks for the specified register.
     *
     * @param  reg  register to be modified
     */
    public void decrSpillLock(RInfo reg) {
        switch (reg.getType()) {
        case RInfo.WORD_REG_TYPE:
            changeSpillCountCPU(reg.reg(), -1);
            break;
        case RInfo.LONG_REG_TYPE:
            changeSpillCountCPU(reg.regLo(), -1);
            changeSpillCountCPU(reg.regHi(), -1);
            break;
        case RInfo.FLOAT_REG_TYPE:
            changeSpillCountFPU(reg.floatReg(), -1);
            break;
        case RInfo.DOUBLE_REG_TYPE:
            changeSpillCountFPU(reg.doubleReg(), -1);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Tests if the spill slot at the specified index is free.
     *
     * @param  spillIx  index of the spill slot to be examined
     * @param  type     type of the spill slot
     */
    public boolean isFreeSpill(int spillIx, ValueType type) {
        return (spillIx >= spillArea.size())
            || (((SpillElem) spillArea.get(spillIx)).isFree()
                && (type.isSingleWord() || (spillIx + 1 >= spillArea.size())
                    || ((SpillElem) spillArea.get(spillIx + 1)).isFree()));
    }

    /**
     * Returns the reference count of the spill slot at the specified index.
     *
     * @param   spillIx  index of the spill slot to be examined
     * @return  the reference count
     */
    public int getRefCountAt(int spillIx) {
        return ((SpillElem) spillArea.get(spillIx)).getRefCount();
    }

    /**
     * Returns the value of the spill slot at the specified index.
     *
     * @param   spillIx  index of the spill slot to be examined
     * @return  the value of the spill slot
     */
    public Instruction getValueSpilledAt(int spillIx) {
        SpillElem spill = (SpillElem) spillArea.get(spillIx);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(spill.getRefCount() > 0, "this is a released spill");
            Assert.that(spill.getInstr() != null, "wrong type");
        }
        return spill.getInstr();
    }

    /**
     * Locks a free spill slot and returns the index of the slot.
     *
     * @param   instr  instruction that wants to lock the spill slot
     * @param   rc     reference count
     * @return  index of the locked spill slot
     */
    public int getLockSpill(Instruction instr, int rc) {
        ValueType type = instr.getType();
        boolean isOop = (type instanceof ObjectType) || (type instanceof ArrayType);
        int len = spillArea.size();
        for (int i = 0; i < len; i++) {
            SpillElem spill = (SpillElem) spillArea.get(i);
            if (spill.isFree()) {
                if (type.isDoubleWord()) {
                    if (i + 1 >= spillArea.size()) {
                        spillArea.add(new SpillElem());
                    }
                    SpillElem spill2 = (SpillElem) spillArea.get(i + 1);
                    if (spill2.isFree()) {
                        spill.set(instr, rc, false);
                        spill2.set(instr, rc, false);
                        return i;
                    }
                } else {
                    spill.set(instr, rc, isOop);
                    return i;
                }
            }
        }
        int res = spillArea.size();
        SpillElem spill = new SpillElem();
        spill.set(instr, rc, isOop);
        spillArea.add(spill);
        if (type.isDoubleWord()) {
            spill = new SpillElem();
            spill.set(instr, rc, isOop);
            spillArea.add(spill);
        }
        return res;
    }

    /**
     * Returns the value with the smallest bytecode index that has locked a
     * register in the specified set and can be spilled.
     *
     * @param   mask  set of registers to be examined
     * @return  spillable value with the smallest bytecode index
     */
    public Instruction getSmallestValueToSpill(RegMask mask) {
        Instruction smallest = null;
        for (int i = 0; i < NUM_CPU_REGS; i++) {
            if (mask.contains(i) && !isFreeCPU(i) && !isSpillLockedCPU(i)) {
                Instruction val = valueCPU[i];
                if (val != null) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!isSpillLocked(val.getItem().getRInfo()), "value is spill locked");
                    }
                    if ((smallest == null) || (smallest.getBci() > val.getBci())) {
                        smallest = val;
                    }
                }
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(smallest != null, "no spillable value found");
            Assert.that(smallest.getItem().isRegister(), "value is not a register");
        }
        return smallest;
    }

    /**
     * Returns the value with the smallest bytecode index that has locked a
     * register with the specified type and can be spilled.
     *
     * @param   type  type of the value to be spilled
     * @return  spillable value with the smallest bytecode index
     */
    public Instruction getSmallestValueToSpill(ValueType type) {
        boolean isFloat = type.isFloatKind();
        Instruction smallest = null;
        for (int i = 0; i < NUM_REGS; i++) {
            Instruction val = null;
            if (!isFloat && !isFreeCPU(i) && !isSpillLockedCPU(i)) {
                val = valueCPU[i];
            } else if (isFloat && !isFreeFPU(i) && !isSpillLockedFPU(i)) {
                val = valueFPU[i];
            }
            if ((val != null) && (val.getItem() != null)
                    && val.getItem().isRegister()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!isSpillLocked(val.getItem().getRInfo()), "value is spill locked");
                }
                if ((smallest == null) || (smallest.getBci() > val.getBci())) {
                    smallest = val;
                }
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(smallest != null, "no spillable value found");
            Assert.that(smallest.getItem().isRegister(), "value is not a register");
        }
        return smallest;
    }

    /**
     * Clears all spill locks and hereby allows the registers to be spilled.
     */
    public void clearSpillLocks() {
        for (int i = 0; i < NUM_CPU_REGS; i++) {
            spillLockCPU[i] = 0;
        }
        for (int i = 0; i < NUM_FPU_REGS; i++) {
            spillLockFPU[i] = 0;
        }
    }

    /**
     * Clears the reference count of the spill element at the specified index.
     *
     * @param  spillIx  index of the slot to be released completely
     * @param  type     type of the spill slot
     */
    public void freeCompleteSpill(int spillIx, ValueType type) {
        SpillElem spill = (SpillElem) spillArea.get(spillIx);
        spill.clearRefCount();
        if (type.isDoubleWord()) {
            spill = (SpillElem) spillArea.get(spillIx + 1);
            spill.clearRefCount();
        }
    }

    /**
     * Tests if none of the registers is spilled.
     *
     * @return  whether or not the spill area is empty
     */
    public boolean isNoneSpilled() {
        for (int i = 0; i < spillArea.size(); i++) {
            if (((SpillElem) spillArea.get(i)).getRefCount() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether or not all spill locks are free.
     *
     * @return  whether or not all spill locks are free
     */
    public boolean areAllSpillLocksFree() {
        for (int i = 0; i < NUM_CPU_REGS; i++) {
            if (spillLockCPU[i] > 0) {
                return false;
            }
        }
        for (int i = 0; i < NUM_FPU_REGS; i++) {
            if (spillLockFPU[i] > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of the indices of spill slots that contain ordinary object
     * pointers.
     *
     * @return  list of spill slots that contain object pointers
     */
    public List oopsInSpill() {
        if (!javac1.Flags.GenerateOopMaps || (spillArea.size() == 0)) {
            return null;
         }
         List oops = new ArrayList();
         for (int i = spillArea.size() - 1; i >= 0; i--) {
            SpillElem spill = (SpillElem) spillArea.get(i);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(spill != null, "no spill element");
            }
            if (!spill.isFree() && spill.isOop()) {
                oops.add(new Integer(i));
            }
         }
         return oops;
    }

    /**
     * Returns the index of a free spill slot that is greater or equal than the
     * specified value.
     *
     * @param   leastSpillIx  lower bound for spill slot index
     * @param   type          type of the spill slot
     * @return  index of free slot after the specified one
     */
    public int getFreeSpillAfter(int leastSpillIx, ValueType type) {
        if (leastSpillIx >= spillArea.size()) {
            extendSpillArea(leastSpillIx + 1);
            return leastSpillIx;
        }
        for (int i = leastSpillIx; i < spillArea.size(); i++) {
            SpillElem spill = (SpillElem) spillArea.get(i);
            if (spill.isFree()) {
                if (type.isDoubleWord() && (i + 1 < spillArea.size())) {
                    SpillElem spill2 = (SpillElem) spillArea.get(i + 1);
                    if (spill2.isFree()) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }
        int len = spillArea.size();
        extendSpillArea(len + 1);
        return len;
    }

    /**
     * Moves a spill element from one slot to another.
     *
     * @param  toSpillIx    index of the slot to move to
     * @param  fromSpillIx  index of the spill element to move
     * @param  type         type of the spill slot
     */
    public void moveSpill(int toSpillIx, int fromSpillIx, ValueType type) {
        Instruction val = ((SpillElem) spillArea.get(fromSpillIx)).getInstr();
        int spillIx = val.getItem().getSpillIx();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((spillIx == fromSpillIx) || (val.getType().isDoubleWord()
                && (spillIx == fromSpillIx - 1)), "spill indices do not match");
        }
        val.getItem().setSpillIx(toSpillIx);
        int len = toSpillIx + type.getSize();
        if (len > spillArea.size()) {
            extendSpillArea(len);
        }
        for (int i = 0; i < type.getSize(); i++) {
            SpillElem from = (SpillElem) spillArea.get(fromSpillIx + i);
            SpillElem to = (SpillElem) spillArea.get(toSpillIx + i);
            to.setUsing(from);
            from.clear();
        }
        if (javac1.Flags.PrintRegAlloc) {
            JavaC1.out.print("moving spill from " + fromSpillIx);
            JavaC1.out.println(" to " + toSpillIx);
        }
    }

    /**
     * Returns the maximum number of spill elements in the spill area.
     *
     * @return  maximum number of spill elements
     */
    public int getMaxSpills() {
        return spillArea.size();
    }

    /**
     * Tests if the floating-point precision is 32 bit.
     *
     * @return  whether or not precision is 32 bit
     */
    public boolean is32bitPrecision() {
        return is32bitPrecision;
    }

    /**
     * Determines whether or not locking of registers is allowed.
     *
     * @param  lockingLocked  if locking should be locked
     */
    public void setLockingLocked(boolean lockingLocked) {
        this.lockingLocked = lockingLocked;
    }

    /**
     * Tests if all registers are free, the spill area is empty and all spill
     * locks are free.
     *
     * @return  whether all registers, spill slots and spill locks are free
     */
    public boolean areAllFree() {
        return areAllRegsFree() && isNoneSpilled() && areAllSpillLocksFree();
    }

    /**
     * Checks the register allocation for consistency.
     */
    public void checkRegisters() {
        for (int i = 0; i < NUM_CPU_REGS; i++) {
            if (!allocTableCPU.isFree(i)) {
                Instruction val = valueCPU[i];
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(refCountCPU[i] > 0, "invalid reference count");
                    Assert.that((val == null) || (val.getItem() == null) || val.getItem().isRegister(), "item is not a register");
                }
            }
        }
        for (int i = 0; i < NUM_FPU_REGS; i++) {
            if (!allocTableFPU.isFree(i)) {
                Instruction val = valueFPU[i];
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(refCountFPU[i] > 0, "invalid reference count");
                    Assert.that((val == null) || (val.getItem() == null) || val.getItem().isRegister(), "item is not a register");
                }
            }
        }
        for (int i = 0; i < spillArea.size(); i++) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(spillArea.get(i) != null, "spill element is not set");
            }
        }
    }

    /**
     * Checks the specified spilled item for correctness.
     */
    public void checkSpilled(Item spilled) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(spilled.isSpilled(), "item is not spilled");
            Assert.that(spilled.getSpillIx() < spillArea.size(), "wrong spill slot index");
            Assert.that(spilled.getType().isSingleWord() || (spilled.getSpillIx() + 1 < spillArea.size()), "wrong spill slot index");
        }
    }

    /**
     * Excludes the specified set of general-purpose registers from locking.
     *
     * @param  lockout  set of registers that must not be locked
     */
    public void setLockout(RegMask lockout) {
        allocTableCPU.setLockout(lockout);
    }
}
