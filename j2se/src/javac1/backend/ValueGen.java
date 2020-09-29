/*
 * @(#)ValueGen.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.BasicType;
import javac1.Bytecodes;
import javac1.ci.Method;
import javac1.ci.Runtime1;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.FrameMap;
import javac1.backend.code.Label;
import javac1.backend.debug.CodeEmitInfo;
import javac1.backend.items.*;
import javac1.backend.reg.RInfo;
import javac1.backend.reg.RegAlloc;
import javac1.backend.reg.RegMask;
import javac1.ir.InstructionVisitor;
import javac1.ir.ValueStack;
import javac1.ir.instr.*;
import javac1.ir.types.*;

/**
 * Generates code for a root instruction and its input values.
 *
 * @see      CodeGenerator
 * @see      CodeEmitter
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class ValueGen implements InstructionVisitor {
    /**
     * The input register that stores the dividend.
     */
    public static final RInfo DIV_IN_REG = FrameMap.EAX_RINFO;

    /**
     * The output register that stores the quotient.
     */
    public static final RInfo DIV_OUT_REG = FrameMap.EAX_RINFO;

    /**
     * The output register that stores the remainder.
     */
    public static final RInfo REM_OUT_REG = FrameMap.EDX_RINFO;

    /**
     * The register that stores the shift count.
     */
    public static final RInfo SHIFT_COUNT_REG = FrameMap.ECX_RINFO;

    /**
     * The result register for word values.
     */
    public static final RInfo RET_1_REG = FrameMap.EAX_RINFO;

    /**
     * The result register for long values.
     */
    public static final RInfo RET_2_REG = FrameMap.EAX_EDX_RINFO;

    /**
     * The result register for float values.
     */
    public static final RInfo RET_F0_REG = FrameMap.F0_RINFO;

    /**
     * The result register for double values.
     */
    public static final RInfo RET_D0_REG = FrameMap.D0_RINFO;

    /**
     * A register that is no result register.
     */
    public static final RInfo NO_RET_REG = FrameMap.ESI_RINFO;

    /**
     * The register that stores the receiver of a method call.
     */
    public static final RInfo RECV_REG = FrameMap.ECX_RINFO;

    /**
     * The temporary register used for synchronization.
     */
    public static final RInfo SYNC_TMP_REG = FrameMap.EAX_RINFO;

    /**
     * The data that is invariant.
     */
    private ValueGenInvariant vgi;

    /**
     * Whether or not code has to be generated.
     */
    private boolean genCode;

    /**
     * The item describing the result.
     */
    private Item result;

    /**
     * The item that hints what the result should look like.
     */
    private Item hint;

    /**
     * Constructs a new value generator. This constructor is called for the main
     * root object that passes the code emitter and register allocation
     * structure to other value generator objects.
     *
     * @param  vgi      the invariant data
     * @param  genCode  whether or not code has to be generated
     */
    public ValueGen(ValueGenInvariant vgi, boolean genCode) {
        this.vgi = vgi;
        this.genCode = genCode;
        this.result = null;
        this.hint = null;
    }

    /**
     * Constructs a new value generator. This constructor is called for each
     * instruction and is used to visit each node. The state is returned in the
     * specified result item.
     *
     * @param  result  the item to store result into
     * @param  hint    item that hints what the result should look like
     * @param  parent  the calling value generator object
     */
    public ValueGen(Item result, Item hint, ValueGen parent) {
        this.result = result;
        this.hint = hint;
        this.vgi = parent.getInvariant();
        this.genCode = parent.isGenCode();
        walk(result.getValue());
    }

    /**
     * Visits the specified instruction. This method is called for each node in
     * the tree. The walk stops if a root instruction is reached.
     *
     * @param  instr  instruction to be handled
     */
    private void walk(Instruction instr) {
        if (instr.isRoot()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(instr.getItem() != null, "this root has not been visited yet");
            }
            result.setFromItem(instr.getItem());
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(result.isValid(), "invalid item");
            }
        } else {
            instr.visit(this);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(instr.getUseCount() > 0, "leaf instruction must have a use");
            }
        }
    }

    /**
     * Returns the method that is currently being compiled.
     *
     * @return  the method being compiled
     */
    private Method method() {
        return vgi.getMethod();
    }

    /**
     * Returns the current register allocator.
     *
     * @return  the register allocator
     */
    private RegAlloc regAlloc() {
        return vgi.getRegAlloc();
    }

    /**
     * Returns the current code emitter.
     *
     * @return  the code emitter
     */
    private CodeEmitter codeEmit() {
        return vgi.getCodeEmit();
    }

    /**
     * Returns the current basic block.
     *
     * @return  the current basic block
     */
    public BlockBegin block() {
        return vgi.getBlock();
    }

    /**
     * Creates new debug information from arguments and current state.
     *
     * @param  bci      bytecode index of the instruction
     * @param  stack    value stack of the instruction
     * @param  oopRegs  list of object pointers in registers
     */
    private CodeEmitInfo emitInfo(int bci, ValueStack stack, List oopRegs) {
        return new CodeEmitInfo(bci, regAlloc().oopsInSpill(), stack, oopRegs);
    }

    /**
     * Creates new debug information from arguments and current state.
     *
     * @see  #emitInfo(int, ValueStack, List)
     */
    private CodeEmitInfo emitInfo(int bci, ValueStack stack) {
        return emitInfo(bci, stack, null);
    }

    /**
     * Spills the specified item onto the stack.
     *
     * @param  item  item to be spilled
     */
    private void spillItem(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item != null, "item must not be null");
            Assert.that((item instanceof RootItem) || !item.getValue().isRoot(), "inconsistent item");
            Assert.that(item.isRegister(), "can only spill register items");
            Assert.that(!item.isCached(), "cannot spill cached item");
        }
        int refCount = regAlloc().getRefCountReg(item.getRInfo());
        int spillIx = regAlloc().getLockSpill(item.getValue(), refCount);
        for (int i = 0; i < refCount; i++) {
            freeRaw(item);
        }
        if (genCode) {
            codeEmit().spill(spillIx, item);
        }
        item.setSpillIx(spillIx);
        regAlloc().checkSpilled(item);
    }

    /**
     * Spills the specified value onto the stack.
     *
     * @param  value  value to be spilled
     */
    private void spillValue(Instruction value) {
        Item item = value.getItem();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister(), "can only spill register items");
            Assert.that(!regAlloc().isSpillLocked(item.getRInfo()), "cannot spill a spill locked value");
            Assert.that(item.getValue() == value, "inconsistent item");
        }
        spillItem(item);
    }

    /**
     * Spills the specified register onto the stack.
     *
     * @param  reg  register to be spilled
     */
    private void spillReg(RInfo reg) {
        Instruction value = regAlloc().getValueReg(reg);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((value != null) && (value.getItem() != null), "illegal value");
        }
        spillValue(value);
    }

    /**
     * Finds an instruction that can be spilled and marks it for spilling. The
     * smaller the bytecode index of an instruction, the higher the chance for
     * spilling.
     *
     * @param  type  type of the instruction to be spilled
     */
    private void spillOne(ValueType type) {
        while (!regAlloc().hasFreeReg(type)) {
            Instruction root = regAlloc().getSmallestValueToSpill(type);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(root != null, "no spillable value");
            }
            spillValue(root);
        }
    }

    /**
     * Spills values in caller-save registers. This method is used to preserve
     * registers across calls.
     *
     * @param  stack  stack of values to be spilled
     */
    private void spillValuesOnStack(ValueStack stack) {
        int i = 0;
        while (i < stack.getStackSize()) {
            Instruction val = stack.stackAt(i);
            Item item = val.getItem();
            if (val.isRoot() && (val.getUseCount() > 0) && item.isValid()) {
                if (item.isRegister()) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!item.isCached(), "cannot spill cached register");
                    }
                    spillValue(val);
                } else if (Assert.ASSERTS_ENABLED) {
                    Assert.that(item.isConstant() || item.isSpilled(), "wrong item state");
                }
            }
            i += val.getType().getSize();
        }
    }

    /**
     * Returns a free register for the specified type. If the allocator has no
     * more free registers for the type then one or two registers will be
     * spilled respectively.
     *
     * @param   type  value type to look for
     * @return  register for the specified type
     */
    private RInfo getFreeReg(ValueType type) {
        if (!regAlloc().hasFreeReg(type)) {
            spillOne(type);
        }
        return regAlloc().getFreeReg(type);
    }

    /**
     * Returns any free word register in the specified register set. If none of
     * the registers is free then one or two registers will be spilled
     * respectively.
     *
     * @param   mask  register mask to be searched
     * @return  free register
     */
    private RInfo getFreeReg(RegMask mask) {
        while (!regAlloc().hasFreeReg(mask)) {
            RInfo reg = mask.getFirstReg();
            spillReg(reg);
        }
        return regAlloc().getFreeRegFromMask(mask);
    }

    /**
     * Tests if the specified item represents a register and if this register
     * and the second one overlap. If the registers overlap and they are not
     * equal, then the item will be spilled.
     *
     * @param  item  item that has to be checked
     * @param  reg   the possibly overlapping register
     */
    private void checkForSpill(Item item, RInfo reg) {
        if (item.isRegister()) {
            RInfo itemReg = item.getRInfo();
            if (!reg.equals(itemReg) && reg.overlaps(itemReg)) {
                spillItem(item);
            }
        }
    }

    /**
     * Marks the specified item as not spillable. For this purpose the
     * corresponding spill lock counter in the register allocator will be
     * increased.
     *
     * @param  item  item that may not be spilled
     */
    private void setMayNotSpill(Item item) {
        if (item.isRegister()) {
            regAlloc().incrSpillLock(item.getRInfo());
        }
    }

    /**
     * Tests if the register must be copied. This is the case if the register
     * will be destroyed and if either the item is cached or the register is
     * referenced more than once.
     *
     * @param  item  item to be tested
     */
    private boolean mustCopyRegister(Item item) {
        return item.destroysRegister() && (item.isCached()
            || (regAlloc().getRefCountReg(item.getRInfo()) > 1));
    }

    /**
     * Locks the specified free register.
     *
     * @param  reg    register to be locked
     * @param  instr  instruction that wants to lock the register
     */
    private void lockReg(RInfo reg, Instruction instr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().isFreeReg(reg), "register must be free");
        }
        regAlloc().lockReg(reg, instr, 1);
    }

    /**
     * Locks the specified register. If it is already locked the register will
     * be spilled before.
     *
     * @param   reg       register to be locked
     * @param   instr     instruction that wants to lock the register
     * @param   refCount  reference count
     */
    private void lockSpillReg(RInfo reg, Instruction instr, int refCount) {
        while (!regAlloc().isFreeReg(reg)) {
            spillReg(reg);
        }
        regAlloc().lockReg(reg, instr, refCount);
    }

    /**
     * Releases the spill slot represented by the specified item.
     *
     * @param  item  spill slot item to be released
     */
    private void freeSpill(Item item) {
        regAlloc().freeSpill(item.getSpillIx(), item.getType());
    }

    /**
     * Releases the register represented by the specified item.
     *
     * @param  item  register item to be released
     */
    private void freeReg(Item item) {
        regAlloc().decrSpillLock(item.getRInfo());
        if (!item.isCached()) {
            regAlloc().freeReg(item.getRInfo());
        }
        if ((item.getValue() != null) && item.getValue().isRoot()) {
            Item rootItem = item.getValue().getItem();
            if (rootItem.isRegister() && regAlloc().isFreeReg(rootItem.getRInfo())) {
                rootItem.setNoResult();
            }
        }
    }

    /**
     * Releases the spill slot or register described by the specified item.
     *
     * @param  item  item to be released
     */
    private void freeItem(Item item) {
        if (item.isSpilled()) {
            freeSpill(item);
        } else if (item.isRegister()) {
            freeReg(item);
        }
    }

    /**
     * Releases the register described by the specified item.
     *
     * @param  item  register item to be released
     */
    private void freeRaw(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!item.isCached(), "cannot release cached item");
        }
        regAlloc().freeReg(item.getRInfo());
    }

    /**
     * Hides the specified register and spills it if necessary. This method is
     * used to prevent locking of a certain register.
     *
     * @param   reg    register to hide
     * @param   spill  whether the register should be spilled if locked
     * @return  the hidden register
     */
    private RInfo hideReg(RInfo reg, boolean spill) {
        if (spill) {
            while(!regAlloc().isFreeReg(reg)) {
                spillReg(reg);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().isFreeReg(reg), "cannot hide locked register");
        }
        regAlloc().lockReg(reg, null, 1);
        regAlloc().incrSpillLock(reg);
        return reg;
    }

    /**
     * Hides a register for the specified type to prevent it from being locked.
     *
     * @param   type  type of the register to hide
     * @return  the hidden register
     */
    private RInfo hideReg(ValueType type) {
        RInfo reg = getFreeReg(type);
        regAlloc().lockReg(reg, null, 1);
        regAlloc().incrSpillLock(reg);
        return reg;
    }

    /**
     * Hides a register in the specified set of registers to prevent it from
     * being locked.
     *
     * @param   mask  set of registers that may be hidden
     * @return  the hidden register
     */
    private RInfo hideReg(RegMask mask) {
        RInfo reg = getFreeReg(mask);
        regAlloc().lockReg(reg, null, 1);
        regAlloc().incrSpillLock(reg);
        return reg;
    }

    /**
     * Marks the specified formerly hidden register as available again.
     *
     * @param  reg  register to show
     */
    private void showReg(RInfo reg) {
        regAlloc().decrSpillLock(reg);
        regAlloc().freeReg(reg);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().isFreeReg(reg), "register must be free");
        }
    }

    /**
     * Tests and handles fanout of floating-point registers. If a fanout has
     * been determined, then the value of the floating-point register will be
     * copied into a new one, so that the new register is referenced only once.
     *
     * @param   item  item to be tested
     * @return  whether or not a fanout has been determined
     */
    private boolean fpuFanoutHandled(Item item) {
        if (item.isRegister() && item.getType().isFloatKind()
                && (regAlloc().getRefCountReg(item.getRInfo()) > 1)) {
            RInfo reg = lockFreeReg(item.getValue(), item.getType());
            if (genCode) {
                codeEmit().copyFpuItem(reg, item);
            }
            freeItem(item);
            item.setRInfo(reg);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Locks a free register for the specified instruction and type. If the
     * allocator has no more free registers for the type then one or two
     * registers will be spilled respectively.
     *
     * @param   instr  instruction that wants to lock the register
     * @param   type   type of the register to be locked
     * @return  the locked register
     */
    private RInfo lockFreeReg(Instruction instr, ValueType type) {
        if (!regAlloc().hasFreeReg(type)) {
            spillOne(type);
        }
        RInfo reg = regAlloc().getFreeReg(type);
        regAlloc().lockReg(reg, instr, 1);
        return reg;
    }

    /**
     * Locks a free register in the set for the specified instruction.
     *
     * @param   instr  instruction that wants to lock the register
     * @param   mask   set of available registers
     * @return  the locked register
     */
    private RInfo lockFreeReg(Instruction instr, RegMask mask) {
        while (!regAlloc().hasFreeReg(mask)) {
            Instruction val = regAlloc().getSmallestValueToSpill(mask);
            spillValue(val);
        }
        RInfo reg = regAlloc().getFreeRegFromMask(mask);
        lockReg(reg, instr);
        return reg;
    }

    /**
     * Locks a free register for the specified instruction. If the allocator
     * has no more free registers for the type then one or two registers will be
     * spilled respectively.
     *
     * @param   instr  instruction that wants to lock the register
     * @return  the locked register
     */
    private RInfo lockReg(Instruction instr) {
        while (!regAlloc().hasFreeReg(instr.getType())) {
            spillOne(instr.getType());
        }
        return regAlloc().getLockReg(instr, instr.getType());
    }

    /**
     * Locks a free register for the specified instruction using the hint if
     * possible.
     *
     * @param   instr  instruction that wants to lock the register
     * @param   hint   item that specifies the preferred register
     * @return  the locked register
     */
    private RInfo lockReg(Instruction instr, Item hint) {
        if (hint.hasHint() && regAlloc().isFreeReg(hint.getRInfo())) {
            if (!hint.isCached()) {
                regAlloc().lockReg(hint.getRInfo(), instr, instr.getUseCount());
            }
            return hint.getRInfo();
        } else {
            return lockReg(instr);
        }
    }

    /**
     * Returns the result register for the specified type.
     *
     * @param   type  type of the result register
     * @return  the result register
     */
    private RInfo resultRegisterFor(ValueType type) {
        switch (type.getTag()) {
        case ValueType.intTag:
            /* falls through */
        case ValueType.objectTag:
            /* falls through */
        case ValueType.addressTag:
            return RET_1_REG;
        case ValueType.longTag:
            return RET_2_REG;
        case ValueType.floatTag:
            return RET_F0_REG;
        case ValueType.doubleTag:
            return RET_D0_REG;
        default:
            Assert.shouldNotReachHere();
            return RInfo.NO_RINFO;
        }
    }

    /**
     * Locks the return value register for a call and spill if necessary.
     *
     * @param   instr  instruction to lock result register for
     * @return  the locked result register
     */
    private RInfo lockResultReg(Instruction instr) {
        RInfo reg = resultRegisterFor(instr.getType());
        while (!regAlloc().isFreeReg(reg)) {
            spillReg(reg);
        }
        regAlloc().lockReg(reg, instr, instr.getUseCount());
        return reg;
    }

    /**
     * Dereferences a floating-point register explicitly by copying it into a
     * new register for correct stack simulation. In this case the item will be
     * set to a new register.
     *
     * @param  item  the item to be checked
     */
    private void checkFloatRegister(Item item) {
        if (item.getType().isFloatKind()) {
            if (regAlloc().getRefCountReg(item.getRInfo()) > 1) {
                RInfo reg = lockFreeReg(item.getValue(), item.getType());
                if (genCode) {
                    codeEmit().copyFpuItem(reg, item);
                }
                freeReg(item);
                item.setRInfo(reg, false);
                setMayNotSpill(item);
            }
        }
    }

    /**
     * Loads the specified item into a free byte register.
     *
     * @param  item  item to be loaded
     */
    private void loadByteItem(Item item) {
        item.update();
        setMayNotSpill(item);
        if (!item.isRegister() || !FrameMap.isByteRInfo(item.getRInfo())
                || mustCopyRegister(item)) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!item.getType().isFloatKind(), "cannot load floats in byte register");
            }
            RInfo reg = lockFreeReg(item.getValue(), FrameMap.BYTE_REG_MASK);
            if (genCode) {
                codeEmit().itemToReg(reg, item);
            }
            freeItem(item);
            item.setRInfo(reg);
            setMayNotSpill(item);
        }
    }

    /**
     * Loads the specified item into a free register. At first the item to be
     * modified must be synchronized with the item that is maintained by the
     * instruction. After its modification the specified item may not correspond
     * to the item of the instruction anymore.
     *
     * @param  item  item to be loaded
     */
    private void loadItem(Item item) {
        item.update();
        setMayNotSpill(item);
        if (item.isRegister() && !mustCopyRegister(item)) {
            checkFloatRegister(item);
        } else {
            if (fpuFanoutHandled(item)) {
                setMayNotSpill(item);
            } else {
                RInfo reg = lockFreeReg(item.getValue(), item.getType());
                if (genCode) {
                    codeEmit().itemToReg(reg, item);
                }
                freeItem(item);
                item.setRInfo(reg);
                setMayNotSpill(item);
            }
        }
    }

    /**
     * Loads the specified item with patching.
     *
     * @param  item  item to be loaded
     */
    private void loadItemPatching(Item item) {
        item.update();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && (item.getType() instanceof ObjectType), "must be an object constant");
        }
        setMayNotSpill(item);
        List oopRegs = null;
        if (genCode) {
            oopRegs = regAlloc().oopsInRegisters();
        }
        RInfo reg = lockFreeReg(item.getValue(), item.getType());
        if (genCode) {
            CodeEmitInfo info = emitInfo(item.getValue().getBci(), null, oopRegs);
            codeEmit().itemToRegWithPatching(reg, item, info);
        }
        freeItem(item);
        item.setRInfo(reg);
        setMayNotSpill(item);
    }

    /**
     * Loads the item into the specified register forcibly.
     *
     * @param  item  item to be loaded
     * @param  reg   destination register
     */
    private void loadItemForce(Item item, RInfo reg) {
        checkForSpill(item, reg);
        item.update();
        setMayNotSpill(item);
        if (!item.isRegister() || !reg.equals(item.getRInfo())
                || mustCopyRegister(item)) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!item.isRegister() || !reg.equals(item.getRInfo()), "should not reach here");
            }
            lockSpillReg(reg, item.getValue(), 1);
            item.update();
            if (genCode) {
                codeEmit().itemToReg(reg, item);
            }
            freeItem(item);
            item.setRInfo(reg);
            setMayNotSpill(item);
        }
    }

    /**
     * Loads the specified item using the hint if possible.
     *
     * @param  item  item to be loaded
     * @param  hint  item that specifies the preferred register
     */
    private void loadItemHint(Item item, Item hint) {
        item.update();
        setMayNotSpill(item);
        if (item.isRegister() && !mustCopyRegister(item)) {
            checkFloatRegister(item);
        } else {
            if (fpuFanoutHandled(item)) {
                setMayNotSpill(item);
            } else {
                boolean isCached = false;
                RInfo reg;
                if (hint.hasHint() && regAlloc().isFreeReg(hint.getRInfo())) {
                    reg = hint.getRInfo();
                    if (hint.isCached()) {
                        isCached = true;
                    } else {
                        lockReg(reg, item.getValue());
                    }
                } else {
                    reg = lockFreeReg(item.getValue(), item.getType());
                }
                if (genCode) {
                    codeEmit().itemToReg(reg, item);
                }
                freeItem(item);
                item.setRInfo(reg, isCached);
                setMayNotSpill(item);
            }
        }
    }

    /**
     * Marks an item that does not need to be loaded. If the item represents a
     * floating-point register that may be destroyed, the register is checked
     * for multiple references.
     *
     * @param  item  item that does not need to be loaded
     * @see    #checkFloatRegister(Item)
     */
    private void dontLoadItem(Item item) {
        item.update();
        setMayNotSpill(item);
        if (item.isRegister() && item.destroysRegister()) {
            checkFloatRegister(item);
        }
    }

    /**
     * Sets the result item to the specified register.
     *
     * @param  instr  instruction that produces the result
     * @param  reg    the result register
     */
    private void setResult(Instruction instr, RInfo reg) {
        result.setRInfo(reg, false);
    }

    /**
     * Specifies that no result value will be produced.
     *
     * @param  instr  instruction that has no result
     */
    private void setNoResult(Instruction instr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instr.getUseCount() == 0, "cannot have a use");
        }
        result.setNoResult();
    }

    /**
     * Tests if the current basic block is in 32-bit mode.
     *
     * @return  whether or not the block is in 32-bit mode
     */
    private boolean is32bitMode() {
        BlockItem item = vgi.getBlock().getBlockItem();
        return (item != null) && item.is32bitPrecision();
    }

    /**
     * Tests if 32-bit rounding precision is required.
     *
     * @param   x     the current operation
     * @param   hint  item that hints what the result should look like
     * @return  whether or not 32-bit rounding is required
     */
    private boolean mustRound(Instruction x, Item hint) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((x instanceof Intrinsic) || (x instanceof NegateOp) || (x instanceof ArithmeticOp) || (x instanceof Convert), "wrong operation");
        }
        if (vgi.getMethod().isStrict()) {
            return true;
        } else if ((x.getType() instanceof FloatType)
                && (x.isRoot() || (x.getUseCount() > 1)
                || (hint.isRound32() && !regAlloc().is32bitPrecision()))) {
            return !is32bitMode();
        } else {
            return false;
        }
    }

    /**
     * Tests if the specified instruction is a backward branch. In this case the
     * specified destination block is flagged as a backward branch target.
     *
     * @param   instr  the branch instruction
     * @param   dest   possible destination of the branch
     * @return  whether or not the instruction is a backward branch
     */
    private boolean isBackwardBranch(Instruction instr, BlockBegin dest) {
        boolean isBackward = vgi.getBlock().isAfterBlock(dest);
        if (isBackward) {
            dest.setFlag(BlockBegin.BACKW_BRANCH_TARGET_FLAG);
        }
        return isBackward;
    }

    /**
     * Returns the list of registers that contain pointers in the current basic
     * block.
     *
     * @return  list of pointers in registers
     */
    private List getBlockItemOopRegs() {
        BlockItem blockItem = block().getBlockItem();
        List res = new ArrayList();
        if (blockItem != null) {
            int len = blockItem.mapLength();
            for (int i = 0; i < len; i++) {
                if (blockItem.isOop(i)) {
                    res.add(blockItem.getCacheReg(i));
                }
            }
        }
        return res.isEmpty() ? null : res;
    }

    /**
     * Returns the register that caches the specified variable.
     *
     * @param   index  index of the local variable
     * @return  the register that caches the variable
     */
    private RInfo cachedLocal(int localIndex) {
        BlockItem blockItem = vgi.getBlock().getBlockItem();
        if (blockItem == null) {
            return RInfo.NO_RINFO;
        } else {
            return blockItem.getCacheReg(localIndex);
        }
    }

    /**
     * Tests if the item is no receiver or is initialized.
     *
     * @param   item  item to be examined
     * @return  whether or not the item may be null
     */
    private boolean itemMayBeNull(Item item) {
        return method().isStatic()
            || codeEmit().getScanResult().hasStore0()
            || !(item.getValue() instanceof LoadLocal)
            || (((LoadLocal) item.getValue()).getIndex() != 0);
    }

    /**
     * Aligns the specified basic block if it is the target of a backward
     * branch.
     *
     * @param  block  basic block to be aligned
     */
    public void alignBlock(BlockBegin block) {
        if (genCode && block.isFlagSet(BlockBegin.BACKW_BRANCH_TARGET_FLAG)) {
            codeEmit().alignBackwardBranch();
        }
    }

    /**
     * Binds the specified label to the current code position.
     *
     * @param   label  label to be bound
     */
    public void bindLabel(Label label) {
        if (genCode) {
            codeEmit().bindLabel(label);
        }
    }

    /**
     * Moves the value that is spilled at the specified index. This method is
     * used to move a spilled value out of the area reserved for phi values.
     *
     * @param  spillIx       index of the source spill slot
     * @param  leastSpillIx  minimum index of the target slot
     * @param  value         spilled value to be moved
     */
    private void moveSpillTo(int spillIx, int leastSpillIx, Instruction value) {
        for (int i = 0; i < value.getType().getSize(); i++) {
            if (regAlloc().getRefCountAt(spillIx + i) > 0) {
                Instruction toMove = regAlloc().getValueSpilledAt(spillIx + i);
                ValueType type = toMove.getType();
                int fromIx = toMove.getItem().getSpillIx();
                int newIx = regAlloc().getFreeSpillAfter(leastSpillIx, type);
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(newIx >= leastSpillIx, "wrong index computation");
                }
                RInfo reg = hideReg(type);
                if (genCode) {
                    codeEmit().moveSpill(newIx, fromIx, type, reg);
                }
                regAlloc().moveSpill(newIx, fromIx, type);
                showReg(reg);
            }
        }
    }

    /**
     * Computes the array of phi values. If the stack is not empty, the value on
     * top is returned. The other values on stack and their spill slot indices
     * are inserted into the specified lists.
     *
     * @param   stack      stack that describes the spill values
     * @param   phiValues  list to add phi values to
     * @param   spillIxs   list to add spill indices to
     * @return  value on top of stack if any
     */
    private Instruction computePhiArrays(ValueStack stack, List phiValues,
            List spillIxs) {
        if (stack.getStackSize() == 0) {
            return null;
        }
        int i = stack.getStackSize();
        Instruction topVal = stack.stackBefore(i);
        i -= topVal.getType().getSize();
        while (i > 0) {
            Instruction val = stack.stackBefore(i);
            i -= val.getType().getSize();
            phiValues.add(val);
            spillIxs.add(new Integer(i));
        }
        return topVal;
    }

    /**
     * Moves all stack values to their phi position. The free registers and
     * spill slots are used to pass the live values from one basic block to
     * the other. The topmost value on the expression stack is passed in a
     * register. All other values are stored in the spill area.
     *
     * @param  stack  the value stack
     */
    private void moveToPhi(ValueStack stack) {
        List phiValues = new ArrayList();
        List spillIxs = new ArrayList();
        Instruction topVal = computePhiArrays(stack, phiValues, spillIxs);
        for (int i = 0; i < phiValues.size(); i++) {
            Instruction val = (Instruction) phiValues.get(i);
            Item item = val.getItem();
            int ix = ((Integer) spillIxs.get(i)).intValue();
            if ((item != null) && item.isSpilled() && (item.getSpillIx() == ix)) {
                regAlloc().freeSpill(ix, val.getType());
            } else {
                if (!regAlloc().isFreeSpill(ix, val.getType())) {
                    moveSpillTo(ix, stack.getStackSize(), val);
                }
                item = new Item(val);
                item.handleFloatKind();
                new ValueGen(item, HintItem.NO_HINT, this);
                loadItem(item);
                freeReg(item);
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(regAlloc().isFreeSpill(ix, val.getType()), "spill slot not empty");
                }
                if (genCode) {
                    codeEmit().spill(ix, item);
                }
                item.dispose();
            }
        }
        if (topVal != null) {
            RInfo reg = resultRegisterFor(topVal.getType());
            HintItem hint = new HintItem(topVal.getType(), reg);
            Item item = new Item(topVal);
            item.handleFloatKind();
            new ValueGen(item, hint, this);
            loadItemHint(item, hint);
            freeReg(item);
            if (genCode) {
                codeEmit().itemToReg(reg, item);
                codeEmit().clearFpuStack();
            }
            item.dispose();
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllFree(), "not all registers are free");
            Assert.that(!genCode || codeEmit().isFpuStackEmpty(), "still some values on FPU stack");
        }
    }

    /**
     * Implements actions for the entry of a basic block. This method sets up
     * the content of phi nodes and locks necessary registers and spill slots.
     *
     * @param  block  basic block to be entered
     */
    public void blockProlog(BlockBegin block) {
        ValueStack stack = block.getState();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(stack != null, "block state must exist");
            Assert.that(regAlloc().areAllFree(), "not all registers are free");
            Assert.that(!genCode || codeEmit().isFpuStackEmpty(), "still some values on FPU stack");
        }
        BlockItem blockItem = block.getBlockItem();
        RegMask lockout = RegMask.EMPTY_SET;
        if (blockItem != null) {
            lockout = blockItem.getLockout();
        }
        regAlloc().setLockout(lockout);
        if (stack.isStackEmpty()) {
            return;
        }
        List phiValues = new ArrayList();
        List spillIxs = new ArrayList();
        Instruction topVal = computePhiArrays(stack, phiValues, spillIxs);
        for (int i = 0; i < phiValues.size(); i++) {
            Phi phi = (Phi) phiValues.get(i);
            int spillIx = ((Integer) spillIxs.get(i)).intValue();
            if (phi.getUseCount() > 0) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(phi.getItem() == null, "item already set");
                }
                RootItem item = new RootItem(phi);
                phi.setItem(item);
                item.setSpillIx(spillIx);
                regAlloc().lockSpill(phi, spillIx, phi.getUseCount());
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(phi.getItem().isValid(), "invalid item");
                }
                regAlloc().checkSpilled(item);
            } else {
                regAlloc().lockSpill(phi, spillIx, 1);
                regAlloc().freeSpill(spillIx, phi.getType());
                phi.setItem(new RootItem(phi));
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(topVal != null, "must generate top value");
            Assert.that(topVal.getItem() == null, "top item already set");
        }
        if (topVal.getUseCount() > 0) {
            RInfo phiReg = resultRegisterFor(topVal.getType());
            RootItem item = new RootItem(topVal);
            item.setRInfo(phiReg);
            topVal.setItem(item);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(topVal.getItem().isValid(), "invalid item");
            }
            regAlloc().lockReg(phiReg, topVal, topVal.getUseCount());
            if (topVal.getType().isFloatKind() && genCode) {
                codeEmit().setFpuResult(topVal.getItem().getRInfo());
            }
        } else {
            topVal.setItem(new RootItem(topVal));
            if (topVal.getType().isFloatKind() && genCode) {
                codeEmit().fpop();
            }
        }
    }

    /**
     * Implements actions for the exit of a basic block.
     *
     * @param  block  basic block to be leaved
     */
    public void blockEpilog(BlockBegin block) {
        /* nothing to do */
    }

    /**
     * Generates code for the specified root instruction.
     *
     * @param  instr  root instruction to generate code for
     */
    public void doRoot(Instruction instr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instr.isRoot(), "use only with roots");
            regAlloc().checkRegisters();
            Assert.that(regAlloc().areAllSpillLocksFree(), "no spill locks may be alive");
            Assert.that(!genCode || (codeEmit().getEspOffset() == 0), "esp offset must be 0 here");
        }
        if (genCode) {
            codeEmit().checkCodespace();
        }
        if (instr instanceof Phi) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(instr.getItem() != null, "phi item must be set");
            }
            return;
        }
        hint = HintItem.NO_HINT;
        result = new RootItem(instr);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instr.getItem() == null, "item already set");
        }
        instr.visit(this);
        finishRoot(instr);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((instr.getUseCount() == 0) || instr.getItem().isValid(),  "invalid item set");
            Assert.that(!genCode || (codeEmit().getEspOffset() == 0), "esp offset must be 0 here");
            Assert.that(regAlloc().areAllSpillLocksFree(), "no spill locks may be alive");
            regAlloc().checkRegisters();
        }
    }

    /**
     * Releases the specified register or spill slot.
     *
     * @param  item  item to be released
     */
    private void releaseItem(Item item) {
        if (item.isRegister()) {
            if (item.getType().isFloatKind() && genCode) {
                codeEmit().removeFpuResult(item.getRInfo());
            }
            if (!item.isCached()) {
                freeRaw(item);
            }
        } else if (item.isSpilled()) {
            freeSpill(item);
        }
    }

    /**
     * Releases the result item if the specified instruction is not used.
     *
     * @param  instr  instruction to be checked
     */
    private void checkResultUsage(Instruction instr) {
        if (result.hasResult() && (instr.getUseCount() == 0)) {
            releaseItem(result);
            result.setNoResult();
        }
    }

    /**
     * Releases the root instructions on the specified stack.
     *
     * @param  stack  stack with root instructions to be released
     */
    private void releaseRoots(ValueStack stack) {
        int i = 0;
        while (i < stack.getStackSize()) {
            Instruction val = stack.stackAt(i);
            if (val.isRoot()) {
                releaseItem(val.getItem());
            }
            i += val.getType().getSize();
        }
    }

    /**
     * Finishes code generation for the specified root instruction.
     *
     * @param  instr  root instruction to be finished
     */
    private void finishRoot(Instruction instr) {
        if (result.hasResult() && (instr.getUseCount() > 0)) {
            if (result.isConstant() || (result.isStack() && result.isSpilled())) {
                instr.setItem(result);
            } else if (result.isStack() || result.isCached()) {
                if (!result.isCached() || !(instr instanceof AccessLocal)
                        || ((AccessLocal) instr).isPinnedByStore()) {
                    RInfo reg = lockReg(instr);
                    if (genCode) {
                        codeEmit().itemToReg(reg, result);
                    }
                    if (result.isSpilled()) {
                        int spillIx = result.getSpillIx();
                        int rc = regAlloc().getRefCountAt(spillIx);
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that(instr.getUseCount() == rc, "invalid use count");
                        }
                        regAlloc().freeCompleteSpill(spillIx, instr.getType());
                    } else if (result.isRegister() && !result.isCached()) {
                        int rc = regAlloc().getRefCountReg(result.getRInfo());
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that(instr.getUseCount() == rc, "invalid use count");
                        }
                        for (int i = 0; i < rc; i++) {
                            freeRaw(result);
                        }
                    }
                    result.setRInfo(reg);
                    regAlloc().setReg(reg, instr.getUseCount(), instr);
                    instr.setItem(result);
                } else {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(result.isCached(), "must be cached");
                    }
                    instr.setItem(result);
                }
            } else {
                RInfo reg = result.getRInfo();
                regAlloc().setReg(reg, instr.getUseCount(), instr);
                instr.setItem(result);
            }
        }
        checkResultUsage(instr);
    }

    /**
     * Emits code and debug information at exception handler start.
     *
     * @param  bci  bytecode index of the handler start
     */
    public void exceptionHandlerStart(int bci) {
        if (genCode) {
            CodeEmitInfo info = emitInfo(bci, null);
            codeEmit().addDebugInfoHere(info);
            codeEmit().handlerEntry();
            if (!method().isStatic() && javac1.Flags.UseCachedReceiver) {
                codeEmit().restoreCachedReceiver();
            }
        }
    }

    /**
     * Tests if the FPU stack is empty.
     *
     * @return  whether or not the stack is empty
     */
    public boolean isFpuStackFree() {
        return !genCode || codeEmit().isFpuStackEmpty();
    }

    /**
     * Tests if the CPU stack is empty.
     *
     * @return  whether or not the stack is empty
     */
    public boolean isCpuStackFree() {
        return !genCode || (codeEmit().getEspOffset() == 0);
    }

    /**
     * Sets the invariant data.
     *
     * @param  vgi  the invariant data
     */
    public void setInvariant(ValueGenInvariant vgi) {
        this.vgi = vgi;
    }

    /**
     * Returns the invariant data.
     *
     * @return  the invariant data
     */
    public ValueGenInvariant getInvariant() {
        return vgi;
    }

    /**
     * Tests if code has to be generated.
     *
     * @return  whether or not code has to be generated
     */
    public boolean isGenCode() {
        return genCode;
    }

    /**
     * Sets the current basic block.
     *
     * @param  block  the current basic block
     */
    public void setBlock(BlockBegin block) {
        vgi.setBlock(block);
    }

    /**
     * Generates code for the specified arithmetic integer operation.
     *
     * @param  x  arithmetic operation to be generated
     */
    private void doArithmeticOpInt(ArithmeticOp x) {
        if ((x.getOp() == Bytecodes._idiv) || (x.getOp() == Bytecodes._irem)) {
            Item left = new Item(x.getX());
            Item right = new Item(x.getY());
            left.setDestroysRegister(true);
            right.setDestroysRegister(true);
            HintItem hint = new HintItem(x.getType(), DIV_IN_REG);
            new ValueGen(right, HintItem.NO_HINT, this);
            new ValueGen(left, hint, this);
            if (left.isRegister() && left.getRInfo().equals(hint.getRInfo())) {
                if (regAlloc().getRefCountReg(left.getRInfo()) > 1) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(x.getX().isRoot(), "must spill the root item");
                    }
                    spillItem(x.getX().getItem());
                    loadItemForce(left, hint.getRInfo());
                } else {
                    dontLoadItem(left);
                }
            } else {
                loadItemForce(left, hint.getRInfo());
            }
            RInfo hr = hideReg(REM_OUT_REG, true);
            loadItem(right);
            freeItem(left);
            freeItem(right);
            showReg(hr);
            if (x.getOp() == Bytecodes._idiv) {
                lockSpillReg(DIV_OUT_REG, x, x.getUseCount());
                setResult(x, DIV_OUT_REG);
            } else {
                lockSpillReg(REM_OUT_REG, x, x.getUseCount());
                setResult(x, REM_OUT_REG);
            }
            if (genCode) {
                if (!javac1.Flags.ImplicitDiv0Checks) {
                    CodeEmitInfo info = emitInfo(x.getBci(), null);
                    codeEmit().explicitDivByZeroCheck(right, info);
                }
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().arithmeticIntDiv(x.getOp(), result, left, right, info);
            }
            left.dispose();
            right.dispose();
        } else {
            Item left = new Item(x.getX());
            Item right = new Item(x.getY());
            new ValueGen(left, hint, this);
            new ValueGen(right, HintItem.NO_HINT, this);
            if (x.isCommutative() && (left.isStack() || left.isCached())
                    && right.isRegister() && !right.isCached()) {
                Item temp = left;
                left = right;
                right = temp;
            }
            left.setDestroysRegister(true);
            loadItemHint(left, hint);
            if (x.getOp() == Bytecodes._imul) {
                loadItem(right);
            } else {
                dontLoadItem(right);
            }
            freeReg(left);
            freeItem(right);
            RInfo reg = lockReg(x, hint);
            setResult(x, reg);
            if (genCode) {
                codeEmit().arithmeticOpInt(x.getOp(), result, left, right);
            }
            left.dispose();
            right.dispose();
        }
    }

    /**
     * Generates code for the specified long arithmetic operation.
     *
     * @param  x  arithmetic operation to be generated
     */
    private void doArithmeticOpLong(ArithmeticOp x) {
        if ((x.getOp() == Bytecodes._ldiv) || (x.getOp() == Bytecodes._lrem)) {
            Item left = new Item(x.getX());
            Item right = new Item(x.getY());
            right.setDestroysRegister(true);
            new ValueGen(left, HintItem.NO_HINT, this);
            new ValueGen(right, HintItem.NO_HINT, this);
            if (genCode) {
                codeEmit().arithmeticCallProlog(x.getType());
            }
            loadItem(left);
            freeReg(left);
            if (genCode) {
                codeEmit().pushItem(left);
            }
            loadItem(right);
            freeReg(right);
            if (genCode) {
                codeEmit().pushItem(right);
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitDivByZeroCheck(right, info);
            }
            RInfo reg = lockResultReg(x);
            setResult(x, reg);
            if (genCode) {
                codeEmit().arithmeticCallOp(x.getOp());
                codeEmit().arithmeticCallEpilog(x.getType());
            }
            left.dispose();
            right.dispose();
        } else if (x.getOp() == Bytecodes._lmul) {
            Item left = new Item(x.getX());
            Item right = new Item(x.getY());
            new ValueGen(left, HintItem.NO_HINT, this);
            new ValueGen(right, HintItem.NO_HINT, this);
            loadItem(left);
            freeReg(left);
            if (genCode) {
                codeEmit().pushItem(left);
            }
            loadItem(right);
            freeReg(right);
            if (genCode) {
                codeEmit().pushItem(right);
            }
            RInfo reg = lockResultReg(x);
            setResult(x, reg);
            if (genCode) {
                codeEmit().longMul();
            }
            left.dispose();
            right.dispose();
        } else {
            Item left = new Item(x.getX());
            Item right = new Item(x.getY());
            left.setDestroysRegister(true);
            new ValueGen(left, HintItem.NO_HINT, this);
            new ValueGen(right, HintItem.NO_HINT, this);
            loadItemHint(left, hint);
            loadItem(right);
            freeReg(left);
            freeItem(right);
            RInfo reg = lockReg(x, hint);
            setResult(x, reg);
            if (genCode) {
                codeEmit().arithmeticOpLong(x.getOp(), result, left, right);
            }
            left.dispose();
            right.dispose();
        }
    }

    /**
     * Generates code for the specified floating-point arithmetic operation.
     *
     * @param  x  arithmetic operation to be generated
     */
    private void doArithmeticOpFloat(ArithmeticOp x) {
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        left.setDestroysRegister(true);
        right.setDestroysRegister(true);
        HintItem leftHint = new HintItem(x.getX().getType());
        leftHint.setFromItem(hint);
        HintItem rightHint = new HintItem(x.getY().getType());
        rightHint.setFromItem(HintItem.NO_HINT);
        if (x.getType() instanceof FloatType) {
            leftHint.setRound32(true);
            rightHint.setRound32(true);
        }
        new ValueGen(left, leftHint, this);
        new ValueGen(right, rightHint, this);
        if (x.isCommutative() && (left.isStack() || left.isCached())
                && right.isRegister() && !right.isCached()) {
            Item temp = left;
            left = right;
            right = temp;
        }
        loadItemHint(left, leftHint);
        if (x.getY().getType().isConstant() || (x.getOp() == Bytecodes._frem)
                || (x.getOp() == Bytecodes._drem)) {
            loadItem(right);
        } else {
            dontLoadItem(right);
        }
        freeReg(left);
        freeItem(right);
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            codeEmit().arithmeticOpFloat(x.getOp(), result, left, right);
        }
        if (mustRound(x, hint)) {
            spillItem(result);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(result.isSpilled(), "spilling failed");
            }
        }
        left.dispose();
        right.dispose();
    }

    public void doArithmeticOp(ArithmeticOp x) {
        switch (x.getType().getTag()) {
        case ValueType.intTag:
            doArithmeticOpInt(x);
            break;
        case ValueType.longTag:
            doArithmeticOpLong(x);
            break;
        case ValueType.floatTag:
            /* falls through */
        case ValueType.doubleTag:
            doArithmeticOpFloat(x);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    public void doArrayLength(ArrayLength x) {
        Item array = new Item(x.getArray());
        new ValueGen(array, HintItem.NO_HINT, this);
        loadItem(array);
        freeReg(array);
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(array, info);
            }
            int arrayLengthOffset = Runtime1.getArrayLengthOffset() / 4;
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().fieldLoad(reg, BasicType.INT, array, arrayLengthOffset,
                false, true, info);
        }
        array.dispose();
    }

    public void doBase(Base x) {
        /* nothing to do */
    }

    public void doBlockBegin(BlockBegin x) {
        /* nothing to do */
    }

    public void doCheckCast(CheckCast x) {
        spillValuesOnStack(x.getState());
        Item obj = new Item(x.getObj());
        obj.setDestroysRegister(true);
        new ValueGen(obj, HintItem.NO_HINT, this);
        loadItem(obj);
        RInfo reg = lockReg(x);
        setResult(x, reg);
        RInfo tmp = getFreeReg(ValueType.objectType);
        freeReg(obj);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().checkcastOp(reg, obj, x.getKlass(), tmp, info);
        }
        obj.dispose();
    }

    public void doCompareOp(CompareOp x) {
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        int tag = x.getX().getType().getTag();
        if ((tag == ValueType.floatTag) || (tag == ValueType.doubleTag)) {
            left.setDestroysRegister(true);
            right.setDestroysRegister(true);
            HintItem leftHint = new HintItem(x.getX().getType());
            leftHint.setFromItem(HintItem.NO_HINT);
            HintItem rightHint = new HintItem(x.getY().getType());
            rightHint.setFromItem(HintItem.NO_HINT);
            if (tag == ValueType.floatTag) {
                leftHint.setRound32(true);
                rightHint.setRound32(true);
            }
            new ValueGen(left, leftHint, this);
            new ValueGen(right, rightHint, this);
            loadItem(left);
            loadItem(right);
        } else {
            if (tag == ValueType.longTag) {
                left.setDestroysRegister(true);
            }
            new ValueGen(left, HintItem.NO_HINT, this);
            new ValueGen(right, HintItem.NO_HINT, this);
            loadItem(left);
            loadItem(right);
        }
        freeReg(left);
        freeReg(right);
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            codeEmit().compareOp(x.getOp(), reg, left, right);
        }
        left.dispose();
        right.dispose();
    }

    public void doConstant(Constant x) {
        result.setConstant();
    }

    public void doConvert(Convert x) {
        HintItem valueHint = new HintItem(x.getValue().getType());
        valueHint.setFromItem(HintItem.NO_HINT);
        if (x.getValue().getType() instanceof FloatType) {
            valueHint.setRound32(true);
        }
        int op = x.getOp();
        if ((op == Bytecodes._f2i) || (op == Bytecodes._d2i)) {
            Item value = new Item(x.getValue());
            new ValueGen(value, valueHint, this);
            value.setDestroysRegister(true);
            loadItem(value);
            freeReg(value);
            RInfo reg = lockResultReg(x);
            setResult(x, reg);
            if (genCode) {
                codeEmit().fastCallConvertOp(op, value, reg, is32bitMode());
            }
            value.dispose();
        } else if ((op == Bytecodes._l2f) || (op == Bytecodes._l2d)
                || (op == Bytecodes._f2l) || (op == Bytecodes._d2l)) {
            Item value = new Item(x.getValue());
            value.handleFloatKind();
            new ValueGen(value, HintItem.NO_HINT, this);
            if (genCode) {
                codeEmit().callConvertProlog(x.getType());
            }
            loadItem(value);
            freeReg(value);
            RInfo reg = lockResultReg(x);
            setResult(x, reg);
            if (genCode) {
                codeEmit().pushItem(value);
                codeEmit().callConvertOp(op);
                codeEmit().callConvertEpilog(x.getType());
                if (x.getType().isFloatKind()) {
                    codeEmit().setFpuResult(result.getRInfo());
                }
            }
            if ((op == Bytecodes._l2d) || (op == Bytecodes._l2f)) {
                spillItem(result);
            }
            value.dispose();
        } else {
            Item value = new Item(x.getValue());
            value.handleFloatKind();
            new ValueGen(value, valueHint, this);
            if (op == Bytecodes._i2b) {
                loadByteItem(value);
            } else if (value.isConstant()) {
                loadItem(value);
            } else if ((op != Bytecodes._i2f) && (op != Bytecodes._i2d)) {
                loadItem(value);
            } else {
                dontLoadItem(value);
            }
            freeItem(value);
            RInfo reg = lockReg(x);
            setResult(x, reg);
            if (genCode) {
                codeEmit().convertOp(op, reg, value);
            }
            if ((op == Bytecodes._d2f) || mustRound(x, hint)) {
                spillItem(result);
            }
            value.dispose();
        }
    }

    /**
     * Jumps to the default successor if it is not the immediate successor in
     * code.
     *
     * @param  blockEnd  end of the basic block to leave
     */
    private void gotoDefaultSux(BlockEnd blockEnd) {
        BlockBegin defaultSux = blockEnd.defaultSux();
        boolean isBackward = isBackwardBranch(blockEnd, defaultSux);
        List oopRegs = isBackward ? getBlockItemOopRegs() : null;
        if (block().getWeight() + 1 != defaultSux.getWeight()) {
            codeEmit().gotoOp(defaultSux, blockEnd.getBci(), isBackward, oopRegs);
        }
    }

    public void doGoto(Goto x) {
        boolean isBackward = isBackwardBranch(x, x.defaultSux());
        setNoResult(x);
        moveToPhi(x.getState());
        if (genCode) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty at backward branch");
            }
            if (isBackward && (x.defaultSux().getBci() == x.getBci())) {
                codeEmit().addNop();
            }
            gotoDefaultSux(x);
        }
    }

    public void doIf(If x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.countSux() == 2, "invalid number of successors");
        }
        int tag = x.getX().getType().getTag();
        boolean isBackward = isBackwardBranch(x, x.suxAt(0))
            || isBackwardBranch(x, x.suxAt(1));
        HintItem leftHint = new HintItem(x.getX().getType());
        leftHint.setFromItem(HintItem.NO_HINT);
        HintItem rightHint = new HintItem(x.getY().getType());
        rightHint.setFromItem(HintItem.NO_HINT);
        if (x.getX().getType() instanceof FloatType) {
            leftHint.setRound32(true);
            rightHint.setRound32(true);
        }
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        int cond = x.getCond();
        new ValueGen(left, leftHint, this);
        new ValueGen(right, rightHint, this);
        if (tag == ValueType.longTag) {
            if ((cond == If.GT) || (cond == If.LE)) {
                cond = If.MIRROR[cond];
                Item temp = left;
                left = right;
                right = temp;
            }
            left.setDestroysRegister(true);
        } else if ((tag == ValueType.floatTag) || (tag == ValueType.doubleTag)) {
            left.setDestroysRegister(true);
            right.setDestroysRegister(true);
        }
        loadItem(left);
        if ((tag == ValueType.longTag) || (tag == ValueType.floatTag)
                || (tag == ValueType.doubleTag)) {
            loadItem(right);
        } else {
            dontLoadItem(right);
        }
        freeReg(left);
        freeItem(right);
        setNoResult(x);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!genCode || !codeEmit().isLockedCC(), "cc register must be unlocked");
        }
        List oopRegs = (genCode && isBackward) ? getBlockItemOopRegs() : null;
        if (genCode) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty at backward branch");
            }
            cond = codeEmit().if1(cond, tag, left, right);
            codeEmit().lockCC();
        }
        moveToPhi(x.getState());
        if (genCode) {
            codeEmit().if2(cond, tag, x.trueSux(), x.falseSux(),
                x.unorderedSux(), x.getBci(), isBackward, oopRegs);
            codeEmit().unlockCC();
            gotoDefaultSux(x);
        }
    }

    public void doIfOp(IfOp x) {
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        HintItem leftHint = new HintItem(x.getX().getType());
        leftHint.setFromItem(HintItem.NO_HINT);
        HintItem rightHint = new HintItem(x.getY().getType());
        rightHint.setFromItem(HintItem.NO_HINT);
        if (x.getX().getType() instanceof FloatType) {
            leftHint.setRound32(true);
            rightHint.setRound32(true);
        }
        new ValueGen(left, leftHint, this);
        new ValueGen(right, rightHint, this);
        loadItem(left);
        loadItem(right);
        freeReg(left);
        freeReg(right);
        if (genCode) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!codeEmit().isLockedCC(), "cc register must be unlocked");
            }
            codeEmit().ifop1(left, right);
            codeEmit().lockCC();
        }
        Instruction tval = x.getTrueVal();
        Instruction fval = x.getFalseVal();
        Item tvalItem = new Item(tval);
        Item fvalItem = new Item(fval);
        HintItem tvalHint = new HintItem(tval.getType());
        if ((tval instanceof LoadLocal) || (tval instanceof Constant)) {
            tvalHint.setFromItem(hint);
        } else {
            tvalHint.setFromItem(HintItem.NO_HINT);
        }
        HintItem fvalHint = new HintItem(fval.getType());
        if ((fval instanceof LoadLocal) || (fval instanceof Constant)) {
            fvalHint.setFromItem(hint);
        } else {
            fvalHint.setFromItem(HintItem.NO_HINT);
        }
        new ValueGen(tvalItem, tvalHint, this);
        new ValueGen(fvalItem, fvalHint, this);
        dontLoadItem(tvalItem);
        dontLoadItem(fvalItem);
        RInfo reg = null;
        if (fval.getType().getTag() == ValueType.longTag) {
            reg = lockReg(x, hint);
            setResult(x, reg);
        }
        freeItem(tvalItem);
        freeItem(fvalItem);
        if (fval.getType().getTag() != ValueType.longTag) {
            reg = lockReg(x, hint);
            setResult(x, reg);
        }
        if (genCode) {
            codeEmit().ifop2(reg, tvalItem, fvalItem, x.getCond());
            codeEmit().unlockCC();
        }
        left.dispose();
        right.dispose();
        tvalItem.dispose();
        fvalItem.dispose();
    }

    public void doInstanceOf(InstanceOf x) {
        spillValuesOnStack(x.getState());
        Item obj = new Item(x.getObj());
        obj.setDestroysRegister(true);
        new ValueGen(obj, HintItem.NO_HINT, this);
        RInfo reg = lockReg(x);
        setResult(x, reg);
        loadItem(obj);
        RInfo tmp = getFreeReg(ValueType.objectType);
        freeReg(obj);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().instanceofOp(reg, obj, x.getKlass(), tmp, info);
        }
        obj.dispose();
    }

    public void doIntrinsic(Intrinsic x) {
        switch (x.getIntrinsicId()) {
        case Intrinsic.DSIN:
            /* falls through */
        case Intrinsic.DCOS:
            /* falls through */
        case Intrinsic.DSQRT:
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(x.countArguments() == 1, "wrong type");
            }
            Item value = new Item(x.argumentAt(0));
            value.setDestroysRegister(true);
            new ValueGen(value, hint, this);
            loadItemHint(value, hint);
            freeReg(value);
            RInfo reg = lockReg(x, hint);
            setResult(x, reg);
            if (genCode) {
                codeEmit().mathIntrinsic(x.getIntrinsicId(), reg, value);
            }
            if (mustRound(x, hint)) {
                spillItem(result);
            }
            value.dispose();
            break;
        case Intrinsic.ARRAYCOPY:
            spillValuesOnStack(x.getState());
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(x.countArguments() == 5, "wrong type");
            }
            Item src = new Item(x.argumentAt(0));
            Item srcPos = new Item(x.argumentAt(1));
            Item dst = new Item(x.argumentAt(2));
            Item dstPos = new Item(x.argumentAt(3));
            Item length = new Item(x.argumentAt(4));
            new ValueGen(src, HintItem.NO_HINT, this);
            new ValueGen(srcPos, HintItem.NO_HINT, this);
            new ValueGen(dst, HintItem.NO_HINT, this);
            new ValueGen(dstPos, HintItem.NO_HINT, this);
            new ValueGen(length, HintItem.NO_HINT, this);
            loadItem(src);
            if (!srcPos.isConstant()) {
                loadItem(srcPos);
            }
            loadItem(dst);
            if (!dstPos.isConstant()) {
                loadItem(dstPos);
            }
            loadItem(length);
            RInfo tmp = getFreeReg(ValueType.intType);
            freeItem(src);
            freeItem(srcPos);
            freeItem(dst);
            freeItem(dstPos);
            freeItem(length);
            setNoResult(x);
            if (genCode) {
                CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
                codeEmit().arraycopy(src, srcPos, dst, dstPos, length, tmp, info);
            }
            src.dispose();
            srcPos.dispose();
            dst.dispose();
            dstPos.dispose();
            length.dispose();
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Visits all arguments and returns items without loading them.
     *
     * @param   x  method call instruction
     * @return  list of argument items
     */
    private List invokeVisitArguments(Invoke x) {
        List args = new ArrayList();
        for (int i = 0; i < x.countArguments(); i++) {
            Item param = new Item(x.argumentAt(i));
            args.add(param);
            new ValueGen(param, HintItem.NO_HINT, this);
        }
        return args;
    }

    /**
     * Loads the items in the specified argument list.
     *
     * @param  x     method call instruction
     * @param  args  list of arguments to be loaded
     */
    private void invokeLoadArguments(Invoke x, List args) {
        for (int i = 0; i < x.countArguments(); i++) {
            Item param = (Item) args.get(i);
            param.handleFloatKind();
            if (param.getType().getTag() == ValueType.longTag) {
                loadItem(param);
            } else {
                dontLoadItem(param);
            }
            freeItem(param);
            if (genCode) {
                codeEmit().pushItem(param);
            }
            param.dispose();
        }
    }

    /**
     * Handles the arguments of the specified method call without receiver.
     *
     * @param  x  method call instruction
     */
    private void invokeDoArguments(Invoke x) {
        for (int i = 0; i < x.countArguments(); i++) {
            Item param = new Item(x.argumentAt(i));
            param.handleFloatKind();
            new ValueGen(param, HintItem.NO_HINT, this);
            if (param.getType().getTag() == ValueType.longTag) {
                loadItem(param);
            } else {
                dontLoadItem(param);
            }
            freeItem(param);
            if (genCode) {
                codeEmit().pushItem(param);
            }
            param.dispose();
        }
    }

    /**
     * Ensures that the values on the stack are spilled.
     *
     * @param  x  method call instruction
     */
    private void invokeDoSpill(Invoke x) {
        spillValuesOnStack(x.getState());
    }

    /**
     * Sets up the result register for the specified method call.
     *
     * @param  x               method call instruction
     * @param  needsNullCheck  whether or not receiver is checked for null
     */
    private void invokeDoResult(Invoke x, boolean needsNullCheck) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "registers must be free across calls");
        }
        if (x.getType() instanceof VoidType) {
            setNoResult(x);
        } else {
            RInfo reg = lockResultReg(x);
            setResult(x, reg);
        }
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().callOp(x.getCode(), null, x.getArgSize(), info,
                (x.isTargetLoaded() && x.isTargetFinal()), needsNullCheck);
            if (x.getType().isFloatKind()) {
                codeEmit().setFpuResult(result.getRInfo());
            }
        }
    }

    public void doInvoke(Invoke x) {
        if (x.hasReceiver()) {
            boolean needsNullCheck = false;
            HintItem hint = new HintItem(x.getReceiver().getType(), RECV_REG);
            Item receiver = new Item(x.getReceiver());
            receiver.setDestroysRegister(true);
            new ValueGen(receiver, hint, this);
            List args = invokeVisitArguments(x);
            loadItemHint(receiver, hint);
            freeReg(receiver);
            if (genCode) {
                if (!javac1.Flags.ImplicitNullChecks) {
                    CodeEmitInfo info = emitInfo(x.getBci(), null);
                    codeEmit().explicitNullCheck(receiver, info);
                }
                codeEmit().pushItem(receiver);
                needsNullCheck = itemMayBeNull(receiver)
                    && ((x.getCode() == Bytecodes._invokespecial)
                    || !x.isTargetLoaded() || x.isTargetFinal());
            }
            RInfo reg = hideReg(receiver.getRInfo(), false);
            invokeLoadArguments(x, args);
            showReg(reg);
            regAlloc().setLockingLocked(true);
            invokeDoSpill(x);
            regAlloc().setLockingLocked(false);
            if (genCode && !receiver.getRInfo().equals(hint.getRInfo())) {
                codeEmit().itemToReg(hint.getRInfo(), receiver);
            }
            invokeDoResult(x, needsNullCheck);
            receiver.dispose();
        } else {
            invokeDoArguments(x);
            invokeDoSpill(x);
            invokeDoResult(x, false);
        }
    }

    public void doJsr(Jsr x) {
        setNoResult(x);
        if (!x.getState().isStackEmpty()) {
            moveToPhi(x.getState());
        }
        if (genCode) {
            codeEmit().jsrOp(x.getSubroutine(), x.getBci());
        }
    }

    public void doJsrContinuation(JsrContinuation x) {
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().addNopWithInfo(info);
        }
    }

    public void doLoadField(LoadField x) {
        boolean needsPatching = !x.isLoaded() || !x.isInitialized();
        Item object = new Item(x.getObj());
        new ValueGen(object, HintItem.NO_HINT, this);
        if (needsPatching && x.isStatic()) {
            loadItemPatching(object);
        } else {
            loadItem(object);
        }
        List oopRegs = needsPatching ? regAlloc().oopsInRegisters() : null;
        freeReg(object);
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(object, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null, oopRegs);
            codeEmit().fieldLoad(reg, x.getFieldType(), object, x.getOffset(),
                needsPatching, x.isLoaded(), info);
        }
        object.dispose();
    }

    public void doLoadIndexed(LoadIndexed x) {
        Item array = new Item(x.getArray());
        Item index = new Item(x.getIndex());
        new ValueGen(array, HintItem.NO_HINT, this);
        new ValueGen(index, HintItem.NO_HINT, this);
        loadItem(array);
        if (!index.isConstant()) {
            loadItem(index);
            freeReg(index);
        }
        if (!(x.getType() instanceof LongType)) {
            freeReg(array);
        }
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(array, info);
            }
            if (javac1.Flags.GenerateRangeChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().rangeCheck(array, index, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().indexedLoad(reg, x.getElemType(), array, index, info);
        }
        if (x.getType() instanceof LongType) {
            freeReg(array);
        }
        array.dispose();
        index.dispose();
    }

    public void doLoadLocal(LoadLocal x) {
        RInfo reg = cachedLocal(x.getIndex());
        if (reg.isValid()) {
            result.setRInfo(reg, true);
        } else {
            result.setStack(x.getIndex());
        }
    }

    public void doLogicOp(LogicOp x) {
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        left.setDestroysRegister(true);
        new ValueGen(left, hint, this);
        new ValueGen(right, HintItem.NO_HINT, this);
        loadItemHint(left, hint);
        if (!right.isConstant()) {
            loadItem(right);
        } else {
            dontLoadItem(right);
        }
        freeReg(left);
        if (!right.isConstant()) {
            freeReg(right);
        }
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            codeEmit().logicOp(x.getOp(), reg, left, right);
        }
        left.dispose();
        right.dispose();
    }

    /**
     * Returns the list of key ranges in the specified lookup switch.
     *
     * @param   x  lookup switch instruction
     * @return  list of lookup ranges
     */
    private List createLookupRanges(LookupSwitch x) {
        List ranges = new ArrayList();
        int len = x.getLength();
        if (len > 0) {
            int key = x.keyAt(0);
            BlockBegin sux = x.suxAt(0);
            LookupRange range = new LookupRange(key, sux);
            ranges.add(range);
            for (int i = 1; i < len; i++) {
                int nextKey = x.keyAt(i);
                BlockBegin nextSux = x.suxAt(i);
                if ((nextKey == key + 1) && (nextSux == sux)) {
                    range.setHighKey(nextKey);
                } else {
                    range = new LookupRange(nextKey, nextSux);
                    ranges.add(range);
                }
                key = nextKey;
                sux = nextSux;
            }
        }
        return ranges;
    }

    /**
     * Emits code for the specified range of jump table keys.
     *
     * @param  tag    item to be looked up in table
     * @param  x      lookup switch instruction
     * @param  range  range of keys to generate code for
     */
    private void doLookupSwitchKey(Item tag, LookupSwitch x, LookupRange range) {
        boolean isBackward = isBackwardBranch(x, range.getSux());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty on switch");
        }
        List oopRegs = isBackward ? getBlockItemOopRegs() : null;
        codeEmit().lookupswitchRangeOp(tag, range.getLowKey(), range.getHighKey(),
            range.getSux(), x.getBci(), isBackward, oopRegs);
    }

    /**
     * Jumps to the default successor of the specified jump table.
     *
     * @param  x  lookup switch instruction
     */
    private void doLookupSwitchDefault(LookupSwitch x) {
        boolean isBackward = isBackwardBranch(x, x.defaultSux());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty on switch");
        }
        List oopRegs = isBackward ? getBlockItemOopRegs() : null;
        codeEmit().gotoOp(x.defaultSux(), x.getBci(), isBackward, oopRegs);
    }

    /**
     * Sets up the phi values for a switch instruction.
     *
     * @param  tag    item to be looked up
     * @param  stack  the value stack
     */
    private void setupPhisForSwitch(Item tag, ValueStack stack) {
        if ((stack != null) && !stack.isStackEmpty()) {
            if (genCode) {
                codeEmit().pushItem(tag);
            }
            moveToPhi(stack);
            RInfo reg = hideReg(NO_RET_REG, false);
            tag.setRInfo(reg);
            if (genCode) {
                codeEmit().popItem(tag);
            }
            showReg(reg);
        }
    }

    public void doLookupSwitch(LookupSwitch x) {
        Item tag = new Item(x.getTag());
        new ValueGen(tag, HintItem.NO_HINT, this);
        loadItem(tag);
        freeReg(tag);
        result.setNoResult();
        setupPhisForSwitch(tag, x.getState());
        List ranges = createLookupRanges(x);
        if (genCode) {
            int len = ranges.size();
            for (int i = 0; i < len; i++) {
                LookupRange range = (LookupRange) ranges.get(i);
                doLookupSwitchKey(tag, x, range);
            }
            doLookupSwitchDefault(x);
            codeEmit().checkCodespace();
        }
        tag.dispose();
    }

    public void doLoopEnter(LoopEnter x) {
        if (genCode) {
            BlockItem blockItem = block().getBlockItem();
            boolean instrAdded = false;
            if ((blockItem != null) && blockItem.is32bitPrecision()) {
                codeEmit().set32bitFpuPrecision();
                instrAdded = true;
            }
            if ((blockItem != null) && (blockItem.mapLength() != 0)) {
                for (int i = blockItem.mapLength() - 1; i >= 0; i--) {
                    RInfo reg = blockItem.getCacheReg(i);
                    if (reg.isValid()) {
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that(reg.isWord(), "invalid register");
                        }
                        codeEmit().localToReg(reg, i);
                    }
                }
            } else if (!instrAdded) {
                codeEmit().addNop();
            }
        }
    }

    public void doLoopExit(LoopExit x) {
        if (genCode) {
            BlockItem blockItem = block().getBlockItem();
            boolean instrAdded = false;
            if ((blockItem != null) && blockItem.is32bitPrecision()) {
                codeEmit().restoreFpuPrecision();
                instrAdded = true;
            }
            if ((blockItem != null) && (blockItem.mapLength() != 0)) {
                for (int i = blockItem.mapLength() - 1; i >= 0; i--) {
                    RInfo reg = blockItem.getCacheReg(i);
                    if (reg.isValid()) {
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that(reg.isWord(), "invalid register");
                        }
                        codeEmit().regToLocal(i, reg);
                    }
                }
            } else if (!instrAdded) {
                codeEmit().addNop();
            }
        }
    }

    public void doMonitorEnter(MonitorEnter x) {
        spillValuesOnStack(x.getState());
        lockSpillReg(SYNC_TMP_REG, null, 1);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.isRoot(), "must be root");
        }
        Item obj = new Item(x.getObj());
        new ValueGen(obj, HintItem.NO_HINT, this);
        loadItem(obj);
        RInfo lock = getFreeReg(ValueType.intType);
        freeReg(obj);
        regAlloc().freeReg(SYNC_TMP_REG);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "slow case does not preserve registers");
        }
        setNoResult(x);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(obj, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().monitorenter(obj, lock, SYNC_TMP_REG, x.getMonitorNo(), info);
        }
        obj.dispose();
    }

    public void doMonitorExit(MonitorExit x) {
        spillValuesOnStack(x.getState());
        lockSpillReg(SYNC_TMP_REG, null, 1);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.isRoot(), "must be root");
        }
        Item obj = new Item(x.getObj());
        new ValueGen(obj, HintItem.NO_HINT, this);
        loadItem(obj);
        RInfo lock = getFreeReg(ValueType.intType);
        freeReg(obj);
        regAlloc().freeReg(SYNC_TMP_REG);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "slow case does not preserve registers");
        }
        setNoResult(x);
        if (genCode) {
            codeEmit().monitorexit(obj, lock, SYNC_TMP_REG, x.getMonitorNo());
        }
    }

    public void doNegateOp(NegateOp x) {
        Item value = new Item(x.getX());
        value.setDestroysRegister(true);
        new ValueGen(value, hint, this);
        loadItemHint(value, hint);
        freeReg(value);
        RInfo reg = lockReg(x, hint);
        setResult(x, reg);
        if (genCode) {
            codeEmit().negate(reg, value);
        }
        if (mustRound(x, hint)) {
            spillItem(result);
        }
    }

    public void doNewInstance(NewInstance x) {
        spillValuesOnStack(x.getState());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "registers must be free across calls");
        }
        RInfo reg = resultRegisterFor(x.getType());
        regAlloc().lockReg(reg, x, x.getUseCount());
        setResult(x, reg);
        RInfo tmp1 = hideReg(ValueType.objectType);
        RInfo tmp2 = hideReg(ValueType.objectType);
        RInfo tmp3 = hideReg(ValueType.objectType);
        RInfo klassReg = hideReg(ValueType.objectType);
        showReg(tmp1);
        showReg(tmp2);
        showReg(tmp3);
        showReg(klassReg);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().newInstance(reg, x.getKlass(), tmp1, tmp2, tmp3,
                klassReg, info);
        }
    }

    public void doNewMultiArray(NewMultiArray x) {
        spillValuesOnStack(x.getState());
        List dims = x.getDims();
        for (int i = dims.size() - 1; i >= 0; i--) {
            Item size = new Item((Instruction) dims.get(i));
            new ValueGen(size, HintItem.NO_HINT, this);
            loadItem(size);
            freeReg(size);
            if (genCode) {
                codeEmit().pushItem(size);
            }
            size.dispose();
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "registers must be free across calls");
        }
        RInfo reg = resultRegisterFor(x.getType());
        regAlloc().lockReg(reg, x, x.getUseCount());
        setResult(x, reg);
        RInfo tmp = getFreeReg(ValueType.intType);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().newMultiArray(reg, x.getKlass(), x.getRank(), tmp, info);
        }
    }

    public void doNewObjectArray(NewObjectArray x) {
        spillValuesOnStack(x.getState());
        Item length = new Item(x.getLength());
        new ValueGen(length, HintItem.NO_HINT, this);
        loadItem(length);
        freeReg(length);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "registers must be free across calls");
        }
        RInfo reg = resultRegisterFor(x.getType());
        regAlloc().lockReg(reg, x, x.getUseCount());
        setResult(x, reg);
        RInfo tmp1 = hideReg(ValueType.objectType);
        RInfo tmp2 = hideReg(ValueType.objectType);
        RInfo tmp3 = hideReg(ValueType.objectType);
        showReg(tmp1);
        showReg(tmp2);
        showReg(tmp3);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().newObjectArray(reg, x.getKlass(), length, tmp1, tmp2,
                tmp3, info);
        }
        length.dispose();
    }

    public void doNewTypeArray(NewTypeArray x) {
        spillValuesOnStack(x.getState());
        Item length = new Item(x.getLength());
        new ValueGen(length, HintItem.NO_HINT, this);
        loadItem(length);
        freeReg(length);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllRegsFree(), "registers must be free across calls");
        }
        RInfo reg = resultRegisterFor(x.getType());
        regAlloc().lockReg(reg, x, x.getUseCount());
        setResult(x, reg);
        RInfo tmp1 = hideReg(ValueType.objectType);
        RInfo tmp2 = hideReg(ValueType.objectType);
        RInfo klassReg = hideReg(ValueType.objectType);
        showReg(tmp1);
        showReg(tmp2);
        showReg(klassReg);
        if (genCode) {
            CodeEmitInfo info = emitInfo(x.getBci(), x.getState());
            codeEmit().newTypeArray(reg, x.getElemType(), length, tmp1, tmp2,
                klassReg, info);
        }
        length.dispose();
    }

    public void doNullCheck(NullCheck x) {
        Item value = new Item(x.getObj());
        new ValueGen(value, HintItem.NO_HINT, this);
        loadItem(value);
        freeItem(value);
        if (genCode && itemMayBeNull(value)) {
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().implicitNullCheck(value, info);
        }
    }

    public void doPhi(Phi x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.isRoot(), "phi must be root");
            Assert.that(x.getItem() != null, "item must be set by block prolog");
        }
    }

    public void doRet(Ret x) {
        setNoResult(x);
        if (genCode) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(x.getState().isStackEmpty(), "stack not empty on ret");
            }
            RInfo reg = cachedLocal(x.getIndex());
            codeEmit().retOp(x.getIndex(), reg, x.getBci());
        }
    }

    public void doReturn(Return x) {
        if (x.getType() instanceof VoidType) {
            if (genCode) {
                if (method().isSynchronized()) {
                    RInfo receiver = cachedLocal(0);
                    codeEmit().returnOpProlog(x.getMonitorNo(), receiver);
                }
                codeEmit().returnOp(null);
            }
        } else {
            RInfo reg = resultRegisterFor(x.getType());
            Item res = new Item(x.getResult());
            res.handleFloatKind();
            HintItem resultHint = new HintItem(x.getType(), reg);
            if (x.getType() instanceof FloatType) {
                resultHint.setRound32(true);
            }
            new ValueGen(res, resultHint, this);
            boolean resultIsLocal0 = !method().isStatic()
                && (x.getResult() instanceof LoadLocal)
                && (((LoadLocal) x.getResult()).getIndex() == 0);
            boolean resultReleased = false;
            boolean cachedReceiver = resultIsLocal0 && res.isRegister();
            if (cachedReceiver) {
                loadItem(res);
                freeItem(res);
                resultReleased = true;
            }
            if (x.isSynchronized()) {
                if (res.isRegister() && !resultIsLocal0) {
                    spillValue(x.getResult());
                    res.update();
                }
                lockSpillReg(SYNC_TMP_REG, null, 1);
                regAlloc().freeReg(SYNC_TMP_REG);
                if (genCode) {
                    RInfo receiver = cachedLocal(0);
                    codeEmit().returnOpProlog(x.getMonitorNo(), receiver);
                }
            }
            if (!cachedReceiver) {
                if ((res.getType() instanceof LongType) && res.isRegister()) {
                    RInfo resReg = res.getRInfo();
                    if (!resReg.equals(reg) && resReg.overlaps(reg)) {
                        spillValue(x.getResult());
                        res.update();
                    }
                }
                loadItemForce(res, reg);
                freeReg(res);
            } else if (!resultReleased) {
                Assert.shouldNotReachHere();
                freeItem(result);
            }
            if (genCode) {
                if (resultIsLocal0) {
                    codeEmit().loadReceiver(reg.getRegister());
                }
                codeEmit().returnOp(res);
            }
            res.dispose();
        }
        setNoResult(x);
    }

    public void doShiftOp(ShiftOp x) {
        Item value = new Item(x.getX());
        Item count = new Item(x.getY());
        value.setDestroysRegister(true);
        HintItem countHint = new HintItem(x.getY().getType(), SHIFT_COUNT_REG);
        new ValueGen(value, hint, this);
        new ValueGen(count, countHint, this);
        int tag = x.getType().getTag();
        if (!count.isConstant() || (tag == ValueType.longTag)) {
            loadItemHint(count, countHint);
        } else {
            dontLoadItem(count);
        }
        loadItemHint(value, hint);
        RInfo tmp = RInfo.NO_RINFO;
        if ((tag == ValueType.intTag) && count.isRegister()) {
            tmp = getFreeReg(ValueType.intType);
        }
        freeItem(count);
        freeReg(value);
        RInfo reg = lockReg(x, value);
        setResult(x, reg);
        if (genCode) {
            codeEmit().shiftOp(x.getOp(), reg, value, count, tmp);
        }
        value.dispose();
        count.dispose();
    }

    public void doStoreField(StoreField x) {
        boolean needsPatching = !x.isLoaded() || !x.isInitialized();
        Item object = new Item(x.getObj());
        Item value = new Item(x.getValue());
        value.handleFloatKind();
        int valueTag = x.getType().getTag();
        if (valueTag == ValueType.objectTag) {
            object.setDestroysRegister(true);
        }
        new ValueGen(object, HintItem.NO_HINT, this);
        new ValueGen(value, HintItem.NO_HINT, this);
        if (needsPatching && x.isStatic()) {
            loadItemPatching(object);
        } else {
            loadItem(object);
        }
        RInfo tmp = RInfo.NO_RINFO;
        if (!needsPatching && value.isStack() && x.getType().isFloatKind()) {
            ValueType type = null;
            if (valueTag == ValueType.floatTag) {
                type = ValueType.intType;
            } else {
                type = ValueType.longType;
            }
            tmp = getFreeReg(type);
        } else if (!value.isConstant() || needsPatching) {
            loadItem(value);
        } else {
            dontLoadItem(value);
        }
        List oopRegs = needsPatching ? regAlloc().oopsInRegisters() : null;
        freeItem(value);
        freeItem(object);
        setNoResult(x);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(object, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null, oopRegs);
            codeEmit().fieldStore(x.getFieldType(), object, x.getOffset(),
                value, needsPatching, x.isLoaded(), info, tmp);
        }
        object.dispose();
        value.dispose();
    }

    public void doStoreIndexed(StoreIndexed x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x.isRoot(), "must be root");
        }
        int elemType = x.getElemType();
        boolean objStore = BasicType.isOop(elemType);
        Item array = new Item(x.getArray());
        if (objStore) {
            array.setDestroysRegister(true);
        }
        Item index = new Item(x.getIndex());
        Item value = new Item(x.getValue());
        value.handleFloatKind();
        new ValueGen(array, HintItem.NO_HINT, this);
        new ValueGen(index, HintItem.NO_HINT, this);
        new ValueGen(value, HintItem.NO_HINT, this);
        loadItem(array);
        if (!index.isConstant()) {
            loadItem(index);
        }
        boolean mustLoad = true;
        if ((elemType == BasicType.SHORT) || (elemType == BasicType.CHAR)) {
            mustLoad = true;
        } else if (value.isConstant() && !objStore) {
            mustLoad = false;
        }
        if (mustLoad) {
            if (elemType == BasicType.BYTE) {
                loadByteItem(value);
            } else {
                loadItem(value);
            }
        }
        List oopRegs = objStore ? regAlloc().oopsInRegisters() : null;
        RInfo tmp1 = hideReg(ValueType.objectType);
        RInfo tmp2 = hideReg(ValueType.objectType);
        freeItem(index);
        freeItem(value);
        freeReg(array);
        showReg(tmp1);
        showReg(tmp2);
        setNoResult(x);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(array, info);
            }
            if (javac1.Flags.GenerateRangeChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().rangeCheck(array, index, info);
            }
            if (objStore && !value.isConstant()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!tmp1.equals(tmp2), "wrong allocation");
                }
                CodeEmitInfo info = emitInfo(x.getBci(), x.getState(), oopRegs);
                codeEmit().arrayStoreCheck(array, value, index, tmp1, tmp2, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().indexedStore(elemType, array, index, value, info);
        }
        array.dispose();
        index.dispose();
        value.dispose();
    }

    /**
     * Tests if the value to be stored is a result of an arithmetic operation
     * that can be combined with the specified store instruction.
     *
     * @param  x  store instruction to be examined
     */
    private boolean canBeShortFormArithmetic(StoreLocal x) {
        if (x.getValue() instanceof Op2) {
            Op2 op2 = (Op2) x.getValue();
            int op = op2.getOp();
            return !op2.isRoot() && (op2.getType() instanceof IntType)
                && (op2.getX() instanceof LoadLocal)
                && (((LoadLocal) op2.getX()).getIndex() == x.getIndex())
                && cachedLocal(x.getIndex()).isValid()
                && (op2 instanceof ArithmeticOp)
                && ((op == Bytecodes._iadd) || (op == Bytecodes._isub));
        } else {
            return false;
        }
    }

    /**
     * Generates optimized code for an arithmetic operation whose result
     * replaces the left operand.
     *
     * @param  x    the arithmetic operation
     * @param  reg  register that caches left operand
     */
    private void doShortArithmeticOp(ArithmeticOp x, RInfo reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((x.getOp() == Bytecodes._iadd) || (x.getOp() == Bytecodes._isub), "invalid operation code");
            Assert.that(!hint.hasHint() && !x.isRoot(), "should not reach here");
            Assert.that(reg.isValid(), "invalid register");
        }
        Item left = new Item(x.getX());
        Item right = new Item(x.getY());
        left.setDestroysRegister(true);
        new ValueGen(right, HintItem.NO_HINT, this);
        new ValueGen(left, HintItem.NO_HINT, this);
        dontLoadItem(right);
        dontLoadItem(left);
        freeItem(right);
        freeItem(left);
        HintItem leftHint = new HintItem(x.getX().getType(), reg);
        leftHint.setDestroysRegister(true);
        if (genCode) {
            codeEmit().arithmeticOpInt(x.getOp(), leftHint, leftHint, right);
        }
        left.dispose();
        right.dispose();
    }

    public void doStoreLocal(StoreLocal x) {
        HintItem theHint = new HintItem(x.getType());
        setNoResult(x);
        if (canBeShortFormArithmetic(x)) {
            ArithmeticOp arithOp = (ArithmeticOp) x.getValue();
            RInfo reg = cachedLocal(x.getIndex());
            Item value = new Item(arithOp);
            doShortArithmeticOp(arithOp, reg);
            if (genCode && !reg.isValid()) {
                codeEmit().itemToLocal(x.getIndex(), value);
            }
            value.dispose();
        } else {
            Item value = new Item(x.getValue());
            value.handleFloatKind();
            new ValueGen(value, theHint, this);
            if (!value.isConstant()) {
                loadItem(value);
                freeReg(value);
            }
            if (genCode) {
                RInfo reg = cachedLocal(x.getIndex());
                if (reg.isValid()) {
                    codeEmit().itemToReg(reg, value);
                } else {
                    codeEmit().itemToLocal(x.getIndex(), value);
                }
            }
            value.dispose();
        }
    }

    public void doTableSwitch(TableSwitch x) {
        Item tag = new Item(x.getTag());
        new ValueGen(tag, HintItem.NO_HINT, this);
        loadItem(tag);
        freeReg(tag);
        setNoResult(x);
        setupPhisForSwitch(tag, x.getState());
        if (genCode) {
            int loKey = x.getLoKey();
            int len = x.getLength();
            for (int i = 0; i < len; i++) {
                int key = loKey + i;
                boolean isBackward = isBackwardBranch(x, x.suxAt(i));
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty on switch");
                }
                List oopRegs = isBackward ? getBlockItemOopRegs() : null;
                codeEmit().tableswitchOp(tag, key, x.suxAt(i), x.getBci(),
                    isBackward, oopRegs);
                codeEmit().checkCodespace();
            }
            boolean isBackward = isBackwardBranch(x, x.defaultSux());
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!isBackward || x.getState().isStackEmpty(), "stack not empty on switch");
            }
            List oopRegs = isBackward ? getBlockItemOopRegs() : null;
            codeEmit().gotoOp(x.defaultSux(), x.getBci(), isBackward, oopRegs);
        }
        tag.dispose();
    }

    public void doThrow(Throw x) {
        if (!x.getState().isStackEmpty()) {
            releaseRoots(x.getState());
        }
        Item exception = new Item(x.getException());
        new ValueGen(exception, HintItem.NO_HINT, this);
        loadItem(exception);
        freeReg(exception);
        setNoResult(x);
        if (genCode) {
            if (!javac1.Flags.ImplicitNullChecks) {
                CodeEmitInfo info = emitInfo(x.getBci(), null);
                codeEmit().explicitNullCheck(exception, info);
            }
            CodeEmitInfo info = emitInfo(x.getBci(), null);
            codeEmit().throwOp(exception, info);
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(regAlloc().areAllFree(), "still some values in registers");
            Assert.that(!genCode || codeEmit().isFpuStackEmpty(), "still some values on FPU stack");
        }
    }
}
