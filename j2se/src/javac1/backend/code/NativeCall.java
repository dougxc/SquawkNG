/*
 * @(#)NativeCall.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

/**
 * Abstraction for accessing and manipulating native call instructions.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NativeCall {
    /**
     * The instruction code for calling procedures. On Intel processors the
     * instruction code <tt>E8</tt> is used.
     */
    public static final int INSTRUCTION_CODE = 0xe8;

    /**
     * The total size of the call instruction in bytes. The constant value of
     * this field is <tt>5</tt>.
     */
    public static final int INSTRUCTION_SIZE = 5;

    /**
     * Inserts a native call instruction at the specified code position.
     *
     * @param  asm      assembler that provides access to the code
     * @param  codePos  code position to insert call instruction at
     * @param  entry    entry address of the procedure to be called
     */
    public static void insert(Assembler asm, int codePos, int entry) {
        int offset = codePos - asm.getCodeBegin();
        asm.setByteAt(offset, INSTRUCTION_CODE);
        asm.setLongAt(offset + 1, entry - (codePos + INSTRUCTION_SIZE));
    }
}
