/*
 * @(#)CodeEmitter.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import javac1.Assert;
import javac1.BasicType;
import javac1.Bytecodes;
import javac1.backend.BackEnd;
import javac1.backend.debug.*;
import javac1.backend.items.Item;
import javac1.backend.reg.CachedLocals;
import javac1.backend.reg.RInfo;
import javac1.backend.stubs.*;
import javac1.ci.ArrayKlass;
import javac1.ci.InstanceKlass;
import javac1.ci.Klass;
import javac1.ci.Method;
import javac1.ci.Obj;
import javac1.ci.Runtime1;
import javac1.ir.IR;
import javac1.ir.ScanBlocks;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.If;
import javac1.ir.instr.Intrinsic;
import javac1.ir.types.DoubleType;
import javac1.ir.types.FloatType;
import javac1.ir.types.IntType;
import javac1.ir.types.LongType;
import javac1.ir.types.ObjectType;
import javac1.ir.types.ValueType;

/**
 * Provides high-level methods for code generation. These methods are called by
 * the value generator and in turn use methods of the macro assembler.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CodeEmitter {
    /**
     * The register that stores the inline cache class.
     */
    public static final Register IC_KLASS_REG = Register.EAX;

    /**
     * The register that must contain the count for shift operations.
     */
    public static final Register SHIFT_COUNT_REG = Register.ECX;

    /**
     * The register that stores the synchronization header.
     */
    public static final Register SYNC_HDR_REG = Register.EAX;

    /**
     * The register that the receiver is preloaded into.
     */
    public static final Register RECV_REG = Register.ECX;

    /**
     * The register that holds the exception object pointer.
     */
    public static final Register EXCEPT_OOP_REG = Register.EAX;

    /**
     * The maximum offset for field accesses in words.
     */
    public static final int MAX_OFFSET = (Integer.MAX_VALUE - 1) / 4;

    /**
     * The macro assembler to be used for code emission.
     */
    private MacroAssembler masm;

    /**
     * The method that is currently being compiled.
     */
    private Method method;

    /**
     * The information about the basic blocks in the control flow graph.
     */
    private ScanBlocks scanResult;

    /**
     * The frame map that maps items to their frame location.
     */
    private FrameMap frameMap;
    
    /**
     * The list of local variables that are cached in registers.
     */
    private CachedLocals cachedLocals;

    /**
     * The stack of floating-point registers.
     */
    private FpuStack fstack;

    /**
     * The table of floating-point constants.
     */
    private ConstantTable constTable;

    /**
     * The mapping of bytecode indices to cached object pointers.
     */
    private Map cachedOops;

    /**
     * The current stack pointer offset.
     */
    private int espOffset;

    /**
     * The entry point for throwing an exception.
     */
    private Label throwEntryLabel;

    /**
     * Whether or not the condition code register is locked.
     */
    private boolean ccLocked;

    /**
     * The list of stubs for slow cases.
     */
    private List slowCaseStubs;

    /**
     * The list of stubs for calls.
     */
    private List callStubs;
    
    /**
     * The offset descriptor.
     */
    private OffsetDesc offsets;

    /**
     * Constructs a new code emitter.
     *
     * @param  masm        the macro assembler
     * @param  ir          the intermediate representation
     * @param  frameMap    the frame map
     * @param  constTable  the constant table
     * @param  cachedOops  the cached object pointers
     */
    public CodeEmitter(MacroAssembler masm, Method method, ScanBlocks scanResult,
            FrameMap frameMap, ConstantTable constTable, Map cachedOops) {
        this.masm = masm;
        this.method = method;
        this.scanResult = scanResult;
        this.frameMap = frameMap;
        this.cachedLocals = frameMap.getCachedLocals();
        this.fstack = new FpuStack();
        this.constTable = constTable;
        this.offsets = masm.getCode().getOffsets();
        this.cachedOops = cachedOops;
        this.espOffset = 0;
        this.throwEntryLabel = new Label();
        this.ccLocked = false;
        this.slowCaseStubs = new ArrayList();
        this.callStubs = new ArrayList();
    }

    /**
     * Tests if the specified single-precision floating-point value is +0.0.
     *
     * @param   value  value to be compared with +0.0
     * @return  whether or not the value is +0.0
     */
    private static boolean isZeroFloat(float value) {
        return Float.floatToIntBits(value) == Float.floatToIntBits(0.0f);
    }

    /**
     * Tests if the specified single-precision floating-point value is +1.0.
     *
     * @param   value  value to be compared with +1.0
     * @return  whether or not the value is +1.0
     */
    private static boolean isOneFloat(float value) {
        return value == 1.0f;
    }

    /**
     * Tests if the specified double-precision floating-point value is +0.0.
     *
     * @param   value  value to be compared with +0.0
     * @return  whether or not the value is +0.0
     */
    private static boolean isZeroDouble(double value) {
        return Double.doubleToLongBits(value) == Double.doubleToLongBits(0.0d);
    }

    /**
     * Tests if the specified double-precision floating-point value is +1.0.
     *
     * @param   value  value to be compared with +1.0
     * @return  wheter or not the value is +1.0
     */
    private static boolean isOneDouble(double value) {
        return value == 1.0d;
    }

    /**
     * Returns the scaling factor for array elements of the specified type.
     *
     * @param   type  basic type of the array elements
     * @return  scaling factor for the elements
     */
    private static int arrayElementSize(int type) {
        switch(BasicType.ARRAY_ELEM_BYTES[type]) {
        case 1:
            return Address.TIMES_1;
        case 2:
            return Address.TIMES_2;
        case 4:
            return Address.TIMES_4;
        case 8:
            return Address.TIMES_8;
        default:
            Assert.shouldNotReachHere();
            return Address.NO_SCALE;
        }
    }

    /**
     * Converts a condition of the intermediate language into a condition of the
     * assembler.
     *
     * @param   cond    condition code to be converted
     * @param   signed  whether or not signed values are compared
     * @return  condition code of the assembler
     */
    private static int icond2acond(int cond, boolean signed) {
        switch (cond) {
        case If.EQ:
            return Assembler.EQUAL;
        case If.NE:
            return Assembler.NOT_EQUAL;
        case If.LT:
            return signed ? Assembler.LESS : Assembler.BELOW;
        case If.LE:
            return signed ? Assembler.LESS_EQUAL : Assembler.BELOW_EQUAL;
        case If.GE:
            return signed ? Assembler.GREATER_EQUAL : Assembler.ABOVE_EQUAL;
        case If.GT:
            return signed ? Assembler.GREATER : Assembler.ABOVE;
        default:
            Assert.shouldNotReachHere();
            return Assembler.ZERO;
        }
    }

    /**
     * Returns the current macro assembler used for code emission.
     *
     * @return  the macro assembler
     */
    public MacroAssembler getMasm() {
        return masm;
    }

    /**
     * Returns the method that is currently being compiled.
     *
     * @return  method being compiled
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns information about the basic blocks in the control flow graph.
     *
     * @return  information about the basic blocks
     */
    public ScanBlocks getScanResult() {
        return scanResult;
    }

    /**
     * Sets the current stack pointer offset.
     *
     * @param  espOffset  current stack pointer offset
     */
    public void setEspOffset(int espOffset) {
        this.espOffset = espOffset;
    }

    /**
     * Returns the current stack pointer offset.
     *
     * @return  the current stack pointer offset
     */
    public int getEspOffset() {
        return espOffset;
    }

    /**
     * Locks the condition code register. This method is used to mark the area
     * where the condition code must not be changed.
     */
    public void lockCC() {
        ccLocked = true;
    }

    /**
     * Unlocks the condition code register.
     */
    public void unlockCC() {
        ccLocked = false;
    }

    /**
     * Tests if the condition code must not be changed.
     *
     * @return  whether or not condition code is locked
     */
    public boolean isLockedCC() {
        return ccLocked;
    }

    /**
     * Returns the list of stubs for calls.
     *
     * @return  the list of call stubs
     */
    public List getCallStubs() {
        return callStubs;
    }

    /**
     * Tests if the method needs an inline cache.
     *
     * @return  whether or not the method needs inline cache
     */
    private boolean needsICache() {
        return !method.isStatic();
    }

    /**
     * Tests if the method needs an explicit null check on entry. The receiver
     * must be checked against the null reference only in final virtual methods
     * and only if the check will not be performed by the caller.
     *
     * @return  whether or not a null check is required on entry
     */
    private boolean needsNullCheckOnEntry() {
        return !javac1.Flags.NullCheckAtCaller && !method.isStatic()
            && method.isFinal();
    }

    /**
     * Checks that the specified index is mapped to a double-word address. An
     * exception will be thrown if the two stack slots for the specified index
     * and the next one are not adjacent.
     *
     * @param  localIndex  index of the local variable
     */
    private void checkDoubleAddress(int localIndex) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(frameMap.areAdjacentIndices(localIndex, localIndex + 1), "double elements not adjacent");
        }
    }

    /**
     * Returns the set of local variables that can contain pointers at the
     * specified bytecode index.
     *
     * @param   bci  the bytecode index
     * @return  set of pointer variables
     */
    private BitSet getOopsForBci(int bci) {
        if (cachedOops == null) {
            return null;
        } else if (bci == javac1.Flags.SyncEntryBCI) {
            return (BitSet) cachedOops.get(new Integer(0));
        } else {
            return (BitSet) cachedOops.get(new Integer(bci));
        }
    }
    
    /**
     * Creates a new pointer map using the specified list of spilled pointers.
     *
     * @param   bci          bytecode index of the instruction
     * @param   spilledOops  list of spilled object pointers
     * @return  map of object pointers
     */
    private BitSet createOopMap(int bci, List spilledOops) {
        int rn = 0;
        BitSet map = new BitSet();
        if (spilledOops != null) {
            for (int i = spilledOops.size() - 1; i >= 0; i--) {
                int spillIx = ((Integer) spilledOops.get(i)).intValue();
                rn = frameMap.spillRegname(spillIx, espOffset);
                map.set(rn);
            }
        }
        BitSet oops = getOopsForBci(bci);
        if (oops != null) {
            RInfo cacheReg = RInfo.NO_RINFO;
            int maxLocals = method.getMaxLocals();
            for (int localIx = 0; localIx < maxLocals; localIx++) {
                if (oops.get(localIx)) {
                    if (cachedLocals != null) {
                        cacheReg = cachedLocals.getCacheReg(localIx);
                    }
                    if (!cacheReg.isIllegal()) {
                        rn = frameMap.registerRegname(cacheReg, espOffset);
                    } else {
                        rn = frameMap.localRegname(localIx, espOffset);
                    }
                    map.set(rn);
                }
            }       
        }
        return map;
    }

    /**
     * Creates a new pointer map using the method signature and the specified
     * list of spilled pointers.
     *
     * @param   spilledOops  list of spilled object pointers
     * @return  map of object pointers
     */
    private BitSet createOopMapForOwnSignature(List spilledOops) {
        BitSet map = new BitSet();
        if (spilledOops != null) {
            for (int i = spilledOops.size() - 1; i >= 0; i--) {
                int spillIx = ((Integer) spilledOops.get(i)).intValue();
                int rn = frameMap.spillRegname(spillIx, espOffset);
                map.set(rn);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!javac1.Flags.PassParametersInRegisters, "should not reach here");
        }
        int slotIx = 0;
        if (!method.isStatic()) {
            int rn = frameMap.localRegname(0, espOffset);
            map.set(rn);
            slotIx++;
        }
        List sig = method.getArgTypes();
        for (int i = 0; i < sig.size() - 1; i++) {
            int type = ((Integer) sig.get(i)).intValue();
            if ((type == BasicType.OBJECT) || (type == BasicType.ARRAY)) {
                int rn = frameMap.localRegname(slotIx, espOffset);
                map.set(rn);
            }
            slotIx += BasicType.isDoubleWord(type) ? 2 : 1;
        }
        return map;
    }

    /**
     * Adds registers that contain pointers to the specified pointer map.
     *
     * @param  map      pointer map to add registers to
     * @param  regOops  list of registers that contain pointers
     */
    private void addRegistersToOopMap(BitSet map, List regOops) {
        if (regOops != null) {
            for (int i = 0; i < regOops.size(); i++) {
                RInfo rinfo = (RInfo) regOops.get(i);
                int rn = frameMap.registerRegname(rinfo, espOffset);
                map.set(rn);
            }
        }
    }

    /**
     * Records debug information for the specified code offset.
     *
     * @param  offset  code offset of the instruction
     * @param  info    basic debug information
     * @param  atCall  whether or not information refers to a call
     */
    private void addDebugInfo(int offset, CodeEmitInfo info, boolean atCall) {
        if (javac1.Flags.GenerateOopMaps) {
            int bci = info.getBci();
            BitSet map = null;
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!javac1.Flags.PassParametersInRegisters, "should not reach here");
            }
            if ((bci == javac1.Flags.SyncEntryBCI) || method.isNative()) {
                map = createOopMapForOwnSignature(info.getSpilledOops());
            } else {
                map = createOopMap(bci, info.getSpilledOops());
            }
            if (info.hasRegisterOops()) {
                addRegistersToOopMap(map, info.getRegisterOops());
            }
            DebugInfo debugInfo = new DebugInfo(offset, bci, atCall, map,
                frameMap.getFrameSize(espOffset), frameMap.getSizeArguments());
            masm.getCode().addDebugInfo(debugInfo);
        } else if (Assert.ASSERTS_ENABLED) {
            Assert.that(info.getSpilledOops() == null, "should not generate spilled oops");
        }
    }

    /**
     * Records debug information for the current code offset.
     *
     * @param  info  basic debug information
     */
    public void addDebugInfoHere(CodeEmitInfo info) {
        addDebugInfo(masm.getOffset(), info, false);
    }

    /**
     * Records debug information for the call at the specified code offset.
     *
     * @param  offset  code offset of the call instruction
     * @param  info    basic debug information
     */
    private void addCallInfo(int offset, CodeEmitInfo info) {
        addDebugInfo(offset, info, true);
    }

    /**
     * Records debug information for the call at the current code offset.
     *
     * @param  info  basic debug information
     */
    public void addCallInfoHere(CodeEmitInfo info) {
        addCallInfo(masm.getOffset(), info);
    }

    /**
     * Records debug information for the branch at the current code offset.
     *
     * @param  bci      bytecode index of the branch
     * @param  oopRegs  list of registers that contain pointers
     */
    private void addDebugInfoForBranch(int bci, List oopRegs) {
        if (javac1.Flags.UseCompilerSafepoints) {
            masm.getCode().relocate(masm.getCodePos(), RelocInfo.SAFEPOINT_TYPE);
            CodeEmitInfo info = new CodeEmitInfo(bci, null, null, oopRegs);
            addDebugInfoHere(info);
        }
    }

    /**
     * Returns the register that the specified item represents.
     *
     * @param   item  item to be converted
     * @return  register that the item represents
     */
    private Register itemToReg(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister() && item.getType().isSingleWord() && !(item.getType() instanceof FloatType), "item type error");
        }
        return item.getRInfo().getRegister();
    }

    /**
     * Returns the register that stores the low word of the specified item.
     *
     * @param   item  item to be converted
     * @return  register that stores the low word
     */
    private Register itemToRegLo(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister() && item.getType().isDoubleWord() && !(item.getType() instanceof DoubleType), "item type error");
        }
        return item.getRInfo().getRegisterLo();
    }

    /**
     * Returns the register that stores the high word of the specified item.
     *
     * @param   item  item to be converted
     * @return  register that stores the high word
     */
    private Register itemToRegHi(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister() && item.getType().isDoubleWord() && !(item.getType() instanceof DoubleType), "item type error");
        }
        return item.getRInfo().getRegisterHi();
    }

    /**
     * Converts the specified stack item to an address.
     *
     * @param   item       item to be converted
     * @param   forHiWord  whether the high word address should be returned
     * @return  address of the stack value
     */
    private Address stackItemToAddr(Item item, boolean forHiWord) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isStack(), "item type error");
        }
        boolean isTwoWord = item.getType().isDoubleWord();
        if (item.isSpilled()) {
            return frameMap.spillAddress(item.getSpillIx(), isTwoWord, forHiWord);
        } else {
            if (isTwoWord) {
                checkDoubleAddress(item.getStack());
            }
            return frameMap.localAddress(item.getStack(), isTwoWord, forHiWord);
        }
    }

    /**
     * Returns the address of the low word of the specified stack item.
     *
     * @param   item  item to be converted
     * @return  address of the low word
     */
    private Address stackItemToAddrLo(Item item) {
        return stackItemToAddr(item, false);
    }

    /**
     * Returns the address of the high word of the specified stack item.
     *
     * @param   item  item to be converted
     * @return  address of the high word
     */
    private Address stackItemToAddrHi(Item item) {
        return stackItemToAddr(item, true);
    }

    /**
     * Returns the address of an element in the specified object.
     *
     * @param  item    item that represents the object register
     * @param  offset  offset of the element within the object
     */
    private Address itemToAddrWithOffset(Item item, int offset) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister() && (item.getType() instanceof ObjectType), "item type error");
        }
        return new Address(itemToReg(item), offset * 4);
    }

    /**
     * Returns the address of the low word of an element in the object.
     *
     * @param  item    item that represents the object register
     * @param  offset  offset of the element within the object
     */
    private Address itemToAddrWithOffsetLo(Item item, int offset) {
        return itemToAddrWithOffset(item, offset);
    }

    /**
     * Returns the address of the high word of an element in the object.
     *
     * @param  item    item that represents the object register
     * @param  offset  offset of the element within the object
     */
    private Address itemToAddrWithOffsetHi(Item item, int offset) {
        return itemToAddrWithOffset(item, offset + 1);
    }

    /**
     * Returns the address of the specified array element.
     *
     * @param   array    item that represents the array register
     * @param   index    register or constant item for the index
     * @param   dstType  basic type of the array elements
     * @param   offset   offset within the array element
     * @return  address of the specified array element
     */
    private Address itemToArrayAddr(Item array, Item index, int dstType,
            int offset) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(array.isRegister(), "item in wrong form");
        }
        Register arrayReg = array.getRInfo().getRegister();
        int base = Runtime1.getArrayBaseOffset(dstType);
        if (index.isRegister()) {
            int factor = arrayElementSize(dstType);
            Register indexReg = index.getRInfo().getRegister();
            return new Address(arrayReg, indexReg, factor, base + offset);
        } else if (index.isConstant()) {
            int elemSize = BasicType.ARRAY_ELEM_BYTES[dstType];
            int addrOffset = itemToInt(index) * elemSize + base + offset;
            return new Address(arrayReg, addrOffset);
        } else {
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Returns the address of the low word of the specified array element.
     *
     * @see  #itemToArrayAddr(Item, Item, int, int)
     */
    private Address itemToArrayAddrLo(Item array, Item index, int dstType) {
        return itemToArrayAddr(array, index, dstType, 0);
    }

    /**
     * Returns the address of the high word of the specified array element.
     *
     * @see  #itemToArrayAddr(Item, Item, int, int)
     */
    private Address itemToArrayAddrHi(Item array, Item index, int dstType) {
        return itemToArrayAddr(array, index, dstType, 4);
    }

    /**
     * Returns the value of the specified object constant item.
     *
     * @param   item  item to be converted
     * @return  value of the object constant
     */
    private Obj itemToObject(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && (item.getType() instanceof ObjectType), "item is not an object constant");
        }
        return item.getObjectConstant();
    }

    /**
     * Returns the integer representation of the specified constant item.
     *
     * @param   item  item to be converted
     * @return  integer representation of the item
     */
    private int itemToInt(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && item.getType().isSingleWord() && !(item.getType() instanceof ObjectType), "item is not an integer constant");
        }
        switch (item.getType().getTag()) {
        case ValueType.intTag:
            return item.getIntConstant();
        case ValueType.addressTag:
            return item.getAddressConstant();
        case ValueType.floatTag:
            return Float.floatToIntBits(item.getFloatConstant());
        default:
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Returns the integer representation of the low word of the specified item.
     *
     * @param   item  item to be converted
     * @return  integer representation of the low word
     */
    private int itemToIntLo(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && item.getType().isDoubleWord(), "item is not a double-word constant");
        }
        if (item.getType() instanceof LongType) {
            return (int) item.getLongConstant();
        } else {
            long value = Double.doubleToLongBits(item.getDoubleConstant());
            return (int) value;
        }
    }

    /**
     * Returns the integer representation of the high word of the specified
     * item.
     *
     * @param   item  item to be converted
     * @return  integer representation of the high word
     */
    private int itemToIntHi(Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && item.getType().isDoubleWord(), "item is not a double-word constant");
        }
        if (item.getType() instanceof LongType) {
            return (int) (item.getLongConstant() >>> 32);
        } else {
            long value = Double.doubleToLongBits(item.getDoubleConstant());
            return (int) (value >>> 32);
        }
    }

    /**
     * Binds the specified label to the current code position.
     *
     * @param  label  label to be bound
     */
    public void bindLabel(Label label) {
        masm.bind(label);
    }

    /**
     * Pops the specified register off the floating-point stack.
     *
     * @param  reg  register to be popped
     */
    private void fpuPop(RInfo reg) {
        fstack.pop(reg.fpu());
    }

    /**
     * Pushes the specified register onto the floating-point stack.
     *
     * @param  reg  register to be pushed
     */
    private void fpuPush(RInfo reg) {
        fstack.push(reg.fpu());
    }

    /**
     * Brings the specified register on the top of the floating-point stack.
     *
     * @param  reg  register to be brought on top
     */
    private void fpuOnTop(RInfo reg) {
        if (!fstack.isStackPos(reg.fpu(), 0)) {
            int delta = fstack.bringOnTop(reg.fpu());
            masm.fxch(delta);
        }
    }

    /**
     * Tests if the specified registers are on top of stack but in reverse
     * order.
     *
     * @param   tos0  register to be on top
     * @param   tos1  register to be next to top
     * @return  whether or not the registers must be swapped
     */
    private boolean mustSwapTwoOnTop(RInfo tos0, RInfo tos1) {
        return fstack.isStackPos(tos1.fpu(), 0)
            && fstack.isStackPos(tos0.fpu(), 1);
    }

    /**
     * Ensures that the specified registers are on top of the stack.
     *
     * @param  tos0           first register to be on top
     * @param  tos1           second register to be on top
     * @param  mustBeOrdered  whether or not registers must be in this order
     */
    private void fpuTwoOnTop(RInfo tos0, RInfo tos1, boolean mustBeOrdered) {
        if (!mustBeOrdered) {
            int pos0 = fstack.getOffset(tos0.fpu());
            int pos1 = fstack.getOffset(tos1.fpu());
            if ((pos0 == 0) && (pos1 == 1)) {
                return;
            } else if ((pos0 == 1) && (pos1 == 0)) {
                fstack.swapTwoOnTop();
                return;
            }
        }
        if (!fstack.isStackPos(tos1.fpu(), 1)) {
            fpuOnTop(tos1);
            fstack.swapTwoOnTop();
            masm.fxch(1);
        }
        fpuOnTop(tos0);
    }

    /**
     * Pushes the specified result register onto the floating-point stack.
     *
     * @param  reg  result register to be pushed
     */
    public void setFpuResult(RInfo reg) {
        fstack.push(reg.fpu());
    }

    /**
     * Removes the specified result register from the floating-point stack.
     *
     * @param  reg  result register to be removed
     */
    public void removeFpuResult(RInfo reg) {
        fstack.pop(reg.fpu());
        masm.fpop();
    }

    /**
     * Pops the topmost element off the floating-point stack.
     */
    public void fpop() {
        masm.fpop();
    }

    /**
     * Tests if the FPU stack is empty.
     *
     * @return  whether or not the stack is empty
     */
    public boolean isFpuStackEmpty() {
        return fstack.isEmpty();
    }

    /**
     * Clears the stack of floating-point registers.
     */
    public void clearFpuStack() {
        fstack.clear();
    }

    /**
     * Copies the specified item onto the top of the floating-point stack.
     *
     * @param  reg   register for the new value
     * @param  item  item to be copied onto top of stack
     */
    public void copyFpuItem(RInfo reg, Item item) {
        RInfo fromReg = item.getRInfo();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(fromReg.isFloatKind(), "wrong item register");
        }
        int offset = fstack.getOffset(fromReg.fpu());
        masm.flds(offset);
        fstack.push(reg.fpu());
    }

    /**
     * Pushes the specified integer value onto the stack.
     *
     * @param  val  integer to be pushed
     */
    public void pushInt(int val) {
        espOffset++;
        masm.pushl(val);
    }

    /**
     * Pushes the specified object pointer onto the stack.
     *
     * @param  obj  object to be pushed
     */
    private void pushOop(Obj obj) {
        espOffset++;
        masm.pushl(obj);
    }

    /**
     * Pushes the value at the specified address onto the stack.
     *
     * @param  adr  address of the value to be pushed
     */
    private void pushAddr(Address adr) {
        espOffset++;
        masm.pushl(adr);
    }

    /**
     * Pushes the value of the specified register onto the stack.
     *
     * @param  reg  register to be pushed
     */
    public void pushReg(Register reg) {
        espOffset++;
        masm.pushl(reg);
    }

    /**
     * Pops one value off the stack and stores it in the specified register.
     *
     * @param  reg  destination register
     */
    private void pop(Register reg) {
        espOffset--;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(espOffset >= 0, "stack offset underflow");
        }
        masm.popl(reg);
    }

    /**
     * Decrements the stack pointer by the specified number of words.
     *
     * @param  numWords  number of words to decrement pointer by
     */
    public void decStack(int numWords) {
        espOffset -= numWords;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(espOffset >= 0, "stack offset underflow");
        }
        masm.addl(Register.ESP, numWords * 4);
    }

    /**
     * Decrements the stack pointer by the specified number of words after a
     * procedure call.
     *
     * @param  numWords  number of words to decrement pointer by
     */
    public void decStackAfterCall(int numWords) {
        espOffset -= numWords;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(espOffset >= 0, "stack offset underflow");
        }
    }

    /**
     * Removes all values from the stack.
     */
    private void removeAllStack() {
        masm.addl(Register.ESP, espOffset * 4);
        espOffset = 0;
    }

    /**
     * Returns the stack pointer decrement needed to build the frame.
     *
     * @param   numSpills  number of spill slots
     * @return  size of stack decrement in bytes
     */
    private int stackOffsetInBytes(int numSpills) {
        return (frameMap.sizeOfStackDecrement() + numSpills) * 4;
    }

    /**
     * Returns the address of the receiver in the prolog. In the prolog the
     * register ESP points to the return address and the stack frame has not
     * been built yet.
     *
     * @return  the receiver address in the prolog
     */
    private Address receiverAddressInProlog() {
        return new Address(Register.ESP, method.getArgSize() * 4);
    }

    /**
     * Checks the inline cache before the frame is built.
     */
    private void checkICache() {
        masm.verifyOop(RECV_REG);
        if (javac1.Flags.VerifyReceiver) {
            Label label = new Label();
            masm.cmpl(RECV_REG, receiverAddressInProlog());
            masm.jcc(Assembler.EQUAL, label);
            masm.stop("receiver must be in ecx at inline cache check");
            masm.bind(label);
        }
        masm.cmpl(IC_KLASS_REG, new Address(RECV_REG, Runtime1.getKlassOffset()));
        masm.jcc(Assembler.NOT_EQUAL, Runtime1.getStubEntry(
            Runtime1.HANDLE_IC_MISS_ID), RelocInfo.RUNTIME_CALL_TYPE);
    }

    /**
     * Bangs the stack with the specified offset.
     *
     * @param  offset  offset to bang stack with
     */
    private void bangStackWithOffset(int offset) {
        masm.movl(new Address(Register.ESP, offset), Register.EAX);
    }

    /**
     * Bangs the stack to avoid a stack overflow.
     */
    public void bangStack() {
        bangStackWithOffset(-Runtime1.getVMPageSize());
    }

    /**
     * Builds the stack frame of a method.
     *
     * @param  maxSpills  maximum number of spill elements
     */
    private void buildFrame(int maxSpills) {
        masm.enter();
        int stackOffset = stackOffsetInBytes(maxSpills);
        masm.decrement(Register.ESP, stackOffset);
        if (method.isNative()) {
            bangStack();
        }
        int delta = Runtime1.getVMPageSize();
        int end = -Runtime1.getBangStackSizeForExceptions();
        for (int offset = stackOffset - delta; offset > end; offset -= delta) {
            bangStackWithOffset(offset);
        }
    }

    /**
     * Initializes the local variable at the specified index. This method is
     * called only for possible pointer locations.
     *
     * @param  localIndex  index of the variable to be initialized
     */
    private void initLocal(int localIndex) {
        masm.movl(frameMap.localAddress(localIndex, false), (Obj) null);
    }

    /**
     * Reloads the receiver from the stack into the receiver register.
     */
    public void restoreCachedReceiver() {
        loadReceiver(RECV_REG);
    }

    /**
     * Loads the receiver from the stack into the specified register.
     *
     * @param  reg  destination register
     */
    public void loadReceiver(Register reg) {
        masm.movl(reg, frameMap.localAddress(0, false));
    }

    /**
     * Copies the value of the source register into the destination register. If
     * the two registers are equal, no code will be emitted.
     *
     * @param  dst  destination register
     * @param  src  source register
     */
    public void moveReg(Register dst, Register src) {
        if (!dst.equals(src)) {
            masm.movl(dst, src);
        }
    }

    /**
     * Swaps the values of the specified registers. For this reason one of the
     * values will be pushed onto the stack temporarily.
     *
     * @param  a  first register
     * @param  b  second register
     */
    private void swapReg(Register a, Register b) {
        pushReg(a);
        masm.movl(a, b);
        pop(b);
    }

    /**
     * Swaps the values of the specified registers via a temporary register.
     *
     * @param  a    first register
     * @param  b    second register
     * @param  tmp  temporary register
     */
    private void swapRegWithTmp(Register a, Register b, Register tmp) {
        masm.movl(tmp, a);
        masm.movl(a, b);
        masm.movl(b, tmp);
    }

    /**
     * Tries the fast way to get the hash code of an object before the frame is
     * constructed.
     */
    private void tryFastObjectHashCode() {
        Label slowCase = new Label();
        final Register hdr = Register.EAX;
        masm.movl(hdr, new Address(RECV_REG, Runtime1.getMarkOffset()));
        masm.testl(hdr, Runtime1.getMarkUnlockedValue());
        masm.jcc(Assembler.ZERO, slowCase);
        masm.andl(hdr, Runtime1.getHashMaskInPlace());
        masm.jcc(Assembler.ZERO, slowCase);
        masm.shrl(hdr, Runtime1.getHashShift());
        int argSize = method.getArgSize() * 4;
        safepointReturn(argSize);
        masm.bind(slowCase);
    }

    /**
     * Emits code for the entry of the method being compiled.
     *
     * @param  initVars   list of variables to be initialized
     * @param  maxSpills  maximum number of spill elements
     */
    public void methodEntry(List initVars, int maxSpills) {
        final int alignEntryCode = 4;
        if (method.isNative() && needsICache()) {
            masm.align(alignEntryCode);
            if (!javac1.Flags.C1Breakpoint) {
                masm.nop();
            }
            masm.nop();
            masm.nop();
        }
        offsets.setEpOffset(masm.getOffset());
        if (needsICache()) {
            if (javac1.Flags.C1Breakpoint) {
                masm.int3();
            }
            checkICache();
        }
        if (method.isFinal() || method.isNative()) {
            masm.align(alignEntryCode);
        }
        offsets.setVepOffset(masm.getOffset());
        offsets.setIepOffset(masm.getOffset());
        if (method.isNative()) {
            masm.fatNop();
        }
        if (javac1.Flags.C1Breakpoint) {
            masm.int3();
        }
        offsets.setCodeOffset(masm.getOffset());
        if (!method.isStatic() && javac1.Flags.VerifyReceiver) {
            Label label = new Label();
            masm.cmpl(Register.ECX, receiverAddressInProlog());
            masm.jcc(Assembler.EQUAL, label);
            masm.stop("receiver must be in ecx");
            masm.bind(label);
        }
        if (method.getIntrinsicId() == Intrinsic.HASH_CODE) {
            tryFastObjectHashCode();
        }
        buildFrame(maxSpills);
        if (initVars != null) {
            for (int i = initVars.size() - 1; i >= 0; i--) {
                initLocal(((Integer) initVars.get(i)).intValue());
            }
        }
        if (method.isSynchronized() && javac1.Flags.GenerateSynchronizationCode) {
            final Register obj = RECV_REG;
            final Register lockReg = Register.ESI;
            if (method.isStatic()) {
                masm.movl(obj, method.getHolder());
            }
            CodeEmitInfo info = new CodeEmitInfo(javac1.Flags.SyncEntryBCI, null, null);
            monitorenter(obj, lockReg, SYNC_HDR_REG, 0, info);
        }
    }

    /**
     * Spills the specified item onto the stack.
     *
     * @param  spillIx  index of the spill slot
     * @param  item     item to be spilled
     */
    public void spill(int spillIx, Item item) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister(), "can only spill register items");
        }
        int tag = item.getType().getTag();
        if (tag == ValueType.longTag) {
            Address dstLo = frameMap.spillAddress(spillIx, true, false);
            Address dstHi = frameMap.spillAddress(spillIx, true, true);
            masm.movl(dstLo, itemToRegLo(item));
            masm.movl(dstHi, itemToRegHi(item));
        } else if (tag == ValueType.floatTag) {
            fpuOnTop(item.getRInfo());
            Address dstAddr = frameMap.spillAddress(spillIx, false, false);
            masm.fstps(dstAddr);
            fpuPop(item.getRInfo());
        } else if (tag == ValueType.doubleTag) {
            fpuOnTop(item.getRInfo());
            Address dstAddr = frameMap.spillAddress(spillIx, true, false);
            masm.fstpd(dstAddr);
            fpuPop(item.getRInfo());
        } else {
            Address dst = frameMap.spillAddress(spillIx, false, false);
            masm.movl(dst, itemToReg(item));
        }
    }

    /**
     * Enters the monitor of an object to lock it.
     *
     * @param  objReg     register that points to the object to be locked
     * @param  lockReg    register for the basic lock
     * @param  hdrReg     register for the object header
     * @param  monitorNo  number of the monitor to enter
     * @param  info       associated debug information
     */
    private void monitorenter(Register objReg, Register lockReg,
            Register hdrReg, int monitorNo, CodeEmitInfo info) {
        if (!javac1.Flags.GenerateSynchronizationCode) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!objReg.equals(SYNC_HDR_REG) && !lockReg.equals(SYNC_HDR_REG), "eax must be available here");
            Assert.that(hdrReg.equals(SYNC_HDR_REG), "wrong header register");
        }
        bangStack();
        masm.leal(lockReg, frameMap.monitorAddress(monitorNo));
        masm.verifyOop(objReg);
        MonitorAccessStub slowCase =
            new MonitorEnterStub(objReg, lockReg, info, espOffset);
        slowCaseStubs.add(slowCase);
        if (javac1.Flags.UseFastLocking) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(Runtime1.getDisplacedHeaderOffset() == 0, "must point to displaced header");
            }
            addDebugInfoHere(info);
            masm.lockObject(hdrReg, objReg, lockReg, slowCase.getEntry());
        } else {
            Assert.shouldNotReachHere();
        }
        masm.bind(slowCase.getContinuation());
    }

    /**
     * Enters the monitor of an object to lock it.
     *
     * @param  object     object to be locked
     * @param  lock       register for the basic lock
     * @param  hdr        register for the object header
     * @param  monitorNo  number of the monitor to enter
     * @param  info       associated debug information
     */
    public void monitorenter(Item object, RInfo lock, RInfo hdr, int monitorNo,
            CodeEmitInfo info) {
        monitorenter(itemToReg(object), lock.getRegister(), hdr.getRegister(),
            monitorNo, info);
    }

    /**
     * Exits the monitor of an object to unlock it.
     *
     * @param  objReg     register that points to the object to be unlocked
     * @param  lockReg    register for the basic lock
     * @param  newHdr     register for the new object header
     * @param  monitorNo  number of the monitor to exit
     * @param  exception  exception to be preserved
     */
    private void monitorexit(Register objReg, Register lockReg, Register newHdr,
            int monitorNo, Register exception) {
        if (!javac1.Flags.GenerateSynchronizationCode) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!objReg.equals(SYNC_HDR_REG) && !lockReg.equals(SYNC_HDR_REG), "eax must be available here");
            Assert.that(newHdr.equals(SYNC_HDR_REG), "wrong header register");
        }
        if (exception.isValid()) {
            pushReg(exception);
        }
        Register hdrReg = lockReg;
        lockReg = newHdr;
        Address lockAddr = frameMap.monitorAddress(monitorNo);
        masm.leal(lockReg, lockAddr);
        masm.verifyOop(objReg);
        MonitorAccessStub slowCase = new MonitorExitStub(objReg, lockReg,
            javac1.Flags.UseFastLocking, lockAddr);
        callStubs.add(slowCase);
        if (javac1.Flags.UseFastLocking) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(Runtime1.getDisplacedHeaderOffset() == 0, "must point to displaced header");
            }
            masm.unlockObject(hdrReg, objReg, lockReg, slowCase.getEntry());
        } else {
            Assert.shouldNotReachHere();
        }
        masm.bind(slowCase.getContinuation());
        if (exception.isValid()) {
            pop(exception);
        }
    }

    /**
     * Exits the monitor of an object to unlock it.
     *
     * @param  object     object to be unlocked
     * @param  lock       register for the basic lock
     * @param  hdr        register for the object header
     * @param  monitorNo  number of the monitor to exit
     */
    public void monitorexit(Item object, RInfo lock, RInfo hdr, int monitorNo) {
        monitorexit(itemToReg(object), lock.getRegister(), hdr.getRegister(),
            monitorNo, Register.NO_REG);
    }

    /**
     * Clears the local variable at the specified index.
     *
     * @param  localIndex  index of the variable to be cleared
     */
    public void clearLocal(int localIndex) {
        masm.movl(frameMap.localAddress(localIndex, false), 0);
    }

    /**
     * Loads a local variable from the stack into a register.
     *
     * @param  reg         destination register
     * @param  localIndex  index of the variable to be loaded
     */
    public void localToReg(RInfo reg, int localIndex) {
        if (reg.isWord()) {
            Address adr = frameMap.localAddress(localIndex, false);
            masm.movl(reg.getRegister(), adr);
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Stores a local variable from a register onto the stack.
     *
     * @param  localIndex  index of the variable to be stored
     * @param  reg         source register
     */
    public void regToLocal(int localIndex, RInfo reg) {
        if (reg.isWord()) {
            Address adr = frameMap.localAddress(localIndex, false);
            masm.movl(adr, reg.getRegister());
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Pads the code to make room for a native call after the verified entry.
     */
    private void nopsForZombie() {
        int limit = NativeCall.INSTRUCTION_SIZE + offsets.getVepOffset();
        while (masm.getOffset() < limit) {
            masm.nop();
        }
    }

    /**
     * Loads the specified object into the destination register.
     *
     * @param  reg   destination register
     * @param  obj   object to be loaded
     * @param  info  associated debug information
     */
    private void objectToRegWithPatching(RInfo reg, Obj obj, CodeEmitInfo info) {
        nopsForZombie();
        if (obj.isLoaded()) {
            masm.movl(reg.getRegister(), obj);
        } else {
            PatchingDesc patch = new PatchingDesc();
            patch.set(masm.getCodePos(), masm.getOffset());
            masm.movl(reg.getRegister(), (Obj) null);
            patchingEpilog(patch, info, PatchingStub.LOAD_KLASS_ID);
        }
    }

    /**
     * Loads the specified object constant into the destination register.
     *
     * @param  reg   destination register
     * @param  item  object constant item to be loaded
     * @param  info  associated debug information
     */
    public void itemToRegWithPatching(RInfo reg, Item item, CodeEmitInfo info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isConstant() && (item.getType() instanceof ObjectType), "wrong item");
        }
        objectToRegWithPatching(reg, item.getObjectConstant(), info);
    }

    /**
     * Loads the specified single-precision floating-point constant into the
     * destination register.
     *
     * @param  reg    destination register
     * @param  value  value of the constant
     */
    private void loadFloatConstant(RInfo reg, float value) {
        if (isZeroFloat(value)) {
            masm.fldz();
        } else if (isOneFloat(value)) {
            masm.fld1();
        } else {
            int constAddr = constTable.addressOfFloatConstant(value);
            if (constAddr == 0) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!javac1.Flags.UseFPConstTables, "entry should exist");
                }
                int bits = Float.floatToIntBits(value);
                pushInt(bits);
                masm.flds(new Address(Register.ESP));
                decStack(1);
            } else {
                masm.flds(new Address(constAddr, RelocInfo.INTERNAL_WORD_TYPE));
            }
        }
    }

    /**
     * Loads the specified double-precision floating-point constant into the
     * destination register.
     *
     * @param  reg    destination register
     * @param  value  value of the constant
     */
    private void loadDoubleConstant(RInfo reg, double value) {
        if (isZeroDouble(value)) {
            masm.fldz();
        } else if (isOneDouble(value)) {
            masm.fld1();
        } else {
            int constAddr = constTable.addressOfDoubleConstant(value);
            if (constAddr == 0) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!javac1.Flags.UseFPConstTables, "entry should exist");
                }
                long bits = Double.doubleToLongBits(value);
                pushInt((int) (bits >>> 32));
                pushInt((int) bits);
                masm.fldd(new Address(Register.ESP));
                decStack(2);
            } else {
                masm.fldd(new Address(constAddr, RelocInfo.INTERNAL_WORD_TYPE));
            }
        }
    }

    /**
     * Loads the specified item into the destination register.
     *
     * @param  reg   destination register
     * @param  item  item to be loaded
     */
    public void itemToReg(RInfo reg, Item item) {
        int tag = item.getType().getTag();
        if (tag == ValueType.floatTag) {
            if (item.isRegister()) {
                fpuOnTop(item.getRInfo());
                fpuPop(item.getRInfo());
            } else if (item.isStack()) {
                masm.flds(stackItemToAddr(item, false));
            } else if (item.isConstant()) {
                loadFloatConstant(reg, item.getFloatConstant());
            } else {
                Assert.shouldNotReachHere();
            }
            fpuPush(reg);
        } else if (tag == ValueType.doubleTag) {
            if (item.isRegister()) {
                fpuOnTop(item.getRInfo());
                fpuPop(item.getRInfo());
            } else if (item.isStack()) {
                masm.fldd(stackItemToAddr(item, false));
            } else if (item.isConstant()) {
                loadDoubleConstant(reg, item.getDoubleConstant());
            } else {
                Assert.shouldNotReachHere();
            }
            fpuPush(reg);
        } else if (tag == ValueType.longTag) {
            Register rlo = reg.getRegisterLo();
            Register rhi = reg.getRegisterHi();
            if (item.isRegister()) {
                if (itemToRegHi(item).equals(rlo)) {
                    moveReg(rhi, itemToRegHi(item));
                    moveReg(rlo, itemToRegLo(item));
                } else {
                    moveReg(rlo, itemToRegLo(item));
                    moveReg(rhi, itemToRegHi(item));
                }
            } else if (item.isStack()) {
                masm.movl(rlo, stackItemToAddrLo(item));
                masm.movl(rhi, stackItemToAddrHi(item));
            } else if (item.isConstant()) {
                masm.movl(rlo, itemToIntLo(item));
                masm.movl(rhi, itemToIntHi(item));
            } else {
                Assert.shouldNotReachHere();
            }
        } else if (item.getType().isSingleWord()) {
            Register r = reg.getRegister();
            if (item.isRegister()) {
                moveReg(r, itemToReg(item));
            } else if (item.isStack()) {
                masm.movl(r, stackItemToAddr(item, false));
            } else if (item.isConstant()) {
                if (tag == ValueType.objectTag) {
                    masm.movl(r, itemToObject(item));
                } else {
                    int value = itemToInt(item);
                    if (!isLockedCC() && value == 0) {
                        masm.xorl(r, r);
                    } else {
                        masm.movl(r, value);
                    }
                }
            } else {
                Assert.shouldNotReachHere();
            }
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Stores the specified item into a local variable on the stack.
     *
     * @param  localIndex  index of the local variable
     * @param  item        item to be stored
     */
    public void itemToLocal(int localIndex, Item item) {
        int tag = item.getType().getTag();
        boolean isTwoWord = item.getType().isDoubleWord();
        if (isTwoWord) {
            checkDoubleAddress(localIndex);
        }
        Address dstAddr = frameMap.localAddress(localIndex, isTwoWord);
        if (item.isRegister()) {
            if (tag == ValueType.floatTag) {
                fpuOnTop(item.getRInfo());
                masm.fstps(dstAddr);
                fpuPop(item.getRInfo());
            } else if (tag == ValueType.doubleTag) {
                fpuOnTop(item.getRInfo());
                masm.fstpd(dstAddr);
                fpuPop(item.getRInfo());
            } else if (tag == ValueType.longTag) {
                Address dstAddrLo = frameMap.localAddress(localIndex, true, false);
                Address dstAddrHi = frameMap.localAddress(localIndex, true, true);
                masm.movl(dstAddrLo, itemToRegLo(item));
                masm.movl(dstAddrHi, itemToRegHi(item));
            } else {
                masm.movl(dstAddr, itemToReg(item));
            }
        } else if (item.isConstant()) {
            if (isTwoWord) {
                Address dstAddrLo = frameMap.localAddress(localIndex, true, false);
                Address dstAddrHi = frameMap.localAddress(localIndex, true, true);
                masm.movl(dstAddrLo, itemToIntLo(item));
                masm.movl(dstAddrHi, itemToIntHi(item));
            } else if (tag == ValueType.objectTag) {
                masm.movl(dstAddr, itemToObject(item));
            } else {
                masm.movl(dstAddr, itemToInt(item));
            }
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Pops one value off the stack into the specified item. This method is
     * currently implemented only for single-word general-purpose register
     * items.
     *
     * @param  item  item to be popped
     */
    public void popItem(Item item) {
        ValueType type = item.getType();
        if (item.isRegister() && type.isSingleWord() && !type.isFloatKind()) {
            pop(itemToReg(item));
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Pushes the specified item onto the stack.
     *
     * @param  item  item to be pushed
     */
    public void pushItem(Item item) {
        int tag = item.getType().getTag();
        if (item.isRegister()) {
            if (tag == ValueType.floatTag) {
                fpuOnTop(item.getRInfo());
                pushReg(Register.EAX);
                masm.fstps(new Address(Register.ESP));
                fpuPop(item.getRInfo());
            } else if (tag == ValueType.doubleTag) {
                fpuOnTop(item.getRInfo());
                pushReg(Register.EAX);
                pushReg(Register.EAX);
                masm.fstpd(new Address(Register.ESP));
                fpuPop(item.getRInfo());
            } else if (tag == ValueType.longTag) {
                pushReg(itemToRegHi(item));
                pushReg(itemToRegLo(item));
            } else {
                pushReg(itemToReg(item));
            }
        } else if (item.isStack()) {
            if ((tag == ValueType.longTag) || (tag == ValueType.doubleTag)) {
                pushAddr(stackItemToAddrHi(item));
                pushAddr(stackItemToAddrLo(item));
            } else {
                pushAddr(stackItemToAddr(item, false));
            }
        } else if (item.isConstant()) {
            if ((tag == ValueType.longTag) || (tag == ValueType.doubleTag)) {
                pushInt(itemToIntHi(item));
                pushInt(itemToIntLo(item));
            } else if (tag == ValueType.objectTag) {
                pushOop(itemToObject(item));
            } else {
                pushInt(itemToInt(item));
            }
        }
    }

    /**
     * Loads a stack item into a register and pushes the register onto the
     * stack.
     *
     * @param  item  item to be loaded
     * @param  reg   destination register
     */
    public void pushItemWithRInfo(Item item, RInfo reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isStack(), "item must be on the stack");
        }
        if (reg.isWord()) {
            masm.movl(reg.getRegister(), stackItemToAddr(item, false));
            pushReg(reg.getRegister());
        } else {
            masm.movl(reg.getRegisterHi(), stackItemToAddrHi(item));
            masm.movl(reg.getRegisterLo(), stackItemToAddrLo(item));
            pushReg(reg.getRegisterHi());
            pushReg(reg.getRegisterLo());
        }
    }

    /**
     * Moves a spill element from one slot to another.
     *
     * @param  toIx    index of the slot to move to
     * @param  fromIx  index of the spill element to move
     * @param  type    type of the spill slot
     * @param  tmp     temporary register
     */
    public void moveSpill(int toIx, int fromIx, ValueType type, RInfo tmp) {
        boolean isTwoWord = type.isDoubleWord();
        Address fromAddr = frameMap.spillAddress(fromIx, isTwoWord, false);
        Address toAddr = frameMap.spillAddress(toIx, isTwoWord, false);
        if (tmp.isFloat()) {
            masm.flds(fromAddr);
            masm.fstps(toAddr);
        } else if (tmp.isDouble()) {
            masm.fldd(fromAddr);
            masm.fstpd(toAddr);
        } else if (tmp.isWord()) {
            masm.movl(tmp.getRegister(), fromAddr);
            masm.movl(toAddr, tmp.getRegister());
        } else if (tmp.isLong()) {
            Address fromAddrLo = frameMap.spillAddress(fromIx, isTwoWord, false);
            Address fromAddrHi = frameMap.spillAddress(fromIx, isTwoWord, true);
            Address toAddrLo = frameMap.spillAddress(toIx, isTwoWord, false);
            Address toAddrHi = frameMap.spillAddress(toIx, isTwoWord, true);
            masm.movl(tmp.getRegisterLo(), fromAddrLo);
            masm.movl(tmp.getRegisterHi(), fromAddrHi);
            masm.movl(toAddrLo, tmp.getRegisterLo());
            masm.movl(toAddrHi, tmp.getRegisterHi());
        }
    }

    /**
     * Emits an epilog for patching. This method pads the code if the patched
     * instruction is too small for a call instruction, sets the field offset so
     * that a high or low word move can be recognized, and creates the patching
     * stub.
     *
     * @param  patch  the patching descriptor
     * @param  info   associated debug information
     * @param  id     the kind of patching
     */
    private void patchingEpilog(PatchingDesc patch, CodeEmitInfo info, int id) {
        addCallInfo(patch.getOffset() + NativeCall.INSTRUCTION_SIZE, info);
        int limit = NativeCall.INSTRUCTION_SIZE + patch.getStart();
        while (masm.getCodePos() < limit) {
            masm.nop();
        }
        if (id == PatchingStub.ACCESS_FIELD_ID) {
            masm.setLongAt(patch.getOffset() + 2, patch.getFieldOffset());
        }
        int instrSize = masm.getCodePos() - patch.getStart();
        PatchingStub stub = new PatchingStub(patch.getStart(), Register.NO_REG,
            Register.NO_REG, instrSize, info, id, espOffset);
        slowCaseStubs.add(stub);
    }

    /**
     * Loads the specified field into the destination register.
     *
     * @param  dst            destination register
     * @param  dstType        type of the field
     * @param  object         object item that the field belongs to
     * @param  offset         offset of the field within the object
     * @param  needsPatching  whether or not patching is required
     * @param  isLoaded       whether or not the object is loaded
     * @param  info           associated debug information
     */
    public void fieldLoad(RInfo dst, int dstType, Item object, int offset,
            boolean needsPatching, boolean isLoaded, CodeEmitInfo info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(object.getType() instanceof ObjectType, "not an object");
        }
        PatchingDesc patch = new PatchingDesc();
        if (needsPatching) {
            offset = MAX_OFFSET;
            patch.set(masm.getCodePos(), masm.getOffset());
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(offset >= 0, "field offset cannot be negative");
        }
        addDebugInfoHere(info);
        Address srcAddr = itemToAddrWithOffset(object, offset);
        if (dstType == BasicType.FLOAT) {
            masm.flds(srcAddr);
            fpuPush(dst);
        } else if (dstType == BasicType.DOUBLE) {
            masm.fldd(srcAddr);
            fpuPush(dst);
        } else if (dstType == BasicType.LONG) {
            Register rlo = dst.getRegisterLo();
            Register rhi = dst.getRegisterHi();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!rlo.equals(rhi), "error in register allocation");
            }
            if (itemToReg(object).equals(rlo)) {
                masm.movl(rhi, itemToAddrWithOffsetHi(object, offset));
                if (needsPatching) {
                    patch.setHiWord();
                    patchingEpilog(patch, info, PatchingStub.ACCESS_FIELD_ID);
                    patch.set(masm.getCodePos(), masm.getOffset());
                    patch.setLoWord();
                }
                masm.movl(rlo, itemToAddrWithOffsetLo(object, offset));
            } else {
                masm.movl(rlo, itemToAddrWithOffsetLo(object, offset));
                if (needsPatching) {
                    patch.setLoWord();
                    patchingEpilog(patch, info, PatchingStub.ACCESS_FIELD_ID);
                    patch.set(masm.getCodePos(), masm.getOffset());
                    patch.setHiWord();
                }
                masm.movl(rhi, itemToAddrWithOffsetHi(object, offset));
            }
        } else {
            masm.movl(dst.getRegister(), srcAddr);
        }
        if (needsPatching) {
            patchingEpilog(patch, info, PatchingStub.ACCESS_FIELD_ID);
        }
    }

    /**
     * Stores a value into the specified field. Note that the object register
     * will be destroyed if the value is an object pointer.
     *
     * @param  valueType      type of the value
     * @param  object         object item that the field belongs to
     * @param  offset         offset of the field within the object
     * @param  value          the value to be stored
     * @param  needsPatching  whether or not patching is required
     * @param  isLoaded       whether or not the object is loaded
     * @param  info           associated debug information
     * @param  tmp            temporary register
     */
    public void fieldStore(int valueType, Item object, int offset, Item value,
            boolean needsPatching, boolean isLoaded, CodeEmitInfo info, RInfo tmp) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(object.isRegister() && (object.getType() instanceof ObjectType), "wrong object type");
        }
        boolean isOop = BasicType.isOop(valueType);
        PatchingDesc patch = new PatchingDesc();
        if (needsPatching) {
            offset = MAX_OFFSET;
            patch.set(masm.getCodePos(), masm.getOffset());
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(offset >= 0, "field offset cannot be negative");
        }
        addDebugInfoHere(info);
        Address dstAddr = itemToAddrWithOffset(object, offset);
        if (value.isConstant()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!needsPatching, "cannot patch with constant");
            }
            if (BasicType.isDoubleWord(valueType)) {
                Address dstAddrLo = itemToAddrWithOffsetLo(object, offset);
                Address dstAddrHi = itemToAddrWithOffsetHi(object, offset);
                masm.movl(dstAddrLo, itemToIntLo(value));
                masm.movl(dstAddrHi, itemToIntHi(value));
            } else if (isOop) {
                masm.movl(dstAddr, itemToObject(value));
            } else {
                masm.movl(dstAddr, itemToInt(value));
            }
        } else if (value.isStack()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!needsPatching, "cannot patch with stack value");
                Assert.that((valueType == BasicType.FLOAT) || (valueType == BasicType.DOUBLE), "only for floats and doubles");
            }
            if (valueType == BasicType.FLOAT) {
                masm.movl(tmp.getRegister(), stackItemToAddr(value, false));
                masm.movl(dstAddr, tmp.getRegister());
            } else {
                Address dstAddrLo = itemToAddrWithOffsetLo(object, offset);
                Address dstAddrHi = itemToAddrWithOffsetHi(object, offset);
                masm.movl(tmp.getRegisterLo(), stackItemToAddrLo(value));
                masm.movl(tmp.getRegisterHi(), stackItemToAddrHi(value));
                masm.movl(dstAddrLo, tmp.getRegisterLo());
                masm.movl(dstAddrHi, tmp.getRegisterHi());
            }
        } else if (value.isRegister()) {
            if (valueType == BasicType.FLOAT) {
                fpuOnTop(value.getRInfo());
                masm.fstps(dstAddr);
                fpuPop(value.getRInfo());
            } else if (valueType == BasicType.DOUBLE) {
                fpuOnTop(value.getRInfo());
                masm.fstpd(dstAddr);
                fpuPop(value.getRInfo());
            } else if (valueType == BasicType.LONG) {
                Address dstAddrLo = itemToAddrWithOffsetLo(object, offset);
                Address dstAddrHi = itemToAddrWithOffsetHi(object, offset);
                masm.movl(dstAddrLo, itemToRegLo(value));
                if (needsPatching) {
                    patch.setLoWord();
                    patchingEpilog(patch, info, PatchingStub.ACCESS_FIELD_ID);
                    patch.set(masm.getCodePos(), masm.getOffset());
                    patch.setHiWord();
                }
                masm.movl(dstAddrHi, itemToRegHi(value));
            } else {
                masm.movl(dstAddr, itemToReg(value));
            }
            if (needsPatching) {
                patchingEpilog(patch, info, PatchingStub.ACCESS_FIELD_ID);
            }
        } else {
            Assert.shouldNotReachHere();
        }
        if (isOop) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(object.destroysRegister(), "register will be destroyed");
            }
            if (!value.isConstant() || (itemToObject(value) != null)) {
                masm.storeCheck(object.getRInfo().getRegister());
            }
        }
    }

    /**
     * Loads the specified array element into the destination register.
     *
     * @param  dst      destination register
     * @param  dstType  type of the array element
     * @param  array    the array item
     * @param  index    index of the element in the array
     * @param  info     associated debug information
     */
    public void indexedLoad(RInfo dst, int dstType, Item array, Item index,
            CodeEmitInfo info) {
        Address srcAddr = itemToArrayAddr(array, index, dstType, 0);
        int offset = masm.getOffset();
        if ((dstType == BasicType.INT) || (dstType == BasicType.OBJECT)
                || (dstType == BasicType.ARRAY)) {
            Register dstReg = dst.getRegister();
            masm.movl(dstReg, srcAddr);
        } else if (dstType == BasicType.BYTE) {
            Register dstReg = dst.getRegister();
            if (javac1.Flags.CodeForP6 || dstReg.hasByteRegister()) {
                offset = masm.loadSignedByte(dstReg, srcAddr);
            } else {
                addDebugInfoHere(info);
                if (javac1.Flags.GenerateCompilerNullChecks) {
                    masm.nullCheck(itemToReg(array));
                }
                int arrayRnr = itemToReg(array).getNumber();
                int indexRnr =
                    index.isConstant() ? -1 : itemToReg(index).getNumber();
                Register tmp = Register.NO_REG;
                for (int i = 0; (i < Register.NUM_REGISTERS)
                        && !tmp.hasByteRegister(); i++) {
                    if ((i != arrayRnr) && (i != indexRnr)) {
                        tmp = new Register(i);
                    }
                }
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(tmp.hasByteRegister(), "no free byte register found");
                }
                pushReg(tmp);
                offset = masm.loadSignedByte(tmp, srcAddr);
                moveReg(dstReg, tmp);
                pop(tmp);
            }
        } else if (dstType == BasicType.CHAR) {
            Register dstReg = dst.getRegister();
            offset = masm.loadUnsignedWord(dstReg, srcAddr);
        } else if (dstType == BasicType.SHORT) {
            Register dstReg = dst.getRegister();
            offset = masm.loadSignedWord(dstReg, srcAddr);
        } else if (dstType == BasicType.LONG) {
            Address srcAddrLo = itemToArrayAddrLo(array, index, dstType);
            Address srcAddrHi = itemToArrayAddrHi(array, index, dstType);
            Register loReg = dst.getRegisterLo();
            Register hiReg = dst.getRegisterHi();
            Register arrayReg = itemToReg(array);
            Register indexReg =
                index.isConstant() ? Register.NO_REG : itemToReg(index);
            if (hiReg.equals(arrayReg) || hiReg.equals(indexReg)) {
                masm.movl(loReg, srcAddrLo);
                masm.movl(hiReg, srcAddrHi);
            } else {
                masm.movl(hiReg, srcAddrHi);
                masm.movl(loReg, srcAddrLo);
            }
        } else if (dstType == BasicType.FLOAT) {
            masm.flds(srcAddr);
            fpuPush(dst);
        } else if (dstType == BasicType.DOUBLE) {
            masm.fldd(srcAddr);
            fpuPush(dst);
        } else {
            Assert.shouldNotReachHere();
        }
        addDebugInfo(offset, info, false);
    }

    /**
     * Stores a value into the specified array element. Note that the array
     * register will be destroyed if the value is an object pointer.
     *
     * @param  dstType  type of the array element
     * @param  array    the array item
     * @param  index    index of the element in the array
     * @param  value    the value to be stored
     * @param  info     associated debug information
     */
    public void indexedStore(int dstType, Item array, Item index, Item value,
            CodeEmitInfo info) {
        Address dstAddr = itemToArrayAddr(array, index, dstType, 0);
        addDebugInfoHere(info);
        if (value.isConstant()) {
            if (BasicType.isDoubleWord(dstType)) {
                Address dstAddrLo = itemToArrayAddrLo(array, index, dstType);
                Address dstAddrHi = itemToArrayAddrHi(array, index, dstType);
                masm.movl(dstAddrLo, itemToIntLo(value));
                masm.movl(dstAddrHi, itemToIntHi(value));
            } else if (dstType == BasicType.BYTE) {
                masm.movb(dstAddr, itemToInt(value) & 0xff);
            } else if ((dstType == BasicType.INT) || (dstType == BasicType.FLOAT)) {
                masm.movl(dstAddr, itemToInt(value));
            } else if (BasicType.isOop(dstType)) {
                masm.movl(dstAddr, itemToObject(value));
            } else {
                Assert.shouldNotReachHere();
            }
        } else if (value.isRegister()) {
            if (dstType == BasicType.BYTE) {
                Register valueReg = itemToReg(value);
                masm.movb(dstAddr, valueReg);
            } else if (dstType == BasicType.CHAR) {
                Register valueReg = itemToReg(value);
                masm.movw(dstAddr, valueReg);
            } else if (dstType == BasicType.SHORT) {
                Register valueReg = itemToReg(value);
                masm.signExtendShort(valueReg);
                masm.movw(dstAddr, valueReg);
            } else if ((dstType == BasicType.INT) || BasicType.isOop(dstType)) {
                Register valueReg = itemToReg(value);
                masm.movl(dstAddr, valueReg);
            } else if (dstType == BasicType.LONG) {
                Address dstAddrLo = itemToArrayAddrLo(array, index, dstType);
                Address dstAddrHi = itemToArrayAddrHi(array, index, dstType);
                masm.movl(dstAddrLo, itemToRegLo(value));
                masm.movl(dstAddrHi, itemToRegHi(value));
            } else if (dstType == BasicType.FLOAT) {
                fpuOnTop(value.getRInfo());
                masm.fstps(dstAddr);
                fpuPop(value.getRInfo());
            } else if (dstType == BasicType.DOUBLE) {
                fpuOnTop(value.getRInfo());
                masm.fstpd(dstAddr);
                fpuPop(value.getRInfo());
            } else {
                Assert.shouldNotReachHere();
            }
        } else {
            Assert.shouldNotReachHere();
        }
        if (BasicType.isOop(dstType)) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(array.destroysRegister(), "register will be destroyed");
            }
            if (!value.isConstant() || (itemToObject(value) != null)) {
                Register reg = itemToReg(array);
                Address adr = itemToArrayAddr(array, index, dstType, 0);
                masm.leal(reg, adr);
                masm.storeCheck(reg);
            }
        }
    }

    /**
     * Stores the negation of the specified value into the destination register.
     * Note that the value register will be destroyed.
     *
     * @param  dst    destination register
     * @param  value  value to be negated
     */
    public void negate(RInfo dst, Item value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(value.isRegister(), "can only handle registers");
            Assert.that(value.destroysRegister(), "register will be destroyed");
        }
        int tag = value.getType().getTag();
        if (tag == ValueType.intTag) {
            masm.negl(itemToReg(value));
            moveReg(dst.getRegister(), itemToReg(value));
        } else if ((tag == ValueType.floatTag) || (tag == ValueType.doubleTag)) {
            fpuOnTop(value.getRInfo());
            fpuPop(value.getRInfo());
            masm.fchs();
            fpuPush(dst);
        } else if (tag == ValueType.longTag) {
            Register loReg = itemToRegLo(value);
            Register hiReg = itemToRegHi(value);
            masm.lneg(hiReg, loReg);
            if (dst.getRegisterLo().equals(hiReg)) {
                moveReg(dst.getRegisterHi(), hiReg);
                moveReg(dst.getRegisterLo(), loReg);
            } else {
                moveReg(dst.getRegisterLo(), loReg);
                moveReg(dst.getRegisterHi(), hiReg);
            }
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Emits code for an intrinsic mathematical function. Note that the value
     * register will be destroyed.
     *
     * @param  id     the intrinsic identification number
     * @param  dst    destination register
     * @param  value  the argument value
     */
    public void mathIntrinsic(int id, RInfo dst, Item value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(value.isRegister(), "can only handle registers");
            Assert.that(value.destroysRegister(), "register will be destroyed");
            Assert.that(value.getType().getTag() == ValueType.doubleTag, "invalid value type");
        }
        MathStub stub = null;
        fpuOnTop(value.getRInfo());
        fpuPop(value.getRInfo());
        switch (id) {
        case Intrinsic.DSIN:
            stub = new MathStub(Runtime1.getRuntimeFnPtr(Runtime1.DSIN));
            masm.fsin();
            masm.jC2(Register.NO_REG, stub.getEntry());
            break;
        case Intrinsic.DCOS:
            stub = new MathStub(Runtime1.getRuntimeFnPtr(Runtime1.DCOS));
            masm.fcos();
            masm.jC2(Register.NO_REG, stub.getEntry());
            break;
        case Intrinsic.DSQRT:
            masm.fsqrt();
            break;
        default:
            Assert.shouldNotReachHere();
        }
        if (stub != null) {
            slowCaseStubs.add(stub);
            masm.bind(stub.getContinuation());
        }
        fpuPush(dst);
    }

    /**
     * Emits code for an arithmetic integer operation. Note that the register of
     * the left operand will be destroyed.
     *
     * @param  code    operation code
     * @param  result  result item
     * @param  left    left operand
     * @param  right   right operand
     */
    public void arithmeticOpInt(int code, Item result, Item left, Item right) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(left.isRegister(), "wrong item state");
            Assert.that(left.destroysRegister(), "register will be destroyed");
        }
        Register lreg = itemToReg(left);
        if (right.isRegister()) {
            Register rreg = itemToReg(right);
            switch (code) {
            case Bytecodes._iadd:
                masm.addl(lreg, rreg);
                break;
            case Bytecodes._isub:
                masm.subl(lreg, rreg);
                break;
            case Bytecodes._imul:
                masm.imull(lreg, rreg);
                break;
            default:
                Assert.shouldNotReachHere();
            }
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(result.isRegister(), "can only handle register results");
            }
        } else if (right.isConstant()) {
            int rconst = itemToInt(right);
            switch (code) {
            case Bytecodes._iadd:
                masm.increment(lreg, rconst);
                break;
            case Bytecodes._isub:
                masm.decrement(lreg, rconst);
                break;
            default:
                Assert.shouldNotReachHere();
            }
        } else if (right.isStack()) {
            Address raddr = stackItemToAddr(right, false);
            switch (code) {
            case Bytecodes._iadd:
                masm.addl(lreg, raddr);
                break;
            case Bytecodes._isub:
                masm.subl(lreg, raddr);
                break;
            default:
                Assert.shouldNotReachHere();
            }
        }
        moveReg(itemToReg(result), lreg);
    }

    /**
     * Emits code for a long arithmetic integer operation. Note that the
     * register of the left operand will be destroyed.
     *
     * @param  code    operation code
     * @param  result  result item
     * @param  left    left operand
     * @param  right   right operand
     */
    public void arithmeticOpLong(int code, Item result, Item left, Item right) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(left.isRegister() && right.isRegister(), "wrong item state");
            Assert.that(left.destroysRegister(), "register will be destroyed");
        }
        Register leftLo = itemToRegLo(left);
        Register leftHi = itemToRegHi(left);
        Register rightLo = itemToRegLo(right);
        Register rightHi = itemToRegHi(right);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!leftLo.equals(rightHi), "overwriting registers");
        }
        if (code == Bytecodes._ladd) {
            masm.addl(leftLo, rightLo);
            masm.adcl(leftHi, rightHi);
        } else {
            masm.subl(leftLo, rightLo);
            masm.sbbl(leftHi, rightHi);
        }
        if (itemToRegLo(result).equals(leftHi)) {
            moveReg(itemToRegHi(result), leftHi);
            moveReg(itemToRegLo(result), leftLo);
        } else {
            moveReg(itemToRegLo(result), leftLo);
            moveReg(itemToRegHi(result), leftHi);
        }
    }

    /**
     * Performs a long multiplication of two values on the stack. The result
     * will be stored in EDX:EAX.
     */
    public void longMul() {
        pushReg(Register.ECX);
        pushReg(Register.EBX);
        masm.lmul(16, 8);
        pop(Register.EBX);
        pop(Register.ECX);
        masm.addl(Register.ESP, 16);
        decStackAfterCall(4);
    }

    /**
     * Performs an arithmetic floating-point operation. Note that both operand
     * registers will be destroyed.
     *
     * @param  code    operation code
     * @param  result  result item
     * @param  left    left operand
     * @param  right   right operand
     */
    public void arithmeticOpFloat(int code, Item result, Item left, Item right) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(left.isRegister() && result.isRegister(), "wrong item state");
            Assert.that(left.destroysRegister() && right.destroysRegister(), "registers will be destroyed");
        }
        if (right.isRegister()) {
            boolean mustBeOrdered =
                (code != Bytecodes._fadd) && (code != Bytecodes._dadd)
                && (code != Bytecodes._fmul) && (code != Bytecodes._dmul);
            fpuTwoOnTop(right.getRInfo(), left.getRInfo(), mustBeOrdered);
            switch (code) {
            case Bytecodes._fadd:
                /* falls through */
            case Bytecodes._dadd:
                masm.faddp(1);
                break;
            case Bytecodes._fsub:
                /* falls through */
            case Bytecodes._dsub:
                masm.fsubp(1);
                break;
            case Bytecodes._fmul:
                /* falls through */
            case Bytecodes._dmul:
                masm.fmulp(1);
                break;
            case Bytecodes._fdiv:
                /* falls through */
            case Bytecodes._ddiv:
                masm.fdivp(1);
                break;
            case Bytecodes._frem:
                /* falls through */
            case Bytecodes._drem:
                masm.fxch(1);
                masm.fremr(Register.NO_REG);
                break;
            default:
                Assert.shouldNotReachHere();
            }
            fpuPop(right.getRInfo());
            fpuPop(left.getRInfo());
        } else if (right.isStack()) {
            fpuOnTop(left.getRInfo());
            Address addr = stackItemToAddr(right, false);
            switch (code) {
            case Bytecodes._fadd:
                masm.fadds(addr);
                break;
            case Bytecodes._dadd:
                masm.faddd(addr);
                break;
            case Bytecodes._fsub:
                masm.fsubs(addr);
                break;
            case Bytecodes._dsub:
                masm.fsubd(addr);
                break;
            case Bytecodes._fmul:
                masm.fmuls(addr);
                break;
            case Bytecodes._dmul:
                masm.fmuld(addr);
                break;
            case Bytecodes._fdiv:
                masm.fdivs(addr);
                break;
            case Bytecodes._ddiv:
                masm.fdivd(addr);
                break;
            default:
                Assert.shouldNotReachHere();
            }
            fpuPop(left.getRInfo());
        } else {
            Assert.shouldNotReachHere();
        }
        fpuPush(result.getRInfo());
    }

    /**
     * Computes quotient or remainder of an integer division.
     *
     * @param  code    operation code
     * @param  result  result item
     * @param  left    dividend item
     * @param  right   divisor item
     */
    public void arithmeticIntDiv(int code, Item result, Item left, Item right,
            CodeEmitInfo info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(left.isRegister() && right.isRegister() && result.isRegister(), "wrong item state");
            Assert.that(left.destroysRegister() && right.destroysRegister(), "registers will be destroyed");
        }
        moveReg(Register.EAX, itemToReg(left));
        int offset = masm.correctedIntDiv(itemToReg(right));
        addDebugInfo(offset, info, false);
        if (code == Bytecodes._idiv) {
            moveReg(itemToReg(result), Register.EAX);
        } else {
            moveReg(itemToReg(result), Register.EDX);
        }
    }

    /**
     * Saves registers on the stack before an arithmetic call.
     *
     * @param  resultType  value type of the result
     * @see    #arithmeticCallEpilog(ValueType)
     */
    public void arithmeticCallProlog(ValueType resultType) {
        pushReg(Register.ESI);
        pushReg(Register.EDI);
        pushReg(Register.ECX);
        pushReg(Register.EBX);
        if (resultType.isFloatKind()) {
            pushReg(Register.EDX);
            pushReg(Register.EAX);
        } else if (resultType.isSingleWord()) {
            pushReg(Register.EDX);
        }
    }

    /**
     * Restores registers from the stack after an arithmetic call.
     *
     * @param  resultType  value type of the result
     * @see    #arithmeticCallProlog(ValueType)
     */
    public void arithmeticCallEpilog(ValueType resultType) {
        if (resultType.isFloatKind()) {
            pop(Register.EAX);
            pop(Register.EDX);
        } else if (resultType.isSingleWord()) {
            pop(Register.EDX);
        }
        pop(Register.EBX);
        pop(Register.ECX);
        pop(Register.EDI);
        pop(Register.ESI);
    }

    /**
     * Calls the runtime for an arithmetic operation.
     *
     * @param  code  operation code
     */
    public void arithmeticCallOp(int code) {
        bangStack();
        switch (code) {
        case Bytecodes._lmul:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.LMUL), 4);
            decStackAfterCall(4);
            break;
        case Bytecodes._ldiv:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.LDIV), 4);
            decStackAfterCall(4);
            break;
        case Bytecodes._lrem:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.LREM), 4);
            decStackAfterCall(4);
            break;
        case Bytecodes._frem:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.FREM), 2);
            decStackAfterCall(2);
            break;
        case Bytecodes._drem:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.DREM), 4);
            decStackAfterCall(4);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Shifts the specified value to the left or right. Note that the value
     * register will be destroyed.
     *
     * @param  code    operation code
     * @param  dstReg  destination register
     * @param  value   value to be shifted
     * @param  count   number of bits to shift by
     * @param  tmp     temporary register
     */
    public void shiftOp(int code, RInfo dstReg, Item value, Item count, RInfo tmp) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(value.destroysRegister(), "register will be destroyed");
        }
        if (value.getType().isSingleWord()) {
            Register valueReg = itemToReg(value);
            if (count.isRegister()) {
                Register countReg = itemToReg(count);
                Register tempReg = Register.NO_REG;
                if (valueReg.equals(SHIFT_COUNT_REG)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(tmp.isValid() && !countReg.equals(SHIFT_COUNT_REG), "not allocated properly");
                    }
                    masm.movl(tmp.getRegister(), valueReg);
                    valueReg = tmp.getRegister();
                    masm.movl(SHIFT_COUNT_REG, countReg);
                    countReg = SHIFT_COUNT_REG;
                } else if (!countReg.equals(SHIFT_COUNT_REG)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(tmp.isValid(), "not allocated properly");
                    }
                    tempReg = tmp.getRegister();
                    masm.movl(tempReg, SHIFT_COUNT_REG);
                    masm.movl(SHIFT_COUNT_REG, countReg);
                    countReg = SHIFT_COUNT_REG;
                }
                switch (code) {
                case Bytecodes._ishl:
                    masm.shll(valueReg);
                    break;
                case Bytecodes._ishr:
                    masm.sarl(valueReg);
                    break;
                case Bytecodes._iushr:
                    masm.shrl(valueReg);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
                if (tempReg.isValid()) {
                    masm.movl(SHIFT_COUNT_REG, tempReg);
                }
                moveReg(dstReg.getRegister(), valueReg);
            } else if (count.isConstant()) {
                int cint = itemToInt(count) & 0x1f;
                switch (code) {
                case Bytecodes._ishl:
                    masm.shll(valueReg, cint);
                    break;
                case Bytecodes._ishr:
                    masm.sarl(valueReg, cint);
                    break;
                case Bytecodes._iushr:
                    masm.shrl(valueReg, cint);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
                moveReg(dstReg.getRegister(), valueReg);
            } else {
                Assert.shouldNotReachHere();
            }
        } else if (count.isRegister()) {
            Register countReg = itemToReg(count);
            Register loReg = itemToRegLo(value);
            Register hiReg = itemToRegHi(value);
            boolean loSwapped = false;
            boolean hiSwapped = false;
            if (hiReg.equals(SHIFT_COUNT_REG)) {
                hiSwapped = true;
                swapReg(hiReg, countReg);
                hiReg = countReg;
            } else if (loReg.equals(SHIFT_COUNT_REG)) {
                loSwapped = true;
                swapReg(loReg, countReg);
                loReg = countReg;
            } else if (!countReg.equals(SHIFT_COUNT_REG)) {
                pushReg(SHIFT_COUNT_REG);
                masm.movl(SHIFT_COUNT_REG, countReg);
            }
            switch (code) {
            case Bytecodes._lshl:
                masm.lshl(hiReg, loReg);
                break;
            case Bytecodes._lshr:
                masm.lshr(hiReg, loReg, true);
                break;
            case Bytecodes._lushr:
                masm.lshr(hiReg, loReg, false);
                break;
            default:
                Assert.shouldNotReachHere();
            }
            if (hiSwapped) {
                swapReg(itemToRegHi(value), countReg);
            } else if (loSwapped) {
                swapReg(itemToRegLo(value), countReg);
            } else if (!countReg.equals(SHIFT_COUNT_REG)) {
                pop(SHIFT_COUNT_REG);
            }
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Performs a bitwise logical operation. Note that the register of the left
     * operand will be destroyed.
     *
     * @param  code    operation code
     * @param  dstReg  destination register
     * @param  left    left operand
     * @param  right   right operand
     */
    public void logicOp(int code, RInfo dstReg, Item left, Item right) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(left.destroysRegister(), "register will be destroyed");
        }
        if (left.getType().isSingleWord()) {
            Register leftReg = itemToReg(left);
            if (right.isConstant()) {
                int value = itemToInt(right);
                switch (code) {
                case Bytecodes._iand:
                    masm.andl(leftReg, value);
                    break;
                case Bytecodes._ior:
                    masm.orl(leftReg, value);
                    break;
                case Bytecodes._ixor:
                    masm.xorl(leftReg, value);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
            } else {
                Register rightReg = itemToReg(right);
                switch (code) {
                case Bytecodes._iand:
                    masm.andl(leftReg, rightReg);
                    break;
                case Bytecodes._ior:
                    masm.orl(leftReg, rightReg);
                    break;
                case Bytecodes._ixor:
                    masm.xorl(leftReg, rightReg);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
            }
            moveReg(dstReg.getRegister(), leftReg);
        } else {
            Register leftLo = itemToRegLo(left);
            Register leftHi = itemToRegHi(left);
            if (right.isConstant()) {
                int rightLo = itemToIntLo(right);
                int rightHi = itemToIntHi(right);
                switch (code) {
                case Bytecodes._land:
                    masm.andl(leftLo, rightLo);
                    masm.andl(leftHi, rightHi);
                    break;
                case Bytecodes._lor:
                    masm.orl(leftLo, rightLo);
                    masm.orl(leftHi, rightHi);
                    break;
                case Bytecodes._lxor:
                    masm.xorl(leftLo, rightLo);
                    masm.xorl(leftHi, rightHi);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
            } else {
                Register rightLo = itemToRegLo(right);
                Register rightHi = itemToRegHi(right);
                switch (code) {
                case Bytecodes._land:
                    masm.andl(leftLo, rightLo);
                    masm.andl(leftHi, rightHi);
                    break;
                case Bytecodes._lor:
                    masm.orl(leftLo, rightLo);
                    masm.orl(leftHi, rightHi);
                    break;
                case Bytecodes._lxor:
                    masm.xorl(leftLo, rightLo);
                    masm.xorl(leftHi, rightHi);
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
            }
            if (dstReg.getRegisterLo().equals(leftHi)) {
                moveReg(dstReg.getRegisterHi(), leftHi);
                moveReg(dstReg.getRegisterLo(), leftLo);
            } else {
                moveReg(dstReg.getRegisterLo(), leftLo);
                moveReg(dstReg.getRegisterHi(), leftHi);
            }
        }
    }

    /**
     * Stores the result of a comparison into the destination register. Note
     * that depending on the type either the left operand register or both
     * operand registers will be destroyed.
     *
     * @param  code    operation code
     * @param  dstReg  destination register
     * @param  left    left operand
     * @param  right   right operand
     */
    public void compareOp(int code, RInfo dstReg, Item left, Item right) {
        if (left.getType().isFloatKind()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(left.destroysRegister() && right.destroysRegister(), "registers will be destroyed");
            }
            fpuTwoOnTop(left.getRInfo(), right.getRInfo(), true);
            fpuPop(left.getRInfo());
            fpuPop(right.getRInfo());
            masm.fcmp2int(dstReg.getRegister(),
                (code == Bytecodes._fcmpl) || (code == Bytecodes._dcmpl));
        } else if (code == Bytecodes._lcmp) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(left.destroysRegister(), "register will be destroyed");
            }
            masm.lcmp2int(itemToRegHi(left), itemToRegLo(left),
                itemToRegHi(right), itemToRegLo(right));
            moveReg(dstReg.getRegister(), itemToRegHi(left));
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Saves registers on the stack before a conversion call.
     *
     * @param  resultType  value type of the result
     * @see    #arithmeticCallProlog(ValueType)
     */
    public void callConvertProlog(ValueType resultType) {
        arithmeticCallProlog(resultType);
    }

    /**
     * Restores registers from the stack after a conversion call.
     *
     * @param  resultType  value type of the result
     * @see    #arithmeticCallEpilog(ValueType)
     */
    public void callConvertEpilog(ValueType resultType) {
        arithmeticCallEpilog(resultType);
    }

    /**
     * Calls the runtime for a type conversion.
     *
     * @param  code  operation code
     */
    public void callConvertOp(int code) {
        bangStack();
        switch (code) {
        case Bytecodes._l2f:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.L2F), 2);
            decStackAfterCall(2);
            break;
        case Bytecodes._l2d:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.L2D), 2);
            decStackAfterCall(2);
            break;
        case Bytecodes._f2i:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.F2I), 1);
            decStackAfterCall(1);
            break;
        case Bytecodes._f2l:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.F2L), 1);
            decStackAfterCall(1);
            break;
        case Bytecodes._d2i:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.D2I), 2);
            decStackAfterCall(2);
            break;
        case Bytecodes._d2l:
            masm.callVMLeaf(Runtime1.getRuntimeFnPtr(Runtime1.D2L), 2);
            decStackAfterCall(2);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Stores the result of a type conversion into the destination register.
     *
     * @param  code   operation code
     * @param  dst    destination register
     * @param  value  value to be converted
     */
    public void convertOp(int code, RInfo dst, Item value) {
        if (value.getType().isFloatKind()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(value.destroysRegister(), "register will be destroyed");
            }
            fpuOnTop(value.getRInfo());
            fpuPop(value.getRInfo());
        }
        if (dst.isFloatKind()) {
            fpuPush(dst);
        }
        switch (code) {
        case Bytecodes._i2l:
            moveReg(dst.getRegisterLo(), itemToReg(value));
            moveReg(dst.getRegisterHi(), itemToReg(value));
            masm.sarl(dst.getRegisterHi(), 31);
            break;
        case Bytecodes._i2f:
            /* falls through */
        case Bytecodes._i2d:
            if (value.isRegister()) {
                pushReg(itemToReg(value));
                masm.filds(new Address(Register.ESP));
                decStack(1);
            } else if (value.isStack()) {
                Address src = stackItemToAddr(value, false);
                masm.filds(src);
            } else {
                Assert.shouldNotReachHere();
            }
            break;
        case Bytecodes._l2i:
            moveReg(dst.getRegister(), itemToRegLo(value));
            break;
        case Bytecodes._f2d:
            /* falls through */
        case Bytecodes._d2f:
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(value.isRegister(), "wrong value state");
            }
            break;
        case Bytecodes._i2b:
            moveReg(dst.getRegister(), itemToReg(value));
            masm.signExtendByte(dst.getRegister());
            break;
        case Bytecodes._i2c:
            moveReg(dst.getRegister(), itemToReg(value));
            masm.andl(dst.getRegister(), 0xffff);
            break;
        case Bytecodes._i2s:
            moveReg(dst.getRegister(), itemToReg(value));
            masm.signExtendShort(dst.getRegister());
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Tries fast conversion of a floating-point value to an integer.
     *
     * @param  code     operation code
     * @param  value    value to be converted
     * @param  dstReg   destination register
     * @param  is32bit  specifies the floating-point precision
     */
    public void fastCallConvertOp(int code, Item value, RInfo dst,
            boolean is32bit) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((code == Bytecodes._f2i) || (code == Bytecodes._d2i), "illegal operation code");
        }
        int entry = Runtime1.getRuntimeFnPtr(Runtime1.F2I);
        fpuOnTop(value.getRInfo());
        ConvertStub stub = new ConvertStub(entry, dst.getRegister());
        slowCaseStubs.add(stub);
        masm.fldcw(new Address(Runtime1.getAddrFpuCntrlWrdTrunc(), RelocInfo.NONE));
        masm.pushl(Register.EAX);
        masm.fists(new Address(Register.ESP));
        masm.popl(Register.EAX);
        masm.cmpl(Register.EAX, 0x80000000);
        masm.jcc(Assembler.EQUAL, stub.getEntry());
        masm.fpop();
        masm.bind(stub.getContinuation());
        if (is32bit) {
            set32bitFpuPrecision();
        } else {
            restoreFpuPrecision();
        }
        fpuPop(value.getRInfo());
    }

    /**
     * Emits code for a virtual or static procedure call.
     *
     * @param  code            call instruction code
     * @param  sigTypes        type signature of the procedure
     * @param  argSize         total size of the arguments
     * @param  info            associated debug information
     * @param  optimized       whether or not virtual calls are optimized
     * @param  needsNullCheck  whether or not receiver is checked for null
     */
    public void callOp(int code, List sigTypes, int argSize, CodeEmitInfo info,
            boolean optimized, boolean needsNullCheck) {
        nopsForZombie();
        switch (code) {
        case Bytecodes._invokestatic:
            callStubs.add(new StaticCallStub(masm.getCodePos(), null));
            masm.call(Runtime1.getStubEntry(Runtime1.RESOLVE_INVOKESTATIC_ID),
                RelocInfo.STATIC_CALL_TYPE);
            break;
        case Bytecodes._invokevirtual:
            /* falls through */
        case Bytecodes._invokespecial:
            /* falls through */
        case Bytecodes._invokeinterface:
            if (needsNullCheck && javac1.Flags.GenerateCompilerNullChecks) {
                addDebugInfoHere(info);
                masm.nullCheck(RECV_REG);
            }
            if ((code == Bytecodes._invokespecial) || optimized) {
                callStubs.add(new StaticCallStub(masm.getCodePos(), null));
                masm.call(Runtime1.getStubEntry(Runtime1.RESOLVE_INVOKE_ID),
                    RelocInfo.OPT_VIRTUAL_CALL_TYPE);
            } else {
                int firstOop = masm.getCodePos();
                int oopLimit = 0;
                masm.movl(IC_KLASS_REG, (Obj) null);
                int addr = masm.getCodePos();
                RelocInfo reloc = new RelocInfo(RelocInfo.VIRTUAL_CALL_TYPE,
                    (addr - firstOop) / RelocInfo.OFFSET_UNIT, (addr - oopLimit) / RelocInfo.OFFSET_UNIT);
                masm.call(Runtime1.getStubEntry(Runtime1.RESOLVE_INVOKE_ID), reloc);
            }
            break;
        default:
            Assert.shouldNotReachHere();
        }
        addCallInfoHere(info);
        decStackAfterCall(argSize);
    }

    /**
     * Constructs a new instance of the specified class.
     *
     * @param  dst       destination register for object address
     * @param  klass     class to be instantiated
     * @param  tmp1      temporary register
     * @param  tmp2      temporary register
     * @param  tmp3      temporary register
     * @param  klassReg  register for caching the instance class
     * @param  info      associated debug information
     */
    public void newInstance(RInfo dst, InstanceKlass klass, RInfo tmp1, RInfo tmp2,
            RInfo tmp3, RInfo klassReg, CodeEmitInfo info) {
        Register obj = dst.getRegister();
        Register kl = klassReg.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.equals(Register.EAX), "must be eax");
        }
        NewInstanceStub stub = new NewInstanceStub(kl, info, espOffset);
        slowCaseStubs.add(stub);
        objectToRegWithPatching(klassReg, klass, info);
        if (javac1.Flags.UseFastNewInstance && klass.isLoaded() && !klass.hasFinalizer()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(klass.isLoaded(), "should not reach here");
                Assert.that(klass.getSizeHelper() > 0, "size helper should be > 0");
            }
            if (!klass.isInitialized()) {
                masm.cmpl(new Address(kl, Runtime1.getInitStateOffset()), Runtime1.getFullyInitializedState());
                masm.jcc(Assembler.NOT_EQUAL, stub.getEntry());
            }
            int instanceSize = Runtime1.alignObjectSize(klass.getSizeHelper());
            masm.allocateObject(obj, tmp1.getRegister(), tmp2.getRegister(),
                Runtime1.getOopDescHeaderSize(), instanceSize, kl, stub.getEntry());
        } else {
            masm.jmp(stub.getEntry());
        }
        masm.bind(stub.getContinuation());
        masm.verifyOop(obj);
        moveReg(dst.getRegister(), obj);
    }

    /**
     * Constructs a new array of the specified basic type.
     *
     * @param  dst       destination register for the array address
     * @param  elemType  basic element type
     * @param  length    length of the array
     * @param  tmp1      temporary register
     * @param  tmp2      temporary register
     * @param  klassReg  register for caching the array class
     * @param  info      associated debug information
     */
    public void newTypeArray(RInfo dst, int elemType, Item length, RInfo tmp1,
            RInfo tmp2, RInfo klassReg, CodeEmitInfo info) {
        Register obj = dst.getRegister();
        Register len = tmp1.getRegister();
        Register t = tmp2.getRegister();
        Register k = klassReg.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.equals(Register.EAX), "must be eax");
        }
        moveReg(len, itemToReg(length));
        NewTypeArrayStub stub = new NewTypeArrayStub(elemType, len,
            Register.NO_REG, info, espOffset);
        slowCaseStubs.add(stub);
        ArrayKlass klass = ArrayKlass.make(elemType);
        if (javac1.Flags.UseFastNewTypeArray) {
            masm.movl(k, klass);
            masm.allocateArray(obj, len, t, Runtime1.getArrayHeaderSize(elemType),
                arrayElementSize(elemType), k, stub.getEntry());
        } else {
            masm.jmp(stub.getEntry());
        }
        masm.bind(stub.getContinuation());
        masm.verifyOop(obj);
        moveReg(dst.getRegister(), obj);
    }

    /**
     * Constructs a new array of the specified reference type.
     *
     * @param  dst        destination register for the array address
     * @param  elemClass  element reference type
     * @param  length     length of the array
     * @param  tmp1       temporary register
     * @param  tmp2       temporary register
     * @param  info       associated debug information
     */
    public void newObjectArray(RInfo dst, Klass elemClass, Item length,
            RInfo tmp1, RInfo tmp2, RInfo tmp3, CodeEmitInfo info) {
        Register obj = dst.getRegister();
        Register len = tmp1.getRegister();
        Register t = tmp2.getRegister();
        Register tmpReg = tmp3.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.equals(Register.EAX), "must be eax");
        }
        moveReg(len, itemToReg(length));
        NewObjectArrayStub stub = new NewObjectArrayStub(tmpReg, len,
            Register.NO_REG, info, espOffset);
        slowCaseStubs.add(stub);
        objectToRegWithPatching(tmp3, ArrayKlass.make(elemClass), info);
        if (javac1.Flags.UseFastNewObjectArray) {
            masm.allocateArray(obj, len, t, Runtime1.getArrayHeaderSize(BasicType.OBJECT),
                Address.TIMES_4, tmpReg, stub.getEntry());
        } else {
            masm.jmp(stub.getEntry());
        }
        masm.bind(stub.getContinuation());
        masm.verifyOop(obj);
        moveReg(dst.getRegister(), obj);
    }

    /**
     * Constructs a new multidimensional array of the specified type.
     *
     * @param  dst    destination register for the array address
     * @param  klass  type of the array
     * @param  rank   number of dimensions
     * @param  tmp    temporary register
     * @param  info   associated debug information
     */
    public void newMultiArray(RInfo dst, Klass klass, int rank, RInfo tmp,
            CodeEmitInfo info) {
        Register dstReg = dst.getRegister();
        Register tmpReg = tmp.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!dstReg.equals(tmpReg), "registers must be different");
        }
        masm.movl(tmpReg, Register.ESP);
        objectToRegWithPatching(dst, klass, info);
        pushReg(dstReg);
        pushInt(rank);
        pushReg(tmpReg);
        nopsForZombie();
        masm.call(Runtime1.getStubEntry(Runtime1.NEW_MULTI_ARRAY_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        addCallInfoHere(info);
        decStackAfterCall(3);
        decStack(rank);
        moveReg(dstReg, Register.EAX);
    }

    /**
     * Throws the specified exception.
     *
     * @param  exception  exception to be thrown
     * @param  info       associated debug information
     */
    public void throwOp(Item exception, CodeEmitInfo info) {
        moveReg(EXCEPT_OOP_REG, itemToReg(exception));
        if (javac1.Flags.GenerateCompilerNullChecks) {
            addDebugInfoHere(info);
            masm.nullCheck(EXCEPT_OOP_REG);
        }
        masm.nop();
        int pos = masm.getCodePos();
        int offset = masm.getOffset();
        masm.nop();
        masm.leal(Register.EDX, new Address(pos, RelocInfo.INTERNAL_WORD_TYPE));
        addCallInfo(offset, info);
        masm.pushl(Register.EDX);
        if (javac1.Flags.UseCompilerSafepoints) {
            espOffset++;
            List oopRegs = new ArrayList();
            oopRegs.add(FrameMap.EAX_RINFO);
            addDebugInfoForBranch(info.getBci(), oopRegs);
            espOffset--;
        }
        masm.jmp(throwEntryLabel);
        masm.nop();
    }

    /**
     * Checks that an object has the specified type. Note that the object
     * register will be destroyed.
     *
     * @param  dst       destination register
     * @param  obj       object to be checked
     * @param  klass     type to check object for
     * @param  klassReg  register for caching the instance class
     * @param  info      associated debug information
     */
    public void checkcastOp(RInfo dst, Item obj, Klass klass, RInfo klassReg,
            CodeEmitInfo info) {
        Register objReg = itemToReg(obj);
        Register objKlass = objReg;
        Register dstReg = dst.getRegister();
        Register testKlass = klassReg.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.destroysRegister(), "register will be destroyed");
            Assert.that(!testKlass.equals(objReg) && !testKlass.equals(dstReg), "registers must be different");
            Assert.that(!dstReg.equals(objReg), "overwriting destination register");
        }
        Label done = new Label();
        objectToRegWithPatching(klassReg, klass, info);
        masm.verifyOop(objReg);
        masm.movl(dstReg, objReg);
        masm.testl(objReg, objReg);
        masm.jcc(Assembler.ZERO, done);
        masm.movl(objKlass, new Address(objReg, Runtime1.getKlassOffset()));
        CheckCastStub stub = new CheckCastStub(testKlass, info, dstReg, espOffset);
        slowCaseStubs.add(stub);
        if (klass.isLoaded() && (klass instanceof InstanceKlass)
                && ((InstanceKlass) klass).isFinal()) {
            masm.cmpl(objKlass, testKlass);
        } else {
            masm.cmpl(testKlass, new Address(objKlass,
                Runtime1.getIsACache1Offset() + 8));
            masm.jcc(Assembler.EQUAL, done);
            masm.cmpl(testKlass, new Address(objKlass,
                Runtime1.getIsACache2Offset() + 8));
        }
        masm.jcc(Assembler.NOT_EQUAL, stub.getEntry());
        masm.bind(stub.getContinuation());
        masm.bind(done);
    }

    /**
     * Tests if an object has the specified type. Note that the object register
     * will be destroyed.
     *
     * @param  dst       destination register
     * @param  obj       object to be checked
     * @param  klass     type to check object for
     * @param  klassReg  register for caching the instance class
     * @param  info      associated debug information
     */
    public void instanceofOp(RInfo dst, Item obj, Klass klass, RInfo klassReg,
            CodeEmitInfo info) {
        Register objReg = itemToReg(obj);
        Register dstReg = dst.getRegister();
        Register testKlass = klassReg.getRegister();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.destroysRegister(), "register will be destroyed");
            Assert.that(!testKlass.equals(dstReg) && !testKlass.equals(objReg), "registers must be different");
            Assert.that(!dstReg.equals(objReg), "overwriting destination register");
        }
        Label done = new Label();
        masm.verifyOop(objReg);
        masm.xorl(dstReg, dstReg);
        masm.testl(objReg, objReg);
        masm.jcc(Assembler.ZERO, done);
        masm.movl(objReg, new Address(objReg, Runtime1.getKlassOffset()));
        objectToRegWithPatching(klassReg, klass, info);
        InstanceOfStub stub = new InstanceOfStub(objReg, dstReg, testKlass,
            info, espOffset);
        slowCaseStubs.add(stub);
        if (klass.isLoaded() && (klass instanceof InstanceKlass)
                && ((InstanceKlass) klass).isFinal()) {
            masm.cmpl(objReg, testKlass);
            masm.jcc(Assembler.NOT_EQUAL, done);
            masm.incl(dstReg);
        } else {
            masm.incl(dstReg);
            masm.cmpl(testKlass, new Address(objReg,
                Runtime1.getIsACache1Offset() + 8));
            masm.jcc(Assembler.EQUAL, done);
            masm.cmpl(testKlass, new Address(objReg,
                Runtime1.getIsACache2Offset() + 8));
            masm.jcc(Assembler.NOT_EQUAL, stub.getEntry());
        }
        masm.bind(stub.getContinuation());
        masm.bind(done);
    }

    /**
     * Jumps to the start of the specified basic block.
     *
     * @param  dst               basic block to jump to
     * @param  bci               bytecode index of the branch
     * @param  isBackwardBranch  whether or not it is a backward branch
     * @param  oopRegs           list of registers that contain pointers
     */
    public void gotoOp(BlockBegin dst, int bci, boolean isBackwardBranch,
            List oopRegs) {
        if (isBackwardBranch) {
            addDebugInfoForBranch(bci, oopRegs);
        }
        masm.jmp(dst.getLabel());
    }

    /**
     * Jumps to the specified subroutine.
     *
     * @param  dst  first basic block of the subroutine
     * @param  bci  byte code index of the jump
     */
    public void jsrOp(BlockBegin dst, int bci) {
        Label l1 = new Label();
        Label l2 = new Label();
        if (javac1.Flags.UseCompilerSafepoints) {
            masm.getCode().relocate(masm.getCodePos(), RelocInfo.SAFEPOINT_TYPE);
            CodeEmitInfo info = new CodeEmitInfo(bci, null, null);
            addDebugInfoHere(info);
        }
        masm.call(l1, RelocInfo.NONE);
        masm.jmp(l2);
        masm.bind(l1);
        masm.popl(Register.EAX);
        masm.jmp(dst.getLabel());
        masm.bind(l2);
    }

    /**
     * Returns from the current subroutine.
     *
     * @param  localIx  index of the variable with the return address
     * @param  reg      register if the variable is cached
     * @param  bci      bytecode index of the return instruction
     */
    public void retOp(int localIx, RInfo reg, int bci) {
        if (reg.isIllegal()) {
            masm.movl(Register.EAX, frameMap.localAddress(localIx, false));
        } else {
            masm.movl(Register.EAX, reg.getRegister());
        }
        if (javac1.Flags.UseCompilerSafepoints) {
            addDebugInfoForBranch(bci, null);
        }
        masm.jmp(Register.EAX, RelocInfo.NONE);
        if (javac1.Flags.UseCompilerSafepoints) {
            masm.nop();
        }
    }

    /**
     * Emits the first part of a conditional jump.
     *
     * @param  cond    condition code
     * @param  cmpTag  type tag of the values being compared
     * @param  x       value to compare
     * @param  y       value to be compared with
     */
    public int if1(int cond, int cmpTag, Item x, Item y) {
        boolean useTestOp = y.isConstant() && ((cond == If.EQ) || (cond == If.NE))
            && (((cmpTag == ValueType.objectTag) && (itemToObject(y) == null))
            || ((cmpTag == ValueType.intTag) && (itemToInt(y) == 0)));
        if (y.isConstant()) {
            Register xreg = itemToReg(x);
            if (useTestOp) {
                masm.testl(xreg, xreg);
            } else if (cmpTag == ValueType.objectTag) {
                masm.cmpl(xreg, itemToObject(y));
            } else if (cmpTag == ValueType.intTag) {
                masm.cmpl(xreg, itemToInt(y));
            } else {
                Assert.shouldNotReachHere();
            }
        } else if (y.isStack()) {
            Register xreg = itemToReg(x);
            if ((cmpTag == ValueType.intTag) || (cmpTag == ValueType.objectTag)) {
                masm.cmpl(xreg, stackItemToAddr(y, false));
            } else {
                Assert.shouldNotReachHere();
            }
        } else {
            if ((cmpTag == ValueType.intTag) || (cmpTag == ValueType.objectTag)) {
                masm.cmpl(itemToReg(x), itemToReg(y));
            } else if (cmpTag == ValueType.longTag) {
                Register xlo = itemToRegLo(x);
                Register xhi = itemToRegHi(x);
                Register ylo = itemToRegLo(y);
                Register yhi = itemToRegHi(y);
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(x.destroysRegister(), "register will be destroyed");
                }
                masm.subl(xlo, ylo);
                masm.sbbl(xhi, yhi);
                if ((cond == If.EQ) || (cond == If.NE)) {
                    masm.orl(xhi, xlo);
                }
            } else if ((cmpTag == ValueType.floatTag)
                    || (cmpTag == ValueType.doubleTag)) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(x.destroysRegister() && y.destroysRegister(), "registers will be destroyed");
                }
                if (mustSwapTwoOnTop(x.getRInfo(), y.getRInfo())) {
                    fpuTwoOnTop(y.getRInfo(), x.getRInfo(), true);
                    fpuPop(y.getRInfo());
                    fpuPop(x.getRInfo());
                    cond = If.MIRROR[cond];
                } else {
                    fpuTwoOnTop(x.getRInfo(), y.getRInfo(), true);
                    fpuPop(x.getRInfo());
                    fpuPop(y.getRInfo());
                }
                masm.fcmp(Register.NO_REG);
            } else {
                Assert.shouldNotReachHere();
            }
        }
        return cond;
    }

    /**
     * Emits the second part of a conditional jump.
     *
     * @param  cond              condition code
     * @param  cmpTag            type tag of the values being compared
     * @param  tdest             destination if condition is true
     * @param  fdest             destination if condition is false
     * @param  udest             destination if one of the values is NaN
     * @param  bci               bytecode index of the branch
     * @param  isBackwardBranch  whether or not it is a backward branch
     * @param  oopRegs           list of registers that contain pointers
     */
    public void if2(int cond, int cmpTag, BlockBegin tdest, BlockBegin fdest,
            BlockBegin udest, int bci, boolean isBackwardBranch, List oopRegs) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(bci >= 0, "bytecode index must be passed in");
        }
        int acond = -1;
        if ((cmpTag == ValueType.floatTag) || (cmpTag == ValueType.doubleTag)) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(udest != null, "must have unordered successor");
            }
            masm.jcc(Assembler.PARITY, udest.getLabel());
            acond = icond2acond(cond, false);
        } else {
            acond = icond2acond(cond, true);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that((cmpTag != ValueType.longTag) || ((acond != Assembler.GREATER)
                    && (acond != Assembler.LESS_EQUAL)), "illegal condition code");
            }
        }
        if (isBackwardBranch) {
            addDebugInfoForBranch(bci, oopRegs);
        }
        masm.jcc(acond, tdest.getLabel());
    }

    /**
     * Branches if the tag matches the specified key.
     *
     * @param  tag               value to be looked up
     * @param  key               key of an entry in the jump table
     * @param  dest              destination if the tag matches the key
     * @param  bci               bytecode index of the branch
     * @param  isBackwardBranch  whether or not it is a backward branch
     * @param  oopRegs           list of registers that contain pointers
     */
    public void tableswitchOp(Item tag, int key, BlockBegin dest, int bci,
            boolean isBackwardBranch, List oopRegs) {
        masm.cmpl(itemToReg(tag), key);
        if (isBackwardBranch) {
            addDebugInfoForBranch(bci, oopRegs);
        }
        masm.jcc(Assembler.EQUAL, dest.getLabel());
    }

    /**
     * Branches if the tag is within the specified range.
     *
     * @param  tag               value to be looked up
     * @param  lowKey            lower bound of the key range
     * @param  highKey           upper bound of the key range
     * @param  dest              destination if the tag is within the range
     * @param  bci               bytecode index of the branch
     * @param  isBackwardBranch  whether or not it is a backward branch
     * @param  oopRegs           list of registers that contain pointers
     */
    public void lookupswitchRangeOp(Item tag, int lowKey, int highKey,
            BlockBegin dest, int bci, boolean isBackwardBranch, List oopRegs) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(lowKey <= highKey, "wrong range");
        }
        if (lowKey == highKey) {
            masm.cmpl(itemToReg(tag), lowKey);
            if (isBackwardBranch) {
                addDebugInfoForBranch(bci, oopRegs);
            }
            masm.jcc(Assembler.EQUAL, dest.getLabel());
        } else if (highKey - lowKey == 1) {
            masm.cmpl(itemToReg(tag), lowKey);
            if (isBackwardBranch) {
                addDebugInfoForBranch(bci, oopRegs);
            }
            masm.jcc(Assembler.EQUAL, dest.getLabel());
            masm.cmpl(itemToReg(tag), highKey);
            if (isBackwardBranch) {
                addDebugInfoForBranch(bci, oopRegs);
            }
            masm.jcc(Assembler.EQUAL, dest.getLabel());
        } else {
            Label done = new Label();
            masm.cmpl(itemToReg(tag), lowKey);
            masm.jcc(Assembler.LESS, done);
            masm.cmpl(itemToReg(tag), highKey);
            if (isBackwardBranch) {
                addDebugInfoForBranch(bci, oopRegs);
            }
            masm.jcc(Assembler.LESS_EQUAL, dest.getLabel());
            masm.bind(done);
        }
    }

    /**
     * Emits the first part of a conditional expression.
     *
     * @param  x  value to compare
     * @param  y  value to be compared with
     */
    public void ifop1(Item x, Item y) {
        masm.cmpl(itemToReg(x), itemToReg(y));
    }

    /**
     * Emits the second part of a conditional expression.
     *
     * @param  dst   destination register
     * @param  tval  result value if condition is true
     * @param  fval  result value if condition is false
     * @param  cond  condition code
     */
    public void ifop2(RInfo dst, Item tval, Item fval, int cond) {
        itemToReg(dst, tval);
        int acond = icond2acond(cond, true);
        Label done = new Label();
        masm.jcc(acond, done);
        itemToReg(dst, fval);
        masm.bind(done);
    }

    /**
     * Emits the prolog for returning from a synchronized method.
     *
     * @param  monitorNo  number of the monitor to exit
     * @param  receiver   the receiver if it is cached
     */
    public void returnOpProlog(int monitorNo, RInfo receiver) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(method.isSynchronized(), "method must be synchronized");
            Assert.that(monitorNo >= 0, "monitor slot must exist");
        }
        Register objReg = Register.ECX;
        Register lockReg = Register.ESI;
        if (method.isStatic()) {
            masm.movl(objReg, method.getHolder());
        } else if (receiver.isIllegal()) {
            loadReceiver(objReg);
        } else {
            objReg = receiver.getRegister();
        }
        monitorexit(objReg, lockReg, SYNC_HDR_REG, monitorNo, Register.NO_REG);
    }

    /**
     * Emits a safepoint return for the method being compiled. The code is
     * padded so that the return can be patched for compiler safepoint.
     *
     * @param  argSize  total size of the arguments
     */
    private void safepointReturn(int argSize) {
        int retAddr = masm.getCodePos();
        masm.ret(argSize);
        masm.getCode().relocate(retAddr + 1, RelocInfo.RETURN_TYPE);
        masm.nop();
        masm.nop();
    }

    /**
     * Returns from the method being compiled.
     *
     * @param  result  result item if any
     */
    public void returnOp(Item result) {
        masm.leave();
        int argSize = method.getArgSize() * 4;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(argSize >= 0, "wrong size of arguments");
        }
        safepointReturn(argSize);
        if ((result != null) && result.getType().isFloatKind()) {
            fpuPop(result.getRInfo());
        }
    }

    /**
     * Performs an implicit null check on the specified item.
     *
     * @param  item  item to be checked for null
     * @param  info  associated debug information
     */
    public void implicitNullCheck(Item item, CodeEmitInfo info) {
        if (javac1.Flags.GenerateCompilerNullChecks) {
            addDebugInfoHere(info);
            masm.nullCheck(itemToReg(item));
        }
    }

    /**
     * Performs an explicit null check on the specified item.
     *
     * @param  item  item to be checked for null
     * @param  info  associated debug information
     */
    public void explicitNullCheck(Item item, CodeEmitInfo info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(item.isRegister() && (item.getType() instanceof ObjectType), "can test only object registers");
        }
        Register reg = itemToReg(item);
        masm.nop();
        int offset = masm.getOffset();
        NullExceptionStub stub = new NullExceptionStub(offset);
        slowCaseStubs.add(stub);
        addCallInfo(offset, info);
        masm.testl(reg, reg);
        masm.jcc(Assembler.ZERO, stub.getEntry());
    }

    /**
     * Checks that the specified divisor is unequal 0. Note that if the divisor
     * is a double-word register, the register will be destroyed.
     *
     * @param  item  the divisor item
     * @param  info  associated debug information
     */
    public void explicitDivByZeroCheck(Item item, CodeEmitInfo info) {
        masm.nop();
        int offset = masm.getOffset();
        DivByZeroStub stub = new DivByZeroStub(offset);
        slowCaseStubs.add(stub);
        addCallInfo(offset, info);
        if (item.isRegister()) {
            if (item.getType().isDoubleWord()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(item.destroysRegister(), "register will be destroyed");
                }
                masm.orl(itemToRegLo(item), itemToRegHi(item));
            } else {
                Register reg = itemToReg(item);
                masm.testl(reg, reg);
            }
            masm.jcc(Assembler.ZERO, stub.getEntry());
        } else if (item.isConstant()) {
            if (item.getType() instanceof LongType) {
                if ((itemToIntLo(item) == 0) && (itemToIntHi(item) == 0)) {
                    masm.jmp(stub.getEntry());
                }
            } else {
                if (itemToInt(item) == 0) {
                    masm.jmp(stub.getEntry());
                }
            }
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Checks that the index is within the bounds of the specified array.
     *
     * @param  array  the array item
     * @param  index  the index item
     * @param  info   associated debug information
     */
    public void rangeCheck(Item array, Item index, CodeEmitInfo info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(array.isRegister() && (array.getType() instanceof ObjectType) && (index.getType() instanceof IntType), "wrong item state");
        }
        int offset = masm.getOffset();
        addDebugInfo(offset, info, false);
        RangeCheckStub stub = null;
        if (index.isConstant()) {
            masm.cmpl(new Address(itemToReg(array),
                Runtime1.getArrayLengthOffset()), itemToInt(index));
            offset = masm.getOffset();
            stub = new RangeCheckStub(offset);
            masm.jcc(Assembler.BELOW_EQUAL, stub.getEntry());
        } else {
            masm.cmpl(itemToReg(index), new Address(itemToReg(array),
                Runtime1.getArrayLengthOffset()));
            offset = masm.getOffset();
            stub = new RangeCheckStub(offset);
            masm.jcc(Assembler.ABOVE_EQUAL, stub.getEntry());
        }
        slowCaseStubs.add(stub);
        addCallInfo(offset, info);
    }

    /**
     * Checks that a value can be stored into the specified array.
     *
     * @param  array  array to store value into
     * @param  value  value to be stored
     * @param  index  index to store value at
     * @param  tmp1   temporary register
     * @param  tmp2   temporary register
     * @param  info   associated debug information
     */
    public void arrayStoreCheck(Item array, Item value, Item index, RInfo tmp1,
            RInfo tmp2, CodeEmitInfo info) {
        if (!javac1.Flags.GenerateArrayStoreCheck) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(array.isRegister() && (array.getType() instanceof ObjectType)
                && value.isRegister() && (value.getType() instanceof ObjectType), "wrong item state");
        }
        Label done = new Label();
        Register arrayReg = itemToReg(array);
        Register valueReg = itemToReg(value);
        Register indexReg =
            index.isConstant() ? Register.NO_REG : itemToReg(index);
        int indexConst = index.isConstant() ? itemToInt(index) : -1;
        Register t1 = tmp1.getRegister();
        Register t2 = tmp2.getRegister();
        ArrayStoreCheckStub stub = new ArrayStoreCheckStub(arrayReg, valueReg,
            indexReg, indexConst, info, espOffset);
        slowCaseStubs.add(stub);
        masm.testl(valueReg, valueReg);
        masm.jcc(Assembler.ZERO, done);
        masm.movl(t2, new Address(arrayReg, Runtime1.getKlassOffset()));
        masm.movl(t1, new Address(valueReg, Runtime1.getKlassOffset()));
        masm.movl(t2, new Address(t2, Runtime1.getArrayElementKlassOffset() + 8));
        masm.cmpl(t2, new Address(t1, Runtime1.getIsACache1Offset() + 8));
        masm.jcc(Assembler.EQUAL, done);
        masm.cmpl(t2, new Address(t1, Runtime1.getIsACache2Offset() + 8));
        masm.jcc(Assembler.NOT_EQUAL, stub.getEntry());
        masm.bind(stub.getContinuation());
        masm.bind(done);
    }

    /**
     * Emits the code of the stubs for slow cases.
     */
    public void emitSlowCaseStubs() {
        for (int i = 0; i < slowCaseStubs.size(); i++) {
            CodeStub stub = (CodeStub) slowCaseStubs.get(i);
            checkCodespace();
            stub.emitCode(this);
            stub.assertNoUnboundLabels();
        }
    }

    /**
     * Emits the code of the stubs for calls.
     */
    public void emitCallStubs() {
        masm.getCode().setStubsBegin(masm.getCodePos());
        for (int i = 0; i < callStubs.size(); i++) {
            CodeStub stub = (CodeStub) callStubs.get(i);
            checkCodespace();
            stub.emitCode(this);
            stub.assertNoUnboundLabels();
        }
        masm.getCode().setStubsEnd(masm.getCodePos());
    }

    /**
     * Emits the floating-point constants in the constant table.
     */
    public void emitConstants() {
        checkCodespace();
        constTable.emitEntries(masm);
    }

    /**
     * Emits code for exception handlers.
     *
     * @param   maxSpills       maximum number of spill elements
     * @return  code offset of the exception handler
     */
    public int emitExceptionHandler(int maxSpills) {
        if (!throwEntryLabel.isUnused()) {
            masm.bind(throwEntryLabel);
            masm.popl(Register.EDX);
        }
        int offset = masm.getOffset();
        if (method.hasExceptionHandlers()) {
            if (scanResult.hasFloats() || scanResult.hasDoubles()) {
                masm.clearFpuStack();
            }
            masm.movl(Register.ECX, stackOffsetInBytes(maxSpills));
            masm.call(Runtime1.getStubEntry(Runtime1.HANDLE_EXCEPTION_ID),
                RelocInfo.RUNTIME_CALL_TYPE);
        } else {
            masm.getThread(Register.ESI);
            masm.movl(new Address(Register.ESI,
                Runtime1.getIsHandlingImplicitExceptionOffset()), 0);
        }
        if (method.isSynchronized()) {
            if (method.isStatic()) {
                masm.movl(Register.EBX, method.getHolder());
            } else {
                loadReceiver(Register.EBX);
            }
            monitorexit(Register.EBX, Register.ECX, SYNC_HDR_REG, 0, Register.EAX);
        }
        masm.jmp(Runtime1.getStubEntry(Runtime1.UNWIND_EXCEPTION_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        return offset;
    }

    /**
     * Inserts a byte that performs no operation.
     */
    public void addNop() {
        masm.nop();
    }

    /**
     * Inserts a byte with debug information that performs no operation. This
     * method is used to mark a continuation attached to a bytecode index.
     *
     * @param  info  associated debug information
     */
    public void addNopWithInfo(CodeEmitInfo info) {
        addDebugInfoHere(info);
        masm.nop();
    }

    /**
     * Increments the value of the specified register by 1.
     *
     * @param  reg  register to be incremented
     */
    public void incrementReg(RInfo reg) {
        masm.incl(reg.getRegister());
    }

    /**
     * Decrements the value of the specified register by 1.
     *
     * @param  reg  register to be decremented
     */
    public void decrementReg(RInfo reg) {
        masm.decl(reg.getRegister());
    }

    /**
     * Increments the value of the specified local variable by 1.
     *
     * @param  localIndex  index of the variable to be incremented
     */
    public void incrementLocal(int localIndex) {
        Address dstAddr = frameMap.localAddress(localIndex, false);
        masm.incl(dstAddr);
    }

    /**
     * Decrements the value of the specified local variable by 1.
     *
     * @param  localIndex  index of the variable to be decremented
     */
    public void decrementLocal(int localIndex) {
        Address dstAddr = frameMap.localAddress(localIndex, false);
        masm.decl(dstAddr);
    }

    /**
     * Inserts a breakpoint into the code buffer.
     */
    public void breakpoint() {
        masm.int3();
    }

    /**
     * Aligns the code for a backward branch.
     */
    public void alignBackwardBranch() {
        masm.align(4);
    }

    /**
     * Sets the floating-point precision to 32 bit.
     */
    public void set32bitFpuPrecision() {
        masm.fldcw(new Address(Runtime1.getAddrFpuCntrlWrd24(), RelocInfo.NONE));
    }

    /**
     * Restores the floating-point precision to the standard setting.
     */
    public void restoreFpuPrecision() {
        masm.fldcw(new Address(Runtime1.getAddrFpuCntrlWrdStd(), RelocInfo.NONE));
    }

    /**
     * Ensures that there is enough space in the code buffer.
     */
    public void checkCodespace() {
        masm.getCode().checkCodespace();
    }

    /**
     * Checks that the specified index is not negative.
     *
     * @param  reg    register that contains the index
     * @param  value  index if it is constant
     * @param  label  jump destination if the index is negative
     */
    private void arrayCheckForNegative(Register reg, int value, Label label) {
        if (reg.isValid()) {
            masm.cmpl(reg, 0);
            masm.jcc(Assembler.LESS, label);
        } else if (value < 0) {
            masm.jmp(label);
        }
    }

    /**
     * Checks that the specified range is within the array.
     *
     * @param  array   register that points to the array
     * @param  lenReg  register with the length of the range
     * @param  posReg  register with the start position
     * @param  pos     start position if it is constant
     * @param  tmp     temporary register
     * @param  label   label to jump to if the check fails
     */
    private void arrayCheckLength(Register array, Register lenReg,
            Register posReg, int pos, Register tmp, Label label) {
        Address lenAddr = new Address(array, Runtime1.getArrayLengthOffset());
        if (posReg.isValid()) {
            masm.movl(tmp, posReg);
            masm.addl(tmp, lenReg);
        } else {
            masm.movl(tmp, lenReg);
            if (pos != 0) {
                masm.addl(tmp, pos);
            }
        }
        masm.cmpl(tmp, lenAddr);
        masm.jcc(Assembler.ABOVE, label);
    }

    /**
     * Loads the start address of the array range to be copied.
     *
     * @param  array   register that points to the array
     * @param  posReg  register with the start position
     * @param  pos     start position if it is constant
     * @param  tmp     temporary register
     * @param  type    basic element type
     */
    private void arrayMemoryStart(Register array, Register posReg, int pos,
            Register tmp, int type) {
        if (posReg.isValid()) {
            masm.leal(array, new Address(array, posReg, Address.TIMES_2,
                Runtime1.getArrayBaseOffset(type)));
        } else {
            masm.movl(tmp, pos);
            masm.leal(array, new Address(array, tmp, Address.TIMES_2,
                Runtime1.getArrayBaseOffset(type)));
        }
    }

    /**
     * Copies a sequence of elements from the source to the destination array.
     *
     * @param  src     source array
     * @param  srcPos  start position in the source array
     * @param  dst     destination array
     * @param  dstPos  start position in the destination array
     * @param  length  number of elements to be copied
     * @param  tmp     temporary register
     * @param  info    associated debug information
     */
    public void arraycopy(Item src, Item srcPos, Item dst, Item dstPos,
            Item length, RInfo tmp, CodeEmitInfo info) {
        Register srcReg = src.getRInfo().getRegister();
        Register dstReg = dst.getRInfo().getRegister();
        Register tmpReg = tmp.getRegister();
        Register srcPosReg = Register.NO_REG;
        int srcPosConst = -1;
        if (srcPos.isRegister()) {
            srcPosReg = srcPos.getRInfo().getRegister();
        } else {
            srcPosConst = itemToInt(srcPos);
        }
        Register dstPosReg = Register.NO_REG;
        int dstPosConst = -1;
        if (dstPos.isRegister()) {
            dstPosReg = dstPos.getRInfo().getRegister();
        } else {
            dstPosConst = itemToInt(dstPos);
        }
        Register lengthReg = length.getRInfo().getRegister();
        ArrayCopyStub stub = new ArrayCopyStub(srcReg, srcPosReg, dstReg,
            dstPosReg, lengthReg, dstPosConst, srcPosConst, -1, info, espOffset);
        slowCaseStubs.add(stub);
        masm.testl(srcReg, srcReg);
        masm.jcc(Assembler.ZERO, stub.getEntry());
        masm.testl(dstReg, dstReg);
        masm.jcc(Assembler.ZERO, stub.getEntry());
        masm.movl(tmpReg, new Address(srcReg, Runtime1.getKlassOffset()));
        masm.cmpl(tmpReg, new Address(dstReg, Runtime1.getKlassOffset()));
        masm.jcc(Assembler.NOT_EQUAL, stub.getEntry());
        arrayCheckForNegative(lengthReg, -1, stub.getEntry());
        arrayCheckForNegative(srcPosReg, srcPosConst, stub.getEntry());
        arrayCheckForNegative(dstPosReg, dstPosConst, stub.getEntry());
        arrayCheckLength(srcReg, lengthReg, srcPosReg, srcPosConst, tmpReg,
            stub.getEntry());
        arrayCheckLength(dstReg, lengthReg, dstPosReg, dstPosConst, tmpReg,
            stub.getEntry());
        masm.movl(tmpReg, new Address(srcReg, Runtime1.getKlassOffset()));
        masm.cmpl(tmpReg, ArrayKlass.make(BasicType.CHAR));
        masm.jcc(Assembler.NOT_EQUAL, stub.getNoCheckEntry());
        masm.cmpl(srcReg, dstReg);
        masm.jcc(Assembler.EQUAL, stub.getNoCheckEntry());
        arrayMemoryStart(srcReg, srcPosReg, srcPosConst, tmpReg, BasicType.CHAR);
        arrayMemoryStart(dstReg, dstPosReg, dstPosConst, tmpReg, BasicType.CHAR);
        Label loopTest = new Label();
        Label loopStart = new Label();
        Label evenLabel = new Label();
        masm.testl(lengthReg, 1);
        masm.jcc(Assembler.ZERO, evenLabel);
        masm.loadUnsignedWord(tmpReg, new Address(srcReg));
        masm.addl(srcReg, 2);
        masm.movw(new Address(dstReg), tmpReg);
        masm.addl(dstReg, 2);
        masm.bind(evenLabel);
        masm.shrl(lengthReg, 1);
        masm.cmpl(lengthReg, 4);
        masm.jcc(Assembler.LESS_EQUAL, loopTest);
        masm.pushl(srcReg);
        masm.pushl(dstReg);
        masm.pushl(lengthReg);
        masm.popl(Register.ECX);
        masm.popl(Register.EDI);
        masm.popl(Register.ESI);
        masm.repmovs();
        Label done = new Label();
        masm.jmp(done);
        masm.jmp(loopTest);
        masm.bind(loopStart);
        masm.decl(lengthReg);
        masm.movl(tmpReg, new Address(srcReg, lengthReg, Address.TIMES_4, 0));
        masm.movl(new Address(dstReg, lengthReg, Address.TIMES_4, 0), tmpReg);
        masm.bind(loopTest);
        masm.cmpl(lengthReg, 0);
        masm.jcc(Assembler.GREATER, loopStart);
        masm.bind(stub.getContinuation());
        masm.bind(done);
    }

    /**
     * Emits a byte at handler entry that performs no operation.
     */
    public void handlerEntry() {
        masm.nop();
    }
    
    /**
     * Emits some instructions for calling the code of a native method.
     *
     * @param  nativeEntry  address of the native code
     * @param  info         basic debug information
     */
    public void nativeCall(int nativeEntry, CodeEmitInfo info) {
        int posInNative = masm.getCodePos();
        int posInNativeOffset = masm.getOffset();
        int srcOffset = method.getArgSize();
        int dstOffset = 0;
        Register loWord = Register.EAX;
        Register hiWord = Register.EDX;
        List sig = method.getArgTypes();
        for (int i = sig.size() - 2; i >= 0; i--) {
            int type = ((Integer) sig.get(i)).intValue();
            masm.movl(loWord, frameMap.localAddress(--srcOffset, false));
            if (BasicType.isOop(type)) {
                Label lbl = new Label();
                masm.testl(loWord, loWord);
                masm.jcc(Assembler.ZERO, lbl);
                masm.leal(loWord, frameMap.localAddress(srcOffset, false));
                masm.bind(lbl);
            } else if (BasicType.isDoubleWord(type)) {
                masm.movl(hiWord, frameMap.localAddress(--srcOffset, false));
                pushReg(hiWord);
                dstOffset++;
            }
            pushReg(loWord);
            dstOffset++;
        }
        Register handle = Register.EAX;
        Register mirror = Register.EDX;
        if (method.isStatic()) {
            masm.movl(mirror, method.getHolder());
            masm.movl(frameMap.spillAddress(0, false, false), mirror);
            masm.leal(handle, frameMap.spillAddress(0, false, false));
        } else {
            masm.leal(handle, frameMap.localAddress(--srcOffset, false));
        }
        pushReg(handle);
        dstOffset++;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(srcOffset == 0, "must have iterated through all arguments");
        }
        Register thread = Register.EBX;
        Register t = Register.EDX;
        masm.getThread(thread);
        masm.leal(t, new Address(thread, Runtime1.getJniEnvOffset()));
        pushReg(t);
        dstOffset++;
        masm.movl(t, new Address(thread, Runtime1.getActiveHandlesOffset()));
        masm.movl(new Address(t, Runtime1.getHandleBlockTopOffset()), 0);
        masm.movl(new Address(thread, Runtime1.getLastJavaFpOffset()), Register.EBP);
        masm.movl(new Address(thread, Runtime1.getLastJavaSpOffset()), Register.ESP);
        masm.leal(t, new Address(posInNative, RelocInfo.INTERNAL_WORD_TYPE));
        masm.movl(new Address(thread, Runtime1.getLastNativePosOffset()), t);
        masm.movl(new Address(thread, Runtime1.getThreadStateOffset()),
            Runtime1.getThreadInNativeState());
        masm.call(nativeEntry, RelocInfo.RUNTIME_CALL_TYPE);
        addCallInfo(posInNativeOffset, info);
        addCallInfoHere(info);
        masm.decrement(Register.ESP, dstOffset * 4);
    }
        
    /**
     * Generates code for the exit of a native method.
     *
     * @param  info  basic debug information
     */
    public void nativeMethodExit(CodeEmitInfo info) {
        Label lbl = null;
        Register thread = Register.EBX;
        masm.getThread(thread);
        masm.movl(new Address(thread, Runtime1.getThreadStateOffset()),
            Runtime1.getThreadInNativeState() + 1);
        int returnType = method.getReturnType();
        switch (returnType) {
        case BasicType.ARRAY:
            /* falls through */
        case BasicType.OBJECT:
            lbl = new Label();
            masm.testl(Register.EAX, Register.EAX);
            masm.jcc(Assembler.ZERO, lbl);
            masm.movl(Register.EAX, new Address(Register.EAX));
            masm.bind(lbl);
            masm.verifyOop(Register.EAX);
            break;
        case BasicType.BOOLEAN:
            masm.c2bool(Register.EAX);
            break;
        case BasicType.CHAR:
            masm.andl(Register.EAX, 0xffff);
            break;
        case BasicType.BYTE:
            masm.signExtendByte(Register.EAX);
            break;
        case BasicType.SHORT:
            masm.signExtendShort(Register.EAX);
            break;
        case BasicType.FLOAT:
            /* falls through */
        case BasicType.DOUBLE:
            /* falls through */
        case BasicType.VOID:
            /* falls through */
        case BasicType.LONG:
            /* falls through */
        case BasicType.INT:
            /* nothing to do */
            break;
        default:
            Assert.shouldNotReachHere();
        }
        lbl = new Label();
        masm.cmpl(new Address(Runtime1.getSafepointSyncStateAddr(), RelocInfo.NONE),
            Runtime1.getNotSafepointSyncState());
        masm.jcc(Assembler.EQUAL, lbl);
        if (BasicType.isOop(returnType)) {
            masm.movl(new Address(thread, Runtime1.getVMResultOffset()), Register.EAX);
        }
        masm.call(Runtime1.getStubEntry(Runtime1.SAFEPOINT_BLOCK_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        addCallInfoHere(info);
        masm.getThread(thread);
        if (BasicType.isOop(returnType)) {
            masm.movl(Register.EAX, new Address(thread, Runtime1.getVMResultOffset()));
            masm.movl(new Address(thread, Runtime1.getVMResultOffset()), 0);
        }
        masm.bind(lbl);
        masm.movl(new Address(thread, Runtime1.getThreadStateOffset()),
            Runtime1.getThreadInJavaState());
        masm.movl(new Address(thread, Runtime1.getLastNativePosOffset()), 0);
        masm.movl(new Address(thread, Runtime1.getLastJavaSpOffset()), 0);
        masm.movl(new Address(thread, Runtime1.getLastJavaFpOffset()), 0);
        lbl = new Label();
        masm.cmpl(new Address(thread, Runtime1.getPendingExceptionOffset()), 0);
        masm.jcc(Assembler.ZERO, lbl);
        masm.movl(Register.EAX, new Address(thread, Runtime1.getPendingExceptionOffset()));
        masm.call(Runtime1.getForwardExceptionEntry(), RelocInfo.RUNTIME_CALL_TYPE);
        masm.bind(lbl);
        if (method.isSynchronized()) {
            RInfo dummyReceiver = new RInfo();
            pushReg(Register.EAX);
            pushReg(Register.EDX);
            returnOpProlog(0, dummyReceiver);
            pop(Register.EDX);
            pop(Register.EAX);
        }
        returnOp(null);
        setEspOffset(0);
    }
    
    /**
     * Emits the entry code for on-stack-replacement.
     *
     * @param  maxSpills     maximum number of spill elements
     * @param  countLocks    number of locks
     * @param  continuation  label to continue at
     * @param  osrBci        bytecode index for OSR
     */
    public void emitOsrEntry(int maxSpills, int countLocks, Label continuation,
            int osrBci) {
        offsets.setOsrOffset(masm.getOffset());
        if (countLocks > 0) {
            masm.movl(Register.EBX, Register.EBP);
        }
        masm.subl(Register.ESP, method.getArgSize() * javac1.Flags.BytesPerWord);
        masm.pushl(Runtime1.getStubEntry(Runtime1.OSR_FRAME_RETURN_ID));
        buildFrame(maxSpills);
        for (int i = 0; i < method.getMaxLocals(); i++) {
            masm.movl(Register.EAX, new Address(Register.EDI, -i * javac1.Flags.BytesPerWord));
            masm.movl(frameMap.localAddress(i, false), Register.EAX);
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(Runtime1.getDisplacedHeaderOffset() == 0, "adjust code below");
        }
        for (int i = 0; i < countLocks; i++) {
            int slotOffset = Runtime1.getMonitorBlockBottomOffset()
                - (i + 1) * Runtime1.getBasicObjectLockSize();
            masm.movl(Register.EAX, new Address(Register.EBX, slotOffset
                * javac1.Flags.BytesPerWord + Runtime1.getBasicObjectLockOffset()));
            masm.movl(frameMap.monitorAddress(i), Register.EAX);
        }
        if (javac1.Flags.AcceptJsrForOSR) {
            List addrLocals = BackEnd.getAddressMap(method, osrBci);
            for (int i = 0; i < addrLocals.size(); i++) {
                int localIndex = ((Integer) addrLocals.get(i)).intValue();
                Address localAddr = frameMap.localAddress(localIndex, false);
                masm.movl(Register.EAX, localAddr);
                masm.call(Runtime1.getStubEntry(Runtime1.ADDRESS_BCI_2_PC_ID),
                    RelocInfo.RUNTIME_CALL_TYPE);
                masm.movl(localAddr, Register.EAX);
            }
        }
        if (method.getMaxLocals() > 0) {
            masm.movl(Register.ECX, frameMap.localAddress(0, false));
        }
        masm.jmp(continuation);
    }
}
