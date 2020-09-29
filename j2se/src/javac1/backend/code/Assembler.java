/*
 * @(#)Assembler.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;
import javac1.ci.Obj;
import javac1.ci.Runtime1;

/**
 * This class contains methods for generating code into a code buffer. For each
 * possibly used machine instruction there exists a method that combines the
 * operation code and the specified parameters into one or more bytes and
 * appends them to the end of the code buffer. All instruction encodings are
 * subsets of the general instruction format:<p>
 *
 * <table align="center" border="1" style="font-family:sans-serif;font-size:10pt"><tr>
 * <td>&nbsp;Prefixes&nbsp;</td><td>&nbsp;Opcode&nbsp;</td>
 * <td>&nbsp;ModR/M&nbsp;</td><td>&nbsp;SIB&nbsp;</td>
 * <td>&nbsp;Displacement&nbsp;</td><td>&nbsp;Immediate&nbsp;</td>
 * </tr></table><p>
 *
 * The FPU instructions treat the eight FPU data registers as a register stack.
 * The register number of the current top-of-stack register ST(0) is stored in
 * the TOP field of the FPU status word. ST(i) denotes the i-th element from the
 * top of the register stack.<p>
 *
 * Some of the condition code constants have the same value. The resulting
 * instructions have the same operation code and test for the same condition.
 * The alternate mnemonics are provided only to make code more intelligible.
 *
 * @see      "IA-32 Intel Architecture Software Developer's Manual"
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Assembler {
    /**
     * A segment override prefix.
     */
    public static final int
            CS_SEGMENT = 0x2e, SS_SEGMENT = 0x36, DS_SEGMENT = 0x3e,
            ES_SEGMENT = 0x26, FS_SEGMENT = 0x64, GS_SEGMENT = 0x65;

    /**
     * A condition code constant.
     */
    public static final int
            OVERFLOW      = 0x0, NO_OVERFLOW   = 0x1, BELOW         = 0x2,
            CARRY_SET     = 0x2, ABOVE_EQUAL   = 0x3, CARRY_CLEAR   = 0x3,
            EQUAL         = 0x4, ZERO          = 0x4, NOT_EQUAL     = 0x5,
            NOT_ZERO      = 0x5, BELOW_EQUAL   = 0x6, ABOVE         = 0x7,
            NEGATIVE      = 0x8, POSITIVE      = 0x9, PARITY        = 0xa,
            NO_PARITY     = 0xb, LESS          = 0xc, GREATER_EQUAL = 0xd,
            LESS_EQUAL    = 0xe, GREATER       = 0xf;

    /**
     * The size of the FPU state in words. The constant value of this field is
     * <tt>27</tt>.
     */
    public static final int FPU_STATE_SIZE_IN_WORDS = 27;

    /**
     * Selects the embedded 32-bit immediate operand.
     */
    public static final int IMM32_OPERAND = 0;

    /**
     * Selects the embedded 32-bit displacement or address.
     */
    public static final int DISP32_OPERAND = 1;

    /**
     * Selects the embedded self-relative displacement.
     */
    public static final int CALL32_OPERAND = 2;

    /**
     * The code buffer that stores the generated instructions.
     */
    private CodeBuffer code;

    /**
     * The instruction mark required when emitting relocatable values.
     */
    private int instrMark;

    /**
     * Constructs a new assembler generating code into the specified buffer.
     *
     * @param  code  code buffer that stores the instructions
     */
    public Assembler(CodeBuffer code) {
        this.code = code;
        instrMark = 0;
    }

    /**
     * Tests if the specified value can be represented with not more than 8
     * bits. This is true if and only if the value is inside the interval
     * [-128,128[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is an 8-bit value
     */
    protected static boolean is8bit(int x) {
        return (-128 <= x) && (x < 128);
    }

    /**
     * Tests if the specified value is within the range of one unsigned byte.
     * This is true if and only if the value is inside the interval [0,256[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a byte
     */
    protected static boolean isByte(int x) {
        return (0 <= x) && (x < 256);
    }

    /**
     * Tests if the specified value is within the range of shift distances. This
     * is true if and only if the value is inside the interval [0,32[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a valid shift distance
     */
    protected static boolean isShiftCount(int x) {
        return (0 <= x) && (x < 32);
    }

    /**
     * Tests if the CMOV instructions are supported by the processor. The
     * conditional moves and some other instructions have been introduced for
     * the Pentium Pro processor family.
     *
     * @return  whether or not CMOV is supported
     */
    protected static boolean supportsCMOV() {
        return javac1.Flags.CodeForP6;
    }

    /**
     * Returns the code buffer that contains the generated instructions.
     *
     * @return  the code buffer
     */
    public CodeBuffer getCode() {
        return code;
    }

    /**
     * Returns the address of the first byte of the code buffer.
     *
     * @return  start address of the code
     */
    public int getCodeBegin() {
        return code.getCodeBegin();
    }

    /**
     * Returns the current code generation position.
     *
     * @return  current code generation position
     */
    public int getCodePos() {
        return code.getCodeEnd();
    }

    /**
     * Returns the current code generation offset.
     *
     * @return  current code generation offset
     */
    public int getOffset() {
        return getCodePos() - getCodeBegin();
    }

    /**
     * Emits the specified unsigned byte value into the code buffer.
     *
     * @param  x  byte to be emitted
     */
    public void emitByte(int x) {
        code.append(x);
    }

    /**
     * Emits the specified 16-bit integer value into the code buffer.
     *
     * @param  x  16-bit value to be emitted
     */
    public void emitInt(int x) {
        emitByte(x & 0xff);
        emitByte(x >>> 8);
    }

    /**
     * Emits the specified 32-bit integer value into the code buffer.
     *
     * @param  x  32-bit value to be emitted
     */
    public void emitLong(int x) {
        emitInt(x & 0xffff);
        emitInt(x >>> 16);
    }

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte at the specified index
     */
    public int byteAt(int pos) {
        return code.byteAt(pos);
    }

    /**
     * Returns the 16-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 16-bit value at the specified index
     */
    public int intAt(int pos) {
        return (byteAt(pos + 1) << 8) | byteAt(pos);
    }

    /**
     * Returns the 32-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 32-bit value at the specified index
     */
    public int longAt(int pos) {
        return (intAt(pos + 2) << 16) | intAt(pos);
    }

    /**
     * Sets the byte at the specified position to the new value.
     *
     * @param  pos  index of the byte to be replaced
     * @param  x    the new value
     */
    public void setByteAt(int pos, int x) {
        code.setByteAt(pos, x);
    }

    /**
     * Sets the 16-bit integer at the specified position to the new value.
     *
     * @param  pos  index of the 16-bit value to be replaced
     * @param  x    the new value
     */
    public void setIntAt(int pos, int x) {
        setByteAt(pos, x & 0xff);
        setByteAt(pos + 1, x >>> 8);
    }

    /**
     * Sets the 32-bit integer at the specified position to the new value.
     *
     * @param  pos  index of the 32-bit value to be replaced
     * @param  x    the new value
     */
    public void setLongAt(int pos, int x) {
        setIntAt(pos, x & 0xffff);
        setIntAt(pos + 2, x >>> 16);
    }

    /**
     * Marks the current instruction as using relocatable values.
     */
    protected void markInstruction() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instrMark == 0, "overlapping instructions");
        }
        instrMark = getCodePos();
    }

    /**
     * Resets the instruction mark.
     */
    protected void unmarkInstruction() {
        instrMark = 0;
    }

    /**
     * Generates relocation information for the current instruction.
     *
     * @param  reloc   the relocation information
     * @param  format  the relocation format
     */
    public void relocate(RelocInfo reloc, int format) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((instrMark == 0) || (instrMark == getCodePos()), "relocate between instructions");
        }
        code.relocate(getCodePos(), reloc, format);
    }
    
    /**
     * Generates relocation information for the current instruction.
     *
     * @see  #relocate(RelocInfo, int)
     */
    public void relocate(RelocInfo reloc) {
        relocate(reloc, 0);
    }
    
    /**
     * Generates relocation information for the current instruction.
     *
     * @see  #relocate(RelocInfo, int)
     */
    public void relocate(int rtype, int format) {
        if (rtype != RelocInfo.NONE) {
            relocate(new RelocInfo(rtype), format);
        }
    }
    
    /**
     * Generates relocation information for the current instruction.
     *
     * @see  #relocate(RelocInfo, int)
     */
    public void relocate(int rtype) {
        relocate(rtype, 0);
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  data    data to be emitted
     * @param  reloc   the relocation information
     * @param  format  the relocation format
     */
    protected void emitData(int data, RelocInfo reloc, int format) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instrMark != 0, "must be inside instruction mark");
        }
        code.relocate(instrMark, reloc, format);
        emitLong(data);
    }
    
    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @see  #emitData(int, RelocInfo, int)
     */
    protected void emitData(int data, RelocInfo reloc) {
        emitData(data, reloc, 0);
    }
    
    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @see  #emitData(int, RelocInfo, int)
     */
    protected void emitData(int data, int rtype, int format) {
        if (rtype == RelocInfo.NONE) {
            emitLong(data);
        } else {
            emitData(data, new RelocInfo(rtype), format);
        }
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @see  #emitData(int, RelocInfo, int)
     */
    protected void emitData(int data, int rtype) {
        emitData(data, rtype, 0);
    }
    
    /**
     * Emits the specified object into the code buffer and generates relocation
     * information.
     *
     * @param  obj    object to be emitted
     * @param  rtype  the relocation type
     */
    protected void emitData(Obj obj, int rtype) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(rtype == RelocInfo.OOP_TYPE, "wrong relocation type");
        }
        Object oop = (obj != null) ? obj.getOop() : null;
        emitData(code.recordOop(oop), rtype);
    }

    /**
     * Emits an arithmetic instruction with an 8-bit immediate operand.
     *
     * @param  op1   primary operation code
     * @param  op2   extension of the operation code
     * @param  dst   destination register
     * @param  imm8  8-bit immediate value
     */
    protected void emitArithByte(int op1, int op2, Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2) && ((op1 & 0x01) == 0), "wrong operation code");
            Assert.that(dst.hasByteRegister(), "must have byte register");
            Assert.that(isByte(imm8), "immediate out of range");
        }
        emitByte(op1);
        emitByte(op2 | dst.getNumber());
        emitByte(imm8);
    }

    /**
     * Emits an arithmetic instruction with a 32-bit immediate operand.
     *
     * @param  op1    primary operation code
     * @param  op2    extension of the operation code
     * @param  dst    destination register
     * @param  imm32  32-bit immediate value
     */
    protected void emitArith(int op1, int op2, Register dst, int imm32) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2) && ((op1 & 0x03) == 1), "wrong operation code");
        }
        if (is8bit(imm32)) {
            emitByte(op1 | 0x02);
            emitByte(op2 | dst.getNumber());
            emitByte(imm32 & 0xff);
        } else {
            emitByte(op1);
            emitByte(op2 | dst.getNumber());
            emitLong(imm32);
        }
    }

    /**
     * Emits an arithmetic instruction with a register operand.
     *
     * @param  op1  primary operation code
     * @param  op2  extension of the operation code
     * @param  dst  destination register
     * @param  src  source register
     */
    protected void emitArith(int op1, int op2, Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2), "wrong operation code");
        }
        emitByte(op1);
        emitByte(op2 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * Emits an arithmetic instruction with an object pointer operand.
     *
     * @param  op1  primary operation code
     * @param  op2  extension of the operation code
     * @param  dst  destination register
     * @param  obj  object pointer operand
     */
    protected void emitArith(int op1, int op2, Register dst, Obj obj) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2) && ((op1 & 0x03) == 1), "wrong operation code");
        }
        markInstruction();
        emitByte(op1);
        emitByte(op2 | dst.getNumber());
        emitData(obj, RelocInfo.OOP_TYPE);
        unmarkInstruction();
    }

    /**
     * Emits the SIB byte of an address. The SIB byte encodes the scaling factor
     * as well as the number of the index register and the number of the base
     * register.
     *
     * @param  scale  scaling factor
     * @param  index  index register
     * @param  base   base register
     */
    private void emitSIB(int scale, Register index, Register base) {
        emitByte((scale << 6) | (index.getNumber() << 3) | base.getNumber());
    }

    /**
     * Emits the instruction part that specifies the operands. Depending on the
     * chosen addressing mode, between 1 and 6 bytes will be emitted.
     *
     * @param  reg    the register operand
     * @param  base   base register
     * @param  index  index register
     * @param  scale  scaling factor
     * @param  disp   displacement if any
     * @param  rtype  the relocation type
     */
    protected void emitOperand(Register reg, Register base, Register index,
            int scale, int disp, int rtype) {
        RelocInfo reloc = null;
        switch (rtype) {
        case RelocInfo.NONE:
            reloc = new RelocInfo(RelocInfo.NONE);
            break;
        case RelocInfo.INTERNAL_WORD_TYPE:
            int offset = (instrMark - disp) / RelocInfo.OFFSET_UNIT;
            reloc = new RelocInfo(RelocInfo.INTERNAL_WORD_TYPE, offset);
            break;
        default:
            Assert.shouldNotReachHere();
        }
        if (base.isValid()) {
            if (index.isValid()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(scale != Address.NO_SCALE, "inconsistent address");
                }
                if ((disp == 0) && (rtype == RelocInfo.NONE)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(Register.ESP) && !base.equals(Register.EBP), "illegal addressing mode");
                    }
                    emitByte(0x04 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                } else if (is8bit(disp) && (rtype == RelocInfo.NONE)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(Register.ESP), "illegal addressing mode");
                    }
                    emitByte(0x44 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                    emitByte(disp & 0xff);
                } else {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(Register.ESP), "illegal addressing mode");
                    }
                    emitByte(0x84 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                    emitData(disp, reloc, DISP32_OPERAND);
                }
            } else if (base.equals(Register.ESP)) {
                if ((disp == 0) && (rtype == RelocInfo.NONE)) {
                    emitByte(0x04 | (reg.getNumber() << 3));
                    emitByte(0x24);
                } else if (is8bit(disp) && (rtype == RelocInfo.NONE)) {
                    emitByte(0x44 | (reg.getNumber() << 3));
                    emitByte(0x24);
                    emitByte(disp & 0xff);
                } else {
                    emitByte(0x84 | (reg.getNumber() << 3));
                    emitByte(0x24);
                    emitData(disp, reloc, DISP32_OPERAND);
                }
            } else {
                if ((disp == 0) && (rtype == RelocInfo.NONE)) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!base.equals(Register.EBP), "illegal addressing mode");
                    }
                    emitByte(0x00 | (reg.getNumber() << 3) | base.getNumber());
                } else if (is8bit(disp) && (rtype == RelocInfo.NONE)) {
                    emitByte(0x40 | (reg.getNumber() << 3) | base.getNumber());
                    emitByte(disp & 0xff);
                } else {
                    emitByte(0x80 | (reg.getNumber() << 3) | base.getNumber());
                    emitData(disp, reloc, DISP32_OPERAND);
                }
            }
        } else {
            if (index.isValid()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(scale != Address.NO_SCALE, "inconsistent address");
                    Assert.that(!index.equals(Register.ESP), "illegal addressing mode");
                }
                emitByte(0x04 | (reg.getNumber() << 3));
                emitByte((scale << 6) | (index.getNumber() << 3) | 0x05);
                emitData(disp, reloc, DISP32_OPERAND);
            } else {
                emitByte(0x05 | (reg.getNumber() << 3));
                emitData(disp, reloc, DISP32_OPERAND);
            }
        }
    }

    /**
     * Emits the instruction part that specifies the operands.
     *
     * @param  reg  the register operand
     * @param  adr  the address part
     * @see    #emitOperand(Register, Register, Register, int, int, int)
     */
    protected void emitOperand(Register reg, Address adr) {
        emitOperand(reg, adr.getBase(), adr.getIndex(), adr.getScale(),
            adr.getDisp(), adr.getRelocType());
    }

    /**
     * Emits the specified floating-point arithmetic instruction.
     *
     * @param  op1  primary operation code
     * @param  op2  extension of the operation code
     * @param  i    floating-point register stack offset
      */
    protected void emitFarith(int op1, int op2, int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2), "wrong operation code");
            Assert.that((i >= 0) && (i < 8), "illegal stack offset");
        }
        emitByte(op1);
        emitByte(op2 + i);
    }

    /**
     * Returns the displacement at the specified position in the code buffer.
     *
     * @param   label  represents a code position
     * @return  displacement at the specified position
     */
    private Displacement dispAt(Label label) {
        return new Displacement(longAt(label.getPos()));
    }

    /**
     * Stores a displacement at the specified position in the code buffer.
     *
     * @param  label  represents a code position
     * @param  disp   displacement to be stored
     */
    private void setDispAt(Label label, Displacement disp) {
        setLongAt(label.getPos(), disp.getData());
    }

    /**
     * Emits the specified displacement into the code buffer.
     *
     * @param  label  the yet unknown code position
     * @param  type   the instruction type
     * @param  info   instruction specific information
     * @see    Displacement
     */
    protected void emitDisp(Label label, int type, int info) {
        Displacement disp = new Displacement(label, type, info);
        label.linkTo(getOffset());
        emitLong(disp.getData());
    }

    /**
     * Binds the specified label to a certain code position.
     *
     * @param  label  label to be bound
     * @param  pos    target code position
     */
    protected void bindTo(Label label, int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((pos >= 0) && (pos <= getOffset()), "must have a valid binding position");
        }
        while (label.isUnbound()) {
            Displacement disp = dispAt(label);
            int fixupPos = label.getPos();
            if (Assert.ASSERTS_ENABLED) {
                switch (disp.getType()) {
                case Displacement.CALL:
                    Assert.that(byteAt(fixupPos - 1) == 0xe8, "procedure call expected");
                    break;
                case Displacement.ABSOLUTE_JUMP:
                    Assert.that(byteAt(fixupPos - 1) == 0xe9, "absolute jump expected");
                    break;
                case Displacement.CONDITIONAL_JUMP:
                    Assert.that((byteAt(fixupPos - 2) == 0x0f) && (byteAt(fixupPos - 1) == (0x80 | disp.getInfo())), "conditional jump expected");
                    break;
                default:
                    Assert.shouldNotReachHere();
                }
            }
            setLongAt(fixupPos, pos - (fixupPos + 4));
            disp.next(label);
        }
        label.bindTo(pos);
    }

    /**
     * Binds the specified label to the current code position.
     *
     * @param  label  label to be bound
     */
    public void bind(Label label) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!label.isBound(), "label can be bound once only");
        }
        bindTo(label, getOffset());
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void adcl(Register dst, int imm32) {
        emitArith(0x81, 0xd0, dst, imm32);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void adcl(Register dst, Register src) {
        emitArith(0x13, 0xc0, dst, src);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void adcl(Register dst, Address src) {
        markInstruction();
        emitByte(0x13);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void addl(Register dst, int imm32) {
        emitArith(0x81, 0xc0, dst, imm32);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void addl(Address dst, int imm32) {
        markInstruction();
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(Register.EAX, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(Register.EAX, dst);
            emitLong(imm32);
        }
        unmarkInstruction();
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void addl(Address dst, Register src) {
        markInstruction();
        emitByte(0x01);
        emitOperand(src, dst);
        unmarkInstruction();
    }

    /**
     * This instruction adds the destination operand and the source operand and
     * stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void addl(Register dst, Register src) {
        emitArith(0x03, 0xc0, dst, src);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void addl(Register dst, Address src) {
        markInstruction();
        emitByte(0x03);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void andl(Register dst, int imm32) {
        emitArith(0x81, 0xe0, dst, imm32);
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void andl(Register dst, Register src) {
        emitArith(0x23, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void andl(Register dst, Address src) {
        markInstruction();
        emitByte(0x23);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction reverses the byte order of the destination register.
     * This instruction is provided for converting little-endian values to
     * big-endian format and vice versa.
     *
     * @param  reg  destination register
     */
    public void bswap(Register reg) {
        emitByte(0x0f);
        emitByte(0xc8 | reg.getNumber());
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure, whose first instruction has the specified
     * address.
     *
     * @param  adr  destination address
     */
    public void call(Address adr) {
        markInstruction();
        emitByte(0xff);
        emitOperand(Register.EDX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param  label  destination operand label
     * @param  rtype  the relocation type
     */
    public void call(Label label, int rtype) {
        if (label.isBound()) {
            final int longSize = 5;
            int offset = label.getPos() - getOffset();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(offset <= 0, "assembler error");
            }
            markInstruction();
            emitByte(0xe8);
            emitData(offset - longSize, rtype);
            unmarkInstruction();
        } else {
            markInstruction();
            emitByte(0xe8);
            Displacement disp = new Displacement(label, Displacement.CALL, 0);
            label.linkTo(getOffset());
            emitData(disp.getData(), rtype);
            unmarkInstruction();
        }
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param  entry  destination operand address
     * @param  rtype  the relocation type
     */
    public void call(int entry, int rtype) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(rtype != RelocInfo.VIRTUAL_CALL_TYPE, "should not reach here");
            Assert.that(entry != 0, "call most probably wrong");
        }
        markInstruction();
        emitByte(0xe8);
        emitData(entry - (getCodePos() + 4), rtype);
        unmarkInstruction();
    }
    
    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param  entry     destination operand address
     * @param  rtype     the relocation type
     * @param  firstOop  address of the first pointer
     */
    public void call(int entry, RelocInfo reloc) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(entry != 0, "call most probably wrong");
        }
        markInstruction();
        emitByte(0xe8);
        emitData(entry - (getCodePos() + 4), reloc);
        unmarkInstruction();
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param   entry  destination operand register
     * @param   rtype  the relocation type
     */
    public void call(Register dst, int rtype) {
        relocate(rtype);
        emitByte(0xff);
        emitByte(0xd0 | dst.getNumber());
    }

    /**
     * This instruction doubles the size of the operand in the EAX register by
     * means of sign extension and stores the result in the EDX:EAX registers.
     */
    public void cdql() {
        emitByte(0x99);
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a move operation if the flags are in a specified state.
     *
     * @param  cc   condition code
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void cmovl(int cc, Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitByte(0x0f);
        emitByte(0x40 | cc);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void cmpl(Register dst, int imm32) {
        emitArith(0x81, 0xf8, dst, imm32);
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst  destination operand register
     * @param  obj  source operand object pointer
     */
    public void cmpl(Register dst, Obj obj) {
        emitArith(0x81, 0xf8, dst, obj);
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void cmpl(Address dst, int imm32) {
        markInstruction();
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(Register.EDI, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(Register.EDI, dst);
            emitLong(imm32);
        }
        unmarkInstruction();
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst  destination operand address
     * @param  obj  source operand object pointer
     */
    public void cmpl(Address dst, Obj obj) {
        markInstruction();
        emitByte(0x81);
        emitOperand(Register.EDI, dst);
        emitData(obj, RelocInfo.OOP_TYPE);
        unmarkInstruction();
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void cmpl(Register dst, Register src) {
        emitArith(0x3b, 0xc0, dst, src);
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand address
     */
    public void cmpl(Register dst, Address src) {
        markInstruction();
        emitByte(0x3b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction compares the value in the EAX register with the
     * destination operand. If the two values are equal, the source operand is
     * loaded into the destination operand. Otherwise, the destination operand
     * is loaded into the EAX register.
     *
     * @param  reg  source operand register
     * @param  adr  destination operand address
     */
    public void cmpxchg(Register reg, Address adr) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xb1);
        emitOperand(reg, adr);
        unmarkInstruction();
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void decb(Register dst) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        emitByte(0xfe);
        emitByte(0xc8 | dst.getNumber());
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void decl(Register dst) {
        emitByte(0x48 | dst.getNumber());
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand address
     */
    public void decl(Address dst) {
        markInstruction();
        emitByte(0xff);
        emitOperand(Register.ECX, dst);
        unmarkInstruction();
    }

    /**
     * This instruction clears the sign bit of the ST(0) register to create the
     * absolute value of the operand.
     */
    public void fabs() {
        emitByte(0xd9);
        emitByte(0xe1);
    }

    /**
     * This instruction adds the single-real source operand to the ST(0)
     * register and stores the sum in ST(0).
     *
     * @param  src  source operand address
     */
    public void fadds(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.EAX, src);
        unmarkInstruction();
    }

    /**
     * This instruction adds the double-real source operand to the ST(0)
     * register and stores the sum in ST(0).
     *
     * @param  src  source operand address
     */
    public void faddd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.EAX, src);
        unmarkInstruction();
    }

    /**
     * This instruction adds the contents of the ST(0) register to the ST(i)
     * register and stores the sum in ST(0).
     *
     * @param  i  source operand index
     */
    public void fadd(int i) {
        emitFarith(0xd8, 0xc0, i);
    }

    /**
     * This instruction adds the contents of the ST(0) register to the ST(i)
     * register, stores the sum in ST(i) and pops the register stack.
     *
     * @param  i  source operand index
     */
    public void faddp(int i) {
        emitFarith(0xde, 0xc0, i);
    }

    /**
     * This instruction complements the sign bit of the ST(0) register, that is
     * changes a positive value into a negative value of equal magnitude or vice
     * versa.
     */
    public void fchs() {
        emitByte(0xd9);
        emitByte(0xe0);
    }

    /**
     * This instruction compares the value of the ST(0) register with the
     * single-real source operand, sets the condition code flags in the FPU
     * status word according to the results and pops the register stack.
     *
     * @param  src  source operand address
     */
    public void fcomps(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.EBX, src);
        unmarkInstruction();
    }

    /**
     * This instruction compares the value of the ST(0) register with the
     * double-real source operand, sets the condition code flags in the FPU
     * status word according to the results and pops the register stack.
     *
     * @param  src  source operand address
     */
    public void fcompd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.EBX, src);
        unmarkInstruction();
    }

    /**
     * This instruction compares the value of the ST(0) register with the value
     * of the ST(1) register, sets the condition code flags in the FPU status
     * word according to the results and pops the register stack twice.
     */
    public void fcompp() {
        emitByte(0xde);
        emitByte(0xd9);
    }

    /**
     * This instruction calculates the cosine of the source operand in the ST(0)
     * register and stores the result in ST(0).
     */
    public void fcos() {
        emitByte(0xd9);
        emitByte(0xff);
    }

    /**
     * This instruction subtracts one from the TOP field of the FPU status word,
     * that is decrements the top-of-stack pointer.
     */
    public void fdecstp() {
        emitByte(0xd9);
        emitByte(0xf6);
    }

    /**
     * This instruction divides the value of the ST(0) register by the
     * single-real source operand and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivs(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.ESI, src);
        unmarkInstruction();
    }

    /**
     * This instruction divides the value of the ST(0) register by the
     * double-real source operand and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.ESI, src);
        unmarkInstruction();
    }

    /**
     * This instruction divides the value of the ST(0) register by the value of
     * the ST(i) register and stores the result in ST(0).
     *
     * @param  i  source operand index
     */
    public void fdiv(int i) {
        emitFarith(0xd8, 0xf0, i);
    }

    /**
     * This instruction divides the value of the ST(0) register by the value of
     * the ST(i) register, stores the result in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fdivp(int i) {
        emitFarith(0xde, 0xf8, i);
    }

    /**
     * This instruction divides the single-real source operand by the value of
     * the ST(0) register and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivrs(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.EDI, src);
        unmarkInstruction();
    }

    /**
     * This instruction divides the double-real source operand by value of the
     * ST(0) register and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivrd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.EDI, src);
        unmarkInstruction();
    }

    /**
     * This instruction divides the value of the ST(i) register by the value of
     * the ST(0) register, stores the result in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fdivrp(int i) {
        emitFarith(0xde, 0xf0, i);
    }

    /**
     * This instruction sets the tag in the FPU tag register associated with the
     * register ST(i) to empty. The contents of the register and the FPU
     * top-of-stack pointer are not affected.
     *
     * @param  i  index of the register
     */
    public void ffree(int i) {
        emitFarith(0xdd, 0xc0, i);
    }

    /**
     * This instruction converts the short signed integer source operand into
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  adr  source operand address
     */
    public void filds(Address adr) {
        markInstruction();
        emitByte(0xdb);
        emitOperand(Register.EAX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction converts the long signed integer source operand into
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  adr  source operand address
     */
    public void fildd(Address adr) {
        markInstruction();
        emitByte(0xdf);
        emitOperand(Register.EBP, adr);
        unmarkInstruction();
    }

    /**
     * This instruction adds one to the TOP field of the FPU status word, that
     * is increments the top-of-stack pointer.
     */
    public void fincstp() {
        emitByte(0xd9);
        emitByte(0xf7);
    }

    /**
     * This instruction sets the FPU control, status, tag, instruction pointer,
     * and data pointer registers to their default states.
     */
    public void finit() {
        emitByte(0x9b);
        emitByte(0xdb);
        emitByte(0xe3);
    }

    /**
     * This instruction converts the value in the ST(0) register to a short
     * signed integer and stores the result in the destination operand.
     *
     * @param  adr  destination operand address
     */
    public void fists(Address adr) {
        markInstruction();
        emitByte(0xdb);
        emitOperand(Register.EDX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction converts the value in the ST(0) register to a short
     * signed integer, stores the result in the destination operand and pops the
     * register stack.
     *
     * @param  adr  destination operand address
     */
    public void fistps(Address adr) {
        markInstruction();
        emitByte(0xdb);
        emitOperand(Register.EBX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction converts the value in the ST(0) register to a long
     * signed integer, stores the result in the destination operand and pops the
     * register stack.
     *
     * @param  adr  destination operand address
     */
    public void fistpd(Address adr) {
        markInstruction();
        emitByte(0xdf);
        emitOperand(Register.EDI, adr);
        unmarkInstruction();
    }

    /**
     * This instruction converts the single-real source operand to the
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  source operand address
     */
    public void flds(Address adr) {
        markInstruction();
        emitByte(0xd9);
        emitOperand(Register.EAX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction converts the double-real source operand to the
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  source operand address
     */
    public void fldd(Address adr) {
        markInstruction();
        emitByte(0xdd);
        emitOperand(Register.EAX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction pushes the value in the ST(i) register onto the stack.
     *
     * @param  i  source operand index
     */
    public void flds(int i) {
        emitFarith(0xd9, 0xc0, i);
    }

    /**
     * This instruction pushes 1.0 onto the FPU register stack.
     */
    public void fld1() {
        emitByte(0xd9);
        emitByte(0xe8);
    }

    /**
     * This instruction pushes 0.0 onto the FPU register stack.
     */
    public void fldz() {
        emitByte(0xd9);
        emitByte(0xee);
    }

    /**
     * This instruction loads the 16-bit source operand into the FPU control
     * word.
     *
     * @param  src  source operand address
     */
    public void fldcw(Address src) {
        markInstruction();
        emitByte(0xd9);
        emitOperand(Register.EBP, src);
        unmarkInstruction();
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the
     * single-real source operand and stores the product in ST(0).
     *
     * @param  src  source operand address
     */
    public void fmuls(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.ECX, src);
        unmarkInstruction();
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the
     * double-real source operand and stores the product in ST(0).
     *
     * @param  src  source operand address
     */
    public void fmuld(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.ECX, src);
        unmarkInstruction();
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the value
     * of the ST(i) register and stores the product in ST(0).
     *
     * @param  i  source operand index
     */
    public void fmul(int i) {
        emitFarith(0xd8, 0xc8, i);
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the value
     * of the ST(i) register, stores the product in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fmulp(int i) {
        emitFarith(0xde, 0xc8, i);
    }

    /**
     * This instruction stores the current FPU state at the specified
     * destination in memory without checking for pending unmasked
     * floating-point exceptions, and then re-initializes the FPU.
     *
     * @param  dst  destination address
     */
    public void fnsave(Address dst) {
        emitByte(0xdd);
        emitOperand(Register.ESI, dst);
    }

    /**
     * This instruction stores the current value of the FPU control word at the
     * specified destination in memory after checking for pending unmasked
     * floating-point exceptions.
     *
     * @param  dst  destination address
     */
    public void fstcw(Address dst) {
        markInstruction();
        emitByte(0x9b);
        emitByte(0xd9);
        emitOperand(Register.EDI, dst);
        unmarkInstruction();
    }

    /**
     * This instruction stores the current value of the FPU status word in the
     * AX register without checking for pending unmasked floating-point
     * exceptions. It is used primarily in conditional branching, where the
     * direction of the branch depends on the state of the FPU condition code
     * flags.
     */
    public void fnstswax() {
        emitByte(0xdf);
        emitByte(0xe0);
    }

    /**
     * This instruction computes the partial remainder obtained from dividing
     * the value in the ST(0) register by the value in the ST(1) register and
     * stores the result in ST(0).
     */
    public void fprem() {
        emitByte(0xd9);
        emitByte(0xf8);
    }

    /**
     * This instruction computes the partial IEEE remainder obtained from
     * dividing the value in the ST(0) register by the value in the ST(1)
     * register and stores the result in ST(0).
     */
    public void fprem1() {
        emitByte(0xd9);
        emitByte(0xf5);
    }

    /**
     * This instruction loads the FPU state from the memory area specified by
     * the source operand.
     *
     * @param  src  source operand address
     */
    public void frstor(Address src) {
        emitByte(0xdd);
        emitOperand(Register.ESP, src);
    }

    /**
     * This instruction calculates the sine of the source operand in the ST(0)
     * register and stores the result in ST(0).
     */
    public void fsin() {
        emitByte(0xd9);
        emitByte(0xfe);
    }

    /**
     * This instruction calculates the square root of the source value in the
     * ST(0) register and stores the result in ST(0).
     */
    public void fsqrt() {
        emitByte(0xD9);
        emitByte(0xfa);
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to single-real format.
     *
     * @param  adr  destination address
     */
    public void fsts(Address adr) {
        markInstruction();
        emitByte(0xD9);
        emitOperand(Register.EDX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to double-real format.
     *
     * @param  adr  destination address
     */
    public void fstd(Address adr) {
        markInstruction();
        emitByte(0xdd);
        emitOperand(Register.EDX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to single-real format and pops
     * the register stack.
     *
     * @param  adr  destination address
     */
    public void fstps(Address adr) {
        markInstruction();
        emitByte(0xd9);
        emitOperand(Register.EBX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to double-real format and pops
     * the register stack.
     *
     * @param  adr  destination address
     */
    public void fstpd(Address adr) {
        markInstruction();
        emitByte(0xdd);
        emitOperand(Register.EBX, adr);
        unmarkInstruction();
    }

    /**
     * This instruction copies the value in the ST(0) register to the ST(i)
     * register and pops the register stack.
     *
     * @param  i  destination operand index
     */
    public void fstpd(int i) {
        emitFarith(0xdd, 0xd8, i);
    }

    /**
     * This instruction subtracts the single-real source operand from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubs(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.ESP, src);
        unmarkInstruction();
    }

    /**
     * This instruction subtracts the double-real source operand from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.ESP, src);
        unmarkInstruction();
    }

    /**
     * This instruction subtracts the value of the ST(i) register from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  i  source operand index
     */
    public void fsub(int i) {
        emitFarith(0xd8, 0xe0, i);
    }

    /**
     * This instruction subtracts the value of the ST(i) register from the value
     * of the ST(0) register, stores the difference in ST(i) and pops the
     * register stack.
     *
     * @param  i  source operand index
     */
    public void fsubp(int i) {
        emitFarith(0xde, 0xe8, i);
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the
     * single-real source operand and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubrs(Address src) {
        markInstruction();
        emitByte(0xd8);
        emitOperand(Register.EBP, src);
        unmarkInstruction();
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the
     * double-real source operand and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubrd(Address src) {
        markInstruction();
        emitByte(0xdc);
        emitOperand(Register.EBP, src);
        unmarkInstruction();
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the value
     * of the ST(i) register, stores the difference in ST(i) and pops the
     * register stack.
     *
     * @param  i  source operand index
     */
    public void fsubrp(int i) {
        emitFarith(0xde, 0xe0, i);
    }

    /**
     * This instruction compares the value in the ST(0) register with 0.0 and
     * sets the condition code flags in the FPU status word according to the
     * results.
     */
    public void ftst() {
        emitByte(0xd9);
        emitByte(0xe4);
    }

    /**
     * This instruction performs an unordered comparison of the contents of the
     * registers ST(0) and ST(i) and sets condition code flags according to the
     * results.
     *
     * @param  i  source operand index
     */
    public void fucomi(int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitFarith(0xdb, 0xe8, i);
    }

    /**
     * This instruction performs an unordered comparison of the contents of the
     * registers ST(0) and ST(i), sets condition code flags according to the
     * results and pops the register stack.
     *
     * @param  i  source operand index
     */
    public void fucomip(int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitFarith(0xdf, 0xe8, i);
    }

    /**
     * This instruction causes the processor to check for and handle pending,
     * unmasked, floating-point exceptions before proceeding.
     */
    public void fwait() {
        emitByte(0x9b);
    }

    /**
     * This instruction exchanges the contents of the ST(0) register and the
     * ST(i) register.
     *
     * @param  i  index of the register
     */
    public void fxch(int i) {
        emitFarith(0xd9, 0xc8, i);
    }

    /**
     * This instruction stops instruction execution and places the processor in
     * a HALT state.
     */
    public void hlt() {
        emitByte(0xf4);
    }

    /**
     * This instruction performs a signed division of the value in the EAX
     * register by the source operand and stores the quotient rounded toward
     * zero in the EAX register and the remainder in the EDX register.
     *
     * @param  src  source operand register
     */
    public void idivl(Register src) {
        emitByte(0xf7);
        emitByte(0xf8 | src.getNumber());
    }

    /**
     * This instruction performs a signed multiplication of the destination
     * operand and the source operand and stores the product in the destination
     * operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void imull(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xaf);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction multiplies the first source operand by the second source
     * operand and stores the product in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  src    first source operand register
     * @param  value  second source operand value
     */
    public void imull(Register dst, Register src, int value) {
        if (is8bit(value)) {
            emitByte(0x6b);
            emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
            emitLong(value);
        }
    }

    /**
     * This instruction adds one to the destination operand, while preserving
     * the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void incl(Register dst) {
        emitByte(0x40 | dst.getNumber());
    }

    /**
     * This instruction adds one to the destination operand, while preserving
     * the state of the CF flag.
     *
     * @param  dst  destination operand address
     */
    public void incl(Address dst) {
        markInstruction();
        emitByte(0xff);
        emitOperand(Register.EAX, dst);
        unmarkInstruction();
    }

    /**
     * This instruction explicitly calls the breakpoint exception handler.
     */
    public void int3() {
        emitByte(0xcc);
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a jump to the target instruction if the flags are in a specified
     * state.
     *
     * @param  cc      condition code
     * @param  label   destination operand label
     * @param  rtrype  the relocation type
     */
    public void jcc(int cc, Label label, int rtype) {
        relocate(rtype);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((cc >= 0) && (cc < 16), "illegal condition code");
        }
        if (label.isBound()) {
            final int shortSize = 2;
            final int longSize = 6;
            int offset = label.getPos() - getOffset();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(offset <= 0, "assembler error");
            }
            if (isByte(offset - shortSize)) {
                emitByte(0x70 | cc);
                emitByte((offset - shortSize) & 0xff);
            } else {
                emitByte(0x0f);
                emitByte(0x80 | cc);
                emitLong(offset - longSize);
            }
        } else {
            emitByte(0x0f);
            emitByte(0x80 | cc);
            emitDisp(label, Displacement.CONDITIONAL_JUMP, cc);
        }
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a jump to the target instruction if the flags are in a specified
     * state.
     *
     * @see  #jcc(int, Label, int)
     */
    public void jcc(int cc, Label label) {
        jcc(cc, label, RelocInfo.NONE);
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a jump to the target instruction if the flags are in a specified
     * state.
     *
     * @param  cc      condition code
     * @param  entry   destination operand address
     * @param  rtrype  the relocation type
     */
    public void jcc(int cc, int entry, int rtype) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((cc >= 0) && (cc < 16), "illegal condition code");
            Assert.that(entry != 0, "jump most probably wrong");
        }
        markInstruction();
        emitByte(0x0f);
        emitByte(0x80 | cc);
        emitData(entry - (getCodePos() + 4), rtype);
        unmarkInstruction();
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  adr  destination operand address
     */
    public void jmp(Address adr) {
        markInstruction();
        emitByte(0xff);
        emitOperand(Register.ESP, adr);
        unmarkInstruction();
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  entry  destination operand address
     * @param  rtype  the relocation type
     */
    public void jmp(int entry, int rtype) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(entry != 0, "jump most probably wrong");
        }
        markInstruction();
        emitByte(0xe9);
        emitData(entry - (getCodePos() + 4), rtype);
        unmarkInstruction();
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param   entry  destination operand register
     * @param   rtype  the relocation type
     */
    public void jmp(Register reg, int rtype) {
        relocate(rtype);
        emitByte(0xff);
        emitByte(0xe0 | reg.getNumber());
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  label  destination operand label
     * @param  rtype  the relocation type
     */
    public void jmp(Label label, int rtype) {
        relocate(rtype);
        if (label.isBound()) {
            final int shortSize = 2;
            final int longSize = 5;
            int offset = label.getPos() - getOffset();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(offset <= 0, "assembler error");
            }
            if (isByte(offset - shortSize)) {
                emitByte(0xeb);
                emitByte((offset - shortSize) & 0xff);
            } else {
                emitByte(0xe9);
                emitLong(offset - longSize);
            }
        } else {
            emitByte(0xe9);
            emitDisp(label, Displacement.ABSOLUTE_JUMP, 0);
        }
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  label  destination operand label
     */
    public void jmp(Label label) {
        jmp(label, RelocInfo.NONE);
    }

    /**
     * This instruction computes the effective address of the source operand and
     * stores it in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void leal(Register dst, Address src) {
        markInstruction();
        emitByte(0x8d);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * The LOCK prefix can be prepended to certain instructions to turn them
     * into atomic instructions.
     */
    public void lock() {
        emitByte(0xf0);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movb(Address dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        markInstruction();
        emitByte(0x88);
        emitOperand(src, dst);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movw(Address dst, Register src) {
        markInstruction();
        emitByte(0x66);
        emitByte(0x89);
        emitOperand(src, dst);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movl(Address dst, Register src) {
        markInstruction();
        emitByte(0x89);
        emitOperand(src, dst);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movb(Register dst, Address src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        markInstruction();
        emitByte(0x8a);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movw(Register dst, Address src) {
        markInstruction();
        emitByte(0x66);
        emitByte(0x8b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movl(Register dst, Register src) {
        emitByte(0x8b);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movl(Register dst, Address src) {
        markInstruction();
        emitByte(0x8b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit immediate
     */
    public void movl(Register dst, int imm32) {
        emitByte(0xb8 | dst.getNumber());
        emitLong(imm32);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand object pointer
     */
    public void movl(Register dst, Obj obj) {
        markInstruction();
        emitByte(0xb8 | dst.getNumber());
        emitData(obj, RelocInfo.OOP_TYPE);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand 8-bit immediate
     */
    public void movb(Address dst, int imm8) {
        markInstruction();
        emitByte(0xc6);
        emitOperand(Register.EAX, dst);
        emitByte(imm8);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand 32-bit immediate
     */
    public void movl(Address dst, int imm32) {
        markInstruction();
        emitByte(0xc7);
        emitOperand(Register.EAX, dst);
        emitLong(imm32);
        unmarkInstruction();
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand object pointer
     */
    public void movl(Address dst, Obj obj) {
        markInstruction();
        emitByte(0xc7);
        emitOperand(Register.EAX, dst);
        emitData(obj, RelocInfo.OOP_TYPE);
        unmarkInstruction();
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movsxb(Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        emitByte(0x0f);
        emitByte(0xbe);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movsxb(Register dst, Address src) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xbe);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movsxw(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xbf);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movsxw(Register dst, Address src) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xbf);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movzxb(Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        emitByte(0x0f);
        emitByte(0xb6);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movzxb(Register dst, Address src) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xb6);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movzxw(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xb7);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movzxw(Register dst, Address src) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xb7);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction performs an unsigned multiplication of the EAX register
     * and the source operand and stores the result in the EDX:EAX registers.
     *
     * @param  src  source operand register
     */
    public void mull(Register src) {
        emitByte(0xf7);
        emitByte(0xe0 | src.getNumber());
    }

    /**
     * This instruction performs an unsigned multiplication of the EAX register
     * and the source operand and stores the result in the EDX:EAX registers.
     *
     * @param  src  source operand address
     */
    public void mull(Address src) {
        markInstruction();
        emitByte(0xf7);
        emitOperand(Register.ESP, src);
        unmarkInstruction();
    }

    /**
     * This instruction replaces the value of the destination operand with its
     * two's complement, that is subtracts the operand from 0.
     *
     * @param  dst  destination operand register
     */
    public void negl(Register dst) {
        emitByte(0xf7);
        emitByte(0xd8 | dst.getNumber());
    }

    /**
     * This instruction performs no operation. It is a one-byte instruction that
     * takes up space in the instruction stream but does not affect the machine
     * context, except the EIP register.
     */
    public void nop() {
        emitByte(0x90);
    }

    /**
     * This instruction performs a bitwise NOT operation on the destination
     * operand and stores the result in the destination operand location.
     *
     * @param  dst  destination operand register
     */
    public void notl(Register dst) {
        emitByte(0xf7);
        emitByte(0xd0 | dst.getNumber());
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit immediate
     */
    public void orl(Register dst, int imm32) {
        emitArith(0x81, 0xc8, dst, imm32);
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void orl(Register dst, Register src) {
        emitArith(0x0b, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void orl(Register dst, Address src) {
        markInstruction();
        emitByte(0x0b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction loads the value from the top of the stack to the
     * location specified with the destination operand and then increments the
     * stack pointer.
     *
     * @param  dst  destination operand register
     */
    public void popl(Register dst) {
        emitByte(0x58 | dst.getNumber());
    }

    /**
     * This instruction loads the value from the top of the stack to the
     * location specified with the destination operand and then increments the
     * stack pointer.
     *
     * @param  dst  destination operand address
     */
    public void popl(Address dst) {
        markInstruction();
        emitByte(0x8f);
        emitOperand(Register.EAX, dst);
        unmarkInstruction();
    }

    /**
     * This instruction pops doublewords from the stack into the general-purpose
     * registers EDI, ESI, EBP, EBX, EDX, ECX, and EAX.
     */
    public void popad() {
        emitByte(0x61);
    }

    /**
     * This instruction pops a doubleword from the top of the stack and stores
     * the value in the EFLAGS register.
     */
    public void popfd() {
        emitByte(0x9d);
    }

    /**
     * Emits the specified prefix byte.
     *
     * @param  prefix  prefix byte
     */
    public void prefix(int prefix) {
        emitByte(prefix);
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src  source operand address
     */
    public void pushl(Address src) {
        markInstruction();
        emitByte(0xFF);
        emitOperand(Register.ESI, src);
        unmarkInstruction();
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src  source operand register
     */
    public void pushl(Register src) {
        emitByte(0x50 | src.getNumber());
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src  source operand 32-bit immediate
     */
    public void pushl(int imm32) {
        emitByte(0x68);
        emitLong(imm32);
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  obj  source operand object pointer
     */
    public void pushl(Obj obj) {
        markInstruction();
        emitByte(0x68);
        emitData(obj, RelocInfo.OOP_TYPE);
        unmarkInstruction();
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src    source operand 32-bit immediate
     * @param  rtype  the relocation type
     */
    public void pushl(int imm32, int rtype) {
        markInstruction();
        emitByte(0x68);
        emitData(imm32, rtype);
        unmarkInstruction();
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src    source operand label
     * @param  rtype  the relocation type
     */
    public void pushl(Label label, int rtype) {
        if (label.isBound()) {
            int offset = getCodeBegin() + label.getPos();
            markInstruction();
            emitByte(0x68);
            emitData(offset, rtype);
            unmarkInstruction();
        } else {
            Assert.shouldNotReachHere();
        }
    }

    /**
     * This instruction pushes the contents of the general-purpose registers
     * EAX, ECX, EDX, EBX, EBP, ESP, EBP, ESI, and EDI onto the stack.
     */
    public void pushad() {
        emitByte(0x60);
    }

    /**
     * This instruction decrements the stack pointer by four and pushes the
     * entire contents of the EFLAGS register onto the stack.
     */
    public void pushfd() {
        emitByte(0x9c);
    }

    /**
     * This instruction rotates 33 bits of the destination operand the specified
     * number of bit positions and stores the result in the destination operand.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void rcll(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xd0 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xd0 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction moves doublewords from DS:ESI to ES:EDI the number of
     * times specified in the count register ECX.
     */
    public void repmovs() {
        emitByte(0xf3);
        emitByte(0xa5);
    }

    /**
     * This instruction fills doublewords at ES:EDI with the value of the EAX
     * register the number of times specified in the count register ECX.
     */
    public void repstos() {
        emitByte(0xf3);
        emitByte(0xab);
    }

    /**
     * This instruction transfers program control to a return address located on
     * the top of the stack and pops the specified number of bytes from the
     * stack.
     *
     * @param  imm16  16-bit number of bytes to pop
     */
    public void ret(int imm16) {
        if (imm16 == 0) {
            emitByte(0xc3);
        } else {
            emitByte(0xc2);
            emitInt(imm16);
        }
    }

    /**
     * This instruction loads the SF, ZF, AF, PF, and CF flags of the EFLAGS
     * register with the values from the bits 7, 6, 4, 2, and 0 in the AH
     * register.
     */
    public void sahf() {
        emitByte(0x9e);
    }

    /**
     * This instruction performs a signed division of the destination operand by
     * two multiple times and rounds toward negative infinity. That is it shifts
     * the operand to the right by the specified number of bits and sets or
     * clears the most significant bits depending on the sign of the original
     * value.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void sarl(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xf8 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xf8 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs a signed division of the destination operand by
     * two multiple times and rounds toward negative infinity. That is it shifts
     * the operand to the right by the number of bits specified in the count
     * register CL and sets or clears the most significant bits depending on the
     * sign of the original value.
     *
     * @param  dst  destination operand register
     */
    public void sarl(Register dst) {
        emitByte(0xd3);
        emitByte(0xf8 | dst.getNumber());
    }

    /**
     * This instruction performs a multiplication of the destination operand by
     * two multiple times, that is shifts the operand to the left by the
     * specified number of bits.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void shll(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xe0 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xe0 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs a multiplication of the destination operand by
     * two multiple times, that is shifts the operand to the left by the number
     * of bits specified in the count register CL.
     *
     * @param  dst  destination operand register
     */
    public void shll(Register dst) {
        emitByte(0xd3);
        emitByte(0xe0 | dst.getNumber());
    }

    /**
     * This instruction performs an unsigned division of the destination operand
     * by two multiple times, that is shifts the operand to the right by the
     * specified number of bits and clears the most significant bits.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void shrl(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xe8 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xe8 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs an unsigned division of the destination operand
     * by two multiple times, that is shifts the operand to the right by the
     * number of bits specified in the count register CL and clears the most
     * significant bits.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void shrl(Register dst) {
        emitByte(0xd3);
        emitByte(0xe8 | dst.getNumber());
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit immediate
     */
    public void sbbl(Register dst, int imm32) {
        emitArith(0x81, 0xd8, dst, imm32);
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void sbbl(Register dst, Register src) {
        emitArith(0x1b, 0xc0, dst, src);
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void sbbl(Register dst, Address src) {
        markInstruction();
        emitByte(0x1b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction sets the destination operand to 0 or 1 depending on the
     * settings of the status flags CF, SF, OF, ZF, and PF in the EFLAGS
     * register.
     *
     * @param  cc   condition being tested for
     * @param  dst  destination operand register
     */
     public void setb(int cc, Register dst) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((cc >= 0) && (cc < 16), "illegal condition code");
        }
        emitByte(0x0f);
        emitByte(0x90 | cc);
        emitByte(0xc0 | dst.getNumber());
    }

    /**
     * This instruction shifts the destination operand to the left the number
     * of bits specified in the count register CL. The source operand provides
     * bits to shift in from the right.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void shldl(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xa5);
        emitByte(0xc0 | (src.getNumber() << 3) | dst.getNumber());
    }

    /**
     * This instruction shifts the destination operand to the right the number
     * of bits specified in the count register CL. The source operand provides
     * bits to shift in from the left.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void shrdl(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xad);
        emitByte(0xc0 | (src.getNumber() << 3) | dst.getNumber());
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit immediate
     */
    public void subl(Register dst, int imm32) {
        emitArith(0x81, 0xe8, dst, imm32);
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand 32-bit immediate
     */
    public void subl(Address dst, int imm32) {
        markInstruction();
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(Register.EBP, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(Register.EBP, dst);
            emitLong(imm32);
        }
        unmarkInstruction();
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit register
     */
    public void subl(Register dst, Register src) {
        emitArith(0x2b, 0xc0, dst, src);
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void subl(Register dst, Address src) {
        markInstruction();
        emitByte(0x2b);
        emitOperand(dst, src);
        unmarkInstruction();
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst   destination operand register
     * @param  imm8  source operand 8-bit immediate
     */
    public void testb(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        emitArithByte(0xf6, 0xc0, dst, imm8);
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void testl(Register dst, int imm32) {
        if (dst.getNumber() == 0) {
            emitByte(0xa9);
        } else {
            emitByte(0xf7);
            emitByte(0xc0 | dst.getNumber());
        }
        emitLong(imm32);
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void testl(Register dst, Register src) {
        emitArith(0x85, 0xc0, dst, src);
    }

    /**
     * This instruction exchanges the destination operand with the source
     * operand and then loads the sum of the two values into the destination
     * operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void xaddl(Address dst, Register src) {
        markInstruction();
        emitByte(0x0f);
        emitByte(0xc1);
        emitOperand(src, dst);
        unmarkInstruction();
    }

    /**
     * This instruction exchanges the contents of the destination and source
     * operand.
     *
     * @param  reg  destination operand register
     * @param  adr  source operand address
     */
    public void xchg(Register reg, Address adr) {
        markInstruction();
        emitByte(0x87);
        emitOperand(reg, adr);
        unmarkInstruction();
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void xorl(Register dst, int imm32) {
        emitArith(0x81, 0xf0, dst, imm32);
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand register
     */
    public void xorl(Register dst, Register src) {
        emitArith(0x33, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand address
     */
    public void xorl(Register dst, Address src) {
        markInstruction();
        emitByte(0x33);
        emitOperand(dst, src);
        unmarkInstruction();
    }
}
