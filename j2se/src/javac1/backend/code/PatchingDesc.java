/*
 * @(#)PatchingDesc.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

/**
 * Describes a patching point. A patching point could be a move with an offset
 * or a simple move of a constant.
 *
 * @see      CodeEmitter#patchingEpilog(PatchingDesc, CodeEmitInfo, int)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class PatchingDesc {
    /**
     * The start position of the patching point.
     */
    private int start;

    /**
     * The code offset of the patching point.
     */
    private int offset;

    /**
     * The offset within the field depending on if the low or high word is
     * moved.
     */
    private int fieldOffset;

    /**
     * Constructs a new patching descriptor.
     */
    public PatchingDesc() {
        start = 0;
        offset = 0;
        fieldOffset = 0;
    }

    /**
     * Sets the start position and offset of the patching point.
     *
     * @param  start   the start position
     * @param  offset  the offset
     */
    public void set(int start, int offset) {
        this.start = start;
        this.offset = offset;
    }

    /**
     * Marks the move that moves the high word.
     */
    public void setHiWord() {
        fieldOffset = 1;
    }

    /**
     * Marks the move that moves the low word.
     */
    public void setLoWord() {
        fieldOffset = 0;
    }

    /**
     * Returns the start position of the patching point.
     *
     * @return  the start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the code offset of the patching point.
     *
     * @return  the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the offset within the field depending on if the low or high word
     * is moved.
     *
     * @return  the field offset
     */
    public int getFieldOffset() {
        return fieldOffset;
    }
}
