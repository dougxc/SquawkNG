/*
 * @(#)CodeBuffer.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javac1.Assert;
import javac1.backend.debug.DebugInfo;
import javac1.backend.debug.OffsetDesc;
import javac1.ci.Runtime1;

/**
 * Represents a buffer into which assembly code is generated. The code buffer
 * also stores information about instructions that must be patched when objects
 * or code move.
 *
 * @see      Assembler
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CodeBuffer {
    /**
     * The byte array that stores the assembly instructions.
     */
    private byte[] code;

    /**
     * The address of the first byte of the code buffer.
     */
    private int codeStart;

    /**
     * The number of bytes in the code buffer.
     */
    private int count;

    /**
     * The offset of the exception handler code.
     */
    private int exceptionOffset;

    /**
     * The address of the first byte of the call stubs.
     */
    private int stubsStart;

    /**
     * The address after the call stubs.
     */
    private int stubsEnd;
    
    /**
     * The array of object pointers in the code.
     */
    private LinkedList oops;

    /**
     * The list of relocation information objects.
     */
    private List relocs;
    
    /**
     * The size of the relocation information.
     */
    private int relocSize;

    /**
     * The last relocation offset from the start of the code buffer.
     */
    private int lastRelocOffset;
    
    /**
     * The list of debug information objects.
     */
    private List debugInfo;
    
    /**
     * The offset descriptor.
     */
    private OffsetDesc offsets;

    /**
     * Constructs an empty code buffer with the specified capacity.
     *
     * @param  codeStart  start address of the code
     * @param  codeSize   size of the code buffer
     */
    public CodeBuffer(int codeStart, int codeSize) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(codeStart > 0, "illegal start address");
        }
        this.code = new byte[codeSize];
        this.codeStart = codeStart;
        this.count = 0;
        this.oops = new LinkedList();
        this.relocs = new ArrayList();
        this.relocSize = 0;
        this.lastRelocOffset = 0;
        this.debugInfo = new ArrayList();
        this.offsets = new OffsetDesc();
        oops.add(null);
    }

    /**
     * Constructs an empty code buffer with an initial capacity of 4096 bytes.
     */
    public CodeBuffer() {
        this(1024, 4096);
    }

    /**
     * Returns the address of the first byte of the code buffer.
     *
     * @return  start address of the code
     */
    public int getCodeBegin() {
        return codeStart;
    }

    /**
     * Returns the current code generation position.
     *
     * @return  current code generation position
     */
    public int getCodeEnd() {
        return codeStart + count;
    }

    /**
     * Returns the number of bytes in the code buffer.
     *
     * @return  size of the code
     */
    public int getCodeSize() {
        return count;
    }

    /**
     * Returns the current capacity of the code buffer.
     *
     * @return  the current capacity
     */
    public int getCodeLimit() {
        return codeStart + code.length;
    }

    /**
     * Ensures that there is enough space in the code buffer.
     */
    public void checkCodespace() {
        if (code.length - count < 1024) {
            int capacity = code.length * 2 + 1024;
            byte[] newCode = new byte[capacity];
            System.arraycopy(code, 0, newCode, 0, count);
            code = newCode;
        }
    }

    /**
     * Sets the offset of the exception handler code.
     *
     * @param  offset  offset of exception handler
     */
    public void setExceptionOffset(int offset) {
        exceptionOffset = offset;
    }

    /**
     * Returns the offset of the exception handler code.
     *
     * @return  offset of exception handler
     */
    public int getExceptionOffset() {
        return exceptionOffset;
    }

    /**
     * Sets the address of the first byte of the call stubs.
     *
     * @param  address  start address of call stubs
     */
    public void setStubsBegin(int address) {
        stubsStart = address;
    }

    /**
     * Returns the address of the first byte of the call stubs.
     *
     * @return  start address of call stubs
     */
    public int getStubsBegin() {
        return stubsStart;
    }
    
    /**
     * Returns the size of the call stubs in the code buffer.
     *
     * @return  size of call stubs
     */
    public int getStubsSize() {
        return stubsEnd - stubsStart;
    }

    /**
     * Sets the address after the call stubs.
     *
     * @param  address  end address of call stubs
     */
    public void setStubsEnd(int address) {
        stubsEnd = address;
    }

    /**
     * Returns the address after the call stubs.
     *
     * @return  end address of call stubs
     */
    public int getStubsEnd() {
        return stubsEnd;
    }

    /**
     * Returns the array of bytes in this code buffer.
     *
     * @return  array of bytes in the buffer
     */
    public byte[] getBytes() {
        return code;
    }

    /**
     * Returns the array of recorded object pointers.
     *
     * @return  array of object pointers
     */
    public Object[] getOops() {
        int length = oops.size();
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            Runtime1.setOopAt(array, i, oops.get(i));
        }
        return array;
    }
    
    /**
     * Appends the specified byte to the end of the code buffer.
     *
     * @param  x  byte value to be appended
     */
    public void append(int x) {
        code[count++] = (byte) x;
    }

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte value at the specified index
     */
    public int byteAt(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((pos >= 0) && (pos < count), "index out of bounds");
        }
        return (int) code[pos] & 0xff;
    }

    /**
     * Sets the byte at the specified position to the new value.
     *
     * @param  pos  index of the byte to be replaced
     * @param  x    the new byte value
     */
    public void setByteAt(int pos, int x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((pos >= 0) && (pos < count), "index out of bounds");
        }
        code[pos] = (byte) x;
    }

    /**
     * Records the specified object pointer and returns its index.
     *
     * @param   oop  the object pointer
     * @return  index of the object pointer
     */
    public int recordOop(Object oop) {
        int index = 0;
        int length = oops.size();
        while (index < length) {
            if (oops.get(index) == oop) {
                return index;
            }
            index++;
        }
        if (index == length) {
            oops.add(oop);
        }
        return index;
    }
    
    /**
     * Generates relocation information for the specified instruction.
     *
     * @param  at      address of the relevant instruction
     * @param  reloc   the relocation information
     * @param  format  the relocation format
     * @see    RelocInfo
     */
    public void relocate(int at, RelocInfo reloc, int format) {
        int rtype = reloc.getType();
        if (rtype == RelocInfo.NONE) {
            return;
        } else if ((rtype == RelocInfo.OOP_TYPE) && (at == 0)) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((at >= getCodeBegin()) && (at <= getCodeEnd() + 1), "address outside code boundaries");
        }
        if (relocs == null) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that((rtype == RelocInfo.NONE)
                    || (rtype == RelocInfo.RUNTIME_CALL_TYPE)
                    || (rtype == RelocInfo.INTERNAL_WORD_TYPE)
                    || (rtype == RelocInfo.EXTERNAL_WORD_TYPE), "code needs relocation information");
            }
            return;
        }
        int len = at - codeStart;
        int offset = len - lastRelocOffset;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(offset >= 0, "relocation addresses must not decrease");
        }
        lastRelocOffset = len;
        while (offset >= RelocInfo.OFFSET_LIMIT) {
            relocs.add(RelocInfo.FILLER_RELOC_INFO);
            offset -= RelocInfo.FILLER_RELOC_INFO.getAddrOffset();
        }
        reloc.setAddrOffset(offset);
        relocs.add(reloc);
        if (RelocInfo.HAS_FORMAT) {
            if (format != 0) {
                reloc.setFormat(format);
            }
        } else if (Assert.ASSERTS_ENABLED) {
            Assert.that(format == 0, "bad format");
        }
    }

    /**
     * Generates relocation information for the specified instruction.
     *
     * @see  #relocate(int, RelocInfo, int)
     */
    public void relocate(int at, int rtype, int format) {
        relocate(at, new RelocInfo(rtype), format);
    }
    
    /**
     * Generates relocation information for the specified instruction.
     *
     * @see  #relocate(int, RelocInfo, int)
     */
    public void relocate(int at, int rtype) {
        relocate(at, new RelocInfo(rtype), 0);
    }

    /**
     * Changes the type of the relocation for the specified address.
     *
     * @param  pos      address of the relocation to be changed
     * @param  oldType  the old relocation type
     * @param  newType  the new relocation type
     */
    public void changeRelocInfoForAddress(int pos, int oldType, int newType) {
        int addr = getCodeBegin();
        RelocInfo reloc = null;
        ListIterator iterator = relocs.listIterator();
        while (iterator.hasNext() && ((addr < pos) || (reloc == null))) {
            reloc = (RelocInfo) iterator.next();
            addr += reloc.getAddrOffset();
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((reloc != null) && (addr == pos), "no relocation found for this address");
            Assert.that(reloc.getType() == oldType, "wrong relocation type found");
        }
        reloc.setType(newType);
    }
    
    /**
     * Returns the array of relocation information values.
     *
     * @return  the relocation information
     */
    public short[] getRelocInfo() {
        int len = relocs.size();
        ListIterator iter = relocs.listIterator();
        while (iter.hasNext()) {
            len += ((RelocInfo) iter.next()).getDataLen();
        }
        short[] result = new short[len];
        int i = 0;
        iter = relocs.listIterator();
        while (iter.hasNext()) {
            RelocInfo reloc = (RelocInfo) iter.next();
            short[] data = reloc.getData();
            if (data != null) {
                System.arraycopy(data, 0, result, i, data.length);
                i += data.length;
            }
            result[i++] = reloc.getValue();
        }
        return result;
    }

    /**
     * Returns the offset descriptor.
     *
     * @return  the offset descriptor
     */
    public OffsetDesc getOffsets() {
        return offsets;
    }
    
    /**
     * Stores the specified debug information in the code buffer.
     *
     * @param  info  the debug information
     */
    public void addDebugInfo(DebugInfo info) {
        debugInfo.add(info);
    }
    
    /**
     * Returns the bytecode index part of the debug information.
     *
     * @return  array of bytecode indices
     */
    public int[] getBcis() {
        int size = debugInfo.size();
        int[] bcis = new int[size];
        for (int i = 0; i < size; i++) {
            DebugInfo info = (DebugInfo) debugInfo.get(i);
            bcis[i] = info.getBci();
        }
        return bcis;
    }
    
    /**
     * Returns the code offset part of the debug information.
     *
     * @return  array of code offsets
     */
    public int[] getCodeOffsets() {
        int size = debugInfo.size();
        int codeOffsets[] = new int[size];
        for (int i = 0; i < size; i++) {
            DebugInfo info = (DebugInfo) debugInfo.get(i);
            codeOffsets[i] = info.getOffset();
        }
        return codeOffsets;
    }
    
    /**
     * Returns the at call part of the debug information.
     *
     * @return  array of boolean values
     */
    public boolean[] getAtCalls() {
        int size = debugInfo.size();
        boolean atCalls[] = new boolean[size];
        for (int i = 0; i < size; i++) {
            DebugInfo info = (DebugInfo) debugInfo.get(i);
            atCalls[i] = info.isAtCall();
        }
        return atCalls;
    }
    
    /**
     * Returns the frame size part of the debug information.
     *
     * @return  array of frame sizes
     */
    public int[] getFrameSizes() {
        int size = debugInfo.size();
        int frameSizes[] = new int[size];
        for (int i = 0; i < size; i++) {
            DebugInfo info = (DebugInfo) debugInfo.get(i);
            frameSizes[i] = info.getFrameSize();
        }
        return frameSizes;
    }
    
    /**
     * Returns the oop map part of the debug information.
     *
     * @return  arrays of pointer registers
     */
    public int[][] getOopRegs() {
        int size = debugInfo.size();
        int[][] oopRegs = new int[size][];
        for (int i = 0; i < size; i++) {
            DebugInfo info = (DebugInfo) debugInfo.get(i);
            oopRegs[i] = info.getOopRegs();
        }
        return oopRegs;
    }
}
