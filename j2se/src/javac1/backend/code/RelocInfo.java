/*
 * @(#)RelocInfo.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;

/**
 * Stores relocation information and defines constants for the relocation types.
 * The relocation mechanism describes values in the instruction stream that must
 * be patched by the runtime system when objects or code move as a result of
 * garbage collection or code heap compaction, respectively.<p>
 *
 * The information is compressed into one integer value with 4 bits indicating
 * the relocation type and 11 bits encoding the scaled offset from the previous
 * relocation address. The offsets accumulate along the list of relocations to
 * encode the address that points to the first byte of the relevant instruction.
 * Since Intel processors define instructions that can sometimes contain more
 * than one relocatable constant, one extra format bit is provided to specify
 * which operand goes with a relocation.
 *
 * @see      CodeBuffer#relocate(int, int, int)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class RelocInfo {
    /**
     * The type constant used when no relocation should be generated.
     */
    public static final int NONE = 0;

    /**
     * The type constant for an embedded ordinary object pointer.
     */
    public static final int OOP_TYPE = 1;

    /**
     * The type constant for a virtual call site that includes an inline cache.
     */
    public static final int VIRTUAL_CALL_TYPE = 2;

    /**
     * The type constant for a virtual call that has been statically bound.
     */
    public static final int OPT_VIRTUAL_CALL_TYPE = 3;

    /**
     * The type constant for a static call.
     */
    public static final int STATIC_CALL_TYPE = 4;

    /**
     * The type constant for an extra stub for each static call type.
     */
    public static final int STATIC_STUB_TYPE = 5;

    /**
     * The type constant for a fixed subroutine in the runtime system.
     */
    public static final int RUNTIME_CALL_TYPE = 6;

    /**
     * The type constant for an absolute reference to an external segment.
     */
    public static final int EXTERNAL_WORD_TYPE = 7;

    /**
     * The type constant for an address within the same code space.
     */
    public static final int INTERNAL_WORD_TYPE = 8;

    /**
     * The type constant for an internal backward branch.
     */
    public static final int SAFEPOINT_TYPE = 9;

    /**
     * The type constant for a return instruction.
     */
    public static final int RETURN_TYPE = 10;

    /**
     * The type constant for a subroutine call.
     */
    public static final int JSR_TYPE = 11;

    /**
     * The type constant for a return from a subroutine.
     */
    public static final int JSR_RET_TYPE = 12;

    /**
     * The type constant for an initialization barrier or safepoint.
     */
    public static final int BREAKPOINT_TYPE = 13;

    /**
     * The tag for a relocation data prefix.
     */
    public static final int DATA_PREFIX_TAG = 14;

    /**
     * The factor that the offset value is scaled.
     */
    public static final int OFFSET_UNIT = 1;

    /**
     * The number of bits that specify which operand goes with a relocation.
     */
    private static final int FORMAT_WIDTH = 1;

    /**
     * Whether or not relocation information contains a format field.
     */
    public static final boolean HAS_FORMAT = FORMAT_WIDTH > 0;

    /**
     * The number of bits available for relocation information.
     */
    public static final int VALUE_WIDTH = 16;

    /**
     * The number of bits that indicate the relocation type.
     */
    private static final int TYPE_WIDTH = 4;

    /**
     * The number of bits left for offset and format.
     */
    private static final int NONTYPE_WIDTH = VALUE_WIDTH - TYPE_WIDTH;

    /**
     * The number of bits encoding the offset from the previous address.
     */
    private static final int OFFSET_WIDTH = NONTYPE_WIDTH - FORMAT_WIDTH;

    /**
     * The upper limit for offset values.
     */
    public static final int OFFSET_LIMIT = (1 << OFFSET_WIDTH) * OFFSET_UNIT;
    
    /**
     * The number of bits encoding an immediate value.
     */
    private static final int IMMEDIATE_WIDTH = NONTYPE_WIDTH - 1;
    
    /**
     * The tag for immediate values.
     */
    private static final int IMMEDIATE_TAG = 1 << IMMEDIATE_WIDTH;
    
    /**
     * The upper limit for immediate values.
     */
    public static final int IMMEDIATE_LIMIT = 1 << IMMEDIATE_WIDTH;
    
    /**
     * The upper limit for the data length.
     */
    private static final int DATALEN_LIMIT = IMMEDIATE_LIMIT;

    /**
     * The bit mask that selects the relocation type value.
     */
    private static final int TYPE_MASK = (1 << TYPE_WIDTH) - 1;

    /**
     * The bit mask that selects the format value.
     */
    private static final int FORMAT_MASK = (1 << FORMAT_WIDTH) - 1;

    /**
     * The bit mask that selects the offset value.
     */
    private static final int OFFSET_MASK = (1 << OFFSET_WIDTH) - 1;
    
    /**
     * The bit mask that selects the immediate value.
     */
    private static final int IMMEDIATE_MASK = (1 << IMMEDIATE_WIDTH) - 1;

    /**
     * The relocation information constant that serves as a filler.
     */
    public static final RelocInfo
            FILLER_RELOC_INFO = valueOf(OFFSET_LIMIT / OFFSET_UNIT - 1);

    /**
     * The integer value that encodes relocation type, offset and format.
     */
    private int value;
    
    /**
     * The relocation type specific data.
     */
    private short[] data;
    
    /**
     * Constructs a new relocation information object.
     *
     * @param  type  the relocation type
     */
    public RelocInfo(int type) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((type & ~TYPE_MASK) == 0, "wrong type");
            Assert.that(type != DATA_PREFIX_TAG, "cannot build a prefix this way");
        }
        value = type << NONTYPE_WIDTH;
        data = null;
    }
    
    /**
     * Constructs a new relocation information object.
     *
     * @param  type  the relocation type
     * @param  x0    one integer of relocation data
     */
    public RelocInfo(int type, int x0) {
        this(type);
        if (x0 == 0) {
            /* nothing to do */
        } else if ((x0 > 0) && (x0 < IMMEDIATE_LIMIT)) {
            immediateRelocInfo(x0);
        } else if (isShort(x0)) {
            prefixRelocInfo(new int[] {x0});
        } else {
            prefixRelocInfo(new int[] {hi(x0), x0});
        }
    }
    
    /**
     * Constructs a new relocation information object.
     *
     * @param  type  the relocation type
     * @param  x0    one integer of relocation data
     * @param  x1    another integer of relocation data
     */
    public RelocInfo(int type, int x0, int x1) {
        this(type);
        if ((x0 == 0) && (x1 == 0)) {
            /* nothing to do */
        } else if (isShort(x0) && (x1 == 0)) {
            prefixRelocInfo(new int[] {x0});
        } else if (isShort(x0) && isShort(x1)) {
            prefixRelocInfo(new int[] {x0, x1});
        } else if (isShort(x1)) {
            prefixRelocInfo(new int[] {hi(x0), x0, x1});
        } else {
            prefixRelocInfo(new int[] {hi(x0), x0, hi(x1), x1});
        }
    }
    
    /**
     * Returns the higher half word of the specified integer value.
     *
     * @param   x  the integer value
     * @return  higher half word of the value
     */
    private static int hi(int x) {
        return x >>> VALUE_WIDTH;
    }
    
    /**
     * Tests if the specified value can be represented by 16 bits.
     *
     * @param   x  value to be tested
     * @return  whether the value is short or not
     */
    private static boolean isShort(int x) {
        return x == (short) x;
    }
    
    /**
     * Initializes the relocation data with the specified values.
     *
     * @param  x  array of half words
     */
    private void prefixRelocInfo(int[] x) {
        int datalen = x.length;
        data = new short[datalen + 1];
        data[0] = (short) ((DATA_PREFIX_TAG  << NONTYPE_WIDTH) | datalen);
        for (int i = 0; i < datalen; i++) {
            data[i + 1] = (short) x[i];
        }
    }
    
    /**
     * Initializes the relocation data with the specified immediate value.
     *
     * @param  x  immediate value
     */
    private void immediateRelocInfo(int x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((x >= 0) && (x < IMMEDIATE_LIMIT), "immediate out of range");
        }
        data = new short[1];
        data[0] = (short) ((DATA_PREFIX_TAG << NONTYPE_WIDTH) | IMMEDIATE_TAG | x);
    }
    
    /**
     * Sets the relocation type.
     *
     * @param  type  the relocation type
     */
    public void setType(int type) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((type & ~TYPE_MASK) == 0, "wrong type");
        }
        value = (value & ~(TYPE_MASK << NONTYPE_WIDTH)) | (type << NONTYPE_WIDTH);
    }

    /**
     * Returns the relocation type.
     *
     * @return  the relocation type
     */
    public int getType() {
        return (value >>> NONTYPE_WIDTH) & TYPE_MASK;
    }

    /**
     * Sets the format that specifies which operand goes with the relocation.
     *
     * @param  format  the relocation format
     */
    public void setFormat(int format) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((format & ~FORMAT_MASK) == 0, "wrong format");
        }
        value = (value & ~(FORMAT_MASK << OFFSET_WIDTH)) | (format << OFFSET_WIDTH);
    }

    /**
     * Returns the format that specifies which operand goes with the relocation.
     *
     * @return  the relocation format
     */
    public int getFormat() {
        return (value >>> OFFSET_WIDTH) & FORMAT_MASK;
    }
        
    /**
     * Tests if this object represents a relocation data prefix.
     *
     * @return  whether or not this is a prefix
     */
    public boolean isPrefix() {
        return getType() == DATA_PREFIX_TAG;
    }

    /**
     * Sets the offset from the previous relocation address.
     *
     * @param  offset  offset from the previous address
     */
    public void setAddrOffset(int offset) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isPrefix(), "must have offset");
            Assert.that((offset >= 0) && (offset < OFFSET_LIMIT), "offset out of bounds");
            Assert.that((offset & (OFFSET_UNIT - 1)) == 0, "misaligned offset");
        }
        value = (value & ~OFFSET_MASK) | (offset / OFFSET_UNIT);
    }
    
    /**
     * Returns the offset from the previous relocation address.
     *
     * @return  offset from the previous address
     */
    public int getAddrOffset() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!isPrefix(), "must have offset");
        }
        return (value & OFFSET_MASK) * OFFSET_UNIT;
    }
    
    /**
     * Returns the value that encodes relocation type, offset and format.
     *
     * @return  value of this relocation information
     */
    public short getValue() {
        return (short) value;
    }
    
    /**
     * Returns the relocation type specific data.
     *
     * @return  the relocation data
     */
    public short[] getData() {
        return data;
    }
    
    /**
     * Returns the length of the relocation type specific data.
     *
     * @return  length of the relocation data
     */
    public int getDataLen() {
        return (data != null) ? data.length : 0;
    }
    
    /**
     * Returns the length of the relocation data prefix.
     *
     * @return  length of the data prefix
     */
    public int getPrefixLength() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isPrefix(), "must be data prefix");
        }
        if ((value & IMMEDIATE_TAG) != 0) {
            return 1;
        } else {
            return 1 + (value & IMMEDIATE_MASK);
        }
    }
    
    /**
     * Returns the relocation information object for the specified value.
     *
     * @param   value  value of the relocation
     * @return  the relocation information object
     */
    protected static RelocInfo valueOf(int value) {
        RelocInfo reloc = new RelocInfo(NONE);
        reloc.value = value;
        return reloc;
    }
}
