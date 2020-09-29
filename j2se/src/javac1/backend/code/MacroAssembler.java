/*
 * @(#)MacroAssembler.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;
import javac1.ci.Runtime1;

/**
 * Extends the assembler by frequently used macros.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class MacroAssembler extends Assembler {
    /**
     * Constructs a new macro assembler generating code into the specified
     * buffer.
     *
     * @param  code  code buffer that stores the instructions
     */
    public MacroAssembler(CodeBuffer code) {
        super(code);
    }

    /**
     * Emits 5 bytes that perform no operation. This instruction is safe for
     * patching.
     *
     * @see  Assembler#nop()
     */
    public void fatNop() {
        prefix(ES_SEGMENT);
        prefix(CS_SEGMENT);
        prefix(FS_SEGMENT);
        prefix(GS_SEGMENT);
        nop();
    }

    /**
     * Loads thread information block pointer from special segment into the
     * specified register.
     *
     * @param  thread  register to load pointer into
     */
    public void getThreadId(Register thread) {
        prefix(FS_SEGMENT);
        movl(thread, new Address(Runtime1.getTLSArrayOffset(), RelocInfo.NONE));
    }

    /**
     * Loads a pointer to thread-local information into the specified thread
     * register.
     *
     * @param  thread  register to load pointer into
     */
    public void getThread(Register thread) {
        getThreadId(thread);
        movl(thread, new Address(thread,
            Runtime1.getTLSBaseOffset() + Runtime1.getTLSThreadOffset()));
    }

    /**
     * Tests if an explicit null pointer check is needed when accessing memory
     * with the specified offset.
     *
     * @param   offset  offset to be tested
     * @return  whether or not an explicit null check is needed
     */
    public static boolean needsExplicitNullCheck(int offset) {
        return (offset < 0) || (offset >= Runtime1.getVMPageSize());
    }

    /**
     * Provokes an exception if the content of the register is null. No explicit
     * code generation is needed if the specified offset is within a certain
     * range.
     *
     * @param  reg     register to be checked for null
     * @param  offset  offset that memory is accessed with
     */
    public void nullCheck(Register reg, int offset) {
        if (needsExplicitNullCheck(offset)) {
            cmpl(Register.EAX, new Address(reg));
        }
    }

    /**
     * Provokes an exception if the content of the register is null.
     *
     * @param  reg  register to be checked for null
     */
    public void nullCheck(Register reg) {
        nullCheck(reg, -1);
    }

    /**
     * Loads the unsigned byte at the source address into the destination
     * register.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public int loadUnsignedByte(Register dst, Address src) {
        int offset;
        if (javac1.Flags.CodeForP6 || src.uses(dst)) {
            offset = getOffset();
            movzxb(dst, src);
        } else {
            xorl(dst, dst);
            offset = getOffset();
            movb(dst, src);
        }
        return offset;
    }

    /**
     * Loads the unsigned word at the source address into the destination
     * register.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public int loadUnsignedWord(Register dst, Address src) {
        int offset;
        if (javac1.Flags.CodeForP6 || src.uses(dst)) {
            offset = getOffset();
            movzxw(dst, src);
        } else {
            xorl(dst, dst);
            offset = getOffset();
            movw(dst, src);
        }
        return offset;
    }

    /**
     * Loads the signed byte at the source address into the destination
     * register.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public int loadSignedByte(Register dst, Address src) {
        int offset;
        if (javac1.Flags.CodeForP6) {
            offset = getOffset();
            movsxb(dst, src);
        } else {
            offset = loadUnsignedByte(dst, src);
            shll(dst, 24);
            sarl(dst, 24);
        }
        return offset;
    }

    /**
     * Loads the signed word at the source address into the destination
     * register.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public int loadSignedWord(Register dst, Address src) {
        int offset;
        if (javac1.Flags.CodeForP6) {
            offset = getOffset();
            movsxw(dst, src);
        } else {
            offset = loadUnsignedWord(dst, src);
            shll(dst, 16);
            sarl(dst, 16);
        }
        return offset;
    }

    /**
     * Extends the sign of the low word into the high word register. Thus the
     * size of the word operand is doubled by means of sign extension.
     *
     * @param  hi  high word register
     * @param  lo  low word register
     */
    public void extendSign(Register hi, Register lo) {
        if (javac1.Flags.CodeForP6 && hi.equals(Register.EDX)
                && lo.equals(Register.EAX)) {
            cdql();
        } else {
            movl(hi, lo);
            sarl(hi, 31);
        }
    }

    /**
     * Increments the contents of a register by the specified value. If the
     * value equals zero then no code will be generated.
     *
     * @param  reg    register to be incremented
     * @param  value  value to increment by
     */
    public void increment(Register reg, int value) {
        if ((value < 0) && (value != Integer.MIN_VALUE)) {
            decrement(reg, -value);
        } else if (value == 1) {
            incl(reg);
        } else if (value != 0) {
            addl(reg, value);
        }
    }

    /**
     * Decrements the contents of a register by the specified value. If the
     * value equals zero then no code will be generated.
     *
     * @param  reg    register to be decremented
     * @param  value  value to decrement by
     */
    public void decrement(Register reg, int value) {
        if ((value < 0) && (value != Integer.MIN_VALUE)) {
            increment(reg, -value);
        } else if (value == 1) {
            decl(reg);
        } else if (value != 0) {
            subl(reg, value);
        }
    }

    /**
     * Aligns the next instruction to the specified boundary.
     *
     * @param  modulus  modulus of alignment
     */
    public void align(int modulus) {
        while (getOffset() % modulus != 0) {
            nop();
        }
    }

    /**
     * Creates a stack frame for a procedure. The code pushes the frame pointer
     * from the EBP register onto the stack before copying the current stack
     * pointer from the ESP register into the EBP register.
     */
    public void enter() {
        pushl(Register.EBP);
        movl(Register.EBP, Register.ESP);
    }

    /**
     * Releases the stack frame of a procedure. The code copies the frame
     * pointer from the EBP register into the stack pointer register ESP, which
     * releases the stack space allocated to the stack frame. The old frame
     * pointer is then popped from the stack into the EBP register, restoring
     * the stack frame of the calling procedure.
     */
    public void leave() {
        movl(Register.ESP, Register.EBP);
        popl(Register.EBP);
    }

    /**
     * Calls a leaf entry point in the Virtual Machine.
     *
     * @param  entryPoint  entry point address
     * @param  numArgs     number of arguments
     */
    public void callVMLeaf(int entryPoint, int numArgs) {
        call(entryPoint, RelocInfo.RUNTIME_CALL_TYPE);
        increment(Register.ESP, numArgs * 4);
    }

    /**
     * Does a store check for the pointer in the specified register. The content
     * of the register is destroyed afterwards.
     *
     * @param  obj  object register to be checked
     */
    public void storeCheck(Register obj) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(Runtime1.getRSByteMapBase() != 0, "byte map base not yet set");
        }
        shrl(obj, Runtime1.getCardShift());
        movb(new Address(Register.NO_REG, obj, Address.TIMES_1,
            Runtime1.getRSByteMapBase()), 0);
    }

    /**
     * Compares the least significant byte of the specified register with 0.
     *
     * @param  x  register to be tested
     */
    public void c2bool(Register x) {
        andl(x, 0xff);
        setb(Assembler.NOT_ZERO, x);
    }
    
    /**
     * Generates code for an integer division as described in the specification.
     * The method returns the idivl instruction offset that may be needed for
     * handling implicit exceptions.
     *
     * @param   reg  register that stores the divisor
     * @return  offset of the idivl instruction
     */
    public int correctedIntDiv(Register reg) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!reg.equals(Register.EAX) && !reg.equals(Register.EDX), "register cannot be eax or edx");
        }
        Label normal = new Label();
        Label special = new Label();
        cmpl(Register.EAX, Integer.MIN_VALUE);
        jcc(NOT_EQUAL, normal);
        xorl(Register.EDX, Register.EDX);
        cmpl(reg, -1);
        jcc(EQUAL, special);
        bind(normal);
        cdql();
        int offset = getOffset();
        idivl(reg);
        bind(special);
        return offset;
    }

    /**
     * Computes the two's complement negation of a double-word value.
     *
     * @param  hi  high word register
     * @param  lo  low word register
     */
    public void lneg(Register hi, Register lo) {
        negl(lo);
        adcl(hi, 0);
        negl(hi);
    }

    /**
     * Multiplies two long integers that are stored on the stack.
     *
     * @param  offsetX  ESP offset of the multiplicand
     * @param  offsetY  ESP offset of the multiplier
     */
    public void lmul(int offsetX, int offsetY) {
        Address xHi = new Address(Register.ESP, offsetX + 4);
        Address xLo = new Address(Register.ESP, offsetX);
        Address yHi = new Address(Register.ESP, offsetY + 4);
        Address yLo = new Address(Register.ESP, offsetY);
        Label quick = new Label();
        movl(Register.EBX, xHi);
        movl(Register.ECX, yHi);
        movl(Register.EAX, Register.EBX);
        orl(Register.EBX, Register.ECX);
        jcc(ZERO, quick);
        mull(yLo);
        movl(Register.EBX, Register.EAX);
        movl(Register.EAX, xLo);
        mull(Register.ECX);
        addl(Register.EBX, Register.EAX);
        bind(quick);
        movl(Register.EAX, xLo);
        mull(yLo);
        addl(Register.EDX, Register.EBX);
    }

    /**
     * Shifts the double-word operand to the left by the number of bits
     * specified in the ECX register.
     *
     * @param  hi  high word register
     * @param  lo  low word register
     */
    public void lshl(Register hi, Register lo) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!hi.equals(Register.ECX) && !lo.equals(Register.ECX), "must not use ecx");
        }
        Label label = new Label();
        andl(Register.ECX, 0x3f);
        cmpl(Register.ECX, javac1.Flags.BitsPerWord);
        jcc(LESS, label);
        movl(hi, lo);
        xorl(lo, lo);
        bind(label);
        shldl(hi, lo);
        shll(lo);
    }

    /**
     * Shifts the double-word operand to the right by the number of bits
     * specified in the ECX register.
     *
     * @param  hi             high word register
     * @param  lo             low word register
     * @param  signExtension  whether or not sign extension is required
     */
    public void lshr(Register hi, Register lo, boolean signExtension) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!hi.equals(Register.ECX) && !lo.equals(Register.ECX), "must not use ecx");
        }
        Label label = new Label();
        andl(Register.ECX, 0x3f);
        cmpl(Register.ECX, javac1.Flags.BitsPerWord);
        jcc(LESS, label);
        movl(lo, hi);
        if (signExtension) {
            sarl(hi, 31);
        } else {
            xorl(hi, hi);
        }
        bind(label);
        shrdl(lo, hi);
        if (signExtension) {
            sarl(hi);
        } else {
            shrl(hi);
        }
    }

    /**
     * Compares two long integer values for order. The first of the specified
     * registers will afterwards contain -1, 0 or 1 as the left value is less
     * than, equal to or greater than the right value.
     *
     * @param  xHi  high word of the value to compare
     * @param  xLo  low word of the value to compare
     * @param  yHi  high word of the value to be compared with
     * @param  yLo  low word of the value to be compared with
     */
    public void lcmp2int(Register xHi, Register xLo, Register yHi, Register yLo) {
        Label l1 = new Label();
        Label l2 = new Label();
        subl(xLo, yLo);
        sbbl(xHi, yHi);
        jcc(NO_OVERFLOW, l1);
        notl(xHi);
        bind(l1);
        orl(xLo, xHi);
        jcc(ZERO, l2);
        sarl(xHi, 30);
        andl(xHi, -2);
        incl(xHi);
        bind(l2);
    }

    /**
     * Copies the value of the EAX register into the specified register.
     *
     * @param  tmp  register used to save EAX temporarily
     */
    public void saveEAX(Register tmp) {
        if (tmp.equals(Register.NO_REG)) {
            pushl(Register.EAX);
        } else if (!tmp.equals(Register.EAX)) {
            movl(tmp, Register.EAX);
        }
    }

    /**
     * Copies the value of the specified register into the EAX register.
     *
     * @param  tmp  register used to save EAX temporarily
     */
    public void restoreEAX(Register tmp) {
        if (tmp.equals(Register.NO_REG)) {
            popl(Register.EAX);
        } else if (!tmp.equals(Register.EAX)) {
            movl(Register.EAX, tmp);
        }
    }

    /**
     * Computes the floating-point remainder for Java. The code re-executes the
     * fprem instruction, which calculates the partial remainder, until the C2
     * flag is cleared.
     *
     * @param  tmp  register used to save EAX temporarily
     * @see    Assembler#fprem()
     */
    public void fremr(Register tmp) {
        saveEAX(tmp);
        Label label = new Label();
        bind(label);
        fprem();
        fwait();
        fnstswax();
        sahf();
        jcc(PARITY, label);
        restoreEAX(tmp);
        fxch(1);
        fpop();
    }

    /**
     * Branches to the specified label if the FPU flag C2 is set.
     *
     * @param  tmp    register used to save EAX temporarily
     * @param  label  jump destination label
     */
    public void jC2(Register tmp, Label label) {
        saveEAX(tmp);
        fwait();
        fnstswax();
        sahf();
        restoreEAX(tmp);
        jcc(PARITY, label);
    }

    /**
     * Branches to the specified label if the FPU flag C2 is not set.
     *
     * @param  tmp    register used to save EAX temporarily
     * @param  label  jump destination label
     */
    public void jnC2(Register tmp, Label label) {
        saveEAX(tmp);
        fwait();
        fnstswax();
        sahf();
        restoreEAX(tmp);
        jcc(NO_PARITY, label);
    }

    /**
     * Compares the topmost stack entries on the FPU stack and sets the EFLAGS
     * register according to the results.
     *
     * @param  tmp  temporary register used to save EAX
     */
    public void fcmp(Register tmp) {
        if (javac1.Flags.CodeForP6) {
            fucomip(1);
            fpop();
        } else {
            fcompp();
            saveEAX(tmp);
            fwait();
            fnstswax();
            sahf();
            restoreEAX(tmp);
        }
    }

    /**
     * Compares the topmost stack entries on the FPU stack and stores the
     * result in the destination register.
     *
     * @param  dst              destination register
     * @param  unorderedIsLess  whether or not unordered means less
     */
    public void fcmp2int(Register dst, boolean unorderedIsLess) {
        fcmp(dst);
        Label label = new Label();
        if (unorderedIsLess) {
            movl(dst, -1);
            jcc(PARITY, label);
            jcc(BELOW, label);
            movl(dst, 0);
            jcc(EQUAL, label);
            incl(dst);
        } else {
            movl(dst, 1);
            jcc(PARITY, label);
            jcc(ABOVE, label);
            movl(dst, 0);
            jcc(EQUAL, label);
            decl(dst);
        }
        bind(label);
    }

    /**
     * Clears and pops the topmost value on the FPU stack.
     */
    public void fpop() {
        ffree(0);
        fincstp();
    }

    /**
     * Sign extends the half-word in the specified register to 32 bits.
     *
     * @param  reg  register that contains the half-word to be extended
     */
    public void signExtendShort(Register reg) {
        if (javac1.Flags.CodeForP6) {
            movsxw(reg, reg);
        } else {
            shll(reg, 16);
            sarl(reg, 16);
        }
    }

    /**
     * Sign extends the byte in the specified register to 32 bits.
     *
     * @param  reg  register that contains the byte to be extended
     */
    public void signExtendByte(Register reg) {
        if (javac1.Flags.CodeForP6 && reg.hasByteRegister()) {
            movsxb(reg, reg);
        } else {
            shll(reg, 24);
            sarl(reg, 24);
        }
    }

    /**
     * Divides the value of the specified register by a power of 2.
     *
     * @param  reg         register that contains the dividend
     * @param  shiftValue  logarithm of the divisor to the base 2
     */
    public void divisionWithShift(Register reg, int shiftValue) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(shiftValue > 0, "illegal shift value");
        }
        Label positive = new Label();
        testl(reg, reg);
        jcc(POSITIVE, positive);
        int offset = (1 << shiftValue) - 1;
        if (offset == 1) {
            incl(reg);
        } else {
            addl(reg, offset);
        }
        bind(positive);
        sarl(reg, shiftValue);
    }

    /**
     * Verifies the object pointer in the specified register.
     *
     * @param  reg  contains the pointer to be verified
     */
    public void verifyOop(Register reg) {
        if (javac1.Flags.VerifyOops) {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Stops with the specified message. The runtime is called to print the
     * message and to dump the registers before execution is stopped.
     *
     * @param  msg  string that specifies the reason of the stop
     */
    public void stop(String msg) {
        Assert.shouldNotReachHere();
    }

    /**
     * Sets an breakpoint at the current code position. Instead of directly
     * emitting a breakpoint, the runtime is called for better debugability.
     */
    public void breakpoint() {
        call(Runtime1.getBreakpointFnPtr(), RelocInfo.RUNTIME_CALL_TYPE);
    }

    /**
     * Pushes the topmost value of the FPU stack onto the CPU stack and pops
     * the FPU stack.
     */
    public void pushFloat() {
        subl(Register.ESP, 8);
        fstpd(new Address(Register.ESP));
    }

    /**
     * Pops a double-word value off the CPU stack and pushes it onto the FPU
     * stack.
     */
    public void popFloat() {
        fldd(new Address(Register.ESP));
        addl(Register.ESP, 8);
    }

    /**
     * Clears the stack of floating-point registers.
     */
    public void clearFpuStack() {
        for (int i = 7; i >= 0; i--) {
            ffree(i);
        }
    }

    /**
     * Pushes the current IU state onto the stack. This way the contents of the
     * general-purpose registers as well as of the EFLAGS register can be saved
     * on the stack.
     */
    public void pushIuState() {
        pushad();
        pushfd();
    }

    /**
     * Pops the IU state off the stack. This way the contents of the EFLAGS
     * register as well as of the general-purpose registers are restored from
     * the stack.
     */
    public void popIuState() {
        popfd();
        popad();
    }

    /**
     * Pushes the current FPU state onto the stack. Aside from the register
     * stack, the FPU operating environment is saved, which consists of the FPU
     * control word, status word, tag word, instruction pointer, data pointer,
     * and last operation code.
     */
    public void pushFpuState() {
        subl(Register.ESP, FPU_STATE_SIZE_IN_WORDS * 4);
        fnsave(new Address(Register.ESP));
        fwait();
    }

    /**
     * Pops the FPU state off the stack. This way the FPU operating environment
     * as well as the register stack is restored from the stack.
     */
    public void popFpuState() {
        frstor(new Address(Register.ESP));
        addl(Register.ESP, FPU_STATE_SIZE_IN_WORDS * 4);
    }

    /**
     * Pushes the current CPU state onto the stack. Both the IU state and the
     * FPU state are saved on the stack.
     */
    public void pushCpuState() {
        pushIuState();
        pushFpuState();
    }

    /**
     * Pops the CPU state off the stack. Both the FPU state and the IU state are
     * restored from the stack.
     */
    public void popCpuState() {
        popFpuState();
        popIuState();
    }

    /**
     * Pushes the values of callee saved registers onto the stack.
     */
    public void pushCalleeSavedRegisters() {
        pushl(Register.ESI);
        pushl(Register.EDI);
        pushl(Register.EDX);
        pushl(Register.ECX);
    }

    /**
     * Pops the values of callee saved registers off the stack.
     */
    public void popCalleeSavedRegister() {
        popl(Register.ECX);
        popl(Register.EDX);
        popl(Register.EDI);
        popl(Register.ESI);
    }

    /**
     * Sets the specified byte register to 0 or 1 depending on if the ZF is set
     * or not.
     *
     * @param  dst  destination operand register
     */
    public void setByteIfNotZero(Register dst) {
        setb(NOT_ZERO, dst);
    }

    /**
     * Sets the specified word register to 0 or 1 depending on if the ZF is set
     * or not.
     *
     * @param  dst  destination operand register
     */
    public void setWordIfNotZero(Register dst) {
        xorl(dst, dst);
        setByteIfNotZero(dst);
    }

    /**
     * Locks an object for the purpose of synchronization.
     *
     * @param  hdr       register for the object header
     * @param  obj       register with the address of the object
     * @param  dispHdr   register pointing to the displaced header location
     * @param  slowCase  entry of the stub calling the runtime
     */
    public void lockObject(Register hdr, Register obj, Register dispHdr,
            Label slowCase) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(hdr.equals(Register.EAX), "register must be eax for cmpxchg");
            Assert.that(!hdr.equals(obj) && !obj.equals(dispHdr) && !dispHdr.equals(hdr), "registers must be different");
            Assert.that(javac1.Flags.BytesPerWord == 4, "adjust alignment mask and code");
        }
        final int alignmentMask = 3;
        final int hdrOffset = Runtime1.getMarkOffset();
        Label done = new Label();
        movl(hdr, new Address(obj, hdrOffset));
        orl(hdr, Runtime1.getMarkUnlockedValue());
        movl(new Address(dispHdr), hdr);
        if (Runtime1.isMultiProcessor()) {
            lock();
        }
        cmpxchg(dispHdr, new Address(obj, hdrOffset));
        jcc(EQUAL, done);
        subl(hdr, Register.ESP);
        andl(hdr, alignmentMask - Runtime1.getVMPageSize());
        movl(new Address(dispHdr), hdr);
        jcc(NOT_ZERO, slowCase);
        bind(done);
    }

    /**
     * Releases an object after the execution of the governed statement.
     *
     * @param  hdr       register for the object header
     * @param  obj       register with the address of the object
     * @param  dispHdr   register pointing to the displaced header location
     * @param  slowCase  entry of the stub calling the runtime
     */
    public void unlockObject(Register hdr, Register obj, Register dispHdr,
            Label slowCase) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dispHdr.equals(Register.EAX), "register must be eax for cmpxchg");
            Assert.that(!hdr.equals(obj) && !obj.equals(dispHdr) && !dispHdr.equals(hdr), "registers must be different");
            Assert.that(javac1.Flags.BytesPerWord == 4, "adjust mask and code");
        }
        final int hdrOffset = Runtime1.getMarkOffset();
        Label done = new Label();
        movl(hdr, new Address(dispHdr));
        testl(hdr, hdr);
        jcc(ZERO, done);
        if (Runtime1.isMultiProcessor()) {
            lock();
        }
        cmpxchg(hdr, new Address(obj, hdrOffset));
        jcc(NOT_EQUAL, slowCase);
        bind(done);
    }

    /**
     * Allocates space on the heap for a new object.
     *
     * @param  obj         register to store address into
     * @param  t1          temporary register
     * @param  t2          temporary register
     * @param  headerSize  size of the object header
     * @param  objectSize  size of the object
     * @param  klass       register that specifies the class of the object
     * @param  slowCase    entry of the stub calling the runtime
     */
    public void allocateObject(Register obj, Register t1, Register t2,
            int headerSize, int objectSize, Register klass, Label slowCase) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.equals(Register.EAX), "object must be in eax for cmpxchg");
            Assert.that(!obj.equals(t1) && !obj.equals(t2) && !t1.equals(t2), "registers must be different");
            Assert.that((headerSize >= 0) && (objectSize >= headerSize), "illegal size information");
        }
        final int hdrOffset = Runtime1.getMarkOffset();
        final Register end = t1;
        Label retry = new Label();
        bind(retry);
        movl(obj, new Address(Runtime1.getHeapTopAddr(), RelocInfo.NONE));
        leal(end, new Address(obj, objectSize * javac1.Flags.BytesPerWord));
        cmpl(end, new Address(Runtime1.getHeapEndAddr(), RelocInfo.NONE));
        jcc(ABOVE, slowCase);
        if (Runtime1.isMultiProcessor()) {
            lock();
        }
        cmpxchg(end, new Address(Runtime1.getHeapTopAddr(), RelocInfo.NONE));
        jcc(NOT_EQUAL, retry);
        movl(new Address(obj, hdrOffset), Runtime1.getMarkPrototype());
        movl(new Address(obj, Runtime1.getKlassOffset()), klass);
        final Register zero = t1;
        final Register index = t2;
        if (objectSize <= 6) {
            xorl(zero, zero);
            for (int i = headerSize; i < objectSize; i++) {
                movl(new Address(obj, i * javac1.Flags.BytesPerWord), zero);
            }
        } else if (objectSize > headerSize) {
            xorl(zero, zero);
            movl(index, (objectSize - headerSize) >> 1);
            if (((objectSize - headerSize) & 1) != 0) {
                movl(new Address(obj,
                    (objectSize - 1) * javac1.Flags.BytesPerWord), zero);
            }
            Label loop = new Label();
            bind(loop);
            movl(new Address(obj, index, Address.TIMES_8,
                (headerSize - 1) * javac1.Flags.BytesPerWord), zero);
            movl(new Address(obj, index, Address.TIMES_8,
                (headerSize - 2) * javac1.Flags.BytesPerWord), zero);
            decl(index);
            jcc(NOT_ZERO, loop);
        }
    }

    /**
     * Allocates space on the heap for a new array.
     *
     * @param  obj         register to store address into
     * @param  len         register that contains the length of the array
     * @param  temp        temporary register
     * @param  headerSize  size of the array header
     * @param  scale       the scaling factor
     * @param  klass       register that specifies the class of the array
     * @param  slowCase    entry of the stub calling the runtime
     */
    public void allocateArray(Register obj, Register len, Register temp,
            int headerSize, int scale, Register klass, Label slowCase) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(obj.equals(Register.EAX), "object must be in eax for cmpxchg");
            Assert.that(!obj.equals(len) && !obj.equals(temp) && !len.equals(temp), "registers must be different");
            Assert.that(javac1.Flags.BytesPerWord == 4, "must be a multiple of 2");
        }
        final int hdrOffset = Runtime1.getMarkOffset();
        final int alignmentMask =
            (javac1.Flags.AlignAllObjects ? 2 : 1) * javac1.Flags.BytesPerWord - 1;
        final int maxLength = 0xffffff;
        cmpl(len, maxLength);
        jcc(ABOVE, slowCase);
        final Register end = temp;
        Label retry = new Label();
        bind(retry);
        movl(obj, new Address(Runtime1.getHeapTopAddr(), RelocInfo.NONE));
        leal(end, new Address(obj, len, scale));
        addl(end, headerSize * javac1.Flags.BytesPerWord + alignmentMask);
        andl(end, ~alignmentMask);
        cmpl(end, obj);
        jcc(BELOW, slowCase);
        cmpl(end, new Address(Runtime1.getHeapEndAddr(), RelocInfo.NONE));
        jcc(ABOVE, slowCase);
        if (Runtime1.isMultiProcessor()) {
            lock();
        }
        cmpxchg(end, new Address(Runtime1.getHeapTopAddr(), RelocInfo.NONE));
        jcc(NOT_EQUAL, retry);
        movl(new Address(obj, hdrOffset), Runtime1.getMarkPrototype());
        movl(new Address(obj, Runtime1.getKlassOffset()), klass);
        movl(new Address(obj, Runtime1.getArrayLengthOffset()), len);
        Label done = new Label();
        final Register zero = len;
        final Register index = temp;
        subl(index, obj);
        subl(index, headerSize * javac1.Flags.BytesPerWord);
        jcc(ZERO, done);
        xorl(zero, zero);
        shrl(index, 3);
        Label even = new Label();
        jcc(CARRY_CLEAR, even);
        movl(new Address(obj, index, Address.TIMES_8,
            headerSize * javac1.Flags.BytesPerWord), zero);
        jcc(ZERO, done);
        bind(even);
        Label loop = new Label();
        bind(loop);
        movl(new Address(obj, index, Address.TIMES_8,
            (headerSize - 1) * javac1.Flags.BytesPerWord), zero);
        movl(new Address(obj, index, Address.TIMES_8,
            (headerSize - 2) * javac1.Flags.BytesPerWord), zero);
        decl(index);
        jcc(NOT_ZERO, loop);
        bind(done);
    }
}
