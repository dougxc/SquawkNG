/*
 * @(#)PatchingStub.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.NativeCall;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.backend.debug.CodeEmitInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for patching an instruction. When code for this stub is emitted
 * the instruction at the specified offset is copied to an inlined buffer and
 * the original code is replaced by a call to the stub. The stub calls the
 * runtime, which preserves all registers, initializes the class, restores the
 * original code and then reexecutes the instruction.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class PatchingStub extends CodeStub {
    /**
     * The identification number for patching a field access.
     */
    public static final int ACCESS_FIELD_ID = 0;

    /**
     * The identification number for patching an unloaded class.
     */
    public static final int LOAD_KLASS_ID = 1;

    /**
     * The start position of the code to be copied.
     */
    private int start;

    /**
     * The register that points to the destination object.
     */
    private Register objReg;

    /**
     * The register that points to the source object.
     */
    private Register srcReg;

    /**
     * The number of bytes to be copied.
     */
    private int bytesToCopy;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The kind of patching.
     */
    private int id;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for patching an instruction.
     *
     * @param  start        start of the code to be copied
     * @param  objReg       points to the destination object
     * @param  srcReg       points to the source object
     * @param  bytesToCopy  the number of bytes to be copied
     * @param  info         associated debug information
     * @param  id           the kind of patching
     * @param  espOffset    offset of the stack pointer
     */
    public PatchingStub(int start, Register objReg, Register srcReg,
            int bytesToCopy, CodeEmitInfo info, int id, int espOffset) {
        this.start = start;
        this.objReg = objReg;
        this.srcReg = srcReg;
        this.bytesToCopy = bytesToCopy;
        this.info = info;
        this.id = id;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((bytesToCopy >= NativeCall.INSTRUCTION_SIZE) && (bytesToCopy <= 0xff), "not enough room for call");
        }
        int offset = start - ce.getMasm().getCodeBegin();
        for (int i = 0; i < bytesToCopy; i++) {
            int val = ce.getMasm().byteAt(offset + i);
            ce.getMasm().emitByte(val);
        }
        ce.getMasm().emitByte(bytesToCopy);
        int entry = ce.getMasm().getCodePos();
        NativeCall.insert(ce.getMasm(), start, entry);
        switch (id) {
        case ACCESS_FIELD_ID:
            ce.getMasm().jmp(Runtime1.getStubEntry(Runtime1.INIT_CHECK_PATCHING_ID),
                RelocInfo.RUNTIME_CALL_TYPE);
            break;
        case LOAD_KLASS_ID:
            ce.getMasm().jmp(Runtime1.getStubEntry(Runtime1.LOAD_KLASS_PATCHING_ID),
                RelocInfo.RUNTIME_CALL_TYPE);
            ce.getMasm().getCode().changeRelocInfoForAddress(
                start, RelocInfo.OOP_TYPE, RelocInfo.NONE);
            break;
        default:
            Assert.shouldNotReachHere();
        }
    }
}
