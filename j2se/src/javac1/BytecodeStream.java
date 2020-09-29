/*
 * @(#)BytecodeStream.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

/**
 * A stream whose source is the bytecode array of a method. This class is used
 * for fast iteration over the bytecodes and for access to their operands.<p>
 *
 * The access methods for signed and unsigned operands are overloaded to take
 * two sets of alternative parameters. Which parameters are actually used
 * depends on whether the current instruction is wide or not.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BytecodeStream {
    /**
     * The internal code buffer holding the bytecodes.
     */
    private byte[] buf;

    /**
     * The index of the current bytecode in the code array.
     */
    private int bci;

    /**
     * The index of the next bytecode in the code array.
     */
    private int nextBci;

    /**
     * The last bytecode read.
     */
    private int code;

    /**
     * Whether or not the current instruction is wide.
     */
    private boolean wide;

    /**
     * Constructs a new bytecode stream for the specified code array.
     *
     * @param  buf  the code buffer
     */
    public BytecodeStream(byte[] buf) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(buf != null, "code buffer must exist");
        }
        this.buf = buf;
        reset(0);
    }

    /**
     * Repositions this stream to the specified index.
     *
     * @param  start  index of the next bytecode to be read
     */
    public void reset(int start) {
        bci = -1;
        nextBci = start;
        code = Bytecodes._illegal;
    }

    /**
     * Moves the reading position to the next instruction and updates the state
     * of this stream.
     *
     * @return  whether or not more instructions can be read
     */
    public boolean next() {
        bci = nextBci;
        if (bci < buf.length) {
            int cur = byteAt(bci, false);
            nextBci = bci + getLength();
            wide = (cur == Bytecodes._wide);
            if (nextBci <= bci) {
                code = Bytecodes._illegal;
            } else if (!wide) {
                code = Bytecodes.getJavaCode(cur);
            } else if (bci + 1 < buf.length) {
                code = Bytecodes.getJavaCode(byteAt(bci + 1, false));
            } else {
                code = Bytecodes._illegal;
            }
            return true;
        } else {
            code = Bytecodes._illegal;
            return false;
        }
    }

    /**
     * Returns the index of the current instruction in the code array.
     *
     * @return  the current bytecode index
     */
    public int getBci() {
        return bci;
    }

    /**
     * Returns the index of the next instruction in the code array.
     *
     * @return  the next bytecode index
     */
    public int getNextBci() {
        return nextBci;
    }

    /**
     * Returns the operation code of the current instruction.
     *
     * @return  the last bytecode read
     */
    public int getBytecode() {
        return code;
    }

    /**
     * Returns the signed or unsigned byte at the specified position.
     *
     * @param   pos     index into the code array
     * @param   signed  whether the value is signed or unsigned
     * @return  the byte value at the specified index
     */
    private int byteAt(int pos, boolean signed) {
        return signed ? (int) buf[pos] : ((int) buf[pos] & 0xff);
    }

    /**
     * Returns the signed or unsigned 16-bit integer at the specified position.
     * The value is read in big-endian order, that is most significant byte
     * first.
     *
     * @param   pos     index into the code array
     * @param   signed  whether the value is signed or unsigned
     * @return  the short integer value at the specified index
     */
    private int shortAt(int pos, boolean signed) {
        return (byteAt(pos, signed) << 8) | byteAt(pos + 1, false);
    }

    /**
     * Returns the signed 32-bit integer at the specified position. The value is
     * read in big-endian order, that is most significant byte first.
     *
     * @param   pos  index into the code array
     * @return  the integer value at the specified index
     */
    private int intAt(int pos) {
        return (shortAt(pos, true) << 16) | shortAt(pos + 2, false);
    }

    /**
     * Returns an unsigned operand of the current instruction.
     *
     * @param   offset  position of the operand in the instruction
     * @param   size    size of the operand in bytes
     * @return  the unsigned operand
     */
    public int getUnsigned(int offset, int size) {
        switch (size) {
        case 1:
            return byteAt(bci + offset, false);
        case 2:
            return shortAt(bci + offset, false);
        default:
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Returns an unsigned operand of the current instruction. If the
     * instruction is wide the alternative parameters are used.
     *
     * @param   offset   position of the small operand
     * @param   size     size of the small operand
     * @param   woffset  position of the wide operand
     * @param   wsize    size of the wide operand
     * @return  the unsigned operand
     */
    public int getUnsigned(int offset, int size, int woffset, int wsize) {
        if (isWide()) {
            return getUnsigned(woffset, wsize);
        } else {
            return getUnsigned(offset, size);
        }
    }

    /**
     * Returns a signed operand of the current instruction.
     *
     * @param   offset  position of the operand in the instruction
     * @param   size    size of the operand in bytes
     * @return  the signed operand
     */
    public int getSigned(int offset, int size) {
        switch (size) {
        case 1:
            return byteAt(bci + offset, true);
        case 2:
            return shortAt(bci + offset, true);
        case 4:
            return intAt(bci + offset);
        default:
            Assert.shouldNotReachHere();
            return 0;
        }

    }

    /**
     * Returns a signed operand of the current instruction. If the instruction
     * is wide the alternative parameters are used.
     *
     * @param   offset   position of the small operand
     * @param   size     size of the small operand
     * @param   woffset  position of the wide operand
     * @param   wsize    size of the wide operand
     * @return  the signed operand
     */
    public int getSigned(int offset, int size, int woffset, int wsize) {
        if (isWide()) {
            return getSigned(woffset, wsize);
        } else {
            return getSigned(offset, size);
        }
    }

    /**
     * Returns the destination of a conditional or unconditional jump. The
     * result is the sum of the current bytecode index and the signed offset
     * stored in the instruction.
     *
     * @param   offset  position of the offset in the instruction
     * @param   far     whether or not it is a far jump
     * @return  the absolute bytecode index of the destination
     */
    public int getDestination(int offset, boolean far) {
        if (far) {
            return bci + intAt(bci + offset);
        } else {
            return bci + shortAt(bci + offset, true);
        }
    }

    /**
     * Returns whether or not the current instruction is wide.
     *
     * @return  whether or not the instruction is wide
     */
    public boolean isWide() {
        return wide;
    }

    /**
     * Returns the length of the instruction at the current position.
     *
     * @return  length of the instruction in bytes
     */
    public int getLength() {
        int aligned = (bci + 4) & ~0x03;
        int cur = byteAt(bci, false);
        switch (cur) {
        case Bytecodes._wide:
            return Bytecodes.getWideLen(byteAt(bci + 1, false));
        case Bytecodes._tableswitch:
            int count = intAt(aligned + 8) - intAt(aligned + 4) + 1;
            return aligned - bci + count * 4 + 12;
        case Bytecodes._lookupswitch:
            /* falls through */
        case Bytecodes._fast_binaryswitch:
            /* falls through */
        case Bytecodes._fast_linearswitch:
            int npairs = intAt(aligned + 4);
            return aligned - bci + npairs * 8 + 8;
        default:
            return Bytecodes.getLength(Bytecodes.getJavaCode(cur));
        }
    }

    /**
     * Returns the number of bytes in the code buffer of this stream.
     *
     * @return  size of the code buffer
     */
    public int size() {
        return buf.length;
    }
}
